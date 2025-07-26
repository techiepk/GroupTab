package com.pennywiseai.tracker.service

import android.util.Log
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.GroupingType
import com.pennywiseai.tracker.repository.TransactionGroupRepository
import com.pennywiseai.tracker.repository.TransactionRepository
import kotlinx.coroutines.flow.first

class PatternMatchingService(
    private val transactionRepository: TransactionRepository,
    private val groupRepository: TransactionGroupRepository
) {
    companion object {
        private const val TAG = "PatternMatchingService"
    }
    
    /**
     * Apply patterns to a newly created transaction
     * @param transaction The transaction to apply patterns to
     * @return true if any patterns were applied
     */
    suspend fun applyPatternsToTransaction(transaction: Transaction): Boolean {
        try {
            // Get all active groups/patterns - use first() to get current value
            val activeGroups = groupRepository.getAllActiveGroups().first()
            
            if (activeGroups.isEmpty()) {
                Log.d(TAG, "No active patterns found")
                return false
            }
            
            var patternsApplied = false
            
            // Check each pattern against the transaction
            for (group in activeGroups) {
                val matches = when (group.groupingType) {
                    GroupingType.MERCHANT_EXACT -> {
                        transaction.merchant.equals(group.merchantPattern, ignoreCase = true)
                    }
                    GroupingType.MERCHANT_FUZZY -> {
                        val pattern = group.merchantPattern.lowercase()
                        val merchantLower = transaction.merchant.lowercase()
                        val smsLower = transaction.rawSms.lowercase()
                        
                        // Check if pattern matches merchant or SMS content
                        merchantLower.contains(pattern) || smsLower.contains(pattern)
                    }
                    GroupingType.CATEGORY_AMOUNT -> {
                        // Would need amount range logic - skip for now
                        false
                    }
                    GroupingType.RECURRING_PATTERN -> {
                        // Would need time interval logic - skip for now
                        false
                    }
                    GroupingType.UPI_ID -> {
                        // Check if UPI ID matches
                        transaction.upiId != null && transaction.upiId.equals(group.merchantPattern, ignoreCase = true)
                    }
                    GroupingType.MANUAL -> {
                        // Manual groups don't auto-apply
                        false
                    }
                }
                
                if (matches) {
                    Log.d(TAG, "Pattern '${group.name}' matches transaction ${transaction.id}")
                    
                    // Check if transaction is already in a group
                    val existingMappings = groupRepository.getMappingsForTransaction(transaction.id)
                    if (existingMappings.isEmpty()) {
                        // Add transaction to group
                        groupRepository.addTransactionToGroup(
                            transactionId = transaction.id,
                            groupId = group.id,
                            confidence = 0.9f,
                            isManual = false
                        )
                        patternsApplied = true
                        Log.i(TAG, "Applied pattern '${group.name}' to transaction ${transaction.id}")
                    }
                }
            }
            
            return patternsApplied
            
        } catch (e: Exception) {
            Log.e(TAG, "Error applying patterns to transaction", e)
            return false
        }
    }
    
    /**
     * Apply patterns to multiple transactions (batch operation)
     * @param transactions List of transactions to process
     * @return Number of transactions that had patterns applied
     */
    suspend fun applyPatternsToTransactions(transactions: List<Transaction>): Int {
        var count = 0
        transactions.forEach { transaction ->
            if (applyPatternsToTransaction(transaction)) {
                count++
            }
        }
        return count
    }
}