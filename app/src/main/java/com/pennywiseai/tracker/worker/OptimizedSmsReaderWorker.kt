package com.pennywiseai.tracker.worker

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.parser.core.bank.BankParserFactory
import com.pennywiseai.parser.core.bank.HDFCBankParser
import com.pennywiseai.parser.core.bank.IndianBankParser
import com.pennywiseai.parser.core.bank.SBIBankParser
import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.tracker.data.mapper.toEntity
import com.pennywiseai.tracker.data.mapper.toEntityType
import com.pennywiseai.tracker.data.repository.AccountBalanceRepository
import com.pennywiseai.tracker.data.repository.CardRepository
import com.pennywiseai.tracker.data.repository.LlmRepository
import com.pennywiseai.tracker.data.repository.MerchantMappingRepository
import com.pennywiseai.tracker.data.repository.SubscriptionRepository
import com.pennywiseai.tracker.data.repository.TransactionRepository
import com.pennywiseai.tracker.data.repository.UnrecognizedSmsRepository
import com.pennywiseai.tracker.domain.repository.RuleRepository
import com.pennywiseai.tracker.domain.service.RuleEngine
import com.pennywiseai.tracker.data.database.entity.AccountBalanceEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.database.entity.UnrecognizedSmsEntity
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.system.measureTimeMillis

/**
 * Optimized SMS Worker with parallel processing and progress tracking.
 * This worker provides significant performance improvements through:
 * 1. Parallel processing of SMS messages
 * 2. Progress reporting with estimated time completion
 * 3. Optimized database operations
 * 4. Efficient memory usage
 */
