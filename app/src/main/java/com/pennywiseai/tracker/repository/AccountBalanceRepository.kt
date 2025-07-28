package com.pennywiseai.tracker.repository

import com.pennywiseai.tracker.dao.AccountBalanceDao
import com.pennywiseai.tracker.data.AccountBalance
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.parser.bank.BankParserFactory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountBalanceRepository @Inject constructor(
    private val accountBalanceDao: AccountBalanceDao
) {
    
    fun getAllBalances(): Flow<List<AccountBalance>> = accountBalanceDao.getAllBalances()
    
    fun getTotalBalance(): Flow<Double?> = accountBalanceDao.getTotalBalance()
    
    suspend fun getBalance(accountId: String): AccountBalance? = accountBalanceDao.getBalance(accountId)
    
    /**
     * Update account balance from a transaction if it has balance information
     */
    suspend fun updateBalanceFromTransaction(transaction: Transaction) {
        // Only update if we have account and balance information
        val accountLast4 = transaction.accountLast4 ?: return
        val availableBalance = transaction.availableBalance ?: return
        val sender = transaction.sender ?: return
        
        // Get bank name from parser factory
        val bankName = BankParserFactory.getBankName(sender)
        
        // Create account ID (e.g., "HDFC_1234")
        val accountId = "${bankName.replace(" ", "").uppercase()}_$accountLast4"
        
        // Update balance only if this transaction is newer
        accountBalanceDao.updateIfNewer(
            accountId = accountId,
            newBalance = availableBalance,
            timestamp = transaction.date,
            bankName = bankName,
            last4 = accountLast4
        )
    }
    
    /**
     * Process a list of transactions and update all account balances
     */
    suspend fun updateBalancesFromTransactions(transactions: List<Transaction>) {
        transactions.forEach { transaction ->
            updateBalanceFromTransaction(transaction)
        }
    }
    
    suspend fun deleteAll() = accountBalanceDao.deleteAll()
}