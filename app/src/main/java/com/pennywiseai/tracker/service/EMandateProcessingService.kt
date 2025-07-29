package com.pennywiseai.tracker.service

import android.content.Context
import android.util.Log
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.parser.bank.BankParserFactory
import com.pennywiseai.tracker.parser.bank.HDFCBankParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.provider.Telephony

/**
 * Service to process E-Mandate messages after SMS scanning is complete
 */
class EMandateProcessingService(private val context: Context) {
    
    companion object {
        private const val TAG = "EMandateProcessingService"
    }
    
    private val subscriptionUpdateService = SubscriptionUpdateService(context)
    
    /**
     * Process all E-Mandate messages in the SMS inbox
     * This should be called after transaction scanning is complete
     */
    suspend fun processEMandateMessages(daysBack: Int = 30) = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "🔍 Starting E-Mandate processing...")
            
            val cutoffDate = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)
            val eMandateMessages = getEMandateMessages(cutoffDate)
            
            Log.i(TAG, "Found ${eMandateMessages.size} E-Mandate messages")
            
            for (message in eMandateMessages) {
                processEMandateMessage(message)
            }
            
            Log.i(TAG, "✅ E-Mandate processing complete. Processed ${eMandateMessages.size} messages")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing E-Mandate messages", e)
        }
    }
    
    /**
     * Get all E-Mandate messages from SMS inbox
     */
    private fun getEMandateMessages(cutoffDate: Long): List<EMandateMessage> {
        val messages = mutableListOf<EMandateMessage>()
        
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        
        // Query for messages containing "E-Mandate!"
        val selection = "${Telephony.Sms.TYPE} = ? AND ${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.BODY} LIKE ?"
        val selectionArgs = arrayOf(
            Telephony.Sms.MESSAGE_TYPE_INBOX.toString(),
            cutoffDate.toString(),
            "%E-Mandate!%"
        )
        
        val cursor = context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} DESC"
        )
        
        cursor?.use { c ->
            val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
            val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)
            
            while (c.moveToNext()) {
                val body = c.getString(bodyIndex) ?: continue
                val sender = c.getString(addressIndex) ?: continue
                val date = c.getLong(dateIndex)
                
                // Check if it's from a bank we support
                val bankParser = BankParserFactory.getParser(sender)
                if (bankParser is HDFCBankParser) {
                    messages.add(EMandateMessage(body, sender, date))
                }
            }
        }
        
        return messages
    }
    
    /**
     * Process a single E-Mandate message
     */
    private suspend fun processEMandateMessage(message: EMandateMessage) {
        try {
            val bankParser = BankParserFactory.getParser(message.sender)
            
            if (bankParser is HDFCBankParser) {
                val eMandateInfo = bankParser.parseEMandateSubscription(message.body)
                
                if (eMandateInfo != null) {
                    Log.i(TAG, "Processing E-Mandate: ${eMandateInfo.merchant} - ₹${eMandateInfo.amount}")
                    
                    // Use the subscription update service to process it
                    subscriptionUpdateService.processEMandateInfo(eMandateInfo, message.sender)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing E-Mandate message", e)
        }
    }
    
    data class EMandateMessage(
        val body: String,
        val sender: String,
        val date: Long
    )
}