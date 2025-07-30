package com.pennywiseai.tracker.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.databinding.DialogModelDownloadBinding
import com.pennywiseai.tracker.llm.PersistentModelDownloader
import com.pennywiseai.tracker.llm.DownloadProgress
import com.pennywiseai.tracker.background.ModelDownloadWorker
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.text.DecimalFormat
import android.widget.Toast
import androidx.work.WorkManager
import androidx.work.WorkInfo
import android.util.Log
import androidx.work.OneTimeWorkRequest

class ModelDownloadDialog : DialogFragment() {
    
    private var _binding: DialogModelDownloadBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var modelDownloader: PersistentModelDownloader
    private var onDownloadComplete: (() -> Unit)? = null
    private var isDownloading = false
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Download AI Model")
            .setMessage("Loading...")
            .create()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogModelDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        modelDownloader = PersistentModelDownloader(requireContext())
        
        // Check if already downloaded
        if (modelDownloader.isModelDownloaded()) {
            binding.statusText.text = "AI model is already downloaded!"
            binding.downloadButton.text = "Close"
            binding.downloadButton.setOnClickListener {
                dismiss()
            }
            binding.progressBar.visibility = View.GONE
            binding.progressDetails.visibility = View.GONE
        } else {
            checkBackgroundDownloadStatus()
        }
    }
    
    private fun checkBackgroundDownloadStatus() {
        // Check if background download is already running
        lifecycleScope.launch {
            val workManager = WorkManager.getInstance(requireContext())
            workManager.getWorkInfosForUniqueWorkLiveData("model_download_work")
                .observe(viewLifecycleOwner) { workInfos ->
                    val runningWork = workInfos.firstOrNull { 
                        it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                    }
                    
                    if (runningWork != null) {
                        // Background download is running
                        showBackgroundDownloadUI(runningWork)
                    } else {
                        // No background download running
                        setupDownloadUI()
                    }
                }
        }
    }
    
    private fun showBackgroundDownloadUI(workInfo: WorkInfo) {
        binding.statusText.text = "Model is downloading in background"
        binding.sizeInfo.text = "You can close this dialog and continue using the app"
        
        // Show progress from WorkInfo
        val progress = workInfo.progress
        val bytesDownloaded = progress.getLong("bytesDownloaded", 0L)
        val totalBytes = progress.getLong("totalBytes", 0L)
        val progressPercentage = progress.getInt("progressPercentage", 0)
        
        if (totalBytes > 0) {
            binding.progressBar.visibility = View.VISIBLE
            binding.progressDetails.visibility = View.VISIBLE
            binding.progressBar.progress = progressPercentage
            binding.progressPercentage.text = "$progressPercentage%"
            
            val downloadedMB = bytesDownloaded / (1024.0 * 1024.0)
            val totalMB = totalBytes / (1024.0 * 1024.0)
            val df = DecimalFormat("#.##")
            binding.progressDetails.text = "${df.format(downloadedMB)} MB / ${df.format(totalMB)} MB"
        }
        
        binding.downloadButton.text = "Switch to Foreground"
        binding.downloadButton.setOnClickListener {
            // Cancel background download and start foreground
            ModelDownloadWorker.cancel(requireContext())
            setupDownloadUI()
        }
        
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun setupDownloadUI() {
        binding.statusText.text = "Download the AI model to enable advanced financial insights and analysis"
        binding.sizeInfo.text = "Model size: ~1.4 GB"
        
        // Add background download option
        binding.downloadButton.text = "Download Now"
        binding.downloadButton.setOnClickListener {
            showDownloadOptions()
        }
        
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }
    
    private fun showDownloadOptions() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Download Options")
            .setMessage("Choose how to download the AI model")
            .setPositiveButton("Download in Background") { _, _ ->
                startBackgroundDownload()
            }
            .setNeutralButton("Download Now") { _, _ ->
                startForegroundDownload()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun startBackgroundDownload() {
        ModelDownloadWorker.enqueue(requireContext())
        Toast.makeText(
            requireContext(), 
            "Model download started in background. You'll be notified when complete.",
            Toast.LENGTH_LONG
        ).show()
        dismiss()
    }
    
    private fun startForegroundDownload() {
        isDownloading = true
        binding.downloadButton.isEnabled = false
        binding.cancelButton.text = "Cancel Download"
        binding.progressBar.visibility = View.VISIBLE
        binding.progressDetails.visibility = View.VISIBLE
        binding.progressPercentage.visibility = View.VISIBLE
        binding.speedText.visibility = View.VISIBLE
        binding.timeRemaining.visibility = View.VISIBLE
        binding.statusText.text = "Downloading AI model..."
        
        lifecycleScope.launch {
            try {
                modelDownloader.downloadModel().collect { progress ->
                    updateProgress(progress)
                }
            } catch (e: Exception) {
                showError("Download failed: ${e.message}")
            }
        }
        
        binding.cancelButton.setOnClickListener {
            if (isDownloading) {
                modelDownloader.cancelDownload()
                isDownloading = false
                dismiss()
            } else {
                dismiss()
            }
        }
    }
    
    private fun updateProgress(progress: DownloadProgress) {
        binding.progressBar.progress = progress.progressPercentage
        binding.progressPercentage.text = "${progress.progressPercentage}%"
        
        val downloadedMB = progress.bytesDownloaded / (1024.0 * 1024.0)
        val totalMB = progress.totalBytes / (1024.0 * 1024.0)
        val df = DecimalFormat("#.##")
        
        binding.progressDetails.text = "${df.format(downloadedMB)} MB / ${df.format(totalMB)} MB"
        
        if (progress.speedMBps > 0) {
            binding.speedText.text = "Speed: ${df.format(progress.speedMBps)} MB/s"
            val remainingMinutes = progress.remainingTimeSeconds / 60
            binding.timeRemaining.text = "Time remaining: ${remainingMinutes} min"
        }
        
        when {
            progress.isComplete && progress.error == null -> {
                isDownloading = false
                binding.statusText.text = "Download complete!"
                binding.downloadButton.text = "Done"
                binding.downloadButton.isEnabled = true
                binding.downloadButton.setOnClickListener {
                    onDownloadComplete?.invoke()
                    dismiss()
                }
                binding.cancelButton.visibility = View.GONE
            }
            progress.error != null -> {
                isDownloading = false
                showError(progress.error)
            }
        }
    }
    
    private fun showError(error: String) {
        binding.statusText.text = "Error: $error"
        binding.downloadButton.text = "Retry"
        binding.downloadButton.isEnabled = true
        binding.progressBar.visibility = View.GONE
        binding.progressDetails.visibility = View.GONE
        binding.speedText.visibility = View.GONE
        binding.timeRemaining.visibility = View.GONE
        binding.progressPercentage.visibility = View.GONE
        
        binding.downloadButton.setOnClickListener {
            setupDownloadUI()
        }
    }
    
    fun setOnDownloadCompleteListener(listener: () -> Unit) {
        onDownloadComplete = listener
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance(): ModelDownloadDialog {
            return ModelDownloadDialog()
        }
    }
}