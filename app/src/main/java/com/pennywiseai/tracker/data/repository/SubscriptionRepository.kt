package com.pennywiseai.tracker.data.repository

import com.pennywiseai.tracker.data.database.dao.SubscriptionDao
import com.pennywiseai.tracker.data.database.entity.SubscriptionEntity
import com.pennywiseai.tracker.data.database.entity.SubscriptionState
import com.pennywiseai.tracker.data.parser.bank.HDFCBankParser
import com.pennywiseai.tracker.ui.icons.CategoryMapping
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository @Inject constructor(
    private val subscriptionDao: SubscriptionDao
) {
    
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>> = 
        subscriptionDao.getAllSubscriptions()
    
    fun getActiveSubscriptions(): Flow<List<SubscriptionEntity>> = 
        subscriptionDao.getActiveSubscriptions()
    
    fun getUpcomingSubscriptions(daysAhead: Int = 7): Flow<List<SubscriptionEntity>> {
        val futureDate = LocalDate.now().plusDays(daysAhead.toLong())
        return subscriptionDao.getUpcomingSubscriptions(futureDate)
    }
    
    suspend fun getSubscriptionById(id: Long): SubscriptionEntity? = 
        subscriptionDao.getSubscriptionById(id)
    
    suspend fun insertSubscription(subscription: SubscriptionEntity): Long = 
        subscriptionDao.insertSubscription(subscription)
    
    suspend fun updateSubscription(subscription: SubscriptionEntity) = 
        subscriptionDao.updateSubscription(subscription)
    
    suspend fun updateSubscriptionState(id: Long, state: SubscriptionState) = 
        subscriptionDao.updateSubscriptionState(id, state)
    
    suspend fun hideSubscription(id: Long) = 
        updateSubscriptionState(id, SubscriptionState.HIDDEN)
    
    suspend fun unhideSubscription(id: Long) = 
        updateSubscriptionState(id, SubscriptionState.ACTIVE)
    
    suspend fun deleteSubscription(id: Long) = 
        subscriptionDao.deleteSubscriptionById(id)
    
    /**
     * Creates or updates a subscription from E-Mandate info
     */
    suspend fun createOrUpdateFromEMandate(
        eMandateInfo: HDFCBankParser.EMandateInfo,
        bankName: String
    ): Long {
        // Check if subscription already exists with this UMN
        val existing = eMandateInfo.umn?.let { 
            subscriptionDao.getSubscriptionByUmn(it) 
        }
        
        val nextPaymentDate = eMandateInfo.nextDeductionDate?.let { dateStr ->
            try {
                // Parse DD/MM/YY format
                LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yy"))
            } catch (e: Exception) {
                // Fallback to 30 days from now if parsing fails
                LocalDate.now().plusDays(30)
            }
        } ?: LocalDate.now().plusDays(30)
        
        val subscription = if (existing != null) {
            // If subscription is hidden and the payment date is the same, don't update it
            // This prevents re-adding hidden subscriptions when rescanning the same SMS
            if (existing.state == SubscriptionState.HIDDEN && 
                existing.nextPaymentDate == nextPaymentDate) {
                // Return the existing ID without updating
                return existing.id
            }
            
            // Check if this is a hidden subscription that should be reactivated
            // Reactivate only if:
            // 1. Subscription is currently hidden
            // 2. New payment date is different AND in the future
            val shouldReactivate = existing.state == SubscriptionState.HIDDEN && 
                                  existing.nextPaymentDate != nextPaymentDate &&
                                  nextPaymentDate.isAfter(LocalDate.now())
            
            // Update existing subscription
            existing.copy(
                amount = eMandateInfo.amount,
                nextPaymentDate = nextPaymentDate,
                merchantName = eMandateInfo.merchant,
                state = if (shouldReactivate) SubscriptionState.ACTIVE else existing.state,
                updatedAt = java.time.LocalDateTime.now()
            )
        } else {
            // Create new subscription
            SubscriptionEntity(
                merchantName = eMandateInfo.merchant,
                amount = eMandateInfo.amount,
                nextPaymentDate = nextPaymentDate,
                state = SubscriptionState.ACTIVE,
                bankName = bankName,
                umn = eMandateInfo.umn,
                category = determineCategory(eMandateInfo.merchant)
            )
        }
        
        return subscriptionDao.insertSubscription(subscription)
    }
    
    /**
     * Checks if a transaction matches any active subscription
     */
    suspend fun matchTransactionToSubscription(
        merchantName: String,
        amount: BigDecimal
    ): SubscriptionEntity? {
        val activeSubscription = subscriptionDao.getActiveSubscriptionByMerchant(merchantName)
        
        // Check if amounts match (with some tolerance for small variations)
        return if (activeSubscription != null && 
                   areAmountsEqual(activeSubscription.amount, amount)) {
            activeSubscription
        } else {
            null
        }
    }
    
    /**
     * Updates the next payment date after a subscription charge
     */
    suspend fun updateNextPaymentDateAfterCharge(
        subscriptionId: Long,
        chargeDate: LocalDate = LocalDate.now()
    ) {
        // Assume monthly subscription, add 30 days
        val nextDate = chargeDate.plusDays(30)
        subscriptionDao.updateNextPaymentDate(subscriptionId, nextDate)
    }
    
    /**
     * Checks if a transaction matches a hidden subscription and reactivates it
     */
    suspend fun checkAndReactivateHiddenSubscription(
        merchantName: String,
        amount: BigDecimal,
        transactionDate: LocalDate
    ): SubscriptionEntity? {
        val hiddenSubscription = subscriptionDao.getHiddenSubscriptionByMerchant(merchantName)
        
        return if (hiddenSubscription != null && areAmountsEqual(hiddenSubscription.amount, amount)) {
            // Only reactivate if the transaction date is after the last known payment date
            // This prevents old transactions from reactivating subscriptions
            if (transactionDate.isAfter(hiddenSubscription.nextPaymentDate.minusDays(30))) {
                // Reactivate the subscription
                subscriptionDao.updateSubscriptionState(hiddenSubscription.id, SubscriptionState.ACTIVE)
                
                // Update next payment date (30 days from transaction)
                val nextDate = transactionDate.plusDays(30)
                subscriptionDao.updateNextPaymentDate(hiddenSubscription.id, nextDate)
                
                // Return the updated subscription
                subscriptionDao.getSubscriptionById(hiddenSubscription.id)
            } else {
                // Transaction is too old, don't reactivate
                null
            }
        } else {
            null
        }
    }
    
    private fun areAmountsEqual(amount1: BigDecimal, amount2: BigDecimal): Boolean {
        // Allow for small variations (up to 5%)
        val tolerance = amount1.multiply(BigDecimal("0.05"))
        val diff = amount1.subtract(amount2).abs()
        return diff <= tolerance
    }
    
    private fun determineCategory(merchantName: String): String {
        // Use unified category mapping
        return CategoryMapping.getCategory(merchantName)
    }
}