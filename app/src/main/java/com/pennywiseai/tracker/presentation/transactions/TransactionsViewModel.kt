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
    
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()
    
    init {
        // Combine search query and period filter
        combine(
            searchQuery.debounce(300), // Debounce search for performance
            selectedPeriod
        ) { query, period ->
            Pair(query, period)
        }.flatMapLatest { (query, period) ->
            getFilteredTransactions(query, period)
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
    
    private fun getFilteredTransactions(
        searchQuery: String,
        period: TimePeriod
    ): Flow<List<TransactionEntity>> {
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
                LocalDateTime.MIN to LocalDateTime.MAX
            }
        }
        
        return if (searchQuery.isBlank()) {
            transactionRepository.getTransactionsBetweenDates(startDate, endDate)
        } else {
            // Search within the date range
            transactionRepository.searchTransactions(searchQuery)
                .map { transactions ->
                    transactions.filter { it.dateTime in startDate..endDate }
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