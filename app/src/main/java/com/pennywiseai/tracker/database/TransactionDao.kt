package com.pennywiseai.tracker.database

import androidx.room.*
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.data.TransactionWithGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: String): Flow<Transaction?>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionByIdSync(id: String): Transaction?
    
    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    fun getTransactionsByCategory(category: TransactionCategory): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :searchTerm || '%' ORDER BY date DESC")
    fun searchTransactions(searchTerm: String): Flow<List<Transaction>>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalSpendingInPeriod(startDate: Long, endDate: Long): Double?
    
    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE date >= :startDate AND date <= :endDate GROUP BY category")
    suspend fun getCategorySpending(startDate: Long, endDate: Long): List<CategorySpending>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)
    
    @Update
    suspend fun updateTransaction(transaction: Transaction)
    
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
    
    @Query("SELECT * FROM transactions WHERE id IN (:transactionIds) ORDER BY date DESC")
    fun getTransactionsByIds(transactionIds: List<String>): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE date >= :cutoffDate ORDER BY date DESC")
    suspend fun getRecentTransactions(cutoffDate: Long): List<Transaction>
    
    @Query("SELECT COUNT(*) FROM transactions WHERE date >= :timestamp")
    suspend fun getTransactionCountSince(timestamp: Long): Int
    
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAllTransactionsSync(): List<Transaction>
    
    @Query("SELECT * FROM transactions WHERE merchant = :merchantName ORDER BY date DESC LIMIT :limit")
    suspend fun getTransactionsByMerchant(merchantName: String, limit: Int): List<Transaction>
    
    // Pagination queries
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsPaginated(limit: Int, offset: Int): List<Transaction>
    
    @Query("""
        SELECT * FROM transactions 
        WHERE (:category IS NULL OR category = :category)
        AND (:merchant IS NULL OR merchant LIKE '%' || :merchant || '%')
        AND (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        AND (:searchQuery IS NULL OR merchant LIKE '%' || :searchQuery || '%' OR rawSms LIKE '%' || :searchQuery || '%')
        AND (:minAmount IS NULL OR amount >= :minAmount)
        AND (:maxAmount IS NULL OR amount <= :maxAmount)
        ORDER BY 
        CASE WHEN :sortBy = 'DATE_DESC' THEN date END DESC,
        CASE WHEN :sortBy = 'DATE_ASC' THEN date END ASC,
        CASE WHEN :sortBy = 'AMOUNT_DESC' THEN amount END DESC,
        CASE WHEN :sortBy = 'AMOUNT_ASC' THEN amount END ASC,
        CASE WHEN :sortBy = 'MERCHANT_ASC' THEN merchant END ASC,
        CASE WHEN :sortBy = 'CATEGORY_ASC' THEN category END ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getTransactionsFiltered(
        category: TransactionCategory?,
        merchant: String?,
        startDate: Long?,
        endDate: Long?,
        searchQuery: String?,
        minAmount: Double?,
        maxAmount: Double?,
        sortBy: String,
        limit: Int,
        offset: Int
    ): List<Transaction>
    
    @Query("""
        SELECT COUNT(*) FROM transactions 
        WHERE (:category IS NULL OR category = :category)
        AND (:merchant IS NULL OR merchant LIKE '%' || :merchant || '%')
        AND (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        AND (:searchQuery IS NULL OR merchant LIKE '%' || :searchQuery || '%' OR rawSms LIKE '%' || :searchQuery || '%')
        AND (:minAmount IS NULL OR amount >= :minAmount)
        AND (:maxAmount IS NULL OR amount <= :maxAmount)
    """)
    suspend fun getTransactionCountFiltered(
        category: TransactionCategory?,
        merchant: String?,
        startDate: Long?,
        endDate: Long?,
        searchQuery: String?,
        minAmount: Double?,
        maxAmount: Double?
    ): Int
    
    // Optimized queries with indexes
    @Query("SELECT * FROM transactions WHERE date >= :startDate ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentTransactionsPaginated(startDate: Long, limit: Int): List<Transaction>
    
    @Query("SELECT DISTINCT merchant FROM transactions ORDER BY merchant ASC")
    suspend fun getAllMerchants(): List<String>
    
    @Query("SELECT DISTINCT category FROM transactions ORDER BY category ASC")
    suspend fun getAllCategories(): List<TransactionCategory>
    
    // Query to get transactions with their group information
    @Query("""
        SELECT t.*, tg.groupId, g.name as groupName
        FROM transactions t
        LEFT JOIN transaction_group_mappings tg ON t.id = tg.transactionId
        LEFT JOIN transaction_groups g ON tg.groupId = g.id
        ORDER BY t.date DESC
    """)
    fun getAllTransactionsWithGroups(): Flow<List<TransactionWithGroupInfo>>
}

data class CategorySpending(
    val category: TransactionCategory,
    val total: Double
)

// Data class for Room query result
data class TransactionWithGroupInfo(
    val id: String,
    val amount: Double,
    val merchant: String,
    val category: TransactionCategory,
    val date: Long,
    val rawSms: String,
    val upiId: String?,
    val transactionType: com.pennywiseai.tracker.data.TransactionType,
    val confidence: Float,
    val subscription: Boolean,
    val groupId: String?,
    val groupName: String?
)