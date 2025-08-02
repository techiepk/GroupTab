package com.pennywiseai.tracker.data.repository

import com.pennywiseai.tracker.data.database.dao.TransactionDao
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getAllTransactions(): Flow<List<TransactionEntity>> = 
        transactionDao.getAllTransactions()
    
    suspend fun getTransactionById(id: Long): TransactionEntity? = 
        transactionDao.getTransactionById(id)
    
    fun getTransactionsBetweenDates(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<TransactionEntity>> = 
        transactionDao.getTransactionsBetweenDates(startDate, endDate)
    
    fun getTransactionsBetweenDates(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<TransactionEntity>> = 
        transactionDao.getTransactionsBetweenDates(
            startDate.atStartOfDay(),
            endDate.atTime(23, 59, 59)
        )
    
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> = 
        transactionDao.getTransactionsByType(type)
    
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>> = 
        transactionDao.getTransactionsByCategory(category)
    
    fun searchTransactions(query: String): Flow<List<TransactionEntity>> = 
        transactionDao.searchTransactions(query)
    
    fun getAllCategories(): Flow<List<String>> = 
        transactionDao.getAllCategories()
    
    fun getAllMerchants(): Flow<List<String>> = 
        transactionDao.getAllMerchants()
    
    suspend fun getTotalAmountByTypeAndPeriod(
        type: TransactionType,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Double? = transactionDao.getTotalAmountByTypeAndPeriod(type, startDate, endDate)
    
    suspend fun insertTransaction(transaction: TransactionEntity): Long = 
        transactionDao.insertTransaction(transaction)
    
    suspend fun insertTransactions(transactions: List<TransactionEntity>) = 
        transactionDao.insertTransactions(transactions)
    
    suspend fun updateTransaction(transaction: TransactionEntity) = 
        transactionDao.updateTransaction(transaction)
    
    suspend fun deleteTransaction(transaction: TransactionEntity) = 
        transactionDao.deleteTransaction(transaction)
    
    suspend fun deleteTransactionById(id: Long) = 
        transactionDao.deleteTransactionById(id)
    
    suspend fun deleteAllTransactions() = 
        transactionDao.deleteAllTransactions()
    
    // Additional methods for Home screen
    fun getCurrentMonthTotal(): Flow<BigDecimal> {
        val now = YearMonth.now()
        val startDate = now.atDay(1).atStartOfDay()
        val endDate = now.atEndOfMonth().atTime(23, 59, 59)
        
        return transactionDao.getTransactionsBetweenDates(startDate, endDate)
            .map { transactions ->
                val income = transactions
                    .filter { it.transactionType == TransactionType.INCOME }
                    .sumOf { it.amount }
                val expenses = transactions
                    .filter { it.transactionType == TransactionType.EXPENSE }
                    .sumOf { it.amount }
                income - expenses // Net amount (positive if saved, negative if spent)
            }
    }
    
    fun getLastMonthTotal(): Flow<BigDecimal> {
        val lastMonth = YearMonth.now().minusMonths(1)
        val startDate = lastMonth.atDay(1).atStartOfDay()
        val endDate = lastMonth.atEndOfMonth().atTime(23, 59, 59)
        
        return transactionDao.getTransactionsBetweenDates(startDate, endDate)
            .map { transactions ->
                val income = transactions
                    .filter { it.transactionType == TransactionType.INCOME }
                    .sumOf { it.amount }
                val expenses = transactions
                    .filter { it.transactionType == TransactionType.EXPENSE }
                    .sumOf { it.amount }
                income - expenses // Net amount
            }
    }
    
    fun getRecentTransactions(limit: Int = 5): Flow<List<TransactionEntity>> {
        return transactionDao.getAllTransactions()
            .map { transactions ->
                transactions.take(limit)
            }
    }
}