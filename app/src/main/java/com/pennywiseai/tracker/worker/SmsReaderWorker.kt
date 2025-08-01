package com.pennywiseai.tracker.worker

import android.content.Context
import android.provider.Telephony
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pennywiseai.tracker.core.Constants
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.parser.bank.BankParserFactory
import com.pennywiseai.tracker.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Worker responsible for reading SMS messages and logging them.
 * This is the first step in our SMS scanning pipeline.
 */
@HiltWorker
class SmsReaderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        const val TAG = "SmsReaderWorker"
        const val WORK_NAME = "sms_reader_work"
        
        // SMS Content Provider columns
        private val SMS_PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.BODY,
            Telephony.Sms.TYPE
        )
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting SMS reading and parsing work...")
            
            // Read SMS messages
            val messages = readSmsMessages()
            Log.d(TAG, "Found ${messages.size} SMS messages")
            
            var parsedCount = 0
            var savedCount = 0
            
            // Process messages
            for (sms in messages.take(Constants.SmsProcessing.DEFAULT_BATCH_SIZE)) {
                // Check if sender is from a known bank
                val parser = BankParserFactory.getParser(sms.sender)
                if (parser != null) {
                    Log.d(TAG, "Processing SMS from ${parser.getBankName()}")
                    Log.d(TAG, "SMS Content: ${sms.body.take(Constants.SmsProcessing.SMS_PREVIEW_LENGTH)}...")
                    
                    // Parse the transaction
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
                            ID: ${parsedTransaction.generateTransactionId()}
                        """.trimIndent())
                        
                        // Convert to entity and save
                        val entity = parsedTransaction.toEntity()
                        try {
                            val rowId = transactionRepository.insertTransaction(entity)
                            if (rowId != -1L) {
                                savedCount++
                                Log.d(TAG, "Saved new transaction with ID: $rowId")
                            } else {
                                Log.d(TAG, "Transaction already exists (duplicate), skipping: ${entity.transactionHash}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving transaction: ${e.message}")
                        }
                    }
                }
            }
            
            Log.d(TAG, "SMS parsing completed. Parsed: $parsedCount, Saved: $savedCount")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in SMS parsing work", e)
            Result.failure()
        }
    }
    
    /**
     * Reads SMS messages from the device's SMS content provider.
     */
    private fun readSmsMessages(): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        
        try {
            // Query SMS inbox
            val cursor = applicationContext.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                SMS_PROJECTION,
                "${Telephony.Sms.TYPE} = ?",
                arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString()),
                "${Telephony.Sms.DATE} DESC LIMIT ${Constants.SmsProcessing.QUERY_LIMIT}"
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
        } catch (e: Exception) {
            Log.e(TAG, "Error querying SMS content provider", e)
        }
        
        return messages
    }
    
    /**
     * Data class representing an SMS message.
     */
    private data class SmsMessage(
        val id: Long,
        val sender: String,
        val timestamp: Long,
        val body: String,
        val type: Int
    )
}