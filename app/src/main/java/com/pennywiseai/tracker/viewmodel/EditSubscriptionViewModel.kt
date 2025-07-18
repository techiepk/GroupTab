package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.liveData
import com.pennywiseai.tracker.data.Subscription
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionRepository
import kotlinx.coroutines.launch

class EditSubscriptionViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = TransactionRepository(AppDatabase.getDatabase(application))
    
    fun getSubscription(id: String): LiveData<Subscription?> {
        return repository.getSubscriptionById(id)
    }
    
    fun updateSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.updateSubscription(subscription)
        }
    }
    
    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.deleteSubscription(subscription)
        }
    }
    
    fun getRecentPayments(subscriptionId: String, limit: Int): LiveData<List<Transaction>> = liveData {
        // Get recent transactions matching this subscription's merchant name
        val subscription = repository.getSubscriptionByIdSync(subscriptionId)
        if (subscription != null) {
            val recentTransactions = repository.getTransactionsByMerchant(subscription.merchantName, limit)
            emit(recentTransactions)
        } else {
            emit(emptyList())
        }
    }
}