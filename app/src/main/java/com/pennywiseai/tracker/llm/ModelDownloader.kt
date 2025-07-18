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
import java.net.HttpURLConnection
import java.net.URL

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isComplete: Boolean = false,
    val error: String? = null
) {
    val progressPercentage: Int
        get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
}

class ModelDownloader(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelDownloader"
        
        // Gemma 2B LiteRT model download URL - hosted on S3
        private const val GEMMA_2B_URL = "https://hugginface-ai-model.s3.us-east-1.amazonaws.com/Gemma2-2B-IT_multi-prefill-seq_q8_ekv1280.task"
        private const val MODEL_FILENAME = "Gemma2-2B-IT_multi-prefill-seq_q8_ekv1280.task"
        // The new Gemma2-2B model is ~2.7GB
        private const val EXPECTED_SIZE = 2_700_000_000L // ~2.7GB for Gemma2-2B-IT
        private const val MIN_VALID_SIZE = 1_000_000_000L // 1GB minimum for any valid model
        
        // List of known model file names (for backward compatibility)
        private val KNOWN_MODEL_FILES = listOf(
            MODEL_FILENAME,
            "gemma-3n-e2b-it-int4.task", // Old model name
            "gemma_model.task" // Another possible old name
        )
    }
    
    fun downloadModel(): Flow<DownloadProgress> = flow {
        Log.i(TAG, "🚀 Starting Gemma 2B model download...")
        
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        
        // Check if model already exists
        if (modelFile.exists() && modelFile.length() > EXPECTED_SIZE * 0.9) {
            Log.i(TAG, "✅ Model already exists and appears complete")
            emit(DownloadProgress(modelFile.length(), modelFile.length(), true))
            return@flow
        }
        
        try {
            val url = URL(GEMMA_2B_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            // Add User-Agent header to avoid 403 errors
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
            
            // No authentication needed for public S3 URLs
            
            val totalBytes = connection.contentLengthLong
            
            connection.inputStream.use { input ->
                FileOutputStream(modelFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesDownloaded = 0L
                    var bytesRead: Int
                    var lastLogTime = System.currentTimeMillis()
                    var lastProgressTime = System.currentTimeMillis()
                    val startTime = System.currentTimeMillis()
                    
                    Log.i(TAG, "🚀 Starting download stream...")
                    emit(DownloadProgress(0, totalBytes))
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead
                        
                        val currentTime = System.currentTimeMillis()
                        val elapsedTime = currentTime - startTime
                        
                        // Log detailed progress every 5MB or every 30 seconds
                        if (bytesDownloaded % (5 * 1024 * 1024) == 0L || currentTime - lastLogTime > 30000) {
                            val progressPercent = (bytesDownloaded * 100 / totalBytes).toInt()
                            val downloadedMB = bytesDownloaded / (1024 * 1024)
                            val totalMB = totalBytes / (1024 * 1024)
                            val speedKBps = if (elapsedTime > 0) (bytesDownloaded / elapsedTime) else 0
                            
                            Log.i(TAG, "📥 Progress: ${downloadedMB}MB / ${totalMB}MB (${progressPercent}%) - Speed: ${speedKBps}KB/s")
                            lastLogTime = currentTime
                        }
                        
                        // Emit progress every 1MB or every 10 seconds
                        if (bytesDownloaded % (1024 * 1024) == 0L || currentTime - lastProgressTime > 10000) {
                            emit(DownloadProgress(bytesDownloaded, totalBytes))
                            lastProgressTime = currentTime
                        }
                        
                        // Log basic progress every 1MB
                        if (bytesDownloaded % (1024 * 1024) == 0L) {
                        }
                    }
                    
                    Log.i(TAG, "✅ Model download completed successfully - Total: ${bytesDownloaded / (1024 * 1024)}MB")
                    emit(DownloadProgress(bytesDownloaded, totalBytes, true))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Model download failed: ${e.message}")
            // Clean up partial download
            if (modelFile.exists()) {
                modelFile.delete()
            }
            emit(DownloadProgress(0, 0, true, "Download failed: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    fun getModelPath(): String? {
        // First try to use the new model if it's valid
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        if (modelFile.exists() && modelFile.length() >= EXPECTED_SIZE * 0.9) {
            return modelFile.absolutePath
        }
        
        // Fall back to old models if they exist (but they should be updated)
        for (oldFilename in KNOWN_MODEL_FILES.drop(1)) {
            val oldFile = File(context.filesDir, oldFilename)
            if (oldFile.exists() && oldFile.length() >= MIN_VALID_SIZE) {
                Log.w(TAG, "⚠️ Using old model: $oldFilename - update recommended")
                return oldFile.absolutePath
            }
        }
        
        Log.e(TAG, "❌ No valid model found")
        return null
    }
    
    fun isModelDownloaded(): Boolean {
        // Only check for the current model file - old models should be re-downloaded
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        val exists = modelFile.exists()
        val size = if (exists) modelFile.length() else 0L
        val expectedMinSize = (EXPECTED_SIZE * 0.9).toLong() // Allow 10% tolerance
        val isValid = exists && size >= expectedMinSize
        
        
        // Also check for old models and log warning
        for (oldFilename in KNOWN_MODEL_FILES.drop(1)) {
            val oldFile = File(context.filesDir, oldFilename)
            if (oldFile.exists()) {
                Log.w(TAG, "⚠️ Found old model file: $oldFilename (${oldFile.length() / (1024 * 1024)}MB) - needs update")
            }
        }
        
        if (!isValid) {
            Log.w(TAG, "❌ No valid model found - download required")
        }
        
        return isValid
    }
    
    fun getModelSize(): Long {
        // Check for any known model files
        for (filename in KNOWN_MODEL_FILES) {
            val modelFile = File(context.filesDir, filename)
            if (modelFile.exists()) {
                return modelFile.length()
            }
        }
        return 0L
    }
    
    fun deleteModel() {
        var deleted = false
        // Delete all known model files
        for (filename in KNOWN_MODEL_FILES) {
            val modelFile = File(context.filesDir, filename)
            if (modelFile.exists()) {
                modelFile.delete()
                deleted = true
            }
        }
        if (!deleted) {
            Log.w(TAG, "⚠️ No model files found to delete")
        }
    }
    
    fun getExpectedModelSize(): Long = EXPECTED_SIZE
}
