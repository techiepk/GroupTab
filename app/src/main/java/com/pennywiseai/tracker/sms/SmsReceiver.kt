package com.pennywiseai.tracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.llm.LLMTransactionExtractor
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.subscription.SubscriptionDetector
import com.pennywiseai.tracker.parser.PatternTransactionParser
import com.pennywiseai.tracker.data.SubscriptionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "SmsReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        
        val repository = TransactionRepository(AppDatabase.getDatabase(context))
        val subscriptionDetector = SubscriptionDetector()
        
        // Get parser preference
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val usePatternParser = prefs.getBoolean("use_pattern_parser", false)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (message in messages) {
                    val smsBody = message.messageBody ?: continue
                    val sender = message.originatingAddress ?: continue
                    val timestamp = message.timestampMillis
                    
                    
                    // Parse transaction based on selected parser
                    val transaction = if (usePatternParser) {
                        // Use pattern-based parser
                        val patternParser = PatternTransactionParser()
                        patternParser.parseTransaction(smsBody, sender, timestamp)
                    } else {
                        // Use AI-based parser
                        val llmExtractor = LLMTransactionExtractor(context)
                        if (!llmExtractor.initialize()) {
                            Log.e(TAG, "Failed to initialize LLM, falling back to pattern parser")
                            val patternParser = PatternTransactionParser()
                            patternParser.parseTransaction(smsBody, sender, timestamp)
                        } else {
                            llmExtractor.extractTransaction(smsBody, sender, timestamp)
                        }
                    }
                    
                    if (transaction != null) {
                        // Check if it's a subscription
                        val existingTransactions = repository.getAllTransactionsSync()
                        val isSubscription = subscriptionDetector.isLikelySubscription(
                            transaction,
                            existingTransactions
                        )
                        
                        val finalTransaction = transaction.copy(subscription = isSubscription)
                        
                        // Save to database
                        repository.insertTransaction(finalTransaction)
                        
                        // If it's a subscription, detect and create subscription records
                        if (isSubscription) {
                            val allTransactions = repository.getAllTransactionsSync()
                            val subscriptions = subscriptionDetector.detectSubscriptions(allTransactions)
                            
                            // Insert new subscriptions
                            subscriptions.forEach { subscription ->
                                // Check if this subscription already exists
                                val existing = repository.getActiveSubscriptionsSync()
                                    .find { it.merchantName.equals(subscription.merchantName, ignoreCase = true) }
                                
                                if (existing == null) {
                                    repository.insertSubscription(subscription)
                                } else {
                                    // Update existing subscription with latest payment info
                                    repository.updateSubscription(existing.copy(
                                        lastPaymentDate = finalTransaction.date,
                                        nextPaymentDate = subscription.nextPaymentDate,
                                        paymentCount = existing.paymentCount + 1,
                                        totalPaid = existing.totalPaid + finalTransaction.amount,
                                        lastAmountPaid = finalTransaction.amount
                                    ))
                                }
                            }
                        }
                        
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            }
        }
    }
}