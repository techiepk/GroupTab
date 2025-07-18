package com.pennywiseai.tracker.firebase

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance

/**
 * Helper class for Firebase Analytics, Crashlytics, and Performance monitoring
 * Provides easy-to-use methods for tracking events, crashes, and performance
 */
object FirebaseHelper {
    
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var performance: FirebasePerformance
    
    fun initialize(context: Context) {
        analytics = FirebaseAnalytics.getInstance(context)
        crashlytics = FirebaseCrashlytics.getInstance()
        performance = FirebasePerformance.getInstance()
    }
    
    // Analytics Events
    fun logEvent(eventName: String, params: Bundle? = null) {
        if (::analytics.isInitialized) {
            analytics.logEvent(eventName, params)
        }
    }
    
    fun logTransactionScanned(count: Int, durationMs: Long) {
        val bundle = Bundle().apply {
            putInt("transaction_count", count)
            putLong("scan_duration_ms", durationMs)
        }
        logEvent("transaction_scan_completed", bundle)
    }
    
    fun logAIClassification(category: String, confidence: Float, processingTimeMs: Long) {
        val bundle = Bundle().apply {
            putString("category", category)
            putFloat("confidence", confidence)
            putLong("processing_time_ms", processingTimeMs)
        }
        logEvent("ai_classification", bundle)
    }
    
    fun logModelDownload(success: Boolean, sizeKB: Long, durationMs: Long) {
        val bundle = Bundle().apply {
            putBoolean("success", success)
            putLong("model_size_kb", sizeKB)
            putLong("download_duration_ms", durationMs)
        }
        logEvent("model_download", bundle)
    }
    
    fun logScreenView(screenName: String, screenClass: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
    
    // Crashlytics
    fun setUserId(userId: String) {
        if (::crashlytics.isInitialized) {
            crashlytics.setUserId(userId)
        }
    }
    
    fun setCustomKey(key: String, value: String) {
        if (::crashlytics.isInitialized) {
            crashlytics.setCustomKey(key, value)
        }
    }
    
    fun setCustomKey(key: String, value: Boolean) {
        if (::crashlytics.isInitialized) {
            crashlytics.setCustomKey(key, value)
        }
    }
    
    fun setCustomKey(key: String, value: Int) {
        if (::crashlytics.isInitialized) {
            crashlytics.setCustomKey(key, value)
        }
    }
    
    fun setCustomKey(key: String, value: Float) {
        if (::crashlytics.isInitialized) {
            crashlytics.setCustomKey(key, value)
        }
    }
    
    fun logException(throwable: Throwable) {
        if (::crashlytics.isInitialized) {
            crashlytics.recordException(throwable)
        }
    }
    
    fun logMessage(message: String) {
        if (::crashlytics.isInitialized) {
            crashlytics.log(message)
        }
    }
    
    // Performance Monitoring
    fun startTrace(traceName: String) = if (::performance.isInitialized) {
        performance.newTrace(traceName).apply { start() }
    } else {
        null
    }
    
    fun stopTrace(trace: com.google.firebase.perf.metrics.Trace?) {
        trace?.stop()
    }
    
    // Debug helpers
    fun logDebugInfo(component: String, message: String) {
        logMessage("[$component] $message")
        setCustomKey("last_debug_component", component)
        setCustomKey("last_debug_message", message)
        setCustomKey("last_debug_timestamp", System.currentTimeMillis().toInt())
        
        // Also log memory usage
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        setCustomKey("memory_used_mb", usedMemory.toInt())
    }
    
    fun logAIDebugInfo(
        availableMemoryMB: Long,
        modelSize: String,
        processingSpeed: String,
        isModelLoaded: Boolean
    ) {
        setCustomKey("ai_available_memory_mb", availableMemoryMB.toInt())
        setCustomKey("ai_model_size", modelSize)
        setCustomKey("ai_processing_speed", processingSpeed)
        setCustomKey("ai_model_loaded", isModelLoaded)
        
        logMessage("AI Debug - Memory: ${availableMemoryMB}MB, Model: $modelSize, Speed: $processingSpeed, Loaded: $isModelLoaded")
    }
    
    fun logSMSDebugInfo(
        messagesFound: Int,
        transactionsExtracted: Int,
        scanDurationMs: Long,
        daysScanned: Int
    ) {
        setCustomKey("sms_messages_found", messagesFound)
        setCustomKey("sms_transactions_extracted", transactionsExtracted)
        setCustomKey("sms_scan_duration_ms", scanDurationMs.toInt())
        setCustomKey("sms_days_scanned", daysScanned)
        
        logMessage("SMS Debug - Messages: $messagesFound, Transactions: $transactionsExtracted, Duration: ${scanDurationMs}ms, Days: $daysScanned")
    }
    
    // Force crash logs to be sent immediately
    fun sendUnsentReports() {
        if (::crashlytics.isInitialized) {
            crashlytics.sendUnsentReports()
        }
    }
}