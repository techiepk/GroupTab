package com.pennywiseai.tracker.dao

import androidx.room.*
import com.pennywiseai.tracker.data.AccountBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountBalanceDao {
    
    @Query("SELECT * FROM account_balances ORDER BY bankName ASC")
    fun getAllBalances(): Flow<List<AccountBalance>>
    
    @Query("SELECT * FROM account_balances WHERE accountId = :accountId")
    suspend fun getBalance(accountId: String): AccountBalance?
    
    @Query("SELECT SUM(balance) FROM account_balances")
    fun getTotalBalance(): Flow<Double?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(balance: AccountBalance)
    
    @Update
    suspend fun update(balance: AccountBalance)
    
    @Delete
    suspend fun delete(balance: AccountBalance)
    
    @Query("DELETE FROM account_balances")
    suspend fun deleteAll()
    
    /**
     * Update balance only if the new timestamp is more recent
     */
    @Transaction
    suspend fun updateIfNewer(accountId: String, newBalance: Double, timestamp: Long, bankName: String, last4: String) {
        val existing = getBalance(accountId)
        if (existing == null || timestamp > existing.lastUpdated) {
            insertOrUpdate(
                AccountBalance(
                    accountId = accountId,
                    balance = newBalance,
                    lastUpdated = timestamp,
                    bankName = bankName,
                    last4Digits = last4
                )
            )
        }
    }
}