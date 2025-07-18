package com.pennywiseai.tracker.database

import androidx.room.*
import com.pennywiseai.tracker.data.TransactionGroup
import com.pennywiseai.tracker.data.TransactionGroupMapping
import com.pennywiseai.tracker.data.GroupingType
import com.pennywiseai.tracker.data.TransactionCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionGroupDao {
    
    // Transaction Group operations
    @Query("SELECT * FROM transaction_groups WHERE isActive = 1 ORDER BY totalAmount DESC")
    fun getAllActiveGroups(): Flow<List<TransactionGroup>>
    
    @Query("SELECT * FROM transaction_groups WHERE id = :groupId")
    fun getGroupById(groupId: String): Flow<TransactionGroup?>
    
    @Query("SELECT * FROM transaction_groups WHERE merchantPattern = :pattern AND groupingType = :type LIMIT 1")
    suspend fun getGroupByPattern(pattern: String, type: GroupingType): TransactionGroup?
    
    @Query("SELECT * FROM transaction_groups WHERE category = :category AND isActive = 1")
    fun getGroupsByCategory(category: TransactionCategory): Flow<List<TransactionGroup>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: TransactionGroup)
    
    @Update
    suspend fun updateGroup(group: TransactionGroup)
    
    @Delete
    suspend fun deleteGroup(group: TransactionGroup)
    
    @Query("UPDATE transaction_groups SET isActive = 0 WHERE id = :groupId")
    suspend fun deactivateGroup(groupId: String)
    
    // Transaction Group Mapping operations
    @Query("SELECT * FROM transaction_group_mappings WHERE groupId = :groupId")
    fun getMappingsForGroup(groupId: String): Flow<List<TransactionGroupMapping>>
    
    @Query("SELECT * FROM transaction_group_mappings WHERE transactionId = :transactionId")
    suspend fun getMappingsForTransaction(transactionId: String): List<TransactionGroupMapping>
    
    @Query("SELECT * FROM transaction_group_mappings WHERE isManuallyAssigned = 1")
    suspend fun getAllManualMappings(): List<TransactionGroupMapping>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: TransactionGroupMapping)
    
    @Delete
    suspend fun deleteMapping(mapping: TransactionGroupMapping)
    
    @Query("DELETE FROM transaction_group_mappings WHERE transactionId = :transactionId")
    suspend fun deleteMappingsForTransaction(transactionId: String)
    
    @Query("DELETE FROM transaction_group_mappings WHERE groupId = :groupId")
    suspend fun deleteMappingsForGroup(groupId: String)
    
    // Complex queries for grouped transactions
    @Query("""
        SELECT g.*, 
               COUNT(m.transactionId) as actualCount,
               COALESCE(SUM(t.amount), 0) as actualTotal,
               COALESCE(AVG(t.amount), 0) as actualAverage,
               MAX(t.date) as actualLastDate
        FROM transaction_groups g
        LEFT JOIN transaction_group_mappings m ON g.id = m.groupId
        LEFT JOIN transactions t ON m.transactionId = t.id
        WHERE g.isActive = 1
        GROUP BY g.id
        ORDER BY actualTotal DESC
    """)
    suspend fun getGroupsWithCalculatedTotals(): List<GroupWithTotals>
    
    @Query("""
        SELECT t.* FROM transactions t
        INNER JOIN transaction_group_mappings m ON t.id = m.transactionId
        WHERE m.groupId = :groupId
        ORDER BY t.date DESC
        LIMIT :limit
    """)
    suspend fun getRecentTransactionsForGroup(groupId: String, limit: Int = 10): List<com.pennywiseai.tracker.data.Transaction>
    
    @Query("""
        SELECT t.* FROM transactions t
        INNER JOIN transaction_group_mappings m ON t.id = m.transactionId
        WHERE m.groupId = :groupId
        ORDER BY t.date DESC
    """)
    fun getAllTransactionsForGroup(groupId: String): Flow<List<com.pennywiseai.tracker.data.Transaction>>
    
    @Query("""
        SELECT COUNT(*) FROM transactions t
        LEFT JOIN transaction_group_mappings m ON t.id = m.transactionId
        WHERE m.groupId IS NULL
    """)
    suspend fun getUngroupedTransactionCount(): Int
    
    @Query("""
        SELECT t.* FROM transactions t
        LEFT JOIN transaction_group_mappings m ON t.id = m.transactionId
        WHERE m.groupId IS NULL
        ORDER BY t.date DESC
        LIMIT :limit
    """)
    suspend fun getUngroupedTransactions(limit: Int = 100): List<com.pennywiseai.tracker.data.Transaction>
    
    // Update cached values in groups
    @Query("""
        UPDATE transaction_groups 
        SET transactionCount = :count,
            totalAmount = :total,
            averageAmount = :average,
            lastTransactionDate = :lastDate,
            lastUpdated = :updateTime
        WHERE id = :groupId
    """)
    suspend fun updateGroupTotals(
        groupId: String, 
        count: Int, 
        total: Double, 
        average: Double, 
        lastDate: Long,
        updateTime: Long
    )
}

// Data class for calculated group totals
data class GroupWithTotals(
    val id: String,
    val name: String,
    val merchantPattern: String,
    val category: TransactionCategory,
    val groupingType: GroupingType,
    val isAutoGenerated: Boolean,
    val createdDate: Long,
    val lastUpdated: Long,
    val transactionCount: Int,
    val totalAmount: Double,
    val averageAmount: Double,
    val lastTransactionDate: Long,
    val isActive: Boolean,
    val actualCount: Int,
    val actualTotal: Double,
    val actualAverage: Double,
    val actualLastDate: Long?
)