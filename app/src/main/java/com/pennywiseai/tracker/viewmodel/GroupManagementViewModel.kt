package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.TransactionGroup
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionGroupRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.util.Log
import com.pennywiseai.tracker.data.GroupSortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import androidx.lifecycle.asLiveData
import android.content.Context

class GroupManagementViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "GroupManagementViewModel"
        private const val PREF_NAME = "group_preferences"
        private const val KEY_SORT_ORDER = "group_sort_order"
    }
    
    private val database = AppDatabase.getDatabase(application)
    private val groupRepository = TransactionGroupRepository(database)
    private val sharedPrefs = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    // Load saved sort order
    private val savedSortOrderName = sharedPrefs.getString(KEY_SORT_ORDER, null)
    private val initialSortOrder = savedSortOrderName?.let { name ->
        GroupSortOrder.values().find { it.name == name }
    } ?: GroupSortOrder.getDefault()
    
    private val _sortOrder = MutableStateFlow(initialSortOrder)
    val sortOrder: LiveData<GroupSortOrder> = _sortOrder.asLiveData()
    
    private val _groupsFlow = MutableStateFlow<List<TransactionGroup>>(emptyList())
    
    // Combine groups with sort order
    val allGroups: LiveData<List<TransactionGroup>> = combine(
        _groupsFlow,
        _sortOrder
    ) { groups, sortOrder ->
        sortGroups(groups, sortOrder)
    }.asLiveData()
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    init {
        loadGroups()
    }
    
    private fun loadGroups() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                groupRepository.getAllActiveGroups()
                    .catch { e ->
                        Log.e(TAG, "Error loading groups", e)
                        _error.value = "Failed to load groups: ${e.message}"
                    }
                    .collectLatest { groups ->
                        _groupsFlow.value = groups
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadGroups", e)
                _error.value = "Failed to load groups: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun deleteGroup(group: TransactionGroup) {
        viewModelScope.launch {
            try {
                groupRepository.deleteGroup(group)
                _error.value = "Group '${group.name}' deleted"
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting group", e)
                _error.value = "Failed to delete group: ${e.message}"
            }
        }
    }
    
    fun mergeGroups(sourceGroup: TransactionGroup, targetGroup: TransactionGroup) {
        viewModelScope.launch {
            try {
                
                // Get all transactions from source group
                val sourceTransactions = groupRepository.getAllTransactionsForGroup(sourceGroup.id)
                    .catch { e ->
                        Log.e(TAG, "Error getting source transactions", e)
                        throw e
                    }
                    .collectLatest { transactions ->
                        // Add each transaction to target group
                        transactions.forEach { transaction ->
                            try {
                                // Remove from source group
                                groupRepository.removeTransactionFromGroup(transaction.id, sourceGroup.id)
                                
                                // Add to target group
                                groupRepository.addTransactionToGroup(
                                    transactionId = transaction.id,
                                    groupId = targetGroup.id,
                                    confidence = 1.0f,
                                    isManual = true
                                )
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to move transaction ${transaction.id}", e)
                            }
                        }
                        
                        // Delete the source group
                        groupRepository.deleteGroup(sourceGroup)
                        
                        Log.i(TAG, "Successfully merged ${transactions.size} transactions")
                        _error.value = "Merged '${sourceGroup.name}' into '${targetGroup.name}'"
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error merging groups", e)
                _error.value = "Failed to merge groups: ${e.message}"
            }
        }
    }
    
    fun refreshGroups() {
        loadGroups()
    }
    
    private fun sortGroups(groups: List<TransactionGroup>, sortOrder: GroupSortOrder): List<TransactionGroup> {
        return when (sortOrder) {
            GroupSortOrder.TRANSACTION_COUNT_DESC -> groups.sortedByDescending { it.transactionCount }
            GroupSortOrder.TRANSACTION_COUNT_ASC -> groups.sortedBy { it.transactionCount }
            GroupSortOrder.TOTAL_AMOUNT_DESC -> groups.sortedByDescending { kotlin.math.abs(it.totalAmount) }
            GroupSortOrder.TOTAL_AMOUNT_ASC -> groups.sortedBy { kotlin.math.abs(it.totalAmount) }
            GroupSortOrder.NAME_ASC -> groups.sortedBy { it.name.lowercase() }
            GroupSortOrder.NAME_DESC -> groups.sortedByDescending { it.name.lowercase() }
            GroupSortOrder.LAST_ACTIVITY_DESC -> groups.sortedByDescending { it.lastTransactionDate }
            GroupSortOrder.LAST_ACTIVITY_ASC -> groups.sortedBy { it.lastTransactionDate }
        }
    }
    
    fun setSortOrder(sortOrder: GroupSortOrder) {
        _sortOrder.value = sortOrder
        // Save to SharedPreferences
        sharedPrefs.edit().putString(KEY_SORT_ORDER, sortOrder.name).apply()
    }
}