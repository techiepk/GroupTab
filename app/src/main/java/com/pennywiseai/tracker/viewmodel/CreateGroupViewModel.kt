package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.data.GroupingType
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.repository.TransactionGroupRepository
import kotlinx.coroutines.launch
import android.util.Log

class CreateGroupViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "CreateGroupViewModel"
    }
    
    private val database = AppDatabase.getDatabase(application)
    private val transactionRepository = TransactionRepository(database)
    private val groupRepository = TransactionGroupRepository(database)
    
    private val _transaction = MutableLiveData<Transaction?>()
    val transaction: LiveData<Transaction?> = _transaction
    
    private val _previewCount = MutableLiveData<Int>(0)
    val previewCount: LiveData<Int> = _previewCount
    
    private val _previewTransactions = MutableLiveData<List<Transaction>>(emptyList())
    val previewTransactions: LiveData<List<Transaction>> = _previewTransactions
    
    fun loadTransaction(transactionId: String) {
        viewModelScope.launch {
            try {
                val txn = transactionRepository.getTransactionByIdSync(transactionId)
                _transaction.value = txn
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load transaction", e)
                _transaction.value = null
            }
        }
    }
    
    fun updatePreviewCount(pattern: String, patternType: Int) {
        viewModelScope.launch {
            try {
                val allTransactions = transactionRepository.getAllTransactionsSync()
                val matchingCount = when (patternType) {
                    0 -> countContainsPattern(allTransactions, pattern) // Contains
                    1 -> countExactPattern(allTransactions, pattern) // Exact
                    2 -> countStartsWithPattern(allTransactions, pattern) // Starts With
                    3 -> countEndsWithPattern(allTransactions, pattern) // Ends With
                    4 -> countMultipleKeywords(allTransactions, pattern) // Multiple Keywords
                    else -> 0
                }
                _previewCount.value = matchingCount
                
                // Also get the actual transactions for preview
                val matchingTransactions = when (patternType) {
                    0 -> getContainsMatches(allTransactions, pattern)
                    1 -> getExactMatches(allTransactions, pattern)
                    2 -> getStartsWithMatches(allTransactions, pattern)
                    3 -> getEndsWithMatches(allTransactions, pattern)
                    4 -> getMultipleKeywordMatches(allTransactions, pattern)
                    else -> emptyList()
                }
                _previewTransactions.value = matchingTransactions.take(20) // Limit preview to 20
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update preview count", e)
                _previewCount.value = 0
            }
        }
    }
    
    private fun countContainsPattern(transactions: List<Transaction>, pattern: String): Int {
        val lowerPattern = pattern.lowercase()
        return transactions.count { 
            it.merchant.lowercase().contains(lowerPattern) ||
            it.rawSms.lowercase().contains(lowerPattern)
        }
    }
    
    private fun getContainsMatches(transactions: List<Transaction>, pattern: String): List<Transaction> {
        val lowerPattern = pattern.lowercase()
        return transactions.filter { 
            it.merchant.lowercase().contains(lowerPattern) ||
            it.rawSms.lowercase().contains(lowerPattern)
        }
    }
    
    private fun countExactPattern(transactions: List<Transaction>, pattern: String): Int {
        return transactions.count { it.merchant.equals(pattern, ignoreCase = true) }
    }
    
    private fun getExactMatches(transactions: List<Transaction>, pattern: String): List<Transaction> {
        return transactions.filter { it.merchant.equals(pattern, ignoreCase = true) }
    }
    
    private fun countStartsWithPattern(transactions: List<Transaction>, pattern: String): Int {
        val lowerPattern = pattern.lowercase()
        return transactions.count { it.merchant.lowercase().startsWith(lowerPattern) }
    }
    
    private fun getStartsWithMatches(transactions: List<Transaction>, pattern: String): List<Transaction> {
        val lowerPattern = pattern.lowercase()
        return transactions.filter { it.merchant.lowercase().startsWith(lowerPattern) }
    }
    
    private fun countEndsWithPattern(transactions: List<Transaction>, pattern: String): Int {
        val lowerPattern = pattern.lowercase()
        return transactions.count { it.merchant.lowercase().endsWith(lowerPattern) }
    }
    
    private fun getEndsWithMatches(transactions: List<Transaction>, pattern: String): List<Transaction> {
        val lowerPattern = pattern.lowercase()
        return transactions.filter { it.merchant.lowercase().endsWith(lowerPattern) }
    }
    
    private fun countMultipleKeywords(transactions: List<Transaction>, pattern: String): Int {
        val keywords = pattern.lowercase().split(" ", ",").filter { it.isNotEmpty() }
        if (keywords.isEmpty()) return 0
        
        return transactions.count { transaction ->
            val lowerMerchant = transaction.merchant.lowercase()
            val lowerSms = transaction.rawSms.lowercase()
            keywords.any { keyword -> 
                lowerMerchant.contains(keyword) || lowerSms.contains(keyword)
            }
        }
    }
    
    private fun getMultipleKeywordMatches(transactions: List<Transaction>, pattern: String): List<Transaction> {
        val keywords = pattern.lowercase().split(" ", ",").filter { it.isNotEmpty() }
        if (keywords.isEmpty()) return emptyList()
        
        return transactions.filter { transaction ->
            val lowerMerchant = transaction.merchant.lowercase()
            val lowerSms = transaction.rawSms.lowercase()
            keywords.any { keyword -> 
                lowerMerchant.contains(keyword) || lowerSms.contains(keyword)
            }
        }
    }
    
    suspend fun createGroup(
        name: String,
        pattern: String,
        patternType: GroupingType,
        category: TransactionCategory,
        applyToExisting: Boolean,
        learnFromPattern: Boolean,
        amountMin: Double? = null,
        amountMax: Double? = null,
        exampleTransactionId: String? = null
    ): Boolean {
        return try {
            
            // Create the group
            val group = groupRepository.createGroup(
                name = name,
                merchantPattern = pattern,
                category = category,
                groupingType = patternType,
                isAutoGenerated = false // This is a manual group
            )
            
            
            // If we have an example transaction and want to learn from it, add it to the group
            if (exampleTransactionId != null && learnFromPattern) {
                groupRepository.addTransactionToGroup(
                    transactionId = exampleTransactionId,
                    groupId = group.id,
                    confidence = 1.0f,
                    isManual = true
                )
            }
            
            // If apply to existing is enabled, find and add matching transactions
            if (applyToExisting) {
                val allTransactions = transactionRepository.getAllTransactionsSync()
                val matchingTransactions = findMatchingTransactions(
                    allTransactions, 
                    pattern, 
                    patternType,
                    amountMin,
                    amountMax
                )
                
                
                matchingTransactions.forEach { transaction ->
                    try {
                        // Check if transaction is already grouped
                        val existingMappings = groupRepository.getMappingsForTransaction(transaction.id)
                        if (existingMappings.isEmpty()) {
                            groupRepository.addTransactionToGroup(
                                transactionId = transaction.id,
                                groupId = group.id,
                                confidence = 0.9f,
                                isManual = false // Auto-applied based on pattern
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to add transaction ${transaction.id} to group", e)
                    }
                }
                
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create group", e)
            false
        }
    }
    
    private fun findMatchingTransactions(
        transactions: List<Transaction>,
        pattern: String,
        patternType: GroupingType,
        amountMin: Double?,
        amountMax: Double?
    ): List<Transaction> {
        // First filter by pattern
        val patternMatches = when (patternType) {
            GroupingType.MERCHANT_EXACT -> transactions.filter { 
                it.merchant.equals(pattern, ignoreCase = true) 
            }
            GroupingType.MERCHANT_FUZZY -> transactions.filter { 
                it.merchant.lowercase().contains(pattern.lowercase()) ||
                it.rawSms.lowercase().contains(pattern.lowercase())
            }
            else -> transactions.filter { 
                it.merchant.lowercase().contains(pattern.lowercase()) 
            }
        }
        
        // Then filter by amount if specified
        return patternMatches.filter { transaction ->
            val amount = Math.abs(transaction.amount)
            val passesMin = amountMin?.let { amount >= it } ?: true
            val passesMax = amountMax?.let { amount <= it } ?: true
            passesMin && passesMax
        }
    }
}