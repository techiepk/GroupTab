package com.pennywiseai.tracker.background

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.sms.HistoricalSmsScanner
import com.pennywiseai.tracker.firebase.FirebaseHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import android.util.Log
import com.pennywiseai.tracker.repository.TransactionGroupRepository
import com.pennywiseai.tracker.grouping.TransactionGroupingService
import com.pennywiseai.tracker.service.EMandateProcessingService

class ScanWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ScanWorker"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sms_scan_channel"
        private const val WORK_NAME = "sms_scan_work"
        
        fun enqueue(context: Context, daysBack: Int = 30) {
            val inputData = Data.Builder()
                .putInt("days_back", daysBack)
                .build()
            
            val scanRequest = OneTimeWorkRequestBuilder<ScanWorker>()
                .setInputData(inputData)
                .addTag(WORK_NAME)
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                scanRequest
            )
            
        }
        
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
        
        fun isRunning(context: Context): Boolean {
            val workInfo = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME)
                .get()
            return workInfo.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "🔍 Starting background SMS scan...")
            
            val daysBack = inputData.getInt("days_back", 30)
            
            // Create notification channel
            createNotificationChannel()
            
            // Show initial notification
            showNotification("Starting SMS scan...", 0, 0, 0)
            
            // Initialize components
            val repository = TransactionRepository(AppDatabase.getDatabase(applicationContext))
            val smsScanner = HistoricalSmsScanner(applicationContext, repository)
            
            // Initialize pattern-based extractor
            smsScanner.initializeExtractor()
            
            var totalTransactions = 0
            var currentProgress = 0
            
            // Start scanning
            var scanResult = Result.success()
            
            smsScanner.scanHistoricalSms(daysBack).collect { progress ->
                currentProgress = if (progress.totalMessages > 0) {
                    (progress.currentMessage * 100) / progress.totalMessages
                } else 0
                
                totalTransactions = progress.transactionsFound
                
                if (progress.isComplete) {
                    if (progress.errorMessage != null) {
                        Log.e(TAG, "❌ Background scan failed: ${progress.errorMessage}")
                        showNotification("SMS scan failed: ${progress.errorMessage}", 100, 0, 0)
                        FirebaseHelper.logException(RuntimeException("Background scan failed: ${progress.errorMessage}"))
                        scanResult = Result.failure()
                    } else {
                        Log.i(TAG, "✅ Background scan completed: $totalTransactions transactions found")
                        
                        // Process E-Mandate messages after scan
                        Log.i(TAG, "📅 Processing E-Mandate messages...")
                        showNotification("Processing subscriptions...", 100, progress.totalMessages, totalTransactions)
                        
                        val eMandateService = EMandateProcessingService(applicationContext)
                        eMandateService.processEMandateMessages(daysBack)
                        
                        // Trigger automatic grouping after scan
                        if (totalTransactions > 0) {
                            Log.i(TAG, "🤖 Starting automatic grouping...")
                            showNotification("Grouping transactions...", 100, progress.totalMessages, totalTransactions)
                            
                            val groupingResult = performAutoGrouping()
                            
                            val finalMessage = if (groupingResult.success) {
                                "✅ Scan complete! Found $totalTransactions transactions, created ${groupingResult.groupsCreated} groups"
                            } else {
                                "✅ Scan complete! Found $totalTransactions transactions"
                            }
                            
                            showNotification(finalMessage, 100, progress.totalMessages, totalTransactions)
                        } else {
                            showNotification("SMS scan completed! No transactions found", 100, progress.totalMessages, 0)
                        }
                        
                        FirebaseHelper.logDebugInfo("BACKGROUND_SCAN", "Completed successfully: $totalTransactions transactions")
                    }
                } else {
                    showNotification(
                        "Scanning messages... ${progress.currentMessage}/${progress.totalMessages}",
                        currentProgress,
                        progress.currentMessage,
                        totalTransactions
                    )
                }
            }
            
            scanResult
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Background scan error: ${e.message}")
            showNotification("SMS scan error: ${e.message}", 100, 0, 0)
            FirebaseHelper.logException(e)
            Result.failure()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS Scan Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of SMS scanning in background"
            }
            
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(message: String, progress: Int, messagesScanned: Int, transactionsFound: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create detailed notification content
        val detailText = if (progress < 100) {
            "Found $transactionsFound transactions so far"
        } else {
            "Scanned $messagesScanned messages total"
        }
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Transaction Tracker")
            .setContentText(message)
            .setSubText(detailText)
            .setSmallIcon(R.drawable.ic_dashboard)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(progress < 100)
            .apply {
                if (progress < 100) {
                    setProgress(100, progress, false)
                }
            }
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
        
        // Remove notification after completion
        if (progress >= 100) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                notificationManager.cancel(NOTIFICATION_ID)
            }, 3000)
        }
    }
    
    private suspend fun performAutoGrouping(): GroupingResult {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val transactionRepository = TransactionRepository(database)
            val groupRepository = TransactionGroupRepository(database)
            val groupingService = TransactionGroupingService(groupRepository, transactionRepository, database)
            
            // Get initial ungrouped count
            val ungroupedBefore = groupRepository.getUngroupedTransactionCount()
            
            // Perform grouping
            groupingService.autoGroupTransactions()
            
            // Get final counts
            val ungroupedAfter = groupRepository.getUngroupedTransactionCount()
            val groupedCount = ungroupedBefore - ungroupedAfter
            val groupsCreated = groupRepository.getAllActiveGroups().first().size
            
            Log.i(TAG, "✅ Grouping complete: $groupedCount transactions grouped into $groupsCreated groups")
            
            GroupingResult(
                success = true,
                transactionsGrouped = groupedCount,
                groupsCreated = groupsCreated
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Auto-grouping failed: ${e.message}")
            GroupingResult(success = false, transactionsGrouped = 0, groupsCreated = 0)
        }
    }
    
    data class GroupingResult(
        val success: Boolean,
        val transactionsGrouped: Int,
        val groupsCreated: Int
    )
}