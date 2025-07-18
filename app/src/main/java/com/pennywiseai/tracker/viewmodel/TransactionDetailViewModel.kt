package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.Subscription
import com.pennywiseai.tracker.data.SubscriptionFrequency
import com.pennywiseai.tracker.data.SubscriptionStatus
import com.pennywiseai.tracker.data.TransactionType
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionRepository
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Calendar

class TransactionDetailViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(database)
    
    fun getTransaction(id: String): LiveData<Transaction?> {
        return repository.getTransactionById(id)
    }
    
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
    
    fun convertToSubscription(transaction: Transaction, frequency: SubscriptionFrequency) {
        viewModelScope.launch {
            // Update transaction type
            val updatedTransaction = transaction.copy(
                transactionType = TransactionType.SUBSCRIPTION,
                subscription = true
            )
            repository.updateTransaction(updatedTransaction)
            
            // Calculate next payment date based on frequency
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = transaction.date
            
            when (frequency) {
                SubscriptionFrequency.WEEKLY -> calendar.add(Calendar.DAY_OF_YEAR, 7)
                SubscriptionFrequency.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                SubscriptionFrequency.QUARTERLY -> calendar.add(Calendar.MONTH, 3)
                SubscriptionFrequency.YEARLY -> calendar.add(Calendar.YEAR, 1)
            }
            
            // Create subscription
            val subscription = Subscription(
                id = UUID.randomUUID().toString(),
                merchantName = transaction.merchant,
                amount = kotlin.math.abs(transaction.amount),
                frequency = frequency,
                nextPaymentDate = calendar.timeInMillis,
                lastPaymentDate = transaction.date,
                active = true,
                transactionIds = listOf(transaction.id),
                startDate = transaction.date,
                endDate = null,
                cancellationDate = null,
                status = SubscriptionStatus.ACTIVE,
                category = transaction.category,
                description = "Created from transaction",
                paymentCount = 1,
                totalPaid = kotlin.math.abs(transaction.amount),
                lastAmountPaid = kotlin.math.abs(transaction.amount),
                averageAmount = kotlin.math.abs(transaction.amount)
            )
            
            // Insert subscription directly through database
            database.subscriptionDao().insertSubscription(subscription)
        }
    }
}