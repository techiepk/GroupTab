package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.R
import com.pennywiseai.tracker.repository.TransactionRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.map
import com.pennywiseai.tracker.utils.SharedEventBus

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = TransactionRepository(AppDatabase.getDatabase(application))
    
    enum class TimePeriod {
        THIS_MONTH,
        LAST_7_DAYS,
        LAST_30_DAYS,
        LAST_3_MONTHS,
        LAST_YEAR,
        ALL_TIME
    }
    
    enum class TransactionTypeFilter {
        EXPENSE, INCOME, BOTH
    }
    
    private val _currentPeriod = MutableLiveData(TimePeriod.THIS_MONTH)
    private val _transactionTypeFilter = MutableLiveData(TransactionTypeFilter.EXPENSE)
    private val _periodText = MutableLiveData<String>()
    val periodText: LiveData<String> = _periodText
    
    private val _overviewData = MutableLiveData<OverviewData>()
    val overviewData: LiveData<OverviewData> = _overviewData
    
    private val _expenseCategoryBreakdown = MutableLiveData<List<CategoryData>>()
    val expenseCategoryBreakdown: LiveData<List<CategoryData>> = _expenseCategoryBreakdown
    
    private val _incomeCategoryBreakdown = MutableLiveData<List<CategoryData>>()
    val incomeCategoryBreakdown: LiveData<List<CategoryData>> = _incomeCategoryBreakdown
    
    private val _topExpenseMerchants = MutableLiveData<List<MerchantData>>()
    val topExpenseMerchants: LiveData<List<MerchantData>> = _topExpenseMerchants
    
    private val _topIncomeSources = MutableLiveData<List<MerchantData>>()
    val topIncomeSources: LiveData<List<MerchantData>> = _topIncomeSources
    
    private val _dailyTrend = MutableLiveData<List<DayData>>()
    val dailyTrend: LiveData<List<DayData>> = _dailyTrend
    
    private val _insights = MutableLiveData<List<InsightData>>()
    val insights: LiveData<List<InsightData>> = _insights
    
    private val _quickStats = MutableLiveData<QuickStats>()
    val quickStats: LiveData<QuickStats> = _quickStats
    
    private val _categoryFilter = MutableLiveData<TransactionCategory?>()
    val categoryFilter: LiveData<TransactionCategory?> = _categoryFilter
    
    private val _multipleCategoryFilter = MutableLiveData<Set<TransactionCategory>?>()
    
    // For category breakdown pie chart
    val categoryData: LiveData<List<CategoryData>> = _expenseCategoryBreakdown
    
    init {
        loadAnalytics()
        observeTransactions()
        observeSharedEvents()
    }
    
    fun setTimePeriod(period: TimePeriod) {
        _currentPeriod.value = period
        loadAnalytics()
    }
    
    fun setTransactionTypeFilter(type: TransactionTypeFilter) {
        _transactionTypeFilter.value = type
        loadAnalytics()
    }
    
    fun setCategoryFilter(category: TransactionCategory?) {
        _categoryFilter.value = category
        loadAnalytics()
    }
    
    fun setMultipleCategoryFilter(categories: Set<TransactionCategory>) {
        // For multiple category filtering
        _multipleCategoryFilter.value = if (categories.isEmpty()) null else categories
        loadAnalytics()
    }
    
    fun getCurrentPeriod(): TimePeriod = _currentPeriod.value ?: TimePeriod.THIS_MONTH
    
    private fun observeTransactions() {
        // Observe transactions reactively
        viewModelScope.launch {
            repository.getAllTransactions().collect { allTransactions ->
                // When transactions update, reload analytics with the new data
                loadAnalyticsWithData(allTransactions)
            }
        }
    }
    
    private fun observeSharedEvents() {
        // Observe shared events for SMS scan completion
        viewModelScope.launch {
            SharedEventBus.events.collect { event ->
                when (event) {
                    is SharedEventBus.Event.SmsScanCompleted -> {
                        // Data will refresh automatically via observeTransactions()
                    }
                    else -> {} // Ignore other events
                }
            }
        }
    }
    
    private fun loadAnalytics() {
        viewModelScope.launch {
            val allTransactions = repository.getAllTransactionsSync()
            loadAnalyticsWithData(allTransactions)
        }
    }
    
    private fun loadAnalyticsWithData(allTransactions: List<Transaction>) {
        viewModelScope.launch {
            val (startDate, endDate) = getDateRange()
            _periodText.value = formatDateRange(startDate, endDate)
            
            
            var periodTransactions = allTransactions.filter { 
                it.date >= startDate && it.date <= endDate 
            }
            
            // Log a few sample transactions to debug
            periodTransactions.take(3).forEach { tx ->
            }
            
            // Apply transaction type filter
            periodTransactions = when (_transactionTypeFilter.value) {
                TransactionTypeFilter.EXPENSE -> periodTransactions.filter { it.amount < 0 }
                TransactionTypeFilter.INCOME -> periodTransactions.filter { it.amount > 0 }
                TransactionTypeFilter.BOTH -> periodTransactions
                else -> periodTransactions
            }
            
            // Apply category filter if set
            _categoryFilter.value?.let { category ->
                periodTransactions = periodTransactions.filter { it.category == category }
            }
            
            // Apply multiple category filter if set
            _multipleCategoryFilter.value?.let { categories ->
                periodTransactions = periodTransactions.filter { it.category in categories }
            }
            
            // Calculate overview
            calculateOverview(periodTransactions, startDate, endDate)
            
            // Calculate category breakdown
            calculateCategoryBreakdown(periodTransactions)
            
            // Calculate top merchants
            calculateTopMerchants(periodTransactions)
            
            // Calculate daily trend
            calculateDailyTrend(periodTransactions, startDate, endDate)
            
            // Generate insights (need to get income/expense totals)
            val totalIncome = periodTransactions.filter { it.amount > 0 }.sumOf { it.amount }
            val totalExpense = periodTransactions.filter { it.amount < 0 }.sumOf { abs(it.amount) }
            generateInsights(periodTransactions, totalIncome, totalExpense)
            
            // Calculate quick stats
            calculateQuickStats(periodTransactions)
        }
    }
    
    private fun getDateRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        // Set end date to end of today to include all of today's transactions
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis
        
        // Reset calendar for start date calculations
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
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            TimePeriod.LAST_YEAR -> {
                calendar.add(Calendar.YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            TimePeriod.ALL_TIME -> {
                // Set to a very early date to get all transactions
                0L
            }
            else -> endDate
        }
        
        return Pair(startDate, endDate)
    }
    
    private fun formatDateRange(startDate: Long, endDate: Long): String {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val dayFormat = SimpleDateFormat("d", Locale.getDefault())
        
        // Check if it's "This Month" - same month and year
        val startCal = Calendar.getInstance().apply { timeInMillis = startDate }
        val endCal = Calendar.getInstance().apply { timeInMillis = endDate }
        
        if (_currentPeriod.value == TimePeriod.THIS_MONTH && 
            startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH) &&
            startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)) {
            // Format as "January 1 - 12, 2025" for current month
            val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(startDate))
            val startDay = dayFormat.format(Date(startDate))
            val endDay = dayFormat.format(Date(endDate))
            return "$monthYear ($startDay - $endDay)"
        }
        
        return "${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}"
    }
    
    private fun calculateOverview(transactions: List<Transaction>, startDate: Long, endDate: Long) {
        val totalIncome = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.amount < 0 }.sumOf { abs(it.amount) }
        val netAmount = totalIncome - totalExpense
        
        
        val days = ((endDate - startDate) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        val averageDaily = totalExpense / days
        
        val savingsRate = if (totalIncome > 0) {
            ((totalIncome - totalExpense) / totalIncome * 100).toInt()
        } else 0
        
        // Calculate highest spending day
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val expensesByDay = transactions
            .filter { it.amount < 0 }
            .groupBy { dateFormat.format(Date(it.date)) }
            .mapValues { entry -> entry.value.sumOf { abs(it.amount) } }
        
        val highestSpendingDay = expensesByDay.maxByOrNull { it.value }?.let {
            Pair(it.key, it.value)
        }
        
        // Find most frequent category
        val mostFrequentCategory = transactions
            .filter { it.amount < 0 }
            .groupBy { it.category }
            .maxByOrNull { it.value.size }?.key
        
        // Calculate previous period for comparison
        val periodLength = endDate - startDate
        val previousStartDate = startDate - periodLength
        val previousEndDate = startDate
        
        // TODO: Calculate previous period comparison in a separate coroutine
        // For now, set to 0 to avoid suspend function call here
        val previousPeriodExpense = 0.0
        val expenseChangePercent = 0.0
        
        _overviewData.value = OverviewData(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netAmount = netAmount,
            averageDaily = averageDaily,
            transactionCount = transactions.size,
            savingsRate = savingsRate.coerceIn(0, 100),
            previousPeriodExpense = previousPeriodExpense,
            expenseChangePercent = expenseChangePercent,
            highestSpendingDay = highestSpendingDay,
            mostFrequentCategory = mostFrequentCategory
        )
    }
    
    private fun calculateCategoryBreakdown(transactions: List<Transaction>) {
        // Categories to exclude from expense analysis
        val excludedCategories = setOf(
            TransactionCategory.INVESTMENT,
            TransactionCategory.TRANSFER
        )
        
        // Separate income and expense transactions
        val expenseTransactions = transactions.filter { 
            it.amount < 0 && it.category !in excludedCategories
        }
        val incomeTransactions = transactions.filter { 
            it.amount > 0 && it.category !in excludedCategories
        }
        
        // Calculate expense categories
        val totalExpense = expenseTransactions.sumOf { abs(it.amount) }
        val expenseCategoryData = expenseTransactions
            .groupBy { it.category }
            .map { (category, txns) ->
                val amount = txns.sumOf { abs(it.amount) }
                val percentage = if (totalExpense > 0) (amount / totalExpense * 100).toInt() else 0
                CategoryData(
                    category = category,
                    amount = amount,
                    percentage = percentage,
                    transactionCount = txns.size,
                    icon = getCategoryIcon(category),
                    color = getCategoryColor(category)
                )
            }
            .sortedByDescending { it.amount }
        
        // Calculate income categories
        val totalIncome = incomeTransactions.sumOf { it.amount }
        val incomeCategoryData = incomeTransactions
            .groupBy { it.category }
            .map { (category, txns) ->
                val amount = txns.sumOf { it.amount }
                val percentage = if (totalIncome > 0) (amount / totalIncome * 100).toInt() else 0
                CategoryData(
                    category = category,
                    amount = amount,
                    percentage = percentage,
                    transactionCount = txns.size,
                    icon = getCategoryIcon(category),
                    color = getCategoryColor(category)
                )
            }
            .sortedByDescending { it.amount }
        
        _expenseCategoryBreakdown.value = expenseCategoryData
        _incomeCategoryBreakdown.value = incomeCategoryData
    }
    
    private fun calculateTopMerchants(transactions: List<Transaction>) {
        // Top expense merchants
        val expenseMerchantData = transactions
            .filter { it.amount < 0 }
            .groupBy { it.merchant }
            .map { (merchant, txns) ->
                MerchantData(
                    name = merchant,
                    totalAmount = txns.sumOf { abs(it.amount) },
                    transactionCount = txns.size,
                    averageAmount = txns.sumOf { abs(it.amount) } / txns.size
                )
            }
            .sortedByDescending { it.totalAmount }
            .take(10)
        
        // Top income sources
        val incomeSourceData = transactions
            .filter { it.amount > 0 }
            .groupBy { it.merchant }
            .map { (merchant, txns) ->
                MerchantData(
                    name = merchant,
                    totalAmount = txns.sumOf { it.amount },
                    transactionCount = txns.size,
                    averageAmount = txns.sumOf { it.amount } / txns.size
                )
            }
            .sortedByDescending { it.totalAmount }
            .take(10)
        
        _topExpenseMerchants.value = expenseMerchantData
        _topIncomeSources.value = incomeSourceData
    }
    
    private fun calculateDailyTrend(transactions: List<Transaction>, startDate: Long, endDate: Long) {
        val calendar = Calendar.getInstance()
        val dailyData = mutableListOf<DayData>()
        val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Debug logging
        transactions.take(5).forEach { tx ->
        }
        
        // Group transactions by date (year-month-day)
        val transactionsByDay = transactions.groupBy { tx ->
            dateKeyFormat.format(Date(tx.date))
        }
        
        // Create data for last 30 days or period
        calendar.timeInMillis = maxOf(startDate, endDate - (30L * 24 * 60 * 60 * 1000))
        val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())
        
        while (calendar.timeInMillis <= endDate) {
            val dateKey = dateKeyFormat.format(calendar.time)
            val dayTransactions = transactionsByDay[dateKey] ?: emptyList()
            val dayExpense = dayTransactions.filter { it.amount < 0 }.sumOf { abs(it.amount) }
            
            dailyData.add(DayData(
                date = calendar.timeInMillis,
                dayLabel = dayLabelFormat.format(calendar.time),
                amount = dayExpense
            ))
            
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        
        // Always provide at least some data points for the chart
        if (dailyData.isEmpty()) {
            // Generate empty data for the last 7 days if no data exists
            val cal = Calendar.getInstance()
            cal.timeInMillis = endDate
            for (i in 6 downTo 0) {
                cal.timeInMillis = endDate - (i * 24 * 60 * 60 * 1000L)
                dailyData.add(DayData(
                    date = cal.timeInMillis,
                    dayLabel = dayLabelFormat.format(cal.time),
                    amount = 0.0
                ))
            }
        }
        
        _dailyTrend.value = dailyData.takeLast(30)
    }
    
    private fun generateInsights(transactions: List<Transaction>, totalIncome: Double, totalExpense: Double) {
        val insights = mutableListOf<InsightData>()
        
        // Spending vs Income insight
        if (totalIncome > 0) {
            val spendingRate = (totalExpense / totalIncome * 100).toInt()
            when {
                spendingRate > 100 -> {
                    insights.add(InsightData(
                        type = InsightType.SPENDING_ALERT,
                        title = "Overspending Alert!",
                        description = "You spent ${spendingRate}% of your income this period",
                        icon = "⚠️"
                    ))
                }
                spendingRate > 80 -> {
                    insights.add(InsightData(
                        type = InsightType.SPENDING_ALERT,
                        title = "High Spending",
                        description = "You spent ${spendingRate}% of your income",
                        icon = "📊"
                    ))
                }
                spendingRate < 50 -> {
                    insights.add(InsightData(
                        type = InsightType.POSITIVE_TREND,
                        title = "Great Savings!",
                        description = "You saved ${100 - spendingRate}% of your income",
                        icon = "🎯"
                    ))
                }
            }
        }
        
        // Top category insight
        _expenseCategoryBreakdown.value?.firstOrNull()?.let { topCategory ->
            insights.add(InsightData(
                type = InsightType.CATEGORY_TREND,
                title = "Top Spending Category",
                description = "${topCategory.category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }} accounts for ${topCategory.percentage}% of expenses",
                icon = topCategory.icon
            ))
        }
        
        // Unusual activity (if daily average varies significantly)
        val expenseTransactions = transactions.filter { it.amount < 0 }
        if (expenseTransactions.size >= 7) {
            val dailyAmounts = expenseTransactions.groupBy { 
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date))
            }.mapValues { it.value.sumOf { tx -> abs(tx.amount) } }
            
            val avgDaily = dailyAmounts.values.average()
            val maxDaily = dailyAmounts.values.maxOrNull() ?: 0.0
            
            if (maxDaily > avgDaily * 2) {
                insights.add(InsightData(
                    type = InsightType.UNUSUAL_ACTIVITY,
                    title = "Spending Spike Detected",
                    description = "One day had unusually high spending (${String.format("%.0f", (maxDaily/avgDaily - 1) * 100)}% above average)",
                    icon = "📈"
                ))
            }
        }
        
        // New merchants
        if (_currentPeriod.value == TimePeriod.THIS_MONTH) {
            // This would need historical data to compare, simplified for now
            val uniqueMerchants = transactions.map { it.merchant }.distinct().size
            if (uniqueMerchants > 0) {
                insights.add(InsightData(
                    type = InsightType.NEW_MERCHANT,
                    title = "Merchant Diversity",
                    description = "You transacted with $uniqueMerchants different merchants",
                    icon = "🏪"
                ))
            }
        }
        
        _insights.value = insights.take(4) // Show top 4 insights
    }
    
    private fun calculateQuickStats(transactions: List<Transaction>) {
        val expenseTransactions = transactions.filter { it.amount < 0 }
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        
        // Group by day for daily stats
        val dailyExpenses = expenseTransactions.groupBy { 
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.date))
        }.mapValues { entry ->
            entry.value.sumOf { abs(it.amount) }
        }
        
        // Most expensive day
        val mostExpensiveDay = dailyExpenses.maxByOrNull { it.value }?.let { entry ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(entry.key)!!
            Pair(dateFormat.format(date), entry.value)
        }
        
        // Cheapest day (excluding days with no spending)
        val cheapestDay = dailyExpenses.filter { it.value > 0 }.minByOrNull { it.value }?.let { entry ->
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(entry.key)!!
            Pair(dateFormat.format(date), entry.value)
        }
        
        // Most frequent merchant
        val mostFrequentMerchant = transactions
            .groupBy { it.merchant }
            .mapValues { it.value.size }
            .maxByOrNull { it.value }
            ?.let { Pair(it.key, it.value) }
        
        // Largest transaction
        val largestTransaction = expenseTransactions
            .maxByOrNull { abs(it.amount) }
        
        // Days with no spending
        val startDate = transactions.minOfOrNull { it.date } ?: System.currentTimeMillis()
        val endDate = transactions.maxOfOrNull { it.date } ?: System.currentTimeMillis()
        val totalDays = ((endDate - startDate) / (1000 * 60 * 60 * 24)).toInt() + 1
        val daysWithSpending = dailyExpenses.size
        val daysWithNoSpending = (totalDays - daysWithSpending).coerceAtLeast(0)
        
        // Average transaction size
        val averageTransactionSize = if (expenseTransactions.isNotEmpty()) {
            expenseTransactions.sumOf { abs(it.amount) } / expenseTransactions.size
        } else 0.0
        
        _quickStats.value = QuickStats(
            mostExpensiveDay = mostExpensiveDay,
            cheapestDay = cheapestDay,
            mostFrequentMerchant = mostFrequentMerchant,
            largestTransaction = largestTransaction,
            daysWithNoSpending = daysWithNoSpending,
            averageTransactionSize = averageTransactionSize
        )
    }
    
    private fun getCategoryIcon(category: TransactionCategory): String {
        return when (category) {
            TransactionCategory.FOOD_DINING -> "🍔"
            TransactionCategory.TRANSPORTATION -> "🚗"
            TransactionCategory.SHOPPING -> "🛍️"
            TransactionCategory.ENTERTAINMENT -> "🎬"
            TransactionCategory.BILLS_UTILITIES -> "💡"
            TransactionCategory.HEALTHCARE -> "🏥"
            TransactionCategory.EDUCATION -> "📚"
            TransactionCategory.TRAVEL -> "✈️"
            TransactionCategory.GROCERIES -> "🛒"
            TransactionCategory.SUBSCRIPTION -> "🔄"
            TransactionCategory.INVESTMENT -> "📈"
            TransactionCategory.TRANSFER -> "💸"
            else -> "💰"
        }
    }
    
    private fun getCategoryColor(category: TransactionCategory): Int {
        return when (category) {
            TransactionCategory.FOOD_DINING -> R.color.category_food_dining
            TransactionCategory.TRANSPORTATION -> R.color.category_transportation
            TransactionCategory.SHOPPING -> R.color.category_shopping
            TransactionCategory.ENTERTAINMENT -> R.color.category_entertainment
            TransactionCategory.BILLS_UTILITIES -> R.color.category_bills_utilities
            TransactionCategory.HEALTHCARE -> R.color.category_healthcare
            TransactionCategory.EDUCATION -> R.color.category_education
            TransactionCategory.TRAVEL -> R.color.category_travel
            TransactionCategory.GROCERIES -> R.color.category_groceries
            TransactionCategory.SUBSCRIPTION -> R.color.category_subscription
            TransactionCategory.INVESTMENT -> R.color.category_investment
            TransactionCategory.TRANSFER -> R.color.category_transfer
            else -> R.color.category_other
        }
    }
    
    // Data classes
    data class OverviewData(
        val totalIncome: Double,
        val totalExpense: Double,
        val netAmount: Double,
        val averageDaily: Double,
        val transactionCount: Int,
        val savingsRate: Int,
        val previousPeriodExpense: Double = 0.0,
        val expenseChangePercent: Double = 0.0,
        val highestSpendingDay: Pair<String, Double>? = null,
        val mostFrequentCategory: TransactionCategory? = null
    )
    
    data class CategoryData(
        val category: TransactionCategory,
        val amount: Double,
        val percentage: Int,
        val transactionCount: Int,
        val icon: String,
        val color: Int
    )
    
    data class MerchantData(
        val name: String,
        val totalAmount: Double,
        val transactionCount: Int,
        val averageAmount: Double
    )
    
    data class DayData(
        val date: Long,
        val dayLabel: String,
        val amount: Double
    )
    
    data class InsightData(
        val type: InsightType,
        val title: String,
        val description: String,
        val icon: String,
        val action: String? = null
    )
    
    enum class InsightType {
        SPENDING_ALERT,
        SAVINGS_UPDATE,
        CATEGORY_TREND,
        NEW_MERCHANT,
        UNUSUAL_ACTIVITY,
        POSITIVE_TREND
    }
    
    data class QuickStats(
        val mostExpensiveDay: Pair<String, Double>?,
        val cheapestDay: Pair<String, Double>?,
        val mostFrequentMerchant: Pair<String, Int>?,
        val largestTransaction: Transaction?,
        val daysWithNoSpending: Int,
        val averageTransactionSize: Double
    )
}