package com.pennywiseai.tracker.background

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionGroupRepository
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.grouping.TransactionGroupingService
import java.util.concurrent.TimeUnit

class AutoGroupingWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "AutoGroupingWorker"
        private const val WORK_NAME = "auto_grouping_work"
        
        fun schedulePeriodicGrouping(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // No network needed
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(false)
                .build()
            
            val groupingWork = PeriodicWorkRequestBuilder<AutoGroupingWorker>(
                repeatInterval = 6, // Every 6 hours
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = 2, // 2-hour flex window
                flexTimeIntervalUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing work if already scheduled
                groupingWork
            )
            
        }
        
        fun cancelPeriodicGrouping(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "🤖 Starting automatic transaction grouping...")
            
            val database = AppDatabase.getDatabase(applicationContext)
            val transactionRepository = TransactionRepository(database)
            val groupRepository = TransactionGroupRepository(database)
            val groupingService = TransactionGroupingService(groupRepository, transactionRepository, database)
            
            // Check if there are ungrouped transactions
            val ungroupedCount = groupRepository.getUngroupedTransactionCount()
            
            if (ungroupedCount > 0) {
                // Perform auto-grouping
                groupingService.autoGroupTransactions()
                
                // Get final count
                val remainingUngrouped = groupRepository.getUngroupedTransactionCount()
                val groupedCount = ungroupedCount - remainingUngrouped
                
                Log.i(TAG, "✅ Auto-grouping completed: $groupedCount transactions grouped")
                
                // Return success with statistics
                val outputData = Data.Builder()
                    .putInt("initial_ungrouped", ungroupedCount)
                    .putInt("grouped_count", groupedCount)
                    .putInt("remaining_ungrouped", remainingUngrouped)
                    .build()
                
                Result.success(outputData)
            } else {
                Result.success()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Auto-grouping failed: ${e.message}")
            e.printStackTrace()
            Result.retry() // Retry on failure
        }
    }
}

/**
 * Helper class to manage auto-grouping scheduling
 */
class AutoGroupingScheduler(private val context: Context) {
    
    companion object {
        private const val TAG = "AutoGroupingScheduler"
    }
    
    /**
     * Enable automatic grouping with periodic scheduling
     */
    fun enableAutoGrouping() {
        AutoGroupingWorker.schedulePeriodicGrouping(context)
    }
    
    /**
     * Disable automatic grouping
     */
    fun disableAutoGrouping() {
        AutoGroupingWorker.cancelPeriodicGrouping(context)
    }
    
    /**
     * Trigger immediate grouping (one-time)
     */
    fun triggerImmediateGrouping(): String {
        val immediateWork = androidx.work.OneTimeWorkRequestBuilder<AutoGroupingWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false) // Allow immediate execution
                    .build()
            )
            .build()
        
        WorkManager.getInstance(context).enqueue(immediateWork)
        
        val workId = immediateWork.id.toString()
        return workId
    }
}