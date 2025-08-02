package com.pennywiseai.tracker.ui.screens.settings

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.tracker.data.repository.ModelRepository
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modelRepository: ModelRepository
) : ViewModel() {
    
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    
    // Download state
    private val _downloadState = MutableStateFlow(DownloadState.NOT_DOWNLOADED)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()
    
    private val _downloadedMB = MutableStateFlow(0L)
    val downloadedMB: StateFlow<Long> = _downloadedMB.asStateFlow()
    
    private val _totalMB = MutableStateFlow(0L)
    val totalMB: StateFlow<Long> = _totalMB.asStateFlow()
    
    private var currentDownloadId: Long? = null
    
    init {
        checkIfModelDownloaded()
        // Also sync with model repository
        modelRepository.checkModelState()
    }
    
    private fun checkIfModelDownloaded() {
        val modelFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), Constants.ModelDownload.MODEL_FILE_NAME)
        Log.d("SettingsViewModel", "Checking model file at: ${modelFile.absolutePath}")
        Log.d("SettingsViewModel", "Model file exists: ${modelFile.exists()}, size: ${modelFile.length()}")
        
        if (modelFile.exists() && modelFile.length() > 0) {
            _downloadState.value = DownloadState.COMPLETED
            _totalMB.value = modelFile.length() / (1024 * 1024)
            _downloadedMB.value = _totalMB.value
            _downloadProgress.value = 100
            // Update model repository state
            Log.d("SettingsViewModel", "Model found, updating repository state to READY")
            modelRepository.updateModelState(com.pennywiseai.tracker.data.repository.ModelState.READY)
        } else {
            Log.d("SettingsViewModel", "Model not found or empty")
        }
    }
    
    fun startModelDownload() {
        // Check storage space
        val availableSpace = context.filesDir.usableSpace
        if (availableSpace < Constants.ModelDownload.REQUIRED_SPACE_BYTES) {
            _downloadState.value = DownloadState.ERROR_INSUFFICIENT_SPACE
            return
        }
        
        // Create download request
        val request = DownloadManager.Request(Uri.parse(Constants.ModelDownload.MODEL_URL))
            .setTitle("Gemma 2B Chat Model")
            .setDescription("Downloading AI chat assistant for PennyWise")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, Constants.ModelDownload.MODEL_FILE_NAME)
            .setAllowedOverMetered(false) // Wi-Fi only by default
            .setAllowedOverRoaming(false)
        
        currentDownloadId = downloadManager.enqueue(request)
        _downloadState.value = DownloadState.DOWNLOADING
        
        // Start monitoring progress
        monitorDownload(currentDownloadId!!)
    }
    
    private fun monitorDownload(downloadId: Long) {
        viewModelScope.launch {
            while (isActive && _downloadState.value == DownloadState.DOWNLOADING) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                
                if (cursor != null && cursor.moveToFirst()) {
                    val bytesColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalBytesColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    
                    if (bytesColumnIndex != -1 && totalBytesColumnIndex != -1) {
                        val bytesDownloaded = cursor.getLong(bytesColumnIndex)
                        val bytesTotal = cursor.getLong(totalBytesColumnIndex)
                        
                        // Calculate progress
                        val progress = if (bytesTotal > 0) {
                            (bytesDownloaded * 100 / bytesTotal).toInt()
                        } else 0
                        
                        _downloadProgress.value = progress
                        _downloadedMB.value = bytesDownloaded / (1024 * 1024)
                        _totalMB.value = bytesTotal / (1024 * 1024)
                    }
                    
                    // Check status
                    if (statusColumnIndex != -1) {
                        when (cursor.getInt(statusColumnIndex)) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                _downloadState.value = DownloadState.COMPLETED
                                _downloadProgress.value = 100
                                // Update model repository state
                                modelRepository.updateModelState(com.pennywiseai.tracker.data.repository.ModelState.READY)
                            }
                            DownloadManager.STATUS_FAILED -> {
                                _downloadState.value = DownloadState.FAILED
                            }
                            DownloadManager.STATUS_PAUSED -> {
                                _downloadState.value = DownloadState.PAUSED
                            }
                        }
                    }
                }
                cursor?.close()
                delay(1000) // Update every second
            }
        }
    }
    
    fun pauseDownload() {
        // Note: DownloadManager doesn't support pause/resume directly
        // We'll cancel and allow re-download from where it left off
        currentDownloadId?.let {
            downloadManager.remove(it)
            _downloadState.value = DownloadState.PAUSED
        }
    }
    
    fun cancelDownload() {
        currentDownloadId?.let {
            downloadManager.remove(it)
            _downloadState.value = DownloadState.NOT_DOWNLOADED
            _downloadProgress.value = 0
            _downloadedMB.value = 0
            _totalMB.value = 0
            
            // Delete partial file
            val modelFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), Constants.ModelDownload.MODEL_FILE_NAME)
            if (modelFile.exists()) {
                modelFile.delete()
            }
        }
    }
    
    fun deleteModel() {
        val modelFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), Constants.ModelDownload.MODEL_FILE_NAME)
        if (modelFile.exists()) {
            modelFile.delete()
            _downloadState.value = DownloadState.NOT_DOWNLOADED
            _downloadProgress.value = 0
            _downloadedMB.value = 0
            _totalMB.value = 0
            // Update model repository state
            modelRepository.updateModelState(com.pennywiseai.tracker.data.repository.ModelState.NOT_DOWNLOADED)
        }
    }
}

enum class DownloadState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    ERROR_INSUFFICIENT_SPACE
}