@HiltWorker
class OptimizedSmsReaderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val cardRepository: CardRepository,
    private val llmRepository: LlmRepository,
    private val merchantMappingRepository: MerchantMappingRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val unrecognizedSmsRepository: UnrecognizedSmsRepository,
    private val ruleRepository: RuleRepository,
    private val ruleEngine: RuleEngine
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "OptimizedSmsReaderWorker"
        const val WORK_NAME = "optimized_sms_reader_work"

        // Progress keys
        const val PROGRESS_TOTAL = "progress_total"
        const val PROGRESS_PROCESSED = "progress_processed"
        const val PROGRESS_PARSED = "progress_parsed"
        const val PROGRESS_SAVED = "progress_saved"
        const val PROGRESS_TIME_ELAPSED = "progress_time_elapsed"
        const val PROGRESS_ESTIMATED_TIME_REMAINING = "progress_estimated_time_remaining"
        const val PROGRESS_CURRENT_BATCH = "progress_current_batch"
        const val PROGRESS_TOTAL_BATCHES = "progress_total_batches"

        // SMS Content Provider columns
        private val SMS_PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.BODY,
            Telephony.Sms.TYPE
        )

        // Parallel processing configuration
        private const val BATCH_SIZE = 50 // Process 50 SMS messages at a time
        private const val PARALLELISM = 4 // Number of parallel processing coroutines
        private const val PROGRESS_REPORT_INTERVAL = 10 // Report progress every 10 messages
    }

    data class ProcessingStats(
        var totalMessages: Int = 0,
        var processedMessages: Int = 0,
        var parsedTransactions: Int = 0,
        var savedTransactions: Int = 0,
        var subscriptionCount: Int = 0,
        var startTime: Long = System.currentTimeMillis(),
        var messagesPerSecond: Double = 0.0
    ) {
        fun updateTimeElapsed(): Long = System.currentTimeMillis() - startTime

        fun updateMessagesPerSecond() {
            val elapsedSeconds = updateTimeElapsed() / 1000.0
            messagesPerSecond = if (elapsedSeconds > 0) processedMessages / elapsedSeconds else 0.0
        }

        fun getEstimatedTimeRemaining(): Long {
            return if (messagesPerSecond > 0 && processedMessages > 0) {
                val remainingMessages = totalMessages - processedMessages
                (remainingMessages / messagesPerSecond * 1000).toLong()
            } else 0L
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting optimized SMS reading and parsing work...")

            val stats = ProcessingStats()

            // Read SMS messages
            val messages = readSmsMessages()
            stats.totalMessages = messages.size
            Log.d(TAG, "Found ${messages.size} SMS messages to process")

            // Debug: Log first 20 unique senders to understand what we're working with
            val uniqueSenders = messages.take(50).map { it.sender }.distinct()
            Log.d(TAG, "First 20 unique senders: ${uniqueSenders.joinToString(", ")}")

            // Debug: Check for any FAB-related senders
            val fabSenders = messages.map { it.sender }.filter {
                it.uppercase().contains("FAB") ||
                it.uppercase().contains("ABU DHABI") ||
                it.uppercase().contains("FIRST ABU DHABI")
            }.distinct()
            Log.d(TAG, "Found FAB-related senders: ${fabSenders.joinToString(", ")}")

            // Report initial progress
            setProgress(
                workDataOf(
                    PROGRESS_TOTAL to messages.size,
                    PROGRESS_PROCESSED to 0,
                    PROGRESS_PARSED to 0,
                    PROGRESS_SAVED to 0,
                    PROGRESS_TIME_ELAPSED to 0L,
                    PROGRESS_ESTIMATED_TIME_REMAINING to 0L,
                    PROGRESS_CURRENT_BATCH to 1,
                    PROGRESS_TOTAL_BATCHES to (messages.size + BATCH_SIZE - 1) / BATCH_SIZE
                )
            )

            // Process messages in parallel for maximum speed
            val processingTime = measureTimeMillis {
                processMessagesInParallel(messages, stats)
            }

            stats.updateTimeElapsed()
            stats.updateMessagesPerSecond()

            Log.d(TAG, """
                SMS parsing completed in ${processingTime}ms:
                - Total Messages: ${stats.totalMessages}
                - Processed: ${stats.processedMessages}
                - Parsed Transactions: ${stats.parsedTransactions}
                - Saved Transactions: ${stats.savedTransactions}
                - Subscriptions: ${stats.subscriptionCount}
                - Processing Speed: ${"%.2f".format(stats.messagesPerSecond)} msg/sec
            """.trimIndent())

            // Clean up old unrecognized SMS entries
            try {
                unrecognizedSmsRepository.cleanupOldEntries()
                Log.d(TAG, "Cleaned up old unrecognized SMS entries")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up unrecognized SMS: ${e.message}")
            }

            // Update system prompt with new financial data if any transactions were saved
            if (stats.savedTransactions > 0) {
                try {
                    llmRepository.updateSystemPrompt()
                    Log.d(TAG, "Updated system prompt with latest financial data")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating system prompt: ${e.message}")
                }
            }

            // Report final progress
            setProgress(
                workDataOf(
                    PROGRESS_TOTAL to messages.size,
                    PROGRESS_PROCESSED to messages.size,
                    PROGRESS_PARSED to stats.parsedTransactions,
                    PROGRESS_SAVED to stats.savedTransactions,
                    PROGRESS_TIME_ELAPSED to stats.updateTimeElapsed(),
                    PROGRESS_ESTIMATED_TIME_REMAINING to 0L,
                    PROGRESS_CURRENT_BATCH to (messages.size + BATCH_SIZE - 1) / BATCH_SIZE,
                    PROGRESS_TOTAL_BATCHES to (messages.size + BATCH_SIZE - 1) / BATCH_SIZE
                )
            )

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in optimized SMS parsing work", e)
            Result.failure()
        }
    }

    private suspend fun processMessagesSequentially(messages: List<SmsMessage>, stats: ProcessingStats) {
        val totalBatches = (messages.size + BATCH_SIZE - 1) / BATCH_SIZE

        messages.chunked(BATCH_SIZE).forEachIndexed { batchIndex, batch ->
            var parsedCount = 0
            var savedCount = 0
            var subscriptionCount = 0

            for (sms in batch) {
                try {
                    // Skip promotional (-P) and government (-G) messages
                    val senderUpper = sms.sender.uppercase()
                    if (senderUpper.endsWith("-P") || senderUpper.endsWith("-G")) {
                        Log.d(TAG, "Skipping promotional/government SMS from: ${sms.sender}")
                        continue
                    }

                    val parser = BankParserFactory.getParser(sms.sender)
                    if (parser != null) {
                        Log.d(TAG, "Found parser: ${parser.getBankName()} for sender: ${sms.sender}")
                        // Check if this is a subscription notification first
                        val smsDateTime = java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(sms.timestamp),
                            java.time.ZoneId.systemDefault()
                        )
                        val thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30)
                        val isRecentMessage = smsDateTime.isAfter(thirtyDaysAgo)
                        val subscriptionResult = processSubscriptionNotifications(parser, sms, smsDateTime, isRecentMessage)

                        if (subscriptionResult != null) {
                            subscriptionCount += subscriptionResult
                        } else {
                            // Parse as regular transaction
                            val parsedTransaction = parser.parse(sms.body, sms.sender, sms.timestamp)
                            if (parsedTransaction != null) {
                                parsedCount++
                                Log.d(TAG, """
                                    Parsed Transaction:
                                    Bank: ${parsedTransaction.bankName}
                                    Amount: ${parsedTransaction.amount}
                                    Type: ${parsedTransaction.type}
                                    Merchant: ${parsedTransaction.merchant}
                                    Reference: ${parsedTransaction.reference}
                                    Account: ${parsedTransaction.accountLast4}
                                    Balance: ${parsedTransaction.balance}
                                    Credit Limit: ${parsedTransaction.creditLimit}
                                    ID: ${parsedTransaction.generateTransactionId()}
                                """.trimIndent())

                                // Save transaction to database
                                val success = saveParsedTransaction(parsedTransaction, sms)
                                if (success) {
                                    savedCount++
                                    Log.d(TAG, "Saved transaction successfully")
                                }
                            } else {
                                // Log some sample unparsed messages for debugging
                                if (batch.indexOf(sms) < 3) { // Only log first 3 to avoid spam
                                    Log.d(TAG, "Failed to parse SMS from ${sms.sender}: ${sms.body.take(100)}...")
                                }
                            }
                        }
                    } else {
                        // Check if it's from a potential financial provider (-T or -S suffix)
                        val upperSender = sms.sender.uppercase()
                        if (upperSender.endsWith("-T") || upperSender.endsWith("-S")) {
                            processUnrecognizedSms(sms)
                        } else {
                            // Log ALL unrecognized senders for debugging (but limit first batch)
                            if (batch.indexOf(sms) < 10) { // Log first 10 of each batch
                                Log.d(TAG, "No parser found for sender: ${sms.sender}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing SMS from ${sms.sender}: ${e.message}")
                }
            }

            // Update stats
            stats.processedMessages += batch.size
            stats.parsedTransactions += parsedCount
            stats.savedTransactions += savedCount
            stats.subscriptionCount += subscriptionCount

            // Update progress
            stats.updateMessagesPerSecond()
            if (stats.processedMessages % PROGRESS_REPORT_INTERVAL == 0 || stats.processedMessages == stats.totalMessages) {
                try {
                    setProgress(
                        workDataOf(
                            PROGRESS_TOTAL to stats.totalMessages,
                            PROGRESS_PROCESSED to stats.processedMessages,
                            PROGRESS_PARSED to stats.parsedTransactions,
                            PROGRESS_SAVED to stats.savedTransactions,
                            PROGRESS_TIME_ELAPSED to stats.updateTimeElapsed(),
                            PROGRESS_ESTIMATED_TIME_REMAINING to stats.getEstimatedTimeRemaining(),
                            PROGRESS_CURRENT_BATCH to batchIndex + 1,
                            PROGRESS_TOTAL_BATCHES to totalBatches
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting progress: ${e.message}")
                }
            }
        }
    }

    private suspend fun processMessagesInParallel(messages: List<SmsMessage>, stats: ProcessingStats) = coroutineScope {
        val totalBatches = (messages.size + BATCH_SIZE - 1) / BATCH_SIZE

        // Use atomic counters for real-time progress tracking
        val atomicProcessed = java.util.concurrent.atomic.AtomicInteger(0)
        val atomicParsed = java.util.concurrent.atomic.AtomicInteger(0)
        val atomicSaved = java.util.concurrent.atomic.AtomicInteger(0)

        // Create parallel processing coroutines that return results directly
        val processingJobs = (1..PARALLELISM).map { coroutineId ->
            async(Dispatchers.IO) {
                processBatchCoroutinesDirect(messages, coroutineId, totalBatches, stats, atomicProcessed, atomicParsed, atomicSaved)
            }
        }

        // Create progress monitoring coroutine that reads atomic counters
        val progressJob = launch(Dispatchers.IO) {
            var lastReportedProcessed = 0

            while (atomicProcessed.get() < stats.totalMessages) {
                val currentProcessed = atomicProcessed.get()
                val currentParsed = atomicParsed.get()
                val currentSaved = atomicSaved.get()

                // Update stats and report progress every few messages or time interval
                if (currentProcessed - lastReportedProcessed >= PROGRESS_REPORT_INTERVAL ||
                    currentProcessed >= stats.totalMessages) {

                    stats.processedMessages = currentProcessed
                    stats.parsedTransactions = currentParsed
                    stats.savedTransactions = currentSaved
                    stats.updateMessagesPerSecond()

                    setProgress(
                        workDataOf(
                            PROGRESS_TOTAL to stats.totalMessages,
                            PROGRESS_PROCESSED to currentProcessed,
                            PROGRESS_PARSED to currentParsed,
                            PROGRESS_SAVED to currentSaved,
                            PROGRESS_TIME_ELAPSED to stats.updateTimeElapsed(),
                            PROGRESS_ESTIMATED_TIME_REMAINING to stats.getEstimatedTimeRemaining(),
                            PROGRESS_CURRENT_BATCH to (currentProcessed + BATCH_SIZE - 1) / BATCH_SIZE,
                            PROGRESS_TOTAL_BATCHES to totalBatches
                        )
                    )

                    lastReportedProcessed = currentProcessed
                }

                delay(50) // Check every 50ms for smooth updates
            }
        }

        // Wait for all jobs to complete
        val results = processingJobs.awaitAll()
        progressJob.cancel()

        // Aggregate final results
        results.forEach { result ->
            stats.parsedTransactions += result.parsedCount
            stats.savedTransactions += result.savedCount
            stats.subscriptionCount += result.subscriptionCount
        }

        // Final progress update
        stats.processedMessages = stats.totalMessages
        stats.updateMessagesPerSecond()
        setProgress(
            workDataOf(
                PROGRESS_TOTAL to stats.totalMessages,
                PROGRESS_PROCESSED to stats.processedMessages,
                PROGRESS_PARSED to stats.parsedTransactions,
                PROGRESS_SAVED to stats.savedTransactions,
                PROGRESS_TIME_ELAPSED to stats.updateTimeElapsed(),
                PROGRESS_ESTIMATED_TIME_REMAINING to 0L,
                PROGRESS_CURRENT_BATCH to totalBatches,
                PROGRESS_TOTAL_BATCHES to totalBatches
            )
        )
    }

    private suspend fun processBatchCoroutines(
        messages: List<SmsMessage>,
        coroutineId: Int,
        totalBatches: Int,
        stats: ProcessingStats,
        resultsChannel: Channel<ProcessingResult>
    ) {
        val batchSize = (messages.size + PARALLELISM - 1) / PARALLELISM
        val startIndex = (coroutineId - 1) * batchSize
        val endIndex = minOf(startIndex + batchSize, messages.size)

        if (startIndex >= messages.size) return

        val assignedMessages = messages.subList(startIndex, endIndex)

        // Process assigned messages in smaller chunks
        for (i in assignedMessages.indices step BATCH_SIZE) {
            val chunkEnd = minOf(i + BATCH_SIZE, assignedMessages.size)
            val chunk = assignedMessages.subList(i, chunkEnd)

            val result = processMessageChunk(chunk, coroutineId, i / BATCH_SIZE + 1)
            resultsChannel.send(result)
        }
    }

    private suspend fun processMessageChunk(messages: List<SmsMessage>, coroutineId: Int, batchNumber: Int): ProcessingResult {
        var parsedCount = 0
        var savedCount = 0
        var subscriptionCount = 0

        for (sms in messages) {
            try {
                // Skip promotional (-P) and government (-G) messages
                val senderUpper = sms.sender.uppercase()
                if (senderUpper.endsWith("-P") || senderUpper.endsWith("-G")) {
                    continue
                }

                // Check if sender is from a known bank
                val parser = BankParserFactory.getParser(sms.sender)
                if (parser != null) {
                    // Calculate SMS age for subscription filtering
                    val smsDateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(sms.timestamp),
                        ZoneId.systemDefault()
                    )
                    val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
                    val isRecentMessage = smsDateTime.isAfter(thirtyDaysAgo)

                    // Process subscription notifications
                    val subscriptionResult = processSubscriptionNotifications(parser, sms, smsDateTime, isRecentMessage)
                    if (subscriptionResult != null) {
                        subscriptionCount += subscriptionResult
                        continue
                    }

                    // Parse the transaction
                    val parsedTransaction = parser.parse(sms.body, sms.sender, sms.timestamp)

                    if (parsedTransaction != null) {
                        parsedCount++
                        val saveResult = saveParsedTransaction(parsedTransaction, sms)
                        if (saveResult) savedCount++
                    }
                } else {
                    // Handle unrecognized SMS
                    processUnrecognizedSms(sms)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS from ${sms.sender}: ${e.message}")
            }
        }

        return ProcessingResult(
            processedCount = messages.size,
            parsedCount = parsedCount,
            savedCount = savedCount,
            subscriptionCount = subscriptionCount,
            coroutineId = coroutineId,
            batchNumber = batchNumber
        )
    }

    private suspend fun processBatchCoroutinesDirect(
        messages: List<SmsMessage>,
        coroutineId: Int,
        totalBatches: Int,
        stats: ProcessingStats,
        atomicProcessed: java.util.concurrent.atomic.AtomicInteger,
        atomicParsed: java.util.concurrent.atomic.AtomicInteger,
        atomicSaved: java.util.concurrent.atomic.AtomicInteger
    ): ProcessingResult {
        var parsedCount = 0
        var savedCount = 0
        var subscriptionCount = 0

        // Calculate batch assignments for this coroutine
        val batchesPerCoroutine = (totalBatches + PARALLELISM - 1) / PARALLELISM
        val startBatch = (coroutineId - 1) * batchesPerCoroutine
        val endBatch = minOf(startBatch + batchesPerCoroutine, totalBatches)

        // Process assigned batches
        for (batchIndex in startBatch until endBatch) {
            val startIndex = batchIndex * BATCH_SIZE
            val endIndex = minOf(startIndex + BATCH_SIZE, messages.size)
            val batch = messages.subList(startIndex, endIndex)

            for (sms in batch) {
                try {
                    // Update processed count immediately for real-time progress
                    atomicProcessed.incrementAndGet()

                    // Skip promotional (-P) and government (-G) messages
                    val senderUpper = sms.sender.uppercase()
                    if (senderUpper.endsWith("-P") || senderUpper.endsWith("-G")) {
                        continue
                    }

                    val parser = BankParserFactory.getParser(sms.sender)
                    if (parser != null) {
                        // Check if this is a subscription notification first
                        val smsDateTime = java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(sms.timestamp),
                            java.time.ZoneId.systemDefault()
                        )
                        val thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30)
                        val isRecentMessage = smsDateTime.isAfter(thirtyDaysAgo)
                        val subscriptionResult = processSubscriptionNotifications(parser, sms, smsDateTime, isRecentMessage)

                        if (subscriptionResult != null) {
                            subscriptionCount += subscriptionResult
                        } else {
                            // Parse as regular transaction
                            val parsedTransaction = parser.parse(sms.body, sms.sender, sms.timestamp)
                            if (parsedTransaction != null) {
                                parsedCount++
                                atomicParsed.incrementAndGet()
                                // Save transaction to database
                                val success = saveParsedTransaction(parsedTransaction, sms)
                                if (success) {
                                    savedCount++
                                    atomicSaved.incrementAndGet()
                                }
                            }
                        }
                    } else {
                        // Handle unrecognized SMS
                        processUnrecognizedSms(sms)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing SMS from ${sms.sender}: ${e.message}")
                }
            }
        }

        return ProcessingResult(
            processedCount = (endBatch - startBatch) * BATCH_SIZE, // Approximate
            parsedCount = parsedCount,
            savedCount = savedCount,
            subscriptionCount = subscriptionCount,
            coroutineId = coroutineId,
            batchNumber = startBatch
        )
    }

    private suspend fun processSubscriptionNotifications(
        parser: com.pennywiseai.parser.core.bank.BankParser,
        sms: SmsMessage,
        smsDateTime: LocalDateTime,
        isRecentMessage: Boolean
    ): Int? {
        return when (parser) {
            is SBIBankParser -> {
                if (parser.isUPIMandateNotification(sms.body)) {
                    if (!isRecentMessage) return null

                    val upiMandateInfo = parser.parseUPIMandateSubscription(sms.body)
                    if (upiMandateInfo != null) {
                        try {
                            subscriptionRepository.createOrUpdateFromSBIMandate(
                                upiMandateInfo,
                                parser.getBankName(),
                                sms.body
                            )
                            return 1
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving SBI UPI-Mandate subscription: ${e.message}")
                        }
                    }
                }
                null
            }
            is HDFCBankParser -> {
                var subscriptionCount = 0

                // Check for E-Mandate notifications
                if (parser.isEMandateNotification(sms.body)) {
                    if (!isRecentMessage) return null

                    val eMandateInfo = parser.parseEMandateSubscription(sms.body)
                    if (eMandateInfo != null) {
                        try {
                            subscriptionRepository.createOrUpdateFromEMandate(
                                eMandateInfo,
                                parser.getBankName(),
                                sms.body
                            )
                            subscriptionCount++
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving HDFC E-Mandate subscription: ${e.message}")
                        }
                    }
                }

                // Check for Future Debit notifications
                if (parser.isFutureDebitNotification(sms.body)) {
                    if (!isRecentMessage) return null

                    val futureDebitInfo = parser.parseFutureDebit(sms.body)
                    if (futureDebitInfo != null) {
                        try {
                            subscriptionRepository.createOrUpdateFromEMandate(
                                futureDebitInfo,
                                parser.getBankName(),
                                sms.body
                            )
                            subscriptionCount++
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving HDFC future debit subscription: ${e.message}")
                        }
                    }
                }

                // Check for Balance Update notifications
                if (parser.isBalanceUpdateNotification(sms.body)) {
                    val balanceUpdateInfo = parser.parseBalanceUpdate(sms.body)
                    if (balanceUpdateInfo != null) {
                        try {
                            accountBalanceRepository.insertBalanceUpdate(
                                bankName = balanceUpdateInfo.bankName,
                                accountLast4 = balanceUpdateInfo.accountLast4,
                                balance = balanceUpdateInfo.balance,
                                timestamp = balanceUpdateInfo.asOfDate ?: smsDateTime,
                                currency = parser.getCurrency()
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving balance update: ${e.message}")
                        }
                    }
                }

                if (subscriptionCount > 0) subscriptionCount else null
            }
            is IndianBankParser -> {
                if (parser.isMandateNotification(sms.body)) {
                    if (!isRecentMessage) return null

                    val mandateInfo = parser.parseMandateSubscription(sms.body)
                    if (mandateInfo != null) {
                        try {
                            subscriptionRepository.createOrUpdateFromIndianBankMandate(
                                mandateInfo,
                                parser.getBankName(),
                                sms.body
                            )
                            return 1
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving Indian Bank subscription: ${e.message}")
                        }
                    }
                }
                null
            }
            else -> null
        }
    }

    private suspend fun saveParsedTransaction(parsedTransaction: ParsedTransaction, sms: SmsMessage): Boolean {
        return try {
            // Convert to entity and save
            val entity = parsedTransaction.toEntity()

            // Check if this transaction was previously deleted by the user
            val existingTransaction = transactionRepository.getTransactionByHash(entity.transactionHash)
            if (existingTransaction != null) {
                if (existingTransaction.isDeleted) {
                    return false
                }
                return false // Transaction already exists
            }

            // Check for custom merchant mapping
            val customCategory = merchantMappingRepository.getCategoryForMerchant(entity.merchantName)
            val entityWithMapping = if (customCategory != null) {
                entity.copy(category = customCategory)
            } else {
                entity
            }

            // Apply rule engine to the transaction
            val activeRules = ruleRepository.getActiveRulesByType(entityWithMapping.transactionType)
            val (entityWithRules, ruleApplications) = ruleEngine.evaluateRules(
                entityWithMapping,
                sms.body,
                activeRules
            )

            // Check if this transaction matches an active subscription
            val matchedSubscription = subscriptionRepository.matchTransactionToSubscription(
                entityWithRules.merchantName,
                entityWithRules.amount
            )

            val finalEntity = if (matchedSubscription != null) {
                subscriptionRepository.updateNextPaymentDateAfterCharge(
                    matchedSubscription.id,
                    entityWithRules.dateTime.toLocalDate()
                )
                entityWithRules.copy(isRecurring = true)
            } else {
                entityWithRules
            }

            val rowId = transactionRepository.insertTransaction(finalEntity)
            if (rowId != -1L) {
                // Save rule applications if any rules were applied
                if (ruleApplications.isNotEmpty()) {
                    ruleRepository.saveRuleApplications(ruleApplications)
                }

                // Process balance updates
                processBalanceUpdate(parsedTransaction, finalEntity, rowId)
                return true
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction: ${e.message}")
            false
        }
    }

    private suspend fun processBalanceUpdate(parsedTransaction: ParsedTransaction, entity: com.pennywiseai.tracker.data.database.entity.TransactionEntity, rowId: Long) {
        if (parsedTransaction.accountLast4 != null) {
            // Determine if this transaction is from a card
            val isFromCard = parsedTransaction.isFromCard

            val targetAccountLast4 = if (isFromCard) {
                var card = parsedTransaction.accountLast4?.let {
                    cardRepository.getCard(parsedTransaction.bankName, it)
                }

                if (card == null) {
                    val isCredit = (parsedTransaction.type.toEntityType() == TransactionType.CREDIT)
                    card = parsedTransaction.accountLast4?.let { accountLast4 ->
                        cardRepository.findOrCreateCard(
                            cardLast4 = accountLast4,
                            bankName = parsedTransaction.bankName,
                            isCredit = isCredit
                        )
                    }
                    card = parsedTransaction.accountLast4?.let {
                        cardRepository.getCard(parsedTransaction.bankName, it)
                    }
                }

                cardRepository.updateCardBalance(
                    cardId = card!!.id,
                    balance = parsedTransaction.balance,
                    source = parsedTransaction.smsBody.take(200),
                    date = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(parsedTransaction.timestamp),
                        ZoneId.systemDefault()
                    )
                )

                when {
                    card.cardType == com.pennywiseai.tracker.data.database.entity.CardType.CREDIT -> {
                        parsedTransaction.accountLast4
                    }
                    card.cardType == com.pennywiseai.tracker.data.database.entity.CardType.DEBIT && card.accountLast4 != null -> {
                        card.accountLast4
                    }
                    else -> null
                }
            } else {
                parsedTransaction.accountLast4
            }

            // Create balance entry if we have a target account
            if (targetAccountLast4 != null) {
                val isCreditCard = (parsedTransaction.type.toEntityType() == TransactionType.CREDIT) ||
                    parsedTransaction.accountLast4?.let {
                        cardRepository.getCard(parsedTransaction.bankName, it)?.cardType
                    } == com.pennywiseai.tracker.data.database.entity.CardType.CREDIT

                val existingAccount = accountBalanceRepository.getLatestBalance(
                    parsedTransaction.bankName,
                    targetAccountLast4
                )

                val newBalance = when {
                    isCreditCard -> {
                        val currentBalance = existingAccount?.balance ?: BigDecimal.ZERO
                        currentBalance + parsedTransaction.amount
                    }
                    existingAccount?.isCreditCard == true && parsedTransaction.type.toEntityType() == TransactionType.INCOME -> {
                        val currentBalance = existingAccount.balance ?: BigDecimal.ZERO
                        (currentBalance - parsedTransaction.amount).max(BigDecimal.ZERO)
                    }
                    parsedTransaction.balance != null -> {
                        parsedTransaction.balance!!
                    }
                    else -> {
                        existingAccount?.balance ?: BigDecimal.ZERO
                    }
                }

                val shouldSaveBalance = when {
                    parsedTransaction.balance != null -> true
                    parsedTransaction.creditLimit != null -> true
                    newBalance != BigDecimal.ZERO -> true
                    existingAccount != null -> true
                    else -> false
                }

                if (shouldSaveBalance) {
                    val balanceEntity = AccountBalanceEntity(
                        bankName = parsedTransaction.bankName,
                        accountLast4 = targetAccountLast4,
                        balance = newBalance,
                        timestamp = entity.dateTime,
                        transactionId = rowId,
                        creditLimit = existingAccount?.creditLimit,
                        isCreditCard = isCreditCard || (existingAccount?.isCreditCard ?: false),
                        smsSource = parsedTransaction.smsBody.take(500),
                        sourceType = "TRANSACTION",
                        currency = parsedTransaction.currency
                    )

                    accountBalanceRepository.insertBalance(balanceEntity)
                }
            }
        }
    }

    private suspend fun processUnrecognizedSms(sms: SmsMessage) {
        val upperSender = sms.sender.uppercase()
        if (upperSender.endsWith("-T") || upperSender.endsWith("-S")) {
            try {
                val alreadyExists = unrecognizedSmsRepository.exists(sms.sender, sms.body)

                if (!alreadyExists) {
                    val unrecognizedSms = UnrecognizedSmsEntity(
                        sender = sms.sender,
                        smsBody = sms.body,
                        receivedAt = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(sms.timestamp),
                            ZoneId.systemDefault()
                        )
                    )
                    unrecognizedSmsRepository.insert(unrecognizedSms)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error storing unrecognized SMS: ${e.message}")
            }
        }
    }

    private suspend fun readSmsMessages(): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()

        try {
            // Get scan parameters
            val lastScanTimestamp = userPreferencesRepository.getLastScanTimestamp().first() ?: 0L
            val scanMonths = userPreferencesRepository.getSmsScanMonths()
            val scanAllTime = userPreferencesRepository.getSmsScanAllTime()
            val lastScanPeriod = userPreferencesRepository.getLastScanPeriod().first() ?: 0
            val now = System.currentTimeMillis()

            // Determine if we need a full scan
            val needsFullScan = lastScanTimestamp == 0L || scanAllTime || scanMonths > lastScanPeriod

            // Calculate scan start time
            val scanStartTime = if (needsFullScan) {
                val calendar = java.util.Calendar.getInstance().apply {
                    if (scanAllTime) {
                        // Scan all time - go back 10 years (effectively all SMS)
                        add(java.util.Calendar.YEAR, -10)
                        Log.d(TAG, "Performing full SMS scan for all time")
                    } else {
                        add(java.util.Calendar.MONTH, -scanMonths)
                        Log.d(TAG, "Performing full SMS scan for last $scanMonths months")
                    }
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                calendar.timeInMillis
            } else {
                val threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000L)
                val periodLimit = java.util.Calendar.getInstance().apply {
                    add(java.util.Calendar.MONTH, -scanMonths)
                }.timeInMillis

                val incrementalStart = maxOf(
                    minOf(lastScanTimestamp, threeDaysAgo),
                    periodLimit
                )

                val daysSinceLastScan = (now - lastScanTimestamp) / (24 * 60 * 60 * 1000L)
                Log.d(TAG, "Performing incremental SMS scan (last scan: $daysSinceLastScan days ago)")
                incrementalStart
            }

            // Query SMS inbox
            val cursor = applicationContext.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                SMS_PROJECTION,
                "${Telephony.Sms.TYPE} = ? AND ${Telephony.Sms.DATE} >= ?",
                arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString(), scanStartTime.toString()),
                "${Telephony.Sms.DATE} DESC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val typeIndex = it.getColumnIndexOrThrow(Telephony.Sms.TYPE)

                while (it.moveToNext()) {
                    val message = SmsMessage(
                        id = it.getLong(idIndex),
                        sender = it.getString(addressIndex) ?: "",
                        timestamp = it.getLong(dateIndex),
                        body = it.getString(bodyIndex) ?: "",
                        type = it.getInt(typeIndex)
                    )
                    messages.add(message)
                }
            }

            // Update scan tracking
            userPreferencesRepository.setLastScanTimestamp(System.currentTimeMillis())
            if (needsFullScan) {
                userPreferencesRepository.setLastScanPeriod(scanMonths)
            }

            Log.d(TAG, "SMS scan completed. Found ${messages.size} messages")

            // Try to read RCS messages from MMS provider
            try {
                // MMS/RCS uses seconds since epoch, not milliseconds
                val scanStartTimeSeconds = scanStartTime / 1000

                val mmsCursor = applicationContext.contentResolver.query(
                    Uri.parse("content://mms"),
                    arrayOf("_id", "thread_id", "date", "tr_id", "m_id"),
                    "date >= ?",
                    arrayOf(scanStartTimeSeconds.toString()),
                    "date DESC"
                )

                mmsCursor?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val messageId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
                        val date = cursor.getLong(cursor.getColumnIndexOrThrow("date"))
                        val trIdIndex = cursor.getColumnIndex("tr_id")
                        val trId = if (trIdIndex >= 0) cursor.getString(trIdIndex) ?: "" else ""

                        // Check if this is an RCS message (has proto: in tr_id)
                        if (trId.startsWith("proto:")) {
                            // Extract sender from tr_id (it's base64 encoded protobuf)
                            val sender = extractRcsSender(trId)

                            // Get message text from parts
                            var messageText = getRcsMessageText(messageId)

                            // If it's JSON (RCS Rich Card), extract the actual text
                            if (messageText != null && messageText.trim().startsWith("{")) {
                                messageText = extractTextFromRcsJson(messageText)
                            }

                            // Convert to SmsMessage format for processing
                            if (messageText != null && sender != null) {
                                // Only process RCS messages from PNB to avoid unnecessary processing
                                if (sender.uppercase().contains("PUNJAB NATIONAL BANK")) {
                                    Log.d(TAG, "RCS message from PNB (sender: $sender)")
                                    val rcsMessage = SmsMessage(
                                        id = messageId,
                                        sender = sender,
                                        timestamp = date * 1000, // MMS uses seconds, SMS uses milliseconds
                                        body = messageText,
                                        type = Telephony.Sms.MESSAGE_TYPE_INBOX
                                    )
                                    messages.add(rcsMessage)
                                } else {
                                    Log.d(TAG, "Skipping RCS message from non-PNB sender: $sender")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading RCS messages: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying SMS content provider", e)
        }

        return messages
    }

    /**
     * Extracts sender name from RCS tr_id field
     * The tr_id contains base64 encoded protobuf data with sender info
     */
    private fun extractRcsSender(trId: String): String? {
        return try {
            // Remove "proto:" prefix and decode base64
            val base64Data = trId.removePrefix("proto:")
            val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
            val decodedString = String(decodedBytes)

            // Look for sender patterns in the decoded data
            // Pattern 1: Agent ID like "ask_apollo_9xdchzx9_agent@rbm.goog"
            val agentPattern = Regex("""([a-z_]+)_[a-z0-9]+_agent@rbm\.goog""")
            agentPattern.find(decodedString)?.let { match ->
                // Convert agent ID to readable name (e.g., "ask_apollo" -> "Ask Apollo")
                return match.groupValues[1].split("_").joinToString(" ") {
                    it.replaceFirstChar { char -> char.uppercase() }
                }
            }

            // Pattern 2: Look for actual sender name in the data
            // RCS messages often have the business name directly in the protobuf
            val namePattern = Regex("""[\x12\x1a][\x00-\x20]([A-Za-z][A-Za-z\s]+)""")
            namePattern.find(decodedString)?.let { match ->
                val name = match.groupValues[1].trim()
                if (name.length > 3 && name.length < 50) {
                    return name
                }
            }

            // If no pattern matches, return null
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting RCS sender: ${e.message}")
            null
        }
    }

    /**
     * Gets the text content of an RCS/MMS message from its parts
     */
    private fun getRcsMessageText(messageId: Long): String? {
        return try {
            // First, let's see what parts exist for this message
            val partsCursor = applicationContext.contentResolver.query(
                Uri.parse("content://mms/part"),
                null, // Get all columns to debug
                "mid = ?",
                arrayOf(messageId.toString()),
                null
            )

            partsCursor?.use { cursor ->
                while (cursor.moveToNext()) {
                    val partId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
                    val ctIndex = cursor.getColumnIndex("ct")
                    val contentType = if (ctIndex >= 0) cursor.getString(ctIndex) ?: "" else ""

                    // Look for text content
                    if (contentType.startsWith("text/") || contentType == "application/smil") {
                        // Try to get text directly from the text column
                        val textIndex = cursor.getColumnIndex("text")
                        if (textIndex >= 0) {
                            val text = cursor.getString(textIndex)
                            if (!text.isNullOrEmpty()) {
                                return text
                            }
                        }

                        // Try to read from _data path (file storage)
                        val dataIndex = cursor.getColumnIndex("_data")
                        if (dataIndex >= 0) {
                            val dataPath = cursor.getString(dataIndex)
                            if (!dataPath.isNullOrEmpty()) {
                                // Try to read the file
                                try {
                                    val partUri = Uri.parse("content://mms/part/$partId")
                                    val inputStream = applicationContext.contentResolver.openInputStream(partUri)
                                    val text = inputStream?.bufferedReader()?.use { it.readText() }
                                    if (!text.isNullOrEmpty()) {
                                        return text
                                    }
                                } catch (e: Exception) {
                                    // Ignore read errors
                                }
                            }
                        }
                    }
                }
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting RCS message text: ${e.message}", e)
            null
        }
    }

    /**
     * Extracts text content from RCS JSON (Rich Cards)
     */
    private fun extractTextFromRcsJson(json: String): String? {
        return try {
            val jsonObject = org.json.JSONObject(json)
            val texts = mutableListOf<String>()

            // Navigate through the JSON structure to find text
            fun extractTexts(obj: Any?, depth: Int = 0) {
                if (depth > 10) return // Prevent infinite recursion

                when (obj) {
                    is org.json.JSONObject -> {
                        // Priority order for text fields
                        val textFields = listOf(
                            "text",           // Plain text message
                            "message",        // Message body
                            "body",           // Body content
                            "title",          // Card title
                            "description",    // Card description
                            "content",        // Content field
                            "caption"         // Media caption
                        )

                        for (field in textFields) {
                            if (obj.has(field)) {
                                val value = obj.getString(field)
                                if (value.isNotEmpty() && !value.startsWith("{")) {
                                    texts.add(value)
                                }
                            }
                        }

                        // Recursively search nested objects
                        obj.keys().forEach { key ->
                            if (key !in listOf("media", "suggestions", "postback", "urlAction")) {
                                try {
                                    extractTexts(obj.get(key), depth + 1)
                                } catch (e: Exception) {
                                    // Skip problematic fields
                                }
                            }
                        }
                    }
                    is org.json.JSONArray -> {
                        for (i in 0 until obj.length()) {
                            extractTexts(obj.get(i), depth + 1)
                        }
                    }
                }
            }

            // Check if it's a simple text message (not a rich card)
            if (jsonObject.has("text")) {
                return jsonObject.getString("text")
            }

            // Check for message.text structure
            if (jsonObject.has("message")) {
                val message = jsonObject.getJSONObject("message")
                if (message.has("text")) {
                    return message.getString("text")
                }
            }

            // Extract from complex structures
            extractTexts(jsonObject)

            // Combine all found texts
            if (texts.isNotEmpty()) {
                return texts.distinct().joinToString(" | ")
            }

            // If no text found, it might be a media-only message
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing RCS JSON: ${e.message}")
            // Not JSON, return as plain text
            json
        }
    }

    data class ProcessingResult(
        val processedCount: Int,
        val parsedCount: Int,
        val savedCount: Int,
        val subscriptionCount: Int,
        val coroutineId: Int,
        val batchNumber: Int
    )

    private data class SmsMessage(
        val id: Long,
        val sender: String,
        val timestamp: Long,
        val body: String,
        val type: Int
    )
}