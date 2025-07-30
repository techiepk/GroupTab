package com.pennywiseai.tracker.service

import android.content.Context
import android.util.Log
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.parser.bank.HDFCBankParser
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.subscription.SubscriptionDetector
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
            
            // Find ALL transactions from this merchant (not just exact amount matches)
            // E-Mandate confirms this merchant is a subscription service
            val allMerchantTransactions = repository.findTransactionsByMerchant(
                merchantPattern = eMandateInfo.merchant
            )
            
            // Update ALL transactions from this merchant to mark as subscriptions
            allMerchantTransactions.forEach { transaction ->
                val updatedTransaction = transaction.copy(
                    subscription = true,
                    transactionType = com.pennywiseai.tracker.data.TransactionType.SUBSCRIPTION
                )
                repository.updateTransaction(updatedTransaction)
                
                Log.i(TAG, "Updated transaction ${transaction.id} as subscription")
            }
            
            // For subscription creation, only use transactions with matching amount
            val matchingTransactions = allMerchantTransactions.filter { 
                kotlin.math.abs(kotlin.math.abs(it.amount) - eMandateInfo.amount) / eMandateInfo.amount <= 0.1
            }
            
            // Also create/update subscription record if needed
            val subscriptionAmount = -kotlin.math.abs(eMandateInfo.amount) // Ensure it's negative (expense)
            
            // Check if subscription already exists
            val existingSubscription = repository.findSubscriptionByMerchantAndAmount(
                merchant = eMandateInfo.merchant,
                amount = subscriptionAmount
            )
            
            if (existingSubscription == null) {
                // Create new subscription using SubscriptionDetector
                val subscriptionDetector = SubscriptionDetector()
                val subscription = subscriptionDetector.createSubscriptionFromEMandate(
                    eMandateInfo,
                    matchingTransactions
                )
                
                // Double-check before inserting
                val doubleCheck = repository.getSubscriptionByMerchantAndAmountSync(
                    subscription.merchantName,
                    subscription.amount
                )
                
                if (doubleCheck == null) {
                    repository.insertSubscription(subscription)
                    Log.i(TAG, "Created new E-Mandate subscription for ${eMandateInfo.merchant}")
                } else {
                    Log.i(TAG, "E-Mandate subscription already exists for ${eMandateInfo.merchant} - ₹${subscriptionAmount}")
                }
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