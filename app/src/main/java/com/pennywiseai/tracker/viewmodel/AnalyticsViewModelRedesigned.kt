package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class AnalyticsViewModelRedesigned(application: Application) : AndroidViewModel(application) {

    private val repository = TransactionRepository(AppDatabase.getDatabase(application))

    enum class TimePeriod {
        THIS_MONTH,
        LAST_7_DAYS,
        LAST_30_DAYS,
        LAST_3_MONTHS,
        LAST_YEAR
    }

    private val _currentPeriod = MutableLiveData(TimePeriod.THIS_MONTH)

    private val _totalSpending = MutableLiveData<Double>()
    val totalSpending: LiveData<Double> = _totalSpending

    private val _dailyAverage = MutableLiveData<Double>()
    val dailyAverage: LiveData<Double> = _dailyAverage

    private val _spendingTrend = MutableLiveData<List<DayData>>()
    val spendingTrend: LiveData<List<DayData>> = _spendingTrend

    private val _topCategories = MutableLiveData<List<CategoryData>>()
    val topCategories: LiveData<List<CategoryData>> = _topCategories

    private val _aiInsight = MutableLiveData<String>()
    val aiInsight: LiveData<String> = _aiInsight

    init {
        loadAnalytics()
    }

    fun setTimePeriod(period: TimePeriod) {
        _currentPeriod.value = period
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            val (startDate, endDate) = getDateRange()
            val allTransactions = repository.getAllTransactionsSync()
            val periodTransactions = allTransactions.filter {
                it.date >= startDate && it.date <= endDate
            }

            calculateSummary(periodTransactions, startDate, endDate)
            calculateSpendingTrend(periodTransactions, startDate, endDate)
            calculateTopCategories(periodTransactions)
            generateInsight(periodTransactions)
        }
    }

    private fun calculateSummary(transactions: List<Transaction>, startDate: Long, endDate: Long) {
        val totalExpense = transactions.filter { it.amount < 0 }.sumOf { abs(it.amount) }
        val days = ((endDate - startDate) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        val averageDaily = totalExpense / days

        _totalSpending.value = totalExpense
        _dailyAverage.value = averageDaily
    }

    private fun calculateSpendingTrend(transactions: List<Transaction>, startDate: Long, endDate: Long) {
        val dailyData = mutableListOf<DayData>()
        val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val transactionsByDay = transactions.groupBy { tx ->
            dateKeyFormat.format(Date(tx.date))
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate

        while (calendar.timeInMillis <= endDate) {
            val dateKey = dateKeyFormat.format(calendar.time)
            val dayTransactions = transactionsByDay[dateKey] ?: emptyList()
            val dayExpense = dayTransactions.filter { it.amount < 0 }.sumOf { abs(it.amount) }

            dailyData.add(DayData(calendar.timeInMillis, dayExpense))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        _spendingTrend.value = dailyData
    }

    private fun calculateTopCategories(transactions: List<Transaction>) {
        val expenseTransactions = transactions.filter { it.amount < 0 }
        val totalExpense = expenseTransactions.sumOf { abs(it.amount) }

        val categoryData = expenseTransactions
            .groupBy { it.category }
            .map { (category, txns) ->
                val amount = txns.sumOf { abs(it.amount) }
                val percentage = if (totalExpense > 0) (amount / totalExpense * 100).toInt() else 0
                CategoryData(category, amount, percentage)
            }
            .sortedByDescending { it.amount }
            .take(5)

        _topCategories.value = categoryData
    }

    private fun generateInsight(transactions: List<Transaction>) {
        if (transactions.isEmpty()) {
            _aiInsight.value = "No transaction data available for this period."
            return
        }

        val totalExpense = transactions.filter { it.amount < 0 }.sumOf { abs(it.amount) }
        _aiInsight.value = "Your spending for this period was ${String.format("%.2f", totalExpense)}"
    }

    private fun getDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        calendar.timeInMillis = System.currentTimeMillis()

        val startDate = when (_currentPeriod.value) {
            TimePeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            TimePeriod.LAST_7_DAYS -> {
                endDate - (7L * 24 * 60 * 60 * 1000)
            }
            TimePeriod.LAST_30_DAYS -> {
                endDate - (30L * 24 * 60 * 60 * 1000)
            }
            TimePeriod.LAST_3_MONTHS -> {
                calendar.add(Calendar.MONTH, -3)
                calendar.timeInMillis
            }
            TimePeriod.LAST_YEAR -> {
                calendar.add(Calendar.YEAR, -1)
                calendar.timeInMillis
            }
            else -> endDate
        }

        return Pair(startDate, endDate)
    }

    data class DayData(val timestamp: Long, val amount: Double)
    data class CategoryData(val category: TransactionCategory, val amount: Double, val percentage: Int)
}
