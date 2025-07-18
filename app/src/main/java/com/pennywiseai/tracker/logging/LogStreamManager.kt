package com.pennywiseai.tracker.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages a real-time stream of log entries for displaying progress
 */
object LogStreamManager {
    data class LogEntry(
        val id: String = UUID.randomUUID().toString(),
        val timestamp: Long = System.currentTimeMillis(),
        val category: LogCategory,
        val message: String,
        val level: LogLevel = LogLevel.INFO,
        val metadata: Map<String, Any>? = null
    ) {
        val formattedTime: String
            get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    }
    
    enum class LogCategory {
        SMS_PROCESSING,
        LLM_ANALYSIS,
        DATABASE,
        CHUNK_PROCESSING,
        GENERAL
    }
    
    enum class LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
    
    data class ScanStats(
        val totalMessages: Int = 0,
        val messagesProcessed: Int = 0,
        val transactionsFound: Int = 0,
        val currentChunk: Int = 0,
        val totalChunks: Int = 0,
        val startTime: Long = System.currentTimeMillis(),
        val isComplete: Boolean = false,
        val error: String? = null
    ) {
        val elapsedSeconds: Long
            get() = (System.currentTimeMillis() - startTime) / 1000
        
        val messagesPerSecond: Float
            get() = if (elapsedSeconds > 0) messagesProcessed.toFloat() / elapsedSeconds else 0f
        
        val progressPercentage: Int
            get() = if (totalMessages > 0) (messagesProcessed * 100) / totalMessages else 0
    }
    
    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries.asStateFlow()
    
    private val _scanStats = MutableStateFlow(ScanStats())
    val scanStats: StateFlow<ScanStats> = _scanStats.asStateFlow()
    
    private val _isModalVisible = MutableStateFlow(false)
    val isModalVisible: StateFlow<Boolean> = _isModalVisible.asStateFlow()
    
    private val _isScanRunning = MutableStateFlow(false)
    val isScanRunning: StateFlow<Boolean> = _isScanRunning.asStateFlow()
    
    // Cancellation flow
    private val _cancellationSignal = MutableSharedFlow<Unit>()
    val cancellationSignal: SharedFlow<Unit> = _cancellationSignal.asSharedFlow()
    
    @Volatile
    private var isCancelled = false
    
    fun showModal() {
        _isModalVisible.value = true
    }
    
    fun hideModal() {
        _isModalVisible.value = false
    }
    
    fun clearLogs() {
        _logEntries.value = emptyList()
        _scanStats.value = ScanStats()
    }
    
    fun log(category: LogCategory, message: String, level: LogLevel = LogLevel.INFO, metadata: Map<String, Any>? = null) {
        val entry = LogEntry(
            category = category,
            message = message,
            level = level,
            metadata = metadata
        )
        
        // Keep only last 100 entries to prevent memory issues
        _logEntries.value = (_logEntries.value + entry).takeLast(100)
    }
    
    fun updateStats(update: (ScanStats) -> ScanStats) {
        _scanStats.value = update(_scanStats.value)
    }
    
    fun startNewScan(totalMessages: Int) {
        clearLogs()
        _scanStats.value = ScanStats(
            totalMessages = totalMessages,
            startTime = System.currentTimeMillis()
        )
        _isScanRunning.value = true
        isCancelled = false
        log(LogCategory.GENERAL, "🚀 Starting SMS scan for $totalMessages messages", LogLevel.INFO)
    }
    
    fun completeCurrentChunk(chunkNumber: Int, totalChunks: Int, transactionsInChunk: Int) {
        updateStats { stats ->
            stats.copy(
                currentChunk = chunkNumber,
                totalChunks = totalChunks,
                transactionsFound = stats.transactionsFound + transactionsInChunk
            )
        }
        log(
            LogCategory.CHUNK_PROCESSING, 
            "✅ Chunk $chunkNumber/$totalChunks processed - Found $transactionsInChunk transactions",
            LogLevel.INFO,
            mapOf("chunk" to chunkNumber, "transactions" to transactionsInChunk)
        )
    }
    
    fun messageProcessed(sender: String, isTransaction: Boolean, merchant: String? = null, amount: Double? = null) {
        updateStats { stats ->
            stats.copy(
                messagesProcessed = stats.messagesProcessed + 1,
                transactionsFound = if (isTransaction) stats.transactionsFound + 1 else stats.transactionsFound
            )
        }
        
        if (isTransaction && merchant != null && amount != null) {
            log(
                LogCategory.SMS_PROCESSING,
                "💳 Transaction found: $merchant - ₹$amount",
                LogLevel.INFO,
                mapOf("sender" to sender, "merchant" to merchant, "amount" to amount)
            )
        } else {
            log(
                LogCategory.SMS_PROCESSING,
                "📭 Non-transaction SMS from $sender",
                LogLevel.DEBUG
            )
        }
    }
    
    fun llmStarted(sender: String, messagePreview: String) {
        log(
            LogCategory.LLM_ANALYSIS,
            "🤖 Analyzing SMS from $sender: ${messagePreview.take(50)}...",
            LogLevel.INFO
        )
    }
    
    fun llmCompleted(success: Boolean, error: String? = null) {
        if (success) {
            log(LogCategory.LLM_ANALYSIS, "✅ LLM analysis complete", LogLevel.DEBUG)
        } else {
            log(LogCategory.LLM_ANALYSIS, "❌ LLM analysis failed: ${error ?: "Unknown error"}", LogLevel.ERROR)
        }
    }
    
    fun databaseOperation(operation: String, count: Int = 1) {
        log(
            LogCategory.DATABASE,
            "💾 Database: $operation ($count items)",
            LogLevel.DEBUG,
            mapOf("operation" to operation, "count" to count)
        )
    }
    
    fun scanCompleted() {
        updateStats { stats -> stats.copy(isComplete = true) }
        _isScanRunning.value = false
        val stats = _scanStats.value
        log(
            LogCategory.GENERAL,
            "🎉 Scan completed! Found ${stats.transactionsFound} transactions from ${stats.messagesProcessed} messages in ${stats.elapsedSeconds}s",
            LogLevel.INFO
        )
    }
    
    fun scanError(error: String) {
        updateStats { stats -> stats.copy(isComplete = true, error = error) }
        _isScanRunning.value = false
        log(LogCategory.GENERAL, "❌ Scan failed: $error", LogLevel.ERROR)
    }
    
    fun cancelScan() {
        isCancelled = true
        updateStats { stats -> stats.copy(isComplete = true, error = "Cancelled by user") }
        _isScanRunning.value = false
        log(LogCategory.GENERAL, "⏹️ Scan cancelled by user", LogLevel.INFO)
        
        // Emit cancellation signal
        CoroutineScope(Dispatchers.Main).launch {
            _cancellationSignal.emit(Unit)
        }
    }
    
    fun checkCancellation(): Boolean {
        return isCancelled
    }
}