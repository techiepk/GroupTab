package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.TransactionGroup
import com.pennywiseai.tracker.data.GroupedTransaction
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionGroupRepository
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.grouping.TransactionGroupingService
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import android.util.Log

class TransactionGroupViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "TransactionGroupViewModel"
    }

    private val database = AppDatabase.getDatabase(application)
    private val transactionRepository = TransactionRepository(database)
    private val groupRepository = TransactionGroupRepository(database)
    private val groupingService = TransactionGroupingService(groupRepository, transactionRepository, database)

    private val _isGrouping = MutableLiveData(false)
    val isGrouping: LiveData<Boolean> = _isGrouping

    private val _groupingStatus = MutableLiveData<String>()
    val groupingStatus: LiveData<String> = _groupingStatus

    private val _ungroupedCount = MutableLiveData<Int>()
    val ungroupedCount: LiveData<Int> = _ungroupedCount

    private val _dateRangeFilter = MutableStateFlow<Pair<Long, Long>?>(null)

    val groupedTransactions: LiveData<List<GroupedTransaction>> = _dateRangeFilter.flatMapLatest { dateRange ->
        groupRepository.getAllActiveGroups().map { groupList ->
            val groupedTransactionsList = mutableListOf<GroupedTransaction>()
            for (group in groupList) {
                val allTransactions = groupRepository.getAllTransactionsForGroup(group.id).first()

                val filteredTransactions = dateRange?.let { (startDate, endDate) ->
                    allTransactions.filter { transaction ->
                        transaction.date >= startDate && transaction.date <= endDate
                    }
                } ?: allTransactions

                if (filteredTransactions.isNotEmpty()) {
                    val filteredGroup = group.copy(
                        transactionCount = filteredTransactions.size,
                        totalAmount = filteredTransactions.sumOf { it.amount }
                    )

                    val groupedTransaction = GroupedTransaction(
                        group = filteredGroup,
                        transactions = filteredTransactions,
                        recentTransactions = filteredTransactions.take(3)
                    )
                    groupedTransactionsList.add(groupedTransaction)
                }
            }
            groupedTransactionsList
        }
    }.asLiveData()

    // Get transaction groups only
    val transactionGroups: LiveData<List<TransactionGroup>> =
        groupRepository.getAllActiveGroups().asLiveData()

    

    init {
        // Clean up any empty groups on startup
        cleanupEmptyGroups()
        // Load initial ungrouped count
        loadUngroupedCount()

    }

    /**
     * Set date range filter for grouped transactions
     */
    fun setDateRangeFilter(startDate: Long?, endDate: Long?) {
        _dateRangeFilter.value = if (startDate != null && endDate != null) {
            Pair(startDate, endDate)
        } else {
            null
        }
    }

    /**
     * Trigger automatic grouping of transactions
     */
    fun startAutoGrouping() {
        viewModelScope.launch {
            try {
                _isGrouping.value = true
                _groupingStatus.value = "Starting automatic grouping..."

                groupingService.autoGroupTransactions()

                _groupingStatus.value = "Grouping completed successfully!"
                loadUngroupedCount()


            } catch (e: Exception) {
                Log.e(TAG, "❌ Auto-grouping failed: ${e.message}", e)
                _groupingStatus.value = "Grouping failed: ${e.message}"
            } finally {
                _isGrouping.value = false
            }
        }
    }

    /**
     * Load count of ungrouped transactions
     */
    fun loadUngroupedCount() {
        viewModelScope.launch {
            try {
                val count = groupRepository.getUngroupedTransactionCount()
                _ungroupedCount.value = count
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load ungrouped count: ${e.message}", e)
                _ungroupedCount.value = 0
            }
        }
    }



    /**
     * Create a manual group
     */
    fun createManualGroup(
        name: String,
        merchantPattern: String,
        category: com.pennywiseai.tracker.data.TransactionCategory
    ) {
        viewModelScope.launch {
            try {
                _groupingStatus.value = "Creating group: $name"

                groupRepository.createGroup(
                    name = name,
                    merchantPattern = merchantPattern,
                    category = category,
                    groupingType = com.pennywiseai.tracker.data.GroupingType.MANUAL,
                    isAutoGenerated = false
                )

                _groupingStatus.value = "Group created: $name"


            } catch (e: Exception) {
                _groupingStatus.value = "Failed to create group: ${e.message}"
            }
        }
    }

    /**
     * Delete a group
     */
    fun deleteGroup(group: TransactionGroup) {
        viewModelScope.launch {
            try {
                _groupingStatus.value = "Deleting group: ${group.name}"

                groupRepository.deleteGroup(group)


                _groupingStatus.value = "Group deleted: ${group.name}"

            } catch (e: Exception) {
                _groupingStatus.value = "Failed to delete group: ${e.message}"
            }
        }
    }

    /**
     * Assign a transaction to a specific group manually
     */
    fun assignTransactionToGroup(transactionId: String, groupId: String) {
        viewModelScope.launch {
            try {

                // Remove from any existing groups first
                groupRepository.removeTransactionFromAllGroups(transactionId)

                // Add to new group
                groupRepository.addTransactionToGroup(
                    transactionId = transactionId,
                    groupId = groupId,
                    confidence = 1.0f,
                    isManual = true
                )

                // Update group totals
                groupRepository.updateGroupTotals(groupId)




            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to assign transaction to group: ${e.message}", e)
            }
        }
    }

    /**
     * Remove a transaction from all groups
     */
    fun removeTransactionFromAllGroups(transactionId: String) {
        viewModelScope.launch {
            try {

                groupRepository.removeTransactionFromAllGroups(transactionId)




            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to remove transaction from groups: ${e.message}", e)
            }
        }
    }

    /**
     * Get detailed statistics about grouping
     */
    fun getGroupingStats(): LiveData<GroupingStats> {
        return combine(
            transactionRepository.getAllTransactions(),
            groupRepository.getAllActiveGroups()
        ) { allTransactions, groups ->
            val totalTransactions = allTransactions.size
            val groupedTransactionsCount = groups.sumOf { it.transactionCount }
            val ungroupedCount = totalTransactions - groupedTransactionsCount
            val groupingPercentage = if (totalTransactions > 0) {
                (groupedTransactionsCount.toFloat() / totalTransactions) * 100
            } else 0f

            GroupingStats(
                totalTransactions = totalTransactions,
                groupedTransactions = groupedTransactionsCount,
                ungroupedTransactions = ungroupedCount,
                totalGroups = groups.size,
                groupingPercentage = groupingPercentage
            )
        }.asLiveData()
    }

    /**
     * Clean up empty groups
     */
    private fun cleanupEmptyGroups() {
        viewModelScope.launch {
            try {
                val groups = groupRepository.getAllActiveGroups().first()
                var deletedCount = 0

                for (group in groups) {
                    val transactionCount = groupRepository.getRecentTransactionsForGroup(group.id, 1).size
                    if (transactionCount == 0) {
                        groupRepository.deleteGroup(group)
                        deletedCount++
                    }
                }

                if (deletedCount > 0) {
                    Log.i(TAG, "✅ Deleted $deletedCount empty groups on startup")

                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to cleanup empty groups: ${e.message}", e)
            }
        }
    }
}

data class GroupingStats(
    val totalTransactions: Int,
    val groupedTransactions: Int,
    val ungroupedTransactions: Int,
    val totalGroups: Int,
    val groupingPercentage: Float
)
