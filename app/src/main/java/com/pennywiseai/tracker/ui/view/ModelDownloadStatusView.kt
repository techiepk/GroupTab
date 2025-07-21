package com.pennywiseai.tracker.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.pennywiseai.tracker.databinding.ViewModelDownloadStatusBinding
import com.pennywiseai.tracker.llm.DownloadProgress
import com.pennywiseai.tracker.viewmodel.SettingsViewModel
import java.text.DecimalFormat

/**
 * Reusable view for displaying model download status
 */
class ModelDownloadStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val binding: ViewModelDownloadStatusBinding
    private var onDownloadClick: (() -> Unit)? = null
    private var onCancelClick: (() -> Unit)? = null
    private var onDeleteClick: (() -> Unit)? = null
    
    init {
        binding = ViewModelDownloadStatusBinding.inflate(LayoutInflater.from(context), this, true)
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        binding.downloadButton.setOnClickListener { onDownloadClick?.invoke() }
        binding.cancelButton.setOnClickListener { onCancelClick?.invoke() }
        binding.deleteButton.setOnClickListener { onDeleteClick?.invoke() }
    }
    
    fun setCallbacks(
        onDownload: () -> Unit,
        onCancel: () -> Unit,
        onDelete: () -> Unit
    ) {
        onDownloadClick = onDownload
        onCancelClick = onCancel
        onDeleteClick = onDelete
    }
    
    fun updateStatus(status: SettingsViewModel.ModelStatus, progress: DownloadProgress? = null) {
        when (status) {
            SettingsViewModel.ModelStatus.NOT_DOWNLOADED -> {
                binding.statusText.text = "AI Model not downloaded"
                binding.statusDescription.text = "Download the AI model to enable smart transaction extraction"
                binding.progressBar.isVisible = false
                binding.progressText.isVisible = false
                binding.downloadButton.isVisible = true
                binding.cancelButton.isVisible = false
                binding.deleteButton.isVisible = false
            }
            
            SettingsViewModel.ModelStatus.DOWNLOADING -> {
                binding.statusText.text = "Downloading AI Model"
                binding.statusDescription.text = "Please wait while the model downloads"
                binding.progressBar.isVisible = true
                binding.progressText.isVisible = true
                binding.speedText.isVisible = true
                binding.downloadButton.isVisible = false
                binding.cancelButton.isVisible = true
                binding.deleteButton.isVisible = false
                
                updateProgress(progress)
            }
            
            SettingsViewModel.ModelStatus.DOWNLOADED -> {
                binding.statusText.text = "AI Model ready"
                binding.statusDescription.text = "Smart transaction extraction is enabled"
                binding.progressBar.isVisible = false
                binding.progressText.isVisible = false
                binding.downloadButton.isVisible = false
                binding.cancelButton.isVisible = false
                binding.deleteButton.isVisible = true
            }
            
            SettingsViewModel.ModelStatus.ERROR -> {
                val hasPartialDownload = progress?.let { it.bytesDownloaded > 0 } ?: false
                binding.statusText.text = "Download failed"
                binding.statusDescription.text = if (hasPartialDownload) {
                    "Download interrupted. Tap to resume."
                } else {
                    "Failed to download model. Please try again."
                }
                binding.progressBar.isVisible = false
                binding.progressText.isVisible = hasPartialDownload
                binding.downloadButton.isVisible = true
                binding.downloadButton.text = if (hasPartialDownload) "Resume Download" else "Retry Download"
                binding.cancelButton.isVisible = false
                binding.deleteButton.isVisible = hasPartialDownload
                
                if (hasPartialDownload) {
                    updateProgress(progress)
                }
            }
            
            SettingsViewModel.ModelStatus.UNKNOWN -> {
                binding.statusText.text = "Checking model status..."
                binding.statusDescription.text = ""
                binding.progressBar.isVisible = true
                binding.progressBar.isIndeterminate = true
                binding.progressText.isVisible = false
                binding.downloadButton.isVisible = false
                binding.cancelButton.isVisible = false
                binding.deleteButton.isVisible = false
            }
        }
    }
    
    private fun updateProgress(progress: DownloadProgress?) {
        progress?.let {
            val downloadedMB = it.bytesDownloaded / (1024.0 * 1024.0)
            val totalMB = it.totalBytes / (1024.0 * 1024.0)
            val percentComplete = it.progressPercentage
            
            val df = DecimalFormat("#.#")
            binding.progressText.text = "${df.format(downloadedMB)} MB / ${df.format(totalMB)} MB ($percentComplete%)"
            
            // Update speed text
            if (it.speedBytesPerSecond > 0) {
                val speedText = when {
                    it.speedMBps >= 1.0 -> "${df.format(it.speedMBps)} MB/s"
                    else -> "${it.speedKBps} KB/s"
                }
                
                val remainingTime = formatRemainingTime(it.remainingTimeSeconds)
                binding.speedText.text = "$speedText • $remainingTime remaining"
                binding.speedText.isVisible = true
            } else {
                binding.speedText.isVisible = false
            }
            
            binding.progressBar.isIndeterminate = false
            binding.progressBar.max = 100
            binding.progressBar.progress = percentComplete
        }
    }
    
    private fun formatRemainingTime(seconds: Long): String {
        return when {
            seconds <= 0 -> "calculating..."
            seconds < 60 -> "$seconds seconds"
            seconds < 3600 -> "${seconds / 60} minutes"
            else -> "${seconds / 3600} hours ${(seconds % 3600) / 60} minutes"
        }
    }
}
