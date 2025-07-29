package com.pennywiseai.tracker.service

import android.content.Context
import android.util.Log
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.parser.bank.HDFCBankParser
import com.pennywiseai.tracker.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service to handle subscription updates from E-Mandate notifications
 */
class SubscriptionUpdateService(private val context: Context) {
    
    companion object {
        private const val TAG = "SubscriptionUpdateService"
    }
    
    private val database by lazy { AppDatabase.getDatabase(context) }
    private val repository by lazy { TransactionRepository(database) }
    
    /**
     * Process E-Mandate information and update matching transactions
     */
    suspend fun processEMandateInfo(
        eMandateInfo: HDFCBankParser.EMandateInfo,
        sender: String
    ) = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Processing E-Mandate for ${eMandateInfo.merchant}")
            
            // Find transactions with matching criteria
            val matchingTransactions = repository.findTransactionsByAmountAndMerchant(
                amount = eMandateInfo.amount,
                merchantPattern = eMandateInfo.merchant
            )
            
            // Update matching transactions to mark as subscriptions
            matchingTransactions.forEach { transaction ->
                val updatedTransaction = transaction.copy(
                    subscription = true,
                    transactionType = com.pennywiseai.tracker.data.TransactionType.SUBSCRIPTION
                )
                repository.updateTransaction(updatedTransaction)
                
                Log.i(TAG, "Updated transaction ${transaction.id} as subscription")
            }
            
            // Also create/update subscription record if needed
            val subscriptionAmount = -kotlin.math.abs(eMandateInfo.amount) // Ensure it's negative (expense)
            
            // Check if subscription already exists
            val existingSubscription = repository.findSubscriptionByMerchantAndAmount(
                merchant = eMandateInfo.merchant,
                amount = subscriptionAmount
            )
            
            if (existingSubscription == null) {
                // Create new subscription
                val nextDate = parseNextBillingDate(eMandateInfo.nextDeductionDate) ?: System.currentTimeMillis()
                val subscription = com.pennywiseai.tracker.data.Subscription(
                    id = java.util.UUID.randomUUID().toString(),
                    merchantName = eMandateInfo.merchant,
                    amount = subscriptionAmount,
                    frequency = com.pennywiseai.tracker.data.SubscriptionFrequency.MONTHLY,
                    nextPaymentDate = nextDate,
                    lastPaymentDate = System.currentTimeMillis(),
                    startDate = System.currentTimeMillis(),
                    category = com.pennywiseai.tracker.data.TransactionCategory.SUBSCRIPTION,
                    status = com.pennywiseai.tracker.data.SubscriptionStatus.ACTIVE
                )
                
                repository.insertSubscription(subscription)
                Log.i(TAG, "Created new subscription for ${eMandateInfo.merchant}")
            } else {
                // Update existing subscription
                val updatedSubscription = existingSubscription.copy(
                    nextPaymentDate = parseNextBillingDate(eMandateInfo.nextDeductionDate) 
                        ?: existingSubscription.nextPaymentDate,
                    lastPaymentDate = System.currentTimeMillis(),
                    status = com.pennywiseai.tracker.data.SubscriptionStatus.ACTIVE
                )
                repository.updateSubscription(updatedSubscription)
                Log.i(TAG, "Updated existing subscription for ${eMandateInfo.merchant}")
            }
            
            Log.i(TAG, "✅ Successfully processed E-Mandate for ${eMandateInfo.merchant}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing E-Mandate: ${e.message}", e)
        }
    }
    
    /**
     * Parse date string from E-Mandate format (dd/MM/yy) to timestamp
     */
    private fun parseNextBillingDate(dateStr: String?): Long? {
        if (dateStr == null) return null
        
        return try {
            val parts = dateStr.split("/")
            if (parts.size == 3) {
                val day = parts[0].toIntOrNull() ?: return null
                val month = parts[1].toIntOrNull() ?: return null
                val year = parts[2].toIntOrNull() ?: return null
                
                // Convert 2-digit year to 4-digit
                val fullYear = if (year < 100) 2000 + year else year
                
                // Create calendar instance
                val calendar = java.util.Calendar.getInstance()
                calendar.set(fullYear, month - 1, day, 0, 0, 0) // month is 0-based
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                
                calendar.timeInMillis
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $dateStr", e)
            null
        }
    }
}