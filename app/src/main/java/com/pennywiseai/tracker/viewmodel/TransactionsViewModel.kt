package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.map
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import com.pennywiseai.tracker.data.TimeRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import com.pennywiseai.tracker.utils.SharedEventBus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.pennywiseai.tracker.data.TransactionSortOrder
import android.content.Context
import android.content.SharedPreferences

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val PREF_NAME = "transaction_preferences"
        private const val KEY_SORT_ORDER = "sort_order"
    }
    
    // Data class to hold filter and sort parameters
    private data class FilterAndSort(
        val category: com.pennywiseai.tracker.data.TransactionCategory?,
        val merchant: String?,
        val dateRange: Pair<Long, Long>?,
        val sortOrder: TransactionSortOrder
    )
    
    private val repository = TransactionRepository(AppDatabase.getDatabase(application))
    private val sharedPrefs: SharedPreferences = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    // Filter properties using StateFlow for better reactivity
    private val _categoryFilter = MutableStateFlow<com.pennywiseai.tracker.data.TransactionCategory?>(null)
    private val _merchantFilter = MutableStateFlow<String?>(null)
    private val _dateRangeFilter = MutableStateFlow<Pair<Long, Long>?>(null)
    val dateRange: LiveData<Pair<Long, Long>?> = _dateRangeFilter.asLiveData()
    private val _timeRangeFilter = MutableStateFlow<TimeRange?>(TimeRange.THIRTY_DAYS)
    
    // Sorting property - load saved preference
    private val savedSortOrderName = sharedPrefs.getString(KEY_SORT_ORDER, null)
    private val initialSortOrder = savedSortOrderName?.let { name ->
        TransactionSortOrder.values().find { it.name == name }
    } ?: TransactionSortOrder.getDefault()
    
    private val _sortOrder = MutableStateFlow(initialSortOrder)
    val sortOrder: LiveData<TransactionSortOrder> = _sortOrder.asLiveData()
    
    // Reactive transactions using Flow transformations
    val transactions: LiveData<List<Transaction>> = combine(
        _categoryFilter,
        _merchantFilter,
        _dateRangeFilter,
        _sortOrder
    ) { category, merchant, dateRange, sortOrder ->
        FilterAndSort(category, merchant, dateRange, sortOrder)
    }.flatMapLatest { filterAndSort ->
        repository.getAllTransactions().combine(
            kotlinx.coroutines.flow.flowOf(filterAndSort)
        ) { allTransactions, filterSort ->
            
            var filtered = allTransactions
            
            // Apply category filter
            filterSort.category?.let { cat ->
                filtered = filtered.filter { it.category == cat }
            }
            
            // Apply merchant filter
            filterSort.merchant?.let { merch ->
                filtered = filtered.filter { it.merchant.equals(merch, ignoreCase = true) }
            }
            
            // Apply date range filter
            filterSort.dateRange?.let { (startDate, endDate) ->
                filtered = filtered.filter { it.date >= startDate && it.date <= endDate }
            }
            
            
            // Apply sorting
            when (filterSort.sortOrder) {
                TransactionSortOrder.DATE_DESC -> filtered.sortedByDescending { it.date }
                TransactionSortOrder.DATE_ASC -> filtered.sortedBy { it.date }
                TransactionSortOrder.AMOUNT_DESC -> filtered.sortedByDescending { kotlin.math.abs(it.amount) }
                TransactionSortOrder.AMOUNT_ASC -> filtered.sortedBy { kotlin.math.abs(it.amount) }
                TransactionSortOrder.MERCHANT_ASC -> filtered.sortedBy { it.merchant.lowercase() }
                TransactionSortOrder.MERCHANT_DESC -> filtered.sortedByDescending { it.merchant.lowercase() }
                TransactionSortOrder.CATEGORY -> filtered.sortedBy { it.category.name }
            }
        }
    }.asLiveData(viewModelScope.coroutineContext)
    
    // Filter info for UI
    private val _filterInfo = MutableLiveData<String?>()
    val filterInfo: LiveData<String?> = _filterInfo
    
    // Reactive monthly stats
    val monthlyStats: LiveData<Pair<Double, Int>> = repository.getAllTransactions().combine(
        kotlinx.coroutines.flow.flowOf(Unit)
    ) { allTransactions, _ ->
        // Get current month boundaries
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        
        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.timeInMillis
        
        val monthTransactions = allTransactions.filter { 
            it.date >= monthStart && it.date < monthEnd 
        }
        
        // Calculate total spending (sum of negative amounts)
        val totalSpending = monthTransactions
            .filter { it.amount < 0 }
            .sumOf { kotlin.math.abs(it.amount) }
        
        
        Pair(totalSpending, monthTransactions.size)
    }.asLiveData(viewModelScope.coroutineContext)
    
    // Expose monthly spending and count as separate LiveData
    val monthlySpending: LiveData<Double> = monthlyStats.map { it.first }
    val monthlyTransactionCount: LiveData<Int> = monthlyStats.map { it.second }
    
    init {
        // Set default time range
        setSelectedTimeRange(TimeRange.THIRTY_DAYS)
        
        // Observe shared events
        viewModelScope.launch {
            SharedEventBus.events.collect { event ->
                when (event) {
                    is SharedEventBus.Event.SmsScanCompleted -> {
                        // Data will automatically refresh due to reactive Flow
                        // No need to do anything - the reactive flow will update automatically
                    }
                    else -> {} // Ignore other events
                }
            }
        }
    }
    
    fun setFilters(
        category: com.pennywiseai.tracker.data.TransactionCategory? = null,
        merchant: String? = null,
        dateRange: Pair<Long, Long>? = null
    ) {
        _categoryFilter.value = category
        _merchantFilter.value = merchant
        _dateRangeFilter.value = dateRange
        _timeRangeFilter.value = null // Clear time range when using explicit date range
        
        // Update filter info for explicit date range
        val filterParts = mutableListOf<String>()
        category?.let { filterParts.add(it.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
        merchant?.let { filterParts.add(it) }
        dateRange?.let { 
            val dateFormat = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            filterParts.add("${dateFormat.format(it.first)} - ${dateFormat.format(it.second)}")
        }
        _filterInfo.value = if (filterParts.isNotEmpty()) filterParts.joinToString(" • ") else null
        
        // No need to call loadTransactions() - filters are reactive
    }
    
    fun clearFilters() {
        _categoryFilter.value = null
        _merchantFilter.value = null
        _dateRangeFilter.value = null
        _timeRangeFilter.value = TimeRange.THIRTY_DAYS // Reset to default
        _filterInfo.value = null
        
        // No need to call loadTransactions() - filters are reactive
    }
    
    // Removed loadTransactions() - now using reactive Flow transformations
    
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
    
    fun setSelectedTimeRange(timeRange: TimeRange) {
        _timeRangeFilter.value = timeRange
        
        // Convert time range to date range
        val (startTime, endTime) = getTimeRangeBounds(timeRange)
        _dateRangeFilter.value = Pair(startTime, endTime)
        
        // Update filter info
        updateFilterInfo()
        
        // No need to call loadTransactions() - filters are reactive
    }
    
    fun getTimeRangeBounds(timeRange: TimeRange): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        return when (timeRange) {
            TimeRange.SEVEN_DAYS -> {
                val startTime = now - (7 * 24 * 60 * 60 * 1000L)
                Pair(startTime, now)
            }
            TimeRange.THIRTY_DAYS -> {
                val startTime = now - (30 * 24 * 60 * 60 * 1000L)
                Pair(startTime, now)
            }
            TimeRange.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis
                Pair(startTime, now)
            }
            TimeRange.ALL_TIME -> {
                Pair(0L, now)
            }
        }
    }
    
    private fun updateFilterInfo() {
        val filterParts = mutableListOf<String>()
        
        _categoryFilter.value?.let { 
            filterParts.add(it.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) 
        }
        _merchantFilter.value?.let { filterParts.add(it) }
        _timeRangeFilter.value?.let { filterParts.add(it.getDisplayName()) }
        
        _filterInfo.value = if (filterParts.isNotEmpty()) filterParts.joinToString(" • ") else null
    }
    
    fun setSortOrder(sortOrder: TransactionSortOrder) {
        _sortOrder.value = sortOrder
        // Save to SharedPreferences
        sharedPrefs.edit().putString(KEY_SORT_ORDER, sortOrder.name).apply()
    }
}