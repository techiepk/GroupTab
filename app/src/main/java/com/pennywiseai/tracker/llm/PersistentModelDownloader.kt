package com.pennywiseai.tracker.llm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/**
 * Download progress data class
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isComplete: Boolean = false,
    val error: String? = null,
    val speedBytesPerSecond: Long = 0
) {
    val progressPercentage: Int
        get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
        
    val speedKBps: Long
        get() = speedBytesPerSecond / 1024
        
    val speedMBps: Double
        get() = speedBytesPerSecond / (1024.0 * 1024.0)
        
    val remainingTimeSeconds: Long
        get() = if (speedBytesPerSecond > 0 && totalBytes > bytesDownloaded) {
            (totalBytes - bytesDownloaded) / speedBytesPerSecond
        } else 0L
}

/**
 * Enhanced model downloader with persistence and resume capability
 */
class PersistentModelDownloader(private val context: Context) {
    
    companion object {
        @Volatile
        private var isDownloading = false
        private const val TAG = "PersistentModelDownloader"
        private const val GEMMA_2B_URL = "https://d3q489kjw0f759.cloudfront.net/Gemma2-2B-IT_multi-prefill-seq_q8_ekv1280.task"
        private const val MODEL_FILENAME = "Gemma2-2B-IT_multi-prefill-seq_q8_ekv1280.task"
        private const val TEMP_SUFFIX = ".download"
        private const val EXPECTED_SIZE = 2_700_000_000L // ~2.7GB
        private const val CHUNK_SIZE = 8192
        private const val PROGRESS_UPDATE_THRESHOLD = 1024 * 1024 // Update every 1MB
        private const val PREF_NAME = "model_download_prefs"
        private const val PREF_DOWNLOAD_ID = "download_id"
        private const val PREF_TOTAL_SIZE = "total_size"
        private const val PREF_DOWNLOADED_SIZE = "downloaded_size"
        private const val PREF_DOWNLOAD_URL = "download_url"
    }
    
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    /**
     * Download or resume model download with automatic retry and persistence
     */
    fun downloadModel(): Flow<DownloadProgress> = flow {
        Log.i(TAG, "🚀 Starting persistent model download...")
        
        // Prevent concurrent downloads
        if (isDownloading) {
            Log.w(TAG, "⚠️ Download already in progress")
            emit(DownloadProgress(0, 0, true, "Download already in progress"))
            return@flow
        }
        
        isDownloading = true
        
        try {
            // Check storage space first
            val availableSpace = getAvailableStorageSpace()
            if (availableSpace < EXPECTED_SIZE * 1.1) { // Need 10% extra space
                emit(DownloadProgress(0, 0, true, "Insufficient storage space. Need ${EXPECTED_SIZE / (1024 * 1024 * 1024)}GB free."))
                return@flow
            }
        
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        val tempFile = File(context.filesDir, "$MODEL_FILENAME$TEMP_SUFFIX")
        
        // Check if model already exists and is valid
        if (modelFile.exists() && modelFile.length() >= EXPECTED_SIZE * 0.9) {
            Log.i(TAG, "✅ Model already exists and is valid")
            emit(DownloadProgress(modelFile.length(), modelFile.length(), true))
            clearDownloadState()
            return@flow
        }
        
        // Check for incomplete download
        val downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L
        val savedUrl = prefs.getString(PREF_DOWNLOAD_URL, null)
        val savedTotalSize = prefs.getLong(PREF_TOTAL_SIZE, 0L)
        
        // Validate saved state
        if (savedUrl != GEMMA_2B_URL || savedTotalSize == 0L) {
            // Different URL or no saved state, start fresh
            tempFile.delete()
            clearDownloadState()
        }
        
            var connection: HttpURLConnection? = null
            try {
                val startBytes = if (tempFile.exists()) tempFile.length() else 0L
            Log.i(TAG, "📥 Starting download from byte: $startBytes")
            
            val url = URL(GEMMA_2B_URL)
            connection = url.openConnection() as HttpURLConnection
            
            // Configure connection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
            
            // Set range header for resume
            if (startBytes > 0) {
                connection.setRequestProperty("Range", "bytes=$startBytes-")
                Log.i(TAG, "📥 Resuming download from byte: $startBytes")
            }
            
            connection.connect()
            
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                throw Exception("Server returned HTTP $responseCode")
            }
            
            // Get total file size
            val totalBytes = if (startBytes > 0) {
                startBytes + connection.contentLengthLong
            } else {
                connection.contentLengthLong
            }
            
            // Save download state
            saveDownloadState(GEMMA_2B_URL, totalBytes, startBytes)
            
            // Start or resume download
            connection.inputStream.use { input ->
                RandomAccessFile(tempFile, "rw").use { output ->
                    output.seek(startBytes)
                    
                    val buffer = ByteArray(CHUNK_SIZE)
                    var bytesDownloaded = startBytes
                    var bytesRead: Int
                    var lastProgressUpdate = 0L
                    val downloadStartTime = System.currentTimeMillis()
                    var lastSpeedCalculationTime = downloadStartTime
                    var lastSpeedCalculationBytes = startBytes
                    
                    emit(DownloadProgress(bytesDownloaded, totalBytes, speedBytesPerSecond = 0))
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        
                        // Update saved progress
                        if (bytesDownloaded - lastProgressUpdate >= PROGRESS_UPDATE_THRESHOLD) {
                            saveDownloadProgress(bytesDownloaded)
                            
                            // Calculate download speed
                            val currentTime = System.currentTimeMillis()
                            val timeDiff = currentTime - lastSpeedCalculationTime
                            val bytesDiff = bytesDownloaded - lastSpeedCalculationBytes
                            
                            val speedBytesPerSecond = if (timeDiff > 0) {
                                (bytesDiff * 1000) / timeDiff
                            } else 0L
                            
                            lastSpeedCalculationTime = currentTime
                            lastSpeedCalculationBytes = bytesDownloaded
                            
                            val progress = DownloadProgress(
                                bytesDownloaded, 
                                totalBytes, 
                                speedBytesPerSecond = speedBytesPerSecond
                            )
                            emit(progress)
                            
                            // Log progress
                            val progressPercent = (bytesDownloaded * 100 / totalBytes).toInt()
                            val downloadedMB = bytesDownloaded / (1024 * 1024)
                            val totalMB = totalBytes / (1024 * 1024)
                            val elapsedTime = System.currentTimeMillis() - downloadStartTime
                            val avgSpeedKBps = if (elapsedTime > 0) {
                                ((bytesDownloaded - startBytes) / 1024) / (elapsedTime / 1000).coerceAtLeast(1)
                            } else 0
                            
                            Log.d(TAG, "📥 Progress: ${downloadedMB}MB / ${totalMB}MB ($progressPercent%) - Avg Speed: ${avgSpeedKBps}KB/s")
                            lastProgressUpdate = bytesDownloaded
                        }
                    }
                    
                    // Download completed - rename temp file to final file
                    output.close()
                    
                    if (tempFile.renameTo(modelFile)) {
                        Log.i(TAG, "✅ Model download completed successfully")
                        clearDownloadState()
                        emit(DownloadProgress(bytesDownloaded, totalBytes, true))
                    } else {
                        throw Exception("Failed to rename temporary file")
                    }
                }
            }
            
            } finally {
                connection?.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Download failed: ${e.message}", e)
            
            // Don't delete temp file on failure - can resume later
            val tempFile = File(context.filesDir, "$MODEL_FILENAME$TEMP_SUFFIX")
            val savedTotalSize = prefs.getLong(PREF_TOTAL_SIZE, 0L)
            emit(DownloadProgress(
                tempFile.length(),
                savedTotalSize,
                true,
                "Download failed: ${e.message}"
            ))
        } finally {
            isDownloading = false
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get current download state
     */
    fun getDownloadState(): DownloadState {
        val downloadedSize = prefs.getLong(PREF_DOWNLOADED_SIZE, 0L)
        val totalSize = prefs.getLong(PREF_TOTAL_SIZE, 0L)
        val url = prefs.getString(PREF_DOWNLOAD_URL, null)
        
        val tempFile = File(context.filesDir, "$MODEL_FILENAME$TEMP_SUFFIX")
        val actualDownloaded = if (tempFile.exists()) tempFile.length() else 0L
        
        return DownloadState(
            isDownloading = false,
            downloadedBytes = actualDownloaded,
            totalBytes = totalSize,
            url = url,
            canResume = tempFile.exists() && actualDownloaded > 0
        )
    }
    
    
    /**
     * Cancel and clean up download
     */
    fun cancelDownload() {
        val tempFile = File(context.filesDir, "$MODEL_FILENAME$TEMP_SUFFIX")
        if (tempFile.exists()) {
            tempFile.delete()
        }
        clearDownloadState()
        isDownloading = false
        Log.i(TAG, "❌ Download cancelled and cleaned up")
    }
    
    /**
     * Check if model is fully downloaded
     */
    fun isModelDownloaded(): Boolean {
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        return modelFile.exists() && modelFile.length() >= EXPECTED_SIZE * 0.9
    }
    
    /**
     * Get model file path if downloaded
     */
    fun getModelPath(): String? {
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        return if (isModelDownloaded()) modelFile.absolutePath else null
    }
    
    /**
     * Get expected model size
     */
    fun getExpectedModelSize(): Long = EXPECTED_SIZE
    
    /**
     * Get current model size if exists
     */
    fun getModelSize(): Long {
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        return if (modelFile.exists()) modelFile.length() else 0L
    }
    
    /**
     * Delete downloaded model
     */
    fun deleteModel() {
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        val tempFile = File(context.filesDir, "$MODEL_FILENAME$TEMP_SUFFIX")
        
        modelFile.delete()
        tempFile.delete()
        clearDownloadState()
        
        Log.i(TAG, "🗑️ Model deleted")
    }
    
    private fun saveDownloadState(url: String, totalSize: Long, downloadedSize: Long) {
        prefs.edit().apply {
            putString(PREF_DOWNLOAD_URL, url)
            putLong(PREF_TOTAL_SIZE, totalSize)
            putLong(PREF_DOWNLOADED_SIZE, downloadedSize)
            putString(PREF_DOWNLOAD_ID, System.currentTimeMillis().toString())
            apply()
        }
    }
    
    private fun saveDownloadProgress(downloadedSize: Long) {
        prefs.edit().putLong(PREF_DOWNLOADED_SIZE, downloadedSize).apply()
    }
    
    private fun clearDownloadState() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Get available storage space in bytes
     */
    private fun getAvailableStorageSpace(): Long {
        return try {
            val statFs = android.os.StatFs(context.filesDir.path)
            statFs.availableBytes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check storage space", e)
            0L
        }
    }
}

/**
 * Represents the current state of a download
 */
data class DownloadState(
    val isDownloading: Boolean,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val url: String?,
    val canResume: Boolean
)
