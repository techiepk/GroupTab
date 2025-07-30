package com.pennywiseai.tracker.subscription

import com.pennywiseai.tracker.data.Subscription
import com.pennywiseai.tracker.data.SubscriptionFrequency
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.parser.bank.HDFCBankParser
import java.util.*

class SubscriptionDetector {
    
    companion object {
        private const val MIN_OCCURRENCES = 3 // Minimum occurrences to detect subscription
        private const val DAY_TOLERANCE = 3 // Days tolerance for frequency matching
        private const val MIN_INTERVAL_DAYS = 5 // Minimum days between transactions to be considered subscription
    }
    
    /**
     * Detect subscriptions from transactions
     * This is now only used as a fallback - primary subscription detection comes from E-Mandate messages
     */
    fun detectSubscriptions(transactions: List<Transaction>): List<Subscription> {
        val subscriptions = mutableListOf<Subscription>()
        val processedMerchantAmounts = mutableSetOf<String>()
        
        // Only process transactions that are already marked as subscriptions
        // These would have been marked by E-Mandate processing
        val subscriptionTransactions = transactions.filter { it.subscription }
        
        // Group by merchant to create subscription records
        val merchantGroups = subscriptionTransactions
            .groupBy { it.merchant.lowercase().trim() }
        
        for ((merchant, merchantTransactions) in merchantGroups) {
            if (merchantTransactions.size < MIN_OCCURRENCES) continue
            
            // Group by similar amounts (within 10% variance)
            val amountGroups = groupBySimilarAmounts(merchantTransactions)
            
            for (amountGroup in amountGroups) {
                if (amountGroup.size < MIN_OCCURRENCES) continue
                
                // Check if intervals are consistent enough
                if (!hasConsistentIntervals(amountGroup)) continue
                
                val frequency = detectFrequency(amountGroup)
                if (frequency != null) {
                    val subscription = createSubscription(merchant, amountGroup, frequency)
                    
                    // Create a unique key for merchant + amount combination
                    val key = "${subscription.merchantName.lowercase()}_${String.format("%.2f", subscription.amount)}"
                    
                    // Only add if we haven't already processed this merchant+amount combination
                    if (!processedMerchantAmounts.contains(key)) {
                        subscriptions.add(subscription)
                        processedMerchantAmounts.add(key)
                    }
                }
            }
        }
        
        return subscriptions
    }
    
