package com.pennywiseai.tracker.subscription

import com.pennywiseai.tracker.data.Subscription
import com.pennywiseai.tracker.data.SubscriptionFrequency
import com.pennywiseai.tracker.data.Transaction
import java.util.*

class SubscriptionDetector {
    
    companion object {
        private const val SUBSCRIPTION_AMOUNT_THRESHOLD = 5000.0 // Max amount for subscription
        private const val MIN_OCCURRENCES = 2 // Minimum occurrences to detect subscription
        private const val DAY_TOLERANCE = 3 // Days tolerance for frequency matching
    }
    
    fun detectSubscriptions(transactions: List<Transaction>): List<Subscription> {
        val subscriptions = mutableListOf<Subscription>()
        
        // Group transactions by merchant and similar amounts
        val merchantGroups = transactions
            .filter { it.amount <= SUBSCRIPTION_AMOUNT_THRESHOLD }
            .groupBy { it.merchant.lowercase().trim() }
        
        for ((merchant, merchantTransactions) in merchantGroups) {
            if (merchantTransactions.size < MIN_OCCURRENCES) continue
            
            // Group by similar amounts (within 10% variance)
            val amountGroups = groupBySimilarAmounts(merchantTransactions)
            
            for (amountGroup in amountGroups) {
                if (amountGroup.size < MIN_OCCURRENCES) continue
                
                val frequency = detectFrequency(amountGroup)
                if (frequency != null) {
                    val subscription = createSubscription(merchant, amountGroup, frequency)
                    subscriptions.add(subscription)
                }
            }
        }
        
        return subscriptions
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
        if (transaction.amount > SUBSCRIPTION_AMOUNT_THRESHOLD) return false
        
        val similarTransactions = existingTransactions.filter { existing ->
            existing.merchant.equals(transaction.merchant, ignoreCase = true) &&
            kotlin.math.abs(existing.amount - transaction.amount) / transaction.amount <= 0.1
        }
        
        return similarTransactions.isNotEmpty() && detectFrequency(similarTransactions + transaction) != null
    }
}