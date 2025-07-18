package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionGroup
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.repository.TransactionGroupRepository
import com.pennywiseai.tracker.data.TransactionSortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import android.content.Context

class GroupDetailViewModel(
    application: Application,
    private val groupId: String
) : AndroidViewModel(application) {
    
    companion object {
        private const val PREF_NAME = "group_detail_preferences"
        private const val KEY_SORT_ORDER = "group_detail_sort_order"
    }
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = TransactionRepository(database)
    private val groupRepository = repository.getGroupRepository()
    private val sharedPrefs = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    // Load saved sort order
    private val savedSortOrderName = sharedPrefs.getString(KEY_SORT_ORDER, null)
    private val initialSortOrder = savedSortOrderName?.let { name ->
        TransactionSortOrder.values().find { it.name == name }
    } ?: TransactionSortOrder.getDefault()
    
    private val _sortOrder = MutableStateFlow(initialSortOrder)
    val sortOrder: LiveData<TransactionSortOrder> = _sortOrder.asLiveData()
    
    // Get group details
    val groupDetails: LiveData<TransactionGroup?> = groupRepository.getGroupById(groupId).asLiveData()
    
    // Get all transactions in this group with sorting
    val transactions: LiveData<List<Transaction>> = combine(
        groupRepository.getAllTransactionsForGroup(groupId),
        _sortOrder
    ) { transactions, sortOrder ->
        sortTransactions(transactions, sortOrder)
    }.asLiveData()
    
    private fun sortTransactions(transactions: List<Transaction>, sortOrder: TransactionSortOrder): List<Transaction> {
        return when (sortOrder) {
            TransactionSortOrder.DATE_DESC -> transactions.sortedByDescending { it.date }
            TransactionSortOrder.DATE_ASC -> transactions.sortedBy { it.date }
            TransactionSortOrder.AMOUNT_DESC -> transactions.sortedByDescending { kotlin.math.abs(it.amount) }
            TransactionSortOrder.AMOUNT_ASC -> transactions.sortedBy { kotlin.math.abs(it.amount) }
            TransactionSortOrder.MERCHANT_ASC -> transactions.sortedBy { it.merchant.lowercase() }
            TransactionSortOrder.MERCHANT_DESC -> transactions.sortedByDescending { it.merchant.lowercase() }
            TransactionSortOrder.CATEGORY -> transactions.sortedBy { it.category.name }
        }
    }
    
    fun setSortOrder(sortOrder: TransactionSortOrder) {
        _sortOrder.value = sortOrder
        // Save to SharedPreferences
        sharedPrefs.edit().putString(KEY_SORT_ORDER, sortOrder.name).apply()
    }
}

class GroupDetailViewModelFactory(
    private val application: Application,
    private val groupId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupDetailViewModel(application, groupId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}