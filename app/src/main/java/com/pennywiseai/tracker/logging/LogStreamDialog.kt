package com.pennywiseai.tracker.logging

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pennywiseai.tracker.databinding.DialogLogStreamBinding
import com.pennywiseai.tracker.databinding.ItemLogEntryBinding
import kotlinx.coroutines.launch
import com.google.android.material.color.MaterialColors

class LogStreamDialog : DialogFragment() {
    
    private var _binding: DialogLogStreamBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var logAdapter: LogAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_NoActionBar_Fullscreen)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLogStreamBinding.inflate(inflater, container, false)
        
        // Make dialog full screen with transparent background
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
        
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeData()
    }
    
    private fun setupUI() {
        // Setup RecyclerView
        logAdapter = LogAdapter()
        binding.logRecyclerView.apply {
            adapter = logAdapter
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true // Start from bottom like a chat
            }
        }
        
        // Close button
        binding.closeButton.setOnClickListener {
            // Check if scan is still running
            if (LogStreamManager.isScanRunning.value) {
                com.google.android.material.snackbar.Snackbar.make(
                    requireView(),
                    "🔍 Scan continues in background. Tap 'View Progress' to reopen.",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
            }
            LogStreamManager.hideModal()
            dismiss()
        }
        
        // Cancel scan button
        binding.cancelScanButton.setOnClickListener {
            // Cancel the background work
            com.pennywiseai.tracker.background.ScanWorker.cancel(requireContext())
            // Update LogStreamManager state
            LogStreamManager.cancelScan()
            // Show confirmation
            com.google.android.material.snackbar.Snackbar.make(
                requireView(),
                "⏹️ Scan cancelled",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
            // Hide modal after short delay
            requireView().postDelayed({
                LogStreamManager.hideModal()
                dismiss()
            }, 1000)
        }
        
        // Filter chips
        binding.apply {
            chipAll.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) logAdapter.setFilter(null)
            }
            chipSms.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) logAdapter.setFilter(LogStreamManager.LogCategory.SMS_PROCESSING)
            }
            chipLlm.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) logAdapter.setFilter(LogStreamManager.LogCategory.LLM_ANALYSIS)
            }
            chipDatabase.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) logAdapter.setFilter(LogStreamManager.LogCategory.DATABASE)
            }
        }
    }
    
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe log entries
                launch {
                    LogStreamManager.logEntries.collect { entries ->
                        logAdapter.submitList(entries)
                        // Auto scroll to bottom when new entries arrive
                        if (entries.isNotEmpty()) {
                            binding.logRecyclerView.smoothScrollToPosition(entries.size - 1)
                        }
                    }
                }
                
                // Observe scan stats
                launch {
                    LogStreamManager.scanStats.collect { stats ->
                        updateStats(stats)
                    }
                }
            }
        }
    }
    
    private fun updateStats(stats: LogStreamManager.ScanStats) {
        binding.apply {
            // Progress
            progressBar.progress = stats.progressPercentage
            progressText.text = "${stats.progressPercentage}%"
            
            // Stats cards
            messagesProcessedText.text = "${stats.messagesProcessed}/${stats.totalMessages}"
            transactionsFoundText.text = stats.transactionsFound.toString()
            
            // Speed
            val speed = if (stats.messagesPerSecond > 0) {
                String.format("%.1f msg/s", stats.messagesPerSecond)
            } else {
                "Calculating..."
            }
            speedText.text = speed
            
            // Time elapsed
            val minutes = stats.elapsedSeconds / 60
            val seconds = stats.elapsedSeconds % 60
            elapsedTimeText.text = String.format("%02d:%02d", minutes, seconds)
            
            // Current chunk
            if (stats.totalChunks > 0) {
                chunkProgressText.text = "Chunk ${stats.currentChunk}/${stats.totalChunks}"
                chunkProgressText.visibility = View.VISIBLE
            } else {
                chunkProgressText.visibility = View.GONE
            }
            
            // Complete/Error state
            if (stats.isComplete) {
                cancelScanButton.visibility = View.GONE
                if (stats.error != null) {
                    progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                        MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorError)
                    )
                    statusText.text = "❌ Error: ${stats.error}"
                    statusText.visibility = View.VISIBLE
                } else {
                    progressBar.progressTintList = android.content.res.ColorStateList.valueOf(
                        MaterialColors.getColor(requireView(), com.google.android.material.R.attr.colorPrimary)
                    )
                    statusText.text = "✅ Scan Complete!"
                    statusText.visibility = View.VISIBLE
                }
            } else {
                cancelScanButton.visibility = View.VISIBLE
                statusText.visibility = View.GONE
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun show(fragmentManager: androidx.fragment.app.FragmentManager) {
            LogStreamDialog().show(fragmentManager, "LogStreamDialog")
        }
    }
}

// Adapter for log entries
class LogAdapter : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {
    
    private var allEntries = listOf<LogStreamManager.LogEntry>()
    private var filteredEntries = listOf<LogStreamManager.LogEntry>()
    private var currentFilter: LogStreamManager.LogCategory? = null
    
    fun submitList(entries: List<LogStreamManager.LogEntry>) {
        allEntries = entries
        applyFilter()
    }
    
    fun setFilter(category: LogStreamManager.LogCategory?) {
        currentFilter = category
        applyFilter()
    }
    
    private fun applyFilter() {
        filteredEntries = if (currentFilter == null) {
            allEntries
        } else {
            allEntries.filter { it.category == currentFilter }
        }
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LogViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(filteredEntries[position])
    }
    
    override fun getItemCount() = filteredEntries.size
    
    class LogViewHolder(private val binding: ItemLogEntryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: LogStreamManager.LogEntry) {
            binding.apply {
                timeText.text = entry.formattedTime
                messageText.text = entry.message
                
                // Category icon and color
                val (icon, color) = when (entry.category) {
                    LogStreamManager.LogCategory.SMS_PROCESSING -> "📱" to com.google.android.material.R.attr.colorPrimary
                    LogStreamManager.LogCategory.LLM_ANALYSIS -> "🤖" to com.google.android.material.R.attr.colorSecondary
                    LogStreamManager.LogCategory.DATABASE -> "💾" to com.google.android.material.R.attr.colorTertiary
                    LogStreamManager.LogCategory.CHUNK_PROCESSING -> "📦" to com.google.android.material.R.attr.colorPrimaryContainer
                    LogStreamManager.LogCategory.GENERAL -> "📋" to com.google.android.material.R.attr.colorSurfaceVariant
                }
                categoryIcon.text = icon
                
                // Level-based text color
                val textColor = when (entry.level) {
                    LogStreamManager.LogLevel.ERROR -> com.google.android.material.R.attr.colorError
                    LogStreamManager.LogLevel.WARN -> com.google.android.material.R.attr.colorErrorContainer
                    LogStreamManager.LogLevel.DEBUG -> com.google.android.material.R.attr.colorOutlineVariant
                    LogStreamManager.LogLevel.INFO -> com.google.android.material.R.attr.colorOnSurface
                }
                
                messageText.setTextColor(MaterialColors.getColor(itemView, textColor))
                
                // Background tint for different levels
                if (entry.level == LogStreamManager.LogLevel.ERROR) {
                    root.setCardBackgroundColor(
                        MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorErrorContainer)
                    )
                } else {
                    root.setCardBackgroundColor(
                        MaterialColors.getColor(itemView, com.google.android.material.R.attr.colorSurfaceVariant)
                    )
                }
            }
        }
    }
}