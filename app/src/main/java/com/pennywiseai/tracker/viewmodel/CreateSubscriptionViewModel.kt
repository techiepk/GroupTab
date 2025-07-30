package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.Subscription
import com.pennywiseai.tracker.data.SubscriptionFrequency
import com.pennywiseai.tracker.data.SubscriptionStatus
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionRepository
import kotlinx.coroutines.launch
import java.util.*

class CreateSubscriptionViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = TransactionRepository(AppDatabase.getDatabase(application))
    
    private val _isCreating = MutableLiveData<Boolean>(false)
    val isCreating: LiveData<Boolean> = _isCreating
    
    private val _creationResult = MutableLiveData<CreationResult?>()
    val creationResult: LiveData<CreationResult?> = _creationResult
    
    private val _suggestedFrequency = MutableLiveData<SubscriptionFrequency?>()
    val suggestedFrequency: LiveData<SubscriptionFrequency?> = _suggestedFrequency
    
    private val _isDuplicateWarning = MutableLiveData<Boolean>(false)
    val isDuplicateWarning: LiveData<Boolean> = _isDuplicateWarning
    
    fun createSubscriptionFromTransaction(
        transaction: Transaction,
        frequency: SubscriptionFrequency,
        startDate: Long,
        endDate: Long? = null,
        description: String? = null,
        customAmount: Double? = null
    ) {
        viewModelScope.launch {
            _isCreating.value = true
            
            try {
                // Check for existing subscription with same merchant
                val existingSubscription = repository.getSubscriptionByMerchantSync(transaction.merchant)
                if (existingSubscription != null && existingSubscription.status == SubscriptionStatus.ACTIVE) {
                    _creationResult.value = CreationResult.Error("Active subscription already exists for ${transaction.merchant}")
                    return@launch
                }
                
                // Calculate next payment date
                val nextPaymentDate = calculateNextPaymentDate(startDate, frequency)
                
                // Create subscription
                val subscription = Subscription(
                    id = generateSubscriptionId(),
                    merchantName = transaction.merchant,
                    amount = customAmount ?: kotlin.math.abs(transaction.amount),
                    frequency = frequency,
                    nextPaymentDate = nextPaymentDate,
                    lastPaymentDate = transaction.date,
                    active = true,
                    transactionIds = listOf(transaction.id),
                    startDate = startDate,
                    endDate = endDate,
                    status = SubscriptionStatus.ACTIVE,
                    category = if (transaction.category == TransactionCategory.OTHER) TransactionCategory.SUBSCRIPTION else transaction.category,
                    description = description,
                    paymentCount = 1,
                    totalPaid = kotlin.math.abs(transaction.amount),
                    lastAmountPaid = kotlin.math.abs(transaction.amount),
                    averageAmount = kotlin.math.abs(transaction.amount)
                )
                
                // Check for duplicate before inserting
                val existing = repository.getSubscriptionByMerchantAndAmountSync(
                    subscription.merchantName,
                    subscription.amount
                )
                
                if (existing != null) {
                    _creationResult.value = CreationResult.Error("Subscription already exists for ${subscription.merchantName} with amount ₹${subscription.amount}")
                    return@launch
                }
                
                // Insert subscription
                repository.insertSubscription(subscription)
                
                _creationResult.value = CreationResult.Success(subscription)
            } catch (e: Exception) {
                _creationResult.value = CreationResult.Error("Failed to create subscription: ${e.message}")
            } finally {
                _isCreating.value = false
            }
        }
    }
    
    fun checkForDuplicateSubscription(merchantName: String) {
        viewModelScope.launch {
            val existingSubscription = repository.getSubscriptionByMerchantSync(merchantName)
            _isDuplicateWarning.value = existingSubscription != null && 
                    existingSubscription.status == SubscriptionStatus.ACTIVE
        }
    }
    
    fun suggestFrequency(transaction: Transaction) {
        val merchantName = transaction.merchant.lowercase()
        val suggested = when {
            // Streaming services - Monthly
            merchantName.contains("netflix") ||
            merchantName.contains("spotify") ||
            merchantName.contains("prime") ||
            merchantName.contains("youtube") ||
            merchantName.contains("disney") ||
            merchantName.contains("hotstar") -> SubscriptionFrequency.MONTHLY
            
            // Utilities - Monthly
            merchantName.contains("electricity") ||
            merchantName.contains("water") ||
            merchantName.contains("gas") ||
            merchantName.contains("internet") ||
            merchantName.contains("broadband") -> SubscriptionFrequency.MONTHLY
            
            // Insurance - Quarterly or Yearly
            merchantName.contains("insurance") ||
            merchantName.contains("policy") -> SubscriptionFrequency.QUARTERLY
            
            // Gym/Fitness - Monthly
            merchantName.contains("gym") ||
            merchantName.contains("fitness") ||
            merchantName.contains("yoga") -> SubscriptionFrequency.MONTHLY
            
            // News/Magazine - Monthly
            merchantName.contains("times") ||
            merchantName.contains("hindu") ||
            merchantName.contains("magazine") -> SubscriptionFrequency.MONTHLY
            
            // Default based on amount
            else -> when {
                kotlin.math.abs(transaction.amount) > 5000 -> SubscriptionFrequency.QUARTERLY
                kotlin.math.abs(transaction.amount) > 1000 -> SubscriptionFrequency.MONTHLY
                else -> SubscriptionFrequency.MONTHLY
            }
        }
        
        _suggestedFrequency.value = suggested
    }
    
    private fun calculateNextPaymentDate(startDate: Long, frequency: SubscriptionFrequency): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate
        
        when (frequency) {
            SubscriptionFrequency.WEEKLY -> calendar.add(Calendar.DAY_OF_YEAR, 7)
            SubscriptionFrequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            SubscriptionFrequency.QUARTERLY -> calendar.add(Calendar.MONTH, 3)
            SubscriptionFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }
        
        return calendar.timeInMillis
    }
    
    private fun generateSubscriptionId(): String {
        return "sub_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    fun clearResult() {
        _creationResult.value = null
    }
    
    sealed class CreationResult {
        data class Success(val subscription: Subscription) : CreationResult()
        data class Error(val message: String) : CreationResult()
    }
}