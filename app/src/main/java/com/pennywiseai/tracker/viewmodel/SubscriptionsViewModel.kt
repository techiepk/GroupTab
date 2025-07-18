package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.Subscription
import com.pennywiseai.tracker.data.SubscriptionStatus
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class SubscriptionsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = TransactionRepository(AppDatabase.getDatabase(application))
    
    // Filter and sort state
    private val filterStatus = MutableStateFlow<SubscriptionStatus?>(null)
    private val sortOrder = MutableStateFlow("name")
    private val upcomingDaysFilter = MutableStateFlow<Int?>(null)
    
    // Active subscriptions with filtering and sorting
    val activeSubscriptions: LiveData<List<Subscription>> = 
        combine(
            repository.getActiveSubscriptions(),
            filterStatus,
            sortOrder,
            upcomingDaysFilter
        ) { subscriptions, filter, sort, upcomingDays ->
            var filtered = subscriptions
            
            // Apply status filter
            if (filter != null) {
                filtered = filtered.filter { it.status == filter }
            }
            
            // Apply upcoming filter
            if (upcomingDays != null) {
                val currentTime = System.currentTimeMillis()
                val futureTime = currentTime + (upcomingDays * 24 * 60 * 60 * 1000L)
                filtered = filtered.filter { subscription ->
                    subscription.status == SubscriptionStatus.ACTIVE &&
                    subscription.nextPaymentDate >= currentTime &&
                    subscription.nextPaymentDate <= futureTime
                }
            }
            
            // Apply sorting
            when (sort) {
                "name" -> filtered.sortedBy { it.merchantName.lowercase() }
                "amount_desc" -> filtered.sortedByDescending { it.amount }
                "amount_asc" -> filtered.sortedBy { it.amount }
                "nextPayment" -> filtered.sortedBy { it.nextPaymentDate }
                else -> filtered
            }
        }.asLiveData()
    
    // Dashboard data
    val monthlyTotal: LiveData<Double> = repository.getActiveSubscriptions().map { subscriptions ->
        subscriptions.filter { it.status == SubscriptionStatus.ACTIVE }
            .sumOf { subscription ->
                when (subscription.frequency.days) {
                    7 -> subscription.amount * 4.33 // Weekly to monthly
                    30 -> subscription.amount // Monthly
                    90 -> subscription.amount / 3 // Quarterly to monthly  
                    365 -> subscription.amount / 12 // Yearly to monthly
                    else -> subscription.amount
                }
            }
    }.asLiveData()
    
    val activeCount: LiveData<Int> = repository.getActiveSubscriptions().map { subscriptions ->
        subscriptions.count { it.status == SubscriptionStatus.ACTIVE }
    }.asLiveData()
    
    // Upcoming payments (next 7 days)
    val upcomingPayments: LiveData<List<Subscription>> = repository.getActiveSubscriptions().map { subscriptions ->
        val currentTime = System.currentTimeMillis()
        val nextWeek = currentTime + (7 * 24 * 60 * 60 * 1000L)
        
        subscriptions.filter { subscription ->
            subscription.status == SubscriptionStatus.ACTIVE && 
            subscription.nextPaymentDate >= currentTime &&
            subscription.nextPaymentDate <= nextWeek
        }.sortedBy { it.nextPaymentDate }
    }.asLiveData()
    
    fun toggleSubscriptionStatus(subscription: Subscription) {
        viewModelScope.launch {
            val newStatus = if (subscription.status == SubscriptionStatus.ACTIVE) {
                SubscriptionStatus.PAUSED
            } else {
                SubscriptionStatus.ACTIVE
            }
            
            val updatedSubscription = subscription.copy(
                status = newStatus,
                active = newStatus == SubscriptionStatus.ACTIVE
            )
            
            repository.updateSubscription(updatedSubscription)
        }
    }
    
    fun cancelSubscription(subscription: Subscription) {
        viewModelScope.launch {
            val updatedSubscription = subscription.copy(
                status = SubscriptionStatus.CANCELLED,
                active = false,
                cancellationDate = System.currentTimeMillis()
            )
            
            repository.updateSubscription(updatedSubscription)
        }
    }
    
    fun setFilter(status: SubscriptionStatus?) {
        filterStatus.value = status
        upcomingDaysFilter.value = null // Clear upcoming filter when setting status filter
    }
    
    fun setUpcomingFilter(days: Int) {
        upcomingDaysFilter.value = days
        filterStatus.value = null // Clear status filter when setting upcoming filter
    }
    
    fun setSortOrder(order: String) {
        sortOrder.value = order
    }
}