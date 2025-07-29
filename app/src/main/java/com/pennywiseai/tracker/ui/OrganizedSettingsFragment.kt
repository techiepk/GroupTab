package com.pennywiseai.tracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.pennywiseai.tracker.data.ScanTimeframe
import com.pennywiseai.tracker.databinding.FragmentSettingsOrganizedBinding
import com.pennywiseai.tracker.viewmodel.SettingsViewModel
import com.pennywiseai.tracker.llm.TransactionClassifier
import com.pennywiseai.tracker.llm.PersistentModelDownloader
import com.pennywiseai.tracker.firebase.FirebaseHelper
import com.pennywiseai.tracker.background.ModelDownloadWorker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import androidx.core.content.ContextCompat
import android.content.Context
import com.pennywiseai.tracker.ui.GroupManagementActivity
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.utils.DataExporter
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionRepository
import java.util.Calendar
import android.app.DatePickerDialog
import com.pennywiseai.tracker.utils.ThemeColorUtils
import java.io.File

class OrganizedSettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsOrganizedBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var classifier: TransactionClassifier
    private lateinit var modelDownloader: PersistentModelDownloader
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        _binding = FragmentSettingsOrganizedBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Log screen view
        FirebaseHelper.logScreenView("OrganizedSettings", "OrganizedSettingsFragment")
        
        classifier = TransactionClassifier(requireContext())
        modelDownloader = PersistentModelDownloader(requireContext())
        
        setupUI()
        observeViewModel()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh model status when fragment resumes
        viewModel.updateModelStatus()
    }
    
    private fun setupUI() {
        binding.apply {
            // General Settings
            scanPeriodItem.setOnClickListener {
                showScanPeriodDialog()
            }
            
            // Analysis mode removed
            // extractionModeItem.setOnClickListener {
            //     showExtractionModeDialog()
            // }
            
            // AI & Intelligence
            modelManagementItem.setOnClickListener {
                showModelManagementDialog()
            }
            
            aiInsightsSwitch.setOnCheckedChangeListener { _, isChecked ->
                // Save AI insights preference
                // TODO: Implement in ViewModel
                Snackbar.make(binding.root, 
                    if (isChecked) "Smart insights enabled" else "Smart insights disabled", 
                    Snackbar.LENGTH_SHORT).show()
            }
            
            // Data Management
            groupManagementItem.setOnClickListener {
                val intent = Intent(requireContext(), GroupManagementActivity::class.java)
                startActivity(intent)
            }
            
            dataExportItem.setOnClickListener {
                showDataExportDialog()
            }
            
            // Privacy & Security
            dataPrivacyItem.setOnClickListener {
                showDataPrivacyDialog()
            }
            
            permissionsItem.setOnClickListener {
                showPermissionsDialog()
            }
            
            // About
            aboutItem.setOnClickListener {
                showAboutDialog()
            }
        }
    }
    
    private fun observeViewModel() {
        // Observe settings
        viewModel.settings.observe(viewLifecycleOwner) { settings ->
            settings?.let {
                binding.scanPeriodValue.text = "Last ${it.historicalScanDays} days"
            }
        }
        
        // Analysis mode removed
        // viewModel.isPatternMode.observe(viewLifecycleOwner) { isPattern ->
        //     binding.extractionModeValue.text = if (!isPattern) {
        //         "AI Assistant"
        //     } else {
        //         "Quick Scan"
        //     }
        // }
        
        // Observe model status
        viewModel.modelStatus.observe(viewLifecycleOwner) { status ->
            android.util.Log.d("OrganizedSettingsFragment", "Model status changed: $status")
            updateModelStatus(status)
        }
        
        // Observe model download progress
        viewModel.modelDownloadProgress.observe(viewLifecycleOwner) { progress ->
            android.util.Log.d("OrganizedSettingsFragment", "Model download progress observer triggered: $progress")
            updateModelProgress(progress)
        }
        
    }
    
    
    private fun showScanPeriodDialog() {
        val options = arrayOf("1 day", "7 days", "14 days", "30 days")
        val values = arrayOf(1, 7, 14, 30)
        val currentDays = viewModel.settings.value?.historicalScanDays ?: 30
        val currentSelection = values.indexOf(currentDays).takeIf { it >= 0 } ?: 3
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Scan Period")
            .setSingleChoiceItems(options, currentSelection) { dialog, which ->
                lifecycleScope.launch {
                    viewModel.updateScanTimeframe(values[which])
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    // Analysis mode removed
    // private fun showExtractionModeDialog() {
    //     val options = arrayOf("Quick Scan", "AI Assistant")
    //     val currentSelection = if (viewModel.isPatternMode.value == true) 0 else 1
    //     
    //     MaterialAlertDialogBuilder(requireContext())
    //         .setTitle("Analysis Mode")
    //         .setSingleChoiceItems(options, currentSelection) { dialog, which ->
    //             viewModel.setExtractionMode(which == 0)
    //             dialog.dismiss()
    //         }
    //         .setNegativeButton("Cancel", null)
    //         .show()
    // }
    
    
    private fun showModelManagementDialog() {
        val currentStatus = viewModel.modelStatus.value ?: SettingsViewModel.ModelStatus.UNKNOWN
        val modelSize = viewModel.getModelSize()
        val expectedSize = viewModel.getExpectedModelSize()
        
        val statusText = when (currentStatus) {
            SettingsViewModel.ModelStatus.NOT_DOWNLOADED -> "Not Downloaded"
            SettingsViewModel.ModelStatus.DOWNLOADING -> "Downloading..."
            SettingsViewModel.ModelStatus.DOWNLOADED -> "Ready"
            SettingsViewModel.ModelStatus.ERROR -> "Error"
            SettingsViewModel.ModelStatus.UNKNOWN -> "Unknown"
        }
        
        val sizeText = if (modelSize > 0) {
            formatFileSize(modelSize)
        } else {
            formatFileSize(expectedSize) + " (expected)"
        }
        
        val deviceInfo = "Device: ${android.os.Build.MODEL}\n" +
                "Android: ${android.os.Build.VERSION.RELEASE}\n" +
                "RAM: ${getDeviceRAM()}"
        
        val message = "🤖 AI Model Information\n\n" +
                "Status: $statusText\n" +
                "Size: $sizeText\n" +
                "Type: Gemma 2B-IT\n" +
                "Processing: On-device only\n\n" +
                "📱 Your Device\n$deviceInfo\n\n" +
                "⚠️ Minimum Requirements\n" +
                "• 4GB+ RAM recommended\n" +
                "• 3GB free storage\n" +
                "• Android 8.0 or higher\n\n" +
                "🔒 Privacy: All analysis happens locally on your device. No data is sent to external servers."
        
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle("AI Model Management")
            .setMessage(message)
            .setNegativeButton("Close", null)
        
        when (currentStatus) {
            SettingsViewModel.ModelStatus.NOT_DOWNLOADED, SettingsViewModel.ModelStatus.ERROR -> {
                builder.setPositiveButton("Download") { _, _ ->
                    showDownloadOptionsDialog()
                }
            }
            SettingsViewModel.ModelStatus.DOWNLOADING -> {
                builder.setNeutralButton("Cancel") { _, _ ->
                    cancelModelDownload()
                }
            }
            SettingsViewModel.ModelStatus.DOWNLOADED -> {
                builder.setPositiveButton("Re-download") { _, _ ->
                    startModelDownload()
                }
                builder.setNeutralButton("Delete Model") { _, _ ->
                    showDeleteConfirmation()
                }
            }
            SettingsViewModel.ModelStatus.UNKNOWN -> {
                builder.setPositiveButton("Check Status") { _, _ ->
                    viewModel.updateModelStatus()
                }
            }
        }
        
        builder.show()
    }
    
    private fun startModelDownload() {
        // Show progress feedback immediately
        Snackbar.make(binding.root, "Starting model download...", Snackbar.LENGTH_SHORT).show()
        
        viewModel.startModelDownload()
    }
    
    private fun startBackgroundDownload() {
        viewModel.startBackgroundModelDownload()
        Snackbar.make(binding.root, "Background download started", Snackbar.LENGTH_SHORT).show()
    }
    
    private fun showDownloadOptionsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Download Options")
            .setMessage(
                "Choose how to download the AI model (2.7GB):\n\n" +
                "• Download Now: Faster, but stops if you leave the app\n" +
                "• Background Download: Continues even if app is closed, shows progress in notifications"
            )
            .setPositiveButton("Download Now") { _, _ ->
                // Foreground download - stops if app is closed
                startModelDownload()
            }
            .setNeutralButton("Background Download") { _, _ ->
                // Background download - continues even if app is closed
                startBackgroundDownload()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun cancelModelDownload() {
        viewModel.cancelModelDownload()
        Snackbar.make(binding.root, "Download cancelled", Snackbar.LENGTH_SHORT).show()
    }
    
    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete AI Model")
            .setMessage("Are you sure you want to delete the AI model?\n\n" +
                    "• Model size: ${formatFileSize(viewModel.getModelSize())}\n" +
                    "• You'll need to download it again to use AI features\n" +
                    "• All AI analysis will be disabled")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteModel()
                Snackbar.make(binding.root, "Model deleted", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun getDeviceRAM(): String {
        return try {
            val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val totalMemory = memInfo.totalMem / (1024 * 1024 * 1024) // Convert to GB
            "${totalMemory}GB"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun showDataExportDialog() {
        val options = arrayOf(
            "Export Transactions (CSV)",
            "Export Subscriptions (CSV)", 
            "Export Analytics Report"
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Export Data")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showTransactionExportOptions()
                    1 -> exportSubscriptions()
                    2 -> showAnalyticsExportOptions()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showTransactionExportOptions() {
        val options = arrayOf("All Transactions", "This Month", "Last 30 Days", "Custom Date Range")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Export Range")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportTransactions(null, null)
                    1 -> {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        val startDate = calendar.timeInMillis
                        exportTransactions(startDate, System.currentTimeMillis())
                    }
                    2 -> {
                        val startDate = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                        exportTransactions(startDate, System.currentTimeMillis())
                    }
                    3 -> showDateRangePicker()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showAnalyticsExportOptions() {
        val options = arrayOf("This Month", "Last 30 Days", "Last 3 Months", "Custom Date Range")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Report Period")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        calendar.set(Calendar.HOUR_OF_DAY, 0)
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        val startDate = calendar.timeInMillis
                        exportAnalyticsReport(startDate, System.currentTimeMillis())
                    }
                    1 -> {
                        val startDate = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                        exportAnalyticsReport(startDate, System.currentTimeMillis())
                    }
                    2 -> {
                        val startDate = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L)
                        exportAnalyticsReport(startDate, System.currentTimeMillis())
                    }
                    3 -> showDateRangePickerForAnalytics()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun exportTransactions(startDate: Long?, endDate: Long?) {
        lifecycleScope.launch {
            try {
                val repository = TransactionRepository(AppDatabase.getDatabase(requireContext()))
                val exporter = DataExporter(requireContext(), repository)
                
                val result = exporter.exportTransactionsToCSV(startDate, endDate)
                
                if (result.success && result.filePath != null) {
                    showExportSuccessDialog(result.filePath, "text/csv")
                } else {
                    Toast.makeText(requireContext(), "Export failed: ${result.error}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun exportSubscriptions() {
        lifecycleScope.launch {
            try {
                val repository = TransactionRepository(AppDatabase.getDatabase(requireContext()))
                val exporter = DataExporter(requireContext(), repository)
                
                val result = exporter.exportSubscriptionsToCSV()
                
                if (result.success && result.filePath != null) {
                    showExportSuccessDialog(result.filePath, "text/csv")
                } else {
                    Toast.makeText(requireContext(), "Export failed: ${result.error}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun exportAnalyticsReport(startDate: Long, endDate: Long) {
        lifecycleScope.launch {
            try {
                val repository = TransactionRepository(AppDatabase.getDatabase(requireContext()))
                val exporter = DataExporter(requireContext(), repository)
                
                val result = exporter.exportAnalyticsReport(startDate, endDate)
                
                if (result.success && result.filePath != null) {
                    showExportSuccessDialog(result.filePath, "text/plain")
                } else {
                    Toast.makeText(requireContext(), "Export failed: ${result.error}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showExportSuccessDialog(filePath: String, mimeType: String) {
        val fileName = File(filePath).name
        val repository = TransactionRepository(AppDatabase.getDatabase(requireContext()))
        val exporter = DataExporter(requireContext(), repository)
        val message = exporter.getDownloadNotificationMessage(fileName)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Export Successful")
            .setMessage(message)
            .setPositiveButton("Share") { _, _ ->
                exporter.shareFile(filePath, mimeType)
            }
            .setNegativeButton("OK", null)
            .show()
    }
    
    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance()
        var startDate: Long? = null
        var endDate: Long? = null
        
        // Pick start date
        DatePickerDialog(requireContext(), { _, year, month, day ->
            calendar.set(year, month, day, 0, 0, 0)
            startDate = calendar.timeInMillis
            
            // Pick end date
            DatePickerDialog(requireContext(), { _, endYear, endMonth, endDay ->
                calendar.set(endYear, endMonth, endDay, 23, 59, 59)
                endDate = calendar.timeInMillis
                
                if (startDate != null && endDate != null) {
                    exportTransactions(startDate, endDate)
                }
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
                setTitle("Select End Date")
                datePicker.minDate = startDate ?: 0
                show()
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
            setTitle("Select Start Date")
            datePicker.maxDate = System.currentTimeMillis()
            show()
        }
    }
    
    private fun showDateRangePickerForAnalytics() {
        val calendar = Calendar.getInstance()
        var startDate: Long? = null
        var endDate: Long? = null
        
        // Pick start date
        DatePickerDialog(requireContext(), { _, year, month, day ->
            calendar.set(year, month, day, 0, 0, 0)
            startDate = calendar.timeInMillis
            
            // Pick end date
            DatePickerDialog(requireContext(), { _, endYear, endMonth, endDay ->
                calendar.set(endYear, endMonth, endDay, 23, 59, 59)
                endDate = calendar.timeInMillis
                
                if (startDate != null && endDate != null) {
                    exportAnalyticsReport(startDate!!, endDate!!)
                }
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
                setTitle("Select End Date")
                datePicker.minDate = startDate ?: 0
                show()
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
            setTitle("Select Start Date")
            datePicker.maxDate = System.currentTimeMillis()
            show()
        }
    }
    
    private fun showDataPrivacyDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Data Privacy")
            .setMessage("🔒 Your privacy is our priority\n\n" +
                    "• All AI processing happens on your device\n" +
                    "• No transaction data is sent to external servers\n" +
                    "• SMS messages are processed locally only\n" +
                    "• No personal data is collected or stored remotely\n" +
                    "• You have complete control over your financial data")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showPermissionsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Permissions")
            .setMessage("PennyWise AI requires the following permissions:\n\n" +
                    "📱 SMS Access: To read bank transaction messages\n" +
                    "💾 Storage: To save the AI model and app data\n" +
                    "🔔 Notifications: To show scan progress and insights\n\n" +
                    "All permissions are used solely for app functionality and your privacy is protected.")
            .setPositiveButton("Manage Permissions") { _, _ ->
                // Open app settings to manage permissions
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:${requireContext().packageName}")
                startActivity(intent)
            }
            .setNegativeButton("Close", null)
            .show()
    }
    
    private fun updateModelStatus(status: SettingsViewModel.ModelStatus) {
        binding.apply {
            when (status) {
                SettingsViewModel.ModelStatus.NOT_DOWNLOADED -> {
                    modelStatusValue.text = "Not downloaded • Tap to download"
                    val warningColor = ContextCompat.getColor(requireContext(), com.google.android.material.R.color.material_dynamic_tertiary40)
                    modelStatusValue.setTextColor(warningColor)
                    modelStatusIcon.setImageResource(R.drawable.ic_ai_assistant)
                    modelStatusIcon.setColorFilter(warningColor)
                    modelProgressIndicator.visibility = android.view.View.GONE
                    modelProgressContainer.visibility = android.view.View.GONE
                }
                SettingsViewModel.ModelStatus.DOWNLOADING -> {
                    val progress = viewModel.modelDownloadProgress.value
                    modelStatusValue.text = "Downloading model..."
                    modelStatusValue.setTextColor(ContextCompat.getColor(requireContext(), com.google.android.material.R.color.material_dynamic_primary40))
                    modelStatusIcon.visibility = android.view.View.GONE
                    modelProgressIndicator.visibility = android.view.View.VISIBLE
                    // Always show progress container when downloading
                    modelProgressContainer.visibility = android.view.View.VISIBLE
                    // If we have progress data, update it immediately
                    if (progress != null && progress.totalBytes > 0) {
                        updateModelProgress(progress)
                    }
                }
                SettingsViewModel.ModelStatus.DOWNLOADED -> {
                    val sizeText = formatFileSize(viewModel.getModelSize())
                    modelStatusValue.text = "Ready • $sizeText • Gemma 2B-IT"
                    val successColor = ThemeColorUtils.getIncomeColor(requireContext())
                    modelStatusValue.setTextColor(successColor)
                    modelStatusIcon.setImageResource(R.drawable.ic_ai_assistant)
                    modelStatusIcon.setColorFilter(successColor)
                    modelStatusIcon.visibility = android.view.View.VISIBLE
                    modelProgressIndicator.visibility = android.view.View.GONE
                    modelProgressContainer.visibility = android.view.View.GONE
                }
                SettingsViewModel.ModelStatus.ERROR -> {
                    modelStatusValue.text = "Error • Tap to retry download"
                    val errorColor = ThemeColorUtils.getExpenseColor(requireContext())
                    modelStatusValue.setTextColor(errorColor)
                    modelStatusIcon.setImageResource(R.drawable.ic_ai_assistant)
                    modelStatusIcon.setColorFilter(errorColor)
                    modelStatusIcon.visibility = android.view.View.VISIBLE
                    modelProgressIndicator.visibility = android.view.View.GONE
                    modelProgressContainer.visibility = android.view.View.GONE
                }
                SettingsViewModel.ModelStatus.UNKNOWN -> {
                    modelStatusValue.text = "Checking model status..."
                    modelStatusValue.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                    modelStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                    modelProgressIndicator.visibility = android.view.View.GONE
                    modelProgressContainer.visibility = android.view.View.GONE
                }
            }
        }
    }
    
    private fun updateModelProgress(progress: com.pennywiseai.tracker.llm.DownloadProgress?) {
        android.util.Log.d("OrganizedSettingsFragment", "updateModelProgress called with: $progress")
        
        if (progress == null) {
            binding.modelProgressContainer.visibility = android.view.View.GONE
            return
        }
        
        binding.apply {
            modelProgressContainer.visibility = android.view.View.VISIBLE
            android.util.Log.d("OrganizedSettingsFragment", "Progress container made visible")
            
            if (progress.totalBytes > 0) {
                modelProgressBar.isIndeterminate = false
                modelProgressBar.progress = progress.progressPercentage
                
                val downloadedMB = progress.bytesDownloaded / (1024 * 1024)
                val totalMB = progress.totalBytes / (1024 * 1024)
                
                // Add speed and time remaining
                val speedText = when {
                    progress.speedMBps >= 1.0 -> String.format("%.1f MB/s", progress.speedMBps)
                    progress.speedKBps > 0 -> "${progress.speedKBps} KB/s"
                    else -> ""
                }
                
                val timeText = if (progress.remainingTimeSeconds > 0) {
                    val minutes = progress.remainingTimeSeconds / 60
                    val seconds = progress.remainingTimeSeconds % 60
                    when {
                        minutes > 60 -> "${minutes / 60}h ${minutes % 60}m remaining"
                        minutes > 0 -> "${minutes}m ${seconds}s remaining"
                        else -> "${seconds}s remaining"
                    }
                } else ""
                
                val statusText = buildString {
                    append("${downloadedMB}MB / ${totalMB}MB (${progress.progressPercentage}%)")
                    if (speedText.isNotEmpty()) append(" • $speedText")
                    if (timeText.isNotEmpty()) append(" • $timeText")
                }
                
                modelProgressText.text = statusText
            } else {
                modelProgressBar.isIndeterminate = true
                modelProgressText.text = "Preparing download..."
            }
            
            if (progress.error != null) {
                modelProgressText.text = "Download failed: ${progress.error}"
                modelProgressText.setTextColor(ThemeColorUtils.getExpenseColor(requireContext()))
            }
        }
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)}MB"
            else -> String.format("%.1fGB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("About PennyWise AI")
            .setMessage("Version 1.0\n\nPennyWise AI uses on-device AI to categorize your financial transactions automatically. All data stays on your device for complete privacy.\n\n" +
                    "🤖 Powered by Gemma AI\n" +
                    "🔒 Complete privacy protection\n" +
                    "📱 On-device processing\n" +
                    "🎯 Smart financial insights")
            .setPositiveButton("OK", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