    private fun hasConsistentIntervals(transactions: List<Transaction>): Boolean {
        if (transactions.size < 2) return false
        
        val sortedTransactions = transactions.sortedBy { it.date }
        val intervals = mutableListOf<Long>()
        
        for (i in 1 until sortedTransactions.size) {
            val intervalDays = (sortedTransactions[i].date - sortedTransactions[i - 1].date) / (24 * 60 * 60 * 1000)
            
            // Skip if interval is too small (likely not a subscription)
            if (intervalDays < MIN_INTERVAL_DAYS) return false
            
            intervals.add(intervalDays)
        }
        
        // Check if intervals are consistent (coefficient of variation < 0.3)
        if (intervals.isEmpty()) return false
        
        val mean = intervals.average()
        val variance = intervals.map { (it - mean) * (it - mean) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        val coefficientOfVariation = stdDev / mean
        
        return coefficientOfVariation < 0.3
    }
    
    private fun groupBySimilarAmounts(transactions: List<Transaction>): List<List<Transaction>> {
        val groups = mutableListOf<MutableList<Transaction>>()
        
        for (transaction in transactions.sortedBy { it.amount }) {
            var addedToGroup = false
            
            for (group in groups) {
                val avgAmount = group.map { it.amount }.average()
                val variance = kotlin.math.abs(transaction.amount - avgAmount) / avgAmount
                
                if (variance <= 0.1) { // 10% variance tolerance
                    group.add(transaction)
                    addedToGroup = true
                    break
                }
            }
            
            if (!addedToGroup) {
                groups.add(mutableListOf(transaction))
            }
        }
        
        return groups.map { it.toList() }
    }
    
    private fun detectFrequency(transactions: List<Transaction>): SubscriptionFrequency? {
        if (transactions.size < 2) return null
        
        val sortedTransactions = transactions.sortedBy { it.date }
        val intervals = mutableListOf<Long>()
        
        for (i in 1 until sortedTransactions.size) {
            val interval = sortedTransactions[i].date - sortedTransactions[i - 1].date
            val intervalDays = interval / (24 * 60 * 60 * 1000)
            intervals.add(intervalDays)
        }
        
        val avgInterval = intervals.average()
        
        return when {
            isCloseToFrequency(avgInterval, SubscriptionFrequency.WEEKLY.days) -> SubscriptionFrequency.WEEKLY
            isCloseToFrequency(avgInterval, SubscriptionFrequency.MONTHLY.days) -> SubscriptionFrequency.MONTHLY
            isCloseToFrequency(avgInterval, SubscriptionFrequency.QUARTERLY.days) -> SubscriptionFrequency.QUARTERLY
            isCloseToFrequency(avgInterval, SubscriptionFrequency.YEARLY.days) -> SubscriptionFrequency.YEARLY
            else -> null
        }
    }
    
    private fun isCloseToFrequency(actualDays: Double, expectedDays: Int): Boolean {
        return kotlin.math.abs(actualDays - expectedDays) <= DAY_TOLERANCE
    }
    
    private fun createSubscription(
        merchant: String,
        transactions: List<Transaction>,
        frequency: SubscriptionFrequency
    ): Subscription {
        val avgAmount = transactions.map { it.amount }.average()
        val lastTransaction = transactions.maxByOrNull { it.date }!!
        val nextPaymentDate = calculateNextPaymentDate(lastTransaction.date, frequency)
        
        return Subscription(
            id = UUID.randomUUID().toString(),
            merchantName = merchant.replaceFirstChar { it.uppercase() },
            amount = avgAmount,
            frequency = frequency,
            nextPaymentDate = nextPaymentDate,
            lastPaymentDate = lastTransaction.date,
            active = true,
            transactionIds = transactions.map { it.id },
            startDate = transactions.minByOrNull { it.date }?.date ?: lastTransaction.date,
            paymentCount = transactions.size,
            totalPaid = transactions.sumOf { it.amount },
            lastAmountPaid = lastTransaction.amount,
            averageAmount = avgAmount
        )
    }
    
    private fun calculateNextPaymentDate(lastPaymentDate: Long, frequency: SubscriptionFrequency): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastPaymentDate
        
        when (frequency) {
            SubscriptionFrequency.WEEKLY -> calendar.add(Calendar.DAY_OF_YEAR, 7)
            SubscriptionFrequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            SubscriptionFrequency.QUARTERLY -> calendar.add(Calendar.MONTH, 3)
            SubscriptionFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }
        
        return calendar.timeInMillis
    }
    
    fun isLikelySubscription(transaction: Transaction, existingTransactions: List<Transaction>): Boolean {
        val similarTransactions = existingTransactions.filter { existing ->
            existing.merchant.equals(transaction.merchant, ignoreCase = true) &&
            kotlin.math.abs(existing.amount - transaction.amount) / transaction.amount <= 0.1
        }
        
        return similarTransactions.isNotEmpty() && detectFrequency(similarTransactions + transaction) != null
    }
    
    /**
     * Create a subscription from E-Mandate info (from HDFC or other banks)
     */
    fun createSubscriptionFromEMandate(
        emandateInfo: HDFCBankParser.EMandateInfo,
        relatedTransactions: List<Transaction> = emptyList()
    ): Subscription {
        // Default to monthly frequency for E-Mandate
        // Most E-Mandates are monthly recurring payments
        val frequency = SubscriptionFrequency.MONTHLY
        
        val nextPaymentDate = calculateNextPaymentDate(System.currentTimeMillis(), frequency)
        
        return Subscription(
            id = UUID.randomUUID().toString(),
            merchantName = emandateInfo.merchant.trim(),
            amount = emandateInfo.amount,
            frequency = frequency,
            nextPaymentDate = nextPaymentDate,
            lastPaymentDate = System.currentTimeMillis(),
            active = true,
            transactionIds = relatedTransactions.map { it.id },
            startDate = System.currentTimeMillis(),
            paymentCount = relatedTransactions.size,
            totalPaid = relatedTransactions.sumOf { it.amount },
            lastAmountPaid = emandateInfo.amount,
            averageAmount = emandateInfo.amount,
            isEMandate = true // Add this flag to identify E-Mandate subscriptions
        )
    }
}