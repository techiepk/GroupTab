package com.pennywiseai.tracker.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _selectedPeriod = MutableStateFlow(TimePeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()
    
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    
    init {
        loadAnalytics()
    }
    
    fun selectPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
        loadAnalytics()
    }
    
    private fun loadAnalytics() {
        viewModelScope.launch {
            val dateRange = getDateRange(_selectedPeriod.value)
            
            transactionRepository.getTransactionsBetweenDates(
                startDate = dateRange.first,
                endDate = dateRange.second
            ).collect { transactions ->
                // Filter only expenses
                val expenses = transactions.filter { it.transactionType == com.pennywiseai.tracker.data.database.entity.TransactionType.EXPENSE }
                
                // Calculate total
                val totalSpending = expenses.sumOf { it.amount.toDouble() }.toBigDecimal()
                
                // Group by category
                val categoryBreakdown = expenses
                    .groupBy { it.category ?: "Others" }
                    .map { (categoryName, txns) -> 
                        val categoryTotal = txns.sumOf { it.amount.toDouble() }.toBigDecimal()
                        CategoryData(
                            name = categoryName,
                            amount = categoryTotal,
                            percentage = if (totalSpending > BigDecimal.ZERO) {
                                (categoryTotal.divide(totalSpending, 4, java.math.RoundingMode.HALF_UP) * BigDecimal(100)).toFloat()
                            } else 0f,
                            transactionCount = txns.size
                        )
                    }
                    .sortedByDescending { it.amount }
                
                // Group by merchant
                val merchantBreakdown = expenses
                    .groupBy { it.merchantName }
                    .mapValues { (merchant, txns) -> 
                        MerchantData(
                            name = merchant,
                            amount = txns.sumOf { it.amount.toDouble() }.toBigDecimal(),
                            transactionCount = txns.size,
                            isSubscription = txns.any { it.isRecurring }
                        )
                    }
                    .values
                    .sortedByDescending { it.amount }
                    .take(10) // Top 10 merchants
                
                _uiState.value = AnalyticsUiState(
                    totalSpending = totalSpending,
                    categoryBreakdown = categoryBreakdown,
                    topMerchants = merchantBreakdown,
                    isLoading = false
                )
            }
        }
    }
    
    private fun getDateRange(period: TimePeriod): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (period) {
            TimePeriod.THIS_MONTH -> {
                val start = YearMonth.now().atDay(1)
                val end = today
                start to end
            }
            TimePeriod.LAST_MONTH -> {
                val lastMonth = YearMonth.now().minusMonths(1)
                val start = lastMonth.atDay(1)
                val end = lastMonth.atEndOfMonth()
                start to end
            }
            TimePeriod.LAST_3_MONTHS -> {
                val start = today.minusMonths(3)
                val end = today
                start to end
            }
        }
    }
}

data class AnalyticsUiState(
    val totalSpending: BigDecimal = BigDecimal.ZERO,
    val categoryBreakdown: List<CategoryData> = emptyList(),
    val topMerchants: List<MerchantData> = emptyList(),
    val isLoading: Boolean = true
)

data class CategoryData(
    val name: String,
    val amount: BigDecimal,
    val percentage: Float,
    val transactionCount: Int
)

data class MerchantData(
    val name: String,
    val amount: BigDecimal,
    val transactionCount: Int,
    val isSubscription: Boolean
)

enum class TimePeriod {
    THIS_MONTH,
    LAST_MONTH,
    LAST_3_MONTHS
}