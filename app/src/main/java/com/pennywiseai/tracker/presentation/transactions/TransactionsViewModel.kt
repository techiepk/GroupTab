package com.pennywiseai.tracker.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedPeriod = MutableStateFlow(TimePeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()
    
    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()
    
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()
    
    private val _deletedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val deletedTransaction: StateFlow<TransactionEntity?> = _deletedTransaction.asStateFlow()
    
    init {
        // Combine all filters: search query, period, and category
        combine(
            searchQuery.debounce(300), // Debounce search for performance
            selectedPeriod,
            categoryFilter
        ) { query, period, category ->
            Triple(query, period, category)
        }.flatMapLatest { (query, period, category) ->
            getFilteredTransactions(query, period, category)
        }.onEach { transactions ->
            _uiState.value = _uiState.value.copy(
                transactions = transactions,
                groupedTransactions = groupTransactionsByDate(transactions),
                isLoading = false
            )
        }.launchIn(viewModelScope)
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun selectPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }
    
    fun setCategoryFilter(category: String) {
        println("DEBUG: Setting category filter to: '$category'")
        _categoryFilter.value = category
    }
    
    fun clearCategoryFilter() {
        _categoryFilter.value = null
    }
    
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            _deletedTransaction.value = transaction
            transactionRepository.softDeleteTransaction(transaction)
        }
    }
    
    fun undoDelete() {
        _deletedTransaction.value?.let { transaction ->
            viewModelScope.launch {
                transactionRepository.undoDeleteTransaction(transaction)
                _deletedTransaction.value = null
            }
        }
    }
    
    private fun getFilteredTransactions(
        searchQuery: String,
        period: TimePeriod,
        category: String?
    ): Flow<List<TransactionEntity>> {
        // Start with the base flow based on category filter
        val baseFlow = if (category != null) {
            println("DEBUG: Filtering by category: '$category'")
            transactionRepository.getTransactionsByCategory(category)
        } else {
            transactionRepository.getAllTransactions()
        }
        
        // Apply period filter
        val periodFilteredFlow = when (period) {
            TimePeriod.ALL -> baseFlow
            else -> {
                val (startDate, endDate) = when (period) {
                    TimePeriod.THIS_MONTH -> {
                        val now = YearMonth.now()
                        now.atDay(1).atStartOfDay() to now.atEndOfMonth().atTime(23, 59, 59)
                    }
                    TimePeriod.LAST_MONTH -> {
                        val lastMonth = YearMonth.now().minusMonths(1)
                        lastMonth.atDay(1).atStartOfDay() to lastMonth.atEndOfMonth().atTime(23, 59, 59)
                    }
                    TimePeriod.ALL -> {
                        // This case is handled above, but compiler needs it
                        LocalDateTime.MIN to LocalDateTime.MAX
                    }
                }
                
                baseFlow.map { transactions ->
                    transactions.filter { it.dateTime in startDate..endDate }
                }
            }
        }
        
        // Apply search filter
        return if (searchQuery.isBlank()) {
            periodFilteredFlow
        } else {
            periodFilteredFlow.map { transactions ->
                transactions.filter { transaction ->
                    transaction.merchantName.contains(searchQuery, ignoreCase = true) ||
                    transaction.description?.contains(searchQuery, ignoreCase = true) == true
                }
            }
        }
    }
    
    private fun groupTransactionsByDate(
        transactions: List<TransactionEntity>
    ): Map<DateGroup, List<TransactionEntity>> {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val weekStart = today.minusWeeks(1)
        
        return transactions.groupBy { transaction ->
            val transactionDate = transaction.dateTime.toLocalDate()
            when {
                transactionDate == today -> DateGroup.TODAY
                transactionDate == yesterday -> DateGroup.YESTERDAY
                transactionDate > weekStart -> DateGroup.THIS_WEEK
                else -> DateGroup.EARLIER
            }
        }
    }
}

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val groupedTransactions: Map<DateGroup, List<TransactionEntity>> = emptyMap(),
    val isLoading: Boolean = true
)

enum class TimePeriod(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    ALL("All")
}

enum class DateGroup(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    EARLIER("Earlier")
}