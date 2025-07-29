package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.AppSettings
import com.pennywiseai.tracker.data.ScanProgress
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.sms.HistoricalSmsScanner
import com.pennywiseai.tracker.llm.PersistentModelDownloader
import com.pennywiseai.tracker.llm.DownloadProgress
import com.pennywiseai.tracker.llm.TransactionClassifier
import com.pennywiseai.tracker.background.ModelDownloadWorker
import androidx.work.WorkManager
import androidx.work.WorkInfo
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import android.content.Context
import androidx.work.await

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = TransactionRepository(AppDatabase.getDatabase(application))
    private val smsScanner = HistoricalSmsScanner(application, repository)
    private val modelDownloader = PersistentModelDownloader(application)
    private val transactionClassifier = TransactionClassifier(application)
    
    val settings: LiveData<AppSettings?> = repository.getSettings().asLiveData()
    
    private val _scanProgress = MutableLiveData<ScanProgress?>()
    val scanProgress: LiveData<ScanProgress?> = _scanProgress
    
    private val _isPatternMode = MutableLiveData(false)
    val isPatternMode: LiveData<Boolean> = _isPatternMode
    
    // Model download status and progress tracking
    private val _modelDownloadProgress = MutableLiveData<DownloadProgress?>()
    val modelDownloadProgress: LiveData<DownloadProgress?> = _modelDownloadProgress
    
    private val _modelStatus = MutableLiveData<ModelStatus>()
    val modelStatus: LiveData<ModelStatus> = _modelStatus
    
    enum class ModelStatus {
        NOT_DOWNLOADED,
        DOWNLOADING,
        DOWNLOADED,
        ERROR,
        UNKNOWN
    }
    
    companion object {
        private const val PREF_EXTRACTION_MODE = "use_pattern_parser"
    }
    
    init {
        // Load saved extraction mode
        val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
        _isPatternMode.value = prefs.getBoolean(PREF_EXTRACTION_MODE, false)
        
        // Initialize model status
        updateModelStatus()
        
        // Monitor WorkManager for background downloads
        monitorModelDownloadWork()
        
        // Check if background download is already running
        checkBackgroundDownloadStatus()
    }
    
    suspend fun updateScanTimeframe(daysBack: Int) {
        val currentSettings = repository.getSettingsSync()
        if (currentSettings != null) {
            val updatedSettings = currentSettings.copy(historicalScanDays = daysBack)
            repository.updateSettings(updatedSettings)
        } else {
            val newSettings = AppSettings(historicalScanDays = daysBack)
            repository.insertSettings(newSettings)
        }
    }
    
    suspend fun rescanMessages() {
        val settings = repository.getSettingsSync()
        val daysBack = settings?.historicalScanDays ?: 30
        
        viewModelScope.launch {
            smsScanner.rescanWithNewTimeframe(daysBack).collect { progress ->
                _scanProgress.postValue(progress)
                
                if (progress.isComplete) {
                    // Update last scan timestamp
                    settings?.let {
                        repository.updateSettings(
                            it.copy(lastScanTimestamp = System.currentTimeMillis())
                        )
                    }
                    
                    // Clear progress after delay
                    kotlinx.coroutines.delay(2000)
                    _scanProgress.postValue(null)
                }
            }
        }
    }
    
    suspend fun clearDatabase() {
        repository.clearAllData()
    }
    
    fun setExtractionMode(usePatternMode: Boolean) {
        viewModelScope.launch {
            _isPatternMode.value = true  // Always use pattern mode
            
            // Save preference - always pattern mode
            val prefs = getApplication<Application>().getSharedPreferences("settings", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_EXTRACTION_MODE, true).apply()
            
            // Initialize pattern-based extractor
            smsScanner.initializeExtractor()
        }
    }
    
    // Model management functions
    fun updateModelStatus() {
        viewModelScope.launch {
            try {
                // Check for any old model files that might exist
                val filesDir = getApplication<Application>().filesDir
                val files = filesDir.listFiles() ?: emptyArray()
                val taskFiles = files.filter { it.name.endsWith(".task") }
                
                taskFiles.forEach { file ->
                }
                
                val isDownloaded = transactionClassifier.isModelDownloaded()
                _modelStatus.value = if (isDownloaded) ModelStatus.DOWNLOADED else ModelStatus.NOT_DOWNLOADED
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "❌ Error checking model status: ${e.message}")
                _modelStatus.value = ModelStatus.ERROR
            }
        }
    }
    
    private fun monitorModelDownloadWork() {
        viewModelScope.launch {
            WorkManager.getInstance(getApplication()).getWorkInfosForUniqueWorkLiveData("model_download_work")
                .observeForever { workInfos ->
                    val workInfo = workInfos?.firstOrNull()
                    workInfo?.let { info ->
                        when (info.state) {
                            WorkInfo.State.RUNNING -> {
                                _modelStatus.value = ModelStatus.DOWNLOADING
                                
                                // Extract progress data from WorkManager
                                val progress = info.progress
                                val bytesDownloaded = progress.getLong("bytesDownloaded", 0)
                                val totalBytes = progress.getLong("totalBytes", 0)
                                val speedBytesPerSecond = progress.getLong("speedBytesPerSecond", 0)
                                val error = progress.getString("error")?.takeIf { it.isNotEmpty() }
                                
                                if (totalBytes > 0) {
                                    _modelDownloadProgress.value = DownloadProgress(
                                        bytesDownloaded = bytesDownloaded,
                                        totalBytes = totalBytes,
                                        speedBytesPerSecond = speedBytesPerSecond,
                                        error = error
                                    )
                                }
                            }
                            WorkInfo.State.SUCCEEDED -> {
                                _modelStatus.value = ModelStatus.DOWNLOADED
                                _modelDownloadProgress.value = null
                                android.util.Log.i("SettingsViewModel", "✅ Model download completed")
                            }
                            WorkInfo.State.FAILED -> {
                                _modelStatus.value = ModelStatus.ERROR
                                _modelDownloadProgress.value = null
                                android.util.Log.e("SettingsViewModel", "❌ Model download failed")
                            }
                            else -> {
                                updateModelStatus()
                            }
                        }
                    }
                }
        }
    }
    
    fun startModelDownload() {
        viewModelScope.launch {
            try {
                _modelStatus.value = ModelStatus.DOWNLOADING
                _modelDownloadProgress.value = DownloadProgress(0, 0, false)
                
                android.util.Log.i("SettingsViewModel", "📥 Starting foreground model download...")
                
                modelDownloader.downloadModel().collect { progress ->
                    _modelDownloadProgress.value = progress
                    
                    if (progress.isComplete) {
                        if (progress.error != null) {
                            _modelStatus.value = ModelStatus.ERROR
                            android.util.Log.e("SettingsViewModel", "❌ Model download failed: ${progress.error}")
                        } else {
                            _modelStatus.value = ModelStatus.DOWNLOADED
                            android.util.Log.i("SettingsViewModel", "✅ Model download completed successfully")
                        }
                    }
                }
            } catch (e: Exception) {
                _modelStatus.value = ModelStatus.ERROR
                _modelDownloadProgress.value = null
                android.util.Log.e("SettingsViewModel", "❌ Error starting model download: ${e.message}")
            }
        }
    }
    
    fun startBackgroundModelDownload() {
        ModelDownloadWorker.enqueue(getApplication())
        _modelStatus.value = ModelStatus.DOWNLOADING
        android.util.Log.i("SettingsViewModel", "📥 Background model download enqueued")
    }
    
    
    private fun checkBackgroundDownloadStatus() {
        viewModelScope.launch {
            val workManager = WorkManager.getInstance(getApplication())
            @Suppress("RestrictedApi")
            val workInfos = workManager.getWorkInfosForUniqueWork("model_download_work").await()
            
            if (workInfos.isNotEmpty()) {
                val workInfo = workInfos[0]
                when (workInfo.state) {
                    WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                        _modelStatus.value = ModelStatus.DOWNLOADING
                        android.util.Log.i("SettingsViewModel", "📥 Background download already in progress")
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        _modelStatus.value = ModelStatus.DOWNLOADED
                        android.util.Log.i("SettingsViewModel", "✅ Background download completed")
                    }
                    WorkInfo.State.FAILED -> {
                        _modelStatus.value = ModelStatus.ERROR
                        android.util.Log.e("SettingsViewModel", "❌ Background download failed")
                    }
                    else -> {
                        // No active download
                    }
                }
            }
        }
    }
    
    fun cancelModelDownload() {
        ModelDownloadWorker.cancel(getApplication())
        _modelDownloadProgress.value = null
        updateModelStatus()
        android.util.Log.i("SettingsViewModel", "❌ Model download cancelled")
    }
    
    fun deleteModel() {
        viewModelScope.launch {
            try {
                transactionClassifier.deleteModel()
                _modelStatus.value = ModelStatus.NOT_DOWNLOADED
                _modelDownloadProgress.value = null
                android.util.Log.i("SettingsViewModel", "🗑️ Model deleted successfully")
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "❌ Error deleting model: ${e.message}")
            }
        }
    }
    
    fun getModelSize(): Long {
        return try {
            transactionClassifier.getModelSize()
        } catch (e: Exception) {
            0L
        }
    }
    
    fun getExpectedModelSize(): Long {
        return modelDownloader.getExpectedModelSize()
    }
}