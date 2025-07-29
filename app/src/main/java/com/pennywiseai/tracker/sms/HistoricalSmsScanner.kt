package com.pennywiseai.tracker.sms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.pennywiseai.tracker.data.ScanProgress
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionType
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.subscription.SubscriptionDetector
import com.pennywiseai.tracker.service.PatternMatchingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import com.pennywiseai.tracker.logging.LogStreamManager
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import com.pennywiseai.tracker.parser.TransactionExtractor
import com.pennywiseai.tracker.parser.PatternExtractorAdapter

class HistoricalSmsScanner(
    private val context: Context,
    private val repository: TransactionRepository
) {
    
    private val subscriptionDetector = SubscriptionDetector()
    private val patternMatchingService = PatternMatchingService(repository, repository.getGroupRepository())
    private var transactionExtractor: TransactionExtractor? = null
    
    companion object {
        private const val TAG = "HistoricalSmsScanner"
        private const val CHUNK_SIZE = 10 // Process 10 messages at a time
    }
    
    /**
     * Initialize pattern-based extractor
     */
    suspend fun initializeExtractor() {
        transactionExtractor = PatternExtractorAdapter().apply { initialize() }
    }
    
    /**
     * Get current extractor, always using pattern-based
     */
    private suspend fun getExtractor(): TransactionExtractor {
        if (transactionExtractor == null) {
            // Always use pattern-based extractor
            transactionExtractor = PatternExtractorAdapter().apply { initialize() }
        }
        return transactionExtractor!!
    }
    
    fun scanHistoricalSms(daysBack: Int): Flow<ScanProgress> = flow {
        val cutoffDate = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)
        val transactions = mutableListOf<Transaction>()
        
        Log.i(TAG, "🔍 Starting historical SMS scan for last $daysBack days")
        
        // Show the log stream modal
        LogStreamManager.showModal()
        
        try {
            // Check permissions first
            if (!hasPermissions()) {
                Log.e(TAG, "❌ SMS permission not granted")
                emit(ScanProgress(0, 0, 0, true, "SMS permission required"))
                return@flow
            }
            
            
            val cursor = getSmsMessages(cutoffDate)
            if (cursor == null) {
                Log.e(TAG, "❌ Cannot access SMS content provider")
                emit(ScanProgress(0, 0, 0, true, "Cannot access SMS messages"))
                return@flow
            }
            
            cursor.use { c ->
                val totalMessages = c.count
                LogStreamManager.startNewScan(totalMessages)
                emit(ScanProgress(0, totalMessages, 0, false))
                
                if (totalMessages == 0) {
                    Log.w(TAG, "📭 No SMS messages found in the selected timeframe")
                    emit(ScanProgress(0, 0, 0, true, "No SMS messages found in the selected timeframe"))
                    return@flow
                }
                
                // Process messages in chunks to prevent token overflow
                val allMessages = mutableListOf<SmsMessage>()
                
                // First, collect all messages
                while (c.moveToNext()) {
                    // Check for cancellation
                    if (LogStreamManager.checkCancellation()) {
                        emit(ScanProgress(0, totalMessages, 0, true, "Cancelled by user"))
                        return@flow
                    }
                    
                    try {
                        val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
                        val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
                        val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)
                        
                        if (bodyIndex == -1 || addressIndex == -1 || dateIndex == -1) {
                            continue
                        }
                        
                        val body = c.getString(bodyIndex) ?: continue
                        val address = c.getString(addressIndex) ?: continue
                        val date = c.getLong(dateIndex)
                        
                        allMessages.add(SmsMessage(body, address, date))
                    } catch (e: Exception) {
                        // Continue collecting even if one message fails
                        continue
                    }
                }
                
                LogStreamManager.log(
                    LogStreamManager.LogCategory.GENERAL,
                    "📦 Processing ${allMessages.size} messages in chunks of $CHUNK_SIZE"
                )
                
                // Process in chunks
                var currentMessage = 0
                var transactionsFound = 0
                
                val totalChunks = (allMessages.size + CHUNK_SIZE - 1) / CHUNK_SIZE
                for ((chunkIndex, chunk) in allMessages.chunked(CHUNK_SIZE).withIndex()) {
                    // Check for cancellation before processing each chunk
                    if (LogStreamManager.checkCancellation()) {
                        LogStreamManager.log(
                            LogStreamManager.LogCategory.GENERAL,
                            "❌ Processing cancelled at chunk ${chunkIndex + 1}/$totalChunks"
                        )
                        emit(ScanProgress(currentMessage, totalMessages, transactionsFound, true, "Cancelled by user"))
                        return@flow
                    }
                    
                    val chunkNumber = chunkIndex + 1
                    LogStreamManager.log(
                        LogStreamManager.LogCategory.CHUNK_PROCESSING,
                        "🔄 Processing chunk $chunkNumber/$totalChunks (${chunk.size} messages)"
                    )
                    
                    try {
                        val chunkTransactions = processChunk(chunk)
                        
                        // Transactions are now saved immediately in processChunk
                        transactions.addAll(chunkTransactions)
                        transactionsFound += chunkTransactions.size
                        
                        LogStreamManager.completeCurrentChunk(chunkNumber, totalChunks, chunkTransactions.size)
                        
                        // Update progress
                        currentMessage += chunk.size
                        // Also update LogStreamManager stats to keep in sync
                        LogStreamManager.updateStats { stats ->
                            stats.copy(
                                messagesProcessed = currentMessage,
                                transactionsFound = transactionsFound
                            )
                        }
                        emit(ScanProgress(currentMessage, totalMessages, transactionsFound, false))
                        
                        // Small delay between chunks to prevent overwhelming processing
                        kotlinx.coroutines.delay(100)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error processing chunk: ${e.message}")
                        // Continue with next chunk even if one fails
                        currentMessage += chunk.size
                        // Also update LogStreamManager stats to keep in sync
                        LogStreamManager.updateStats { stats ->
                            stats.copy(
                                messagesProcessed = currentMessage,
                                transactionsFound = transactionsFound
                            )
                        }
                        emit(ScanProgress(currentMessage, totalMessages, transactionsFound, false))
                    }
                }
                
                // Subscriptions are now created immediately during transaction processing
                if (transactions.isNotEmpty()) {
                    Log.i(TAG, "✅ Processing completed: ${transactions.size} transactions found")
                } else {
                    Log.w(TAG, "📭 No valid transactions found")
                }
                
                Log.i(TAG, "🎉 Scan completed: $transactionsFound transactions found from $totalMessages messages")
                LogStreamManager.scanCompleted()
                emit(ScanProgress(totalMessages, totalMessages, transactionsFound, true))
            }
            
        } catch (e: SecurityException) {
            val error = "Permission denied: ${e.message}"
            LogStreamManager.scanError(error)
            emit(ScanProgress(0, 0, 0, true, error))
        } catch (e: Exception) {
            val error = "Scan failed: ${e.message}"
            LogStreamManager.scanError(error)
            emit(ScanProgress(0, 0, 0, true, error))
        }
    }.flowOn(Dispatchers.IO)
    
    private data class SmsMessage(
        val body: String,
        val address: String,
        val date: Long
    )
    
    private suspend fun processChunk(chunk: List<SmsMessage>): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        
        for (smsMessage in chunk) {
            // Check for cancellation before processing each message
            if (LogStreamManager.checkCancellation()) {
                break // Exit the loop but return transactions found so far
            }
            
            try {
                // Log start of processing
                LogStreamManager.log(
                    LogStreamManager.LogCategory.SMS_PROCESSING,
                    "Processing SMS from ${smsMessage.address}",
                    LogStreamManager.LogLevel.DEBUG,
                    "Preview: ${smsMessage.body.take(50)}..."
                )
                
                val extractor = getExtractor()
                val transaction = extractor.extractTransaction(smsMessage.body, smsMessage.address, smsMessage.date)
                if (transaction != null) {
                    // Save transaction immediately to database
                    repository.insertTransaction(transaction)
                    
                    // Apply patterns to the transaction
                    patternMatchingService.applyPatternsToTransaction(transaction)
                    
                    transactions.add(transaction)
                    LogStreamManager.messageProcessed(smsMessage.address, true, transaction.merchant, transaction.amount)
                    LogStreamManager.databaseOperation("Saved transaction", 1)
                    
                    // Create or update subscription immediately if it's a subscription transaction
                    if (transaction.transactionType == TransactionType.SUBSCRIPTION || 
                        transaction.category == TransactionCategory.SUBSCRIPTION) {
                        createSubscriptionFromTransaction(transaction)
                    }
                    
                    // Transaction extracted successfully
                } else {
                    LogStreamManager.messageProcessed(smsMessage.address, false)
                    // Transaction extracted successfully
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error processing SMS: ${e.message}")
                LogStreamManager.log(
                    LogStreamManager.LogCategory.SMS_PROCESSING,
                    "Error processing SMS: ${e.message}",
                    LogStreamManager.LogLevel.ERROR
                )
                // Continue processing other messages in the chunk
                continue
            }
        }
        
        return transactions
    }
    
    private fun getSmsMessages(cutoffDate: Long): Cursor? {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        
        val selection = "${Telephony.Sms.TYPE} = ? AND ${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(
            Telephony.Sms.MESSAGE_TYPE_INBOX.toString(),
            cutoffDate.toString()
        )
        val sortOrder = "${Telephony.Sms.DATE} DESC"
        
        return try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
        } catch (e: SecurityException) {
            null
        }
    }
    
    suspend fun rescanWithNewTimeframe(newDaysBack: Int): Flow<ScanProgress> {
        // Clear existing transactions first (optional - could keep and just add new ones)
        // For now, we'll just scan new timeframe and let duplicates be handled by unique IDs
        return scanHistoricalSms(newDaysBack)
    }
    
    fun hasPermissions(): Boolean {
        return try {
            // Test query to check permissions
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT 1"
            )?.use { true } ?: false
        } catch (e: SecurityException) {
            false
        }
    }
    
    private suspend fun detectAndCreateSubscriptions(newTransactions: List<Transaction>) {
        try {
            
            // For immediate subscription creation, check if any transaction is marked as SUBSCRIPTION type or category
            val subscriptionTransactions = newTransactions.filter { 
                it.transactionType == TransactionType.SUBSCRIPTION || 
                it.category == TransactionCategory.SUBSCRIPTION
            }
            
            if (subscriptionTransactions.isNotEmpty()) {
                
                for (transaction in subscriptionTransactions) {
                    createSubscriptionFromTransaction(transaction)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error detecting subscriptions: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private suspend fun createSubscriptionFromTransaction(transaction: Transaction) {
        try {
            // Check if subscription already exists for this merchant
            val existing = repository.getSubscriptionByMerchantSync(transaction.merchant)
            if (existing != null) {
                
                // Add this transaction to existing subscription
                val updatedTransactionIds = existing.transactionIds + transaction.id
                val newPaymentCount = existing.paymentCount + 1
                val newTotalPaid = existing.totalPaid + transaction.amount
                val newAverageAmount = newTotalPaid / newPaymentCount
                
                val updatedSubscription = existing.copy(
                    transactionIds = updatedTransactionIds,
                    lastPaymentDate = transaction.date,
                    lastAmountPaid = transaction.amount,
                    paymentCount = newPaymentCount,
                    totalPaid = newTotalPaid,
                    averageAmount = newAverageAmount,
                    // Update next payment date based on frequency
                    nextPaymentDate = transaction.date + (existing.frequency.days * 24 * 60 * 60 * 1000L),
                    // Update amount to average if significant difference
                    amount = if (kotlin.math.abs(existing.amount - newAverageAmount) > (existing.amount * 0.1)) {
                        newAverageAmount
                    } else {
                        existing.amount
                    }
                )
                
                repository.updateSubscription(updatedSubscription)
                return
            }
            
            // Create a new subscription from this transaction
            val subscription = com.pennywiseai.tracker.data.Subscription(
                id = java.util.UUID.randomUUID().toString(),
                merchantName = transaction.merchant,
                amount = transaction.amount,
                frequency = com.pennywiseai.tracker.data.SubscriptionFrequency.MONTHLY, // Default to monthly
                nextPaymentDate = transaction.date + (30 * 24 * 60 * 60 * 1000L), // +30 days
                lastPaymentDate = transaction.date,
                active = true,
                transactionIds = listOf(transaction.id),
                startDate = transaction.date,
                status = com.pennywiseai.tracker.data.SubscriptionStatus.ACTIVE,
                category = transaction.category,
                paymentCount = 1,
                totalPaid = transaction.amount,
                lastAmountPaid = transaction.amount,
                averageAmount = transaction.amount
            )
            
            repository.insertSubscription(subscription)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating subscription from transaction: ${e.message}")
            e.printStackTrace()
        }
    }
}