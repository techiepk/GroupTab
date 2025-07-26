package com.pennywiseai.tracker.repository

import androidx.lifecycle.asLiveData
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.Subscription
import com.pennywiseai.tracker.data.AppSettings
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.data.TransactionWithGroup
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.database.CategorySpending
import com.pennywiseai.tracker.database.TransactionWithGroupInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepository(private val database: AppDatabase) {
    
    private val transactionDao = database.transactionDao()
    private val subscriptionDao = database.subscriptionDao()
    private val settingsDao = database.settingsDao()
    private val _groupRepository by lazy { TransactionGroupRepository(database) }
    
    // Transaction operations
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    
    fun getTransactionById(id: String): androidx.lifecycle.LiveData<Transaction?> = transactionDao.getTransactionById(id).asLiveData()
    
    suspend fun getTransactionByIdSync(id: String): Transaction? = transactionDao.getTransactionByIdSync(id)
    
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)
    
    fun getTransactionsByCategory(category: TransactionCategory): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(category)
    
    fun searchTransactions(searchTerm: String): Flow<List<Transaction>> =
        transactionDao.searchTransactions(searchTerm)
    
    suspend fun getTotalSpendingInPeriod(startDate: Long, endDate: Long): Double =
        transactionDao.getTotalSpendingInPeriod(startDate, endDate) ?: 0.0
    
    suspend fun getCategorySpending(startDate: Long, endDate: Long): List<CategorySpending> =
        transactionDao.getCategorySpending(startDate, endDate)
    
    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }
    
    suspend fun insertTransactions(transactions: List<Transaction>) =
        transactionDao.insertTransactions(transactions)
    
    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction)
    
    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.deleteTransaction(transaction)
    
    fun getTransactionsByIds(transactionIds: List<String>): Flow<List<Transaction>> = 
        transactionDao.getTransactionsByIds(transactionIds)
    
    suspend fun getTransactionCountSince(timestamp: Long): Int =
        transactionDao.getTransactionCountSince(timestamp)
    
    // Subscription operations
    fun getActiveSubscriptions(): Flow<List<Subscription>> = subscriptionDao.getActiveSubscriptions()
    
    fun getAllSubscriptions(): Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions()
    
    suspend fun getUpcomingSubscriptions(date: Long): List<Subscription> =
        subscriptionDao.getUpcomingSubscriptions(date)
    
    suspend fun insertSubscription(subscription: Subscription) =
        subscriptionDao.insertSubscription(subscription)
    
    suspend fun updateSubscription(subscription: Subscription) =
        subscriptionDao.updateSubscription(subscription)
    
    suspend fun deleteSubscription(subscription: Subscription) =
        subscriptionDao.deleteSubscription(subscription)
    
    suspend fun getSubscriptionByMerchantSync(merchantName: String): Subscription? =
        subscriptionDao.getSubscriptionByMerchant(merchantName)
    
    fun getSubscriptionById(id: String): androidx.lifecycle.LiveData<Subscription?> =
        subscriptionDao.getSubscriptionById(id).asLiveData()
    
    suspend fun getRecentTransactionsSync(days: Int): List<Transaction> {
        val cutoffDate = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return transactionDao.getRecentTransactions(cutoffDate)
    }
    
    suspend fun getAllTransactionsSync(): List<Transaction> =
        transactionDao.getAllTransactionsSync()
    
    suspend fun getSubscriptionByIdSync(id: String): Subscription? =
        subscriptionDao.getSubscriptionByIdSync(id)
    
    suspend fun getTransactionsByMerchant(merchantName: String, limit: Int): List<Transaction> =
        transactionDao.getTransactionsByMerchant(merchantName, limit)
    
    suspend fun getActiveSubscriptionsSync(): List<Subscription> =
        subscriptionDao.getActiveSubscriptionsSync()
    
    // Settings operations
    fun getSettings(): Flow<AppSettings?> = settingsDao.getSettings()
    
    suspend fun getSettingsSync(): AppSettings? = settingsDao.getSettingsSync()
    
    suspend fun insertSettings(settings: AppSettings) = settingsDao.insertSettings(settings)
    
    suspend fun updateSettings(settings: AppSettings) = settingsDao.updateSettings(settings)
    
    suspend fun clearAllData() {
        transactionDao.deleteAllTransactions()
        subscriptionDao.deleteAllSubscriptions()
        settingsDao.deleteAllSettings()
    }
    
    // Transaction grouping support
    fun getGroupRepository(): TransactionGroupRepository = _groupRepository
    
    // Get transactions with their group information
    fun getAllTransactionsWithGroups(): Flow<List<TransactionWithGroup>> =
        transactionDao.getAllTransactionsWithGroups().map { list ->
            list.map { info ->
                TransactionWithGroup(
                    transaction = Transaction(
                        id = info.id,
                        amount = info.amount,
                        merchant = info.merchant,
                        category = info.category,
                        date = info.date,
                        rawSms = info.rawSms,
                        upiId = info.upiId,
                        transactionType = info.transactionType,
                        confidence = info.confidence,
                        subscription = info.subscription
                    ),
                    groupId = info.groupId,
                    groupName = info.groupName
                )
            }
        }
}