package com.pennywiseai.tracker.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pennywiseai.tracker.MainActivity
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.data.parser.bank.BankParserFactory
import com.pennywiseai.tracker.data.repository.TransactionRepository
import com.pennywiseai.tracker.utils.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver for handling incoming SMS messages in real-time.
 * Automatically adds transactions and notifies user for quick edits.
 */
@AndroidEntryPoint
class SmsBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var transactionRepository: TransactionRepository
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "SmsBroadcastReceiver"
        private const val CHANNEL_ID = "transaction_notifications"
        private const val NOTIFICATION_ID_BASE = 10000
        const val ACTION_EDIT_TRANSACTION = "com.pennywiseai.tracker.EDIT_TRANSACTION"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }
        
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages?.forEach { smsMessage ->
                processSms(
                    context = context,
                    sender = smsMessage.displayOriginatingAddress ?: "",
                    body = smsMessage.displayMessageBody ?: "",
                    timestamp = smsMessage.timestampMillis
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS", e)
        }
    }
    
    private fun processSms(context: Context, sender: String, body: String, timestamp: Long) {
        // Check if sender is from a supported bank
        val parser = BankParserFactory.getParser(sender)
        if (parser == null) {
            Log.d(TAG, "No parser found for sender: $sender")
            return
        }
        
        // Query content provider to get the same timestamp that SmsReaderWorker would use
        val contentProviderTimestamp = getTimestampFromContentProvider(context, sender, body)
        val actualTimestamp = contentProviderTimestamp ?: timestamp
        
        if (contentProviderTimestamp != null) {
            Log.d(TAG, "Using content provider timestamp: $contentProviderTimestamp (original: $timestamp, diff: ${contentProviderTimestamp - timestamp}ms)")
        } else {
            Log.d(TAG, "Content provider query failed, using original timestamp: $timestamp")
        }
        
        // Parse the transaction using the content provider timestamp (same as SmsReaderWorker)
        val parsedTransaction = parser.parse(body, sender, actualTimestamp)
        if (parsedTransaction == null) {
            Log.d(TAG, "Could not parse transaction from SMS")
            return
        }
        
        // Generate hash for duplicate checking
        val transactionHash = parsedTransaction.transactionHash ?: parsedTransaction.generateTransactionId()
        
        Log.d(TAG, """
            Parsed transaction from SmsBroadcastReceiver:
            Bank: ${parser.getBankName()}
            Amount: ${parsedTransaction.amount}
            Type: ${parsedTransaction.type}
            Original Timestamp: $timestamp
            Used Timestamp: $actualTimestamp
            Hash: $transactionHash
        """.trimIndent())
        
        // Add transaction immediately
        scope.launch {
            try {
                // Convert to entity
                val entity = parsedTransaction.toEntity()
                
                // Use INSERT OR IGNORE strategy (same as SmsReaderWorker)
                val rowId = transactionRepository.insertTransaction(entity)
                
                if (rowId != -1L) {
                    Log.i(TAG, "New transaction added by SmsBroadcastReceiver: ${entity.merchantName} - ${entity.amount}")
                    
                    // Show notification with quick edit action
                    showTransactionNotification(
                        context = context,
                        transactionId = rowId,
                        merchant = entity.merchantName ?: "Unknown",
                        amount = entity.amount,
                        type = entity.transactionType.name
                    )
                } else {
                    Log.d(TAG, "Transaction already exists (duplicate), skipping. Hash: $transactionHash")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error saving transaction", e)
            }
        }
    }
    
    private fun showTransactionNotification(
        context: Context,
        transactionId: Long,
        merchant: String,
        amount: java.math.BigDecimal,
        type: String
    ) {
        // Create notification channel for Android O and above
        createNotificationChannel(context)
        
        // Format amount
        val formattedAmount = CurrencyFormatter.formatCurrency(amount)
        
        // Create intent for editing transaction
        val editIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_EDIT_TRANSACTION
            putExtra(EXTRA_TRANSACTION_ID, transactionId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val editPendingIntent = PendingIntent.getActivity(
            context,
            transactionId.toInt(),
            editIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: Add proper icon
            .setContentTitle("Transaction Added")
            .setContentText("$merchant: $formattedAmount")
            .setSubText(type)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(editPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground, // TODO: Add edit icon
                "Edit",
                editPendingIntent
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$merchant\n$formattedAmount\nTap to edit or swipe to dismiss")
            )
            .build()
        
        // Show notification
        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID_BASE + transactionId.toInt(),
                notification
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing notification permission", e)
        }
    }
    
    private fun getTimestampFromContentProvider(context: Context, sender: String, body: String): Long? {
        return try {
            // Query for the SMS in the content provider using sender and body
            // We look for messages from the last minute to account for processing delay
            val oneMinuteAgo = System.currentTimeMillis() - 60000
            
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.DATE),
                "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.BODY} = ? AND ${Telephony.Sms.DATE} > ?",
                arrayOf(sender, body, oneMinuteAgo.toString()),
                "${Telephony.Sms.DATE} DESC"
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    return it.getLong(dateIndex)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error querying content provider for timestamp", e)
            null
        }
    }
    
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Transaction Notifications"
            val descriptionText = "Notifications for new transactions with quick edit options"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}