package com.pennywiseai.tracker.utils

import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.data.Subscription
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.repository.TransactionGroupRepository
import com.pennywiseai.tracker.database.CategorySpending
import kotlinx.coroutines.flow.firstOrNull
import java.util.*
import java.text.SimpleDateFormat
import kotlin.math.abs
import android.util.Log

/**
 * Generates dynamic, contextual AI insights based on transaction patterns
 */
class AIInsightsGenerator(
    private val transactionRepository: TransactionRepository,
    private val groupRepository: TransactionGroupRepository
) {
    
    companion object {
        private const val TAG = "AIInsightsGenerator"
        private val SPENDING_THRESHOLDS = mapOf(
            "very_high" to 100000.0,
            "high" to 50000.0,
            "moderate" to 20000.0,
            "low" to 5000.0
        )
    }
    
    data class AIInsight(
        val title: String,
        val message: String,
        val type: InsightType,
        val priority: InsightPriority,
        val actionable: Boolean = true,
        val icon: String = "💡"
    )
    
    enum class InsightType {
        SPENDING_ALERT,
        SAVINGS_OPPORTUNITY,
        TREND_ANALYSIS,
        SUBSCRIPTION_REMINDER,
        CATEGORY_INSIGHT,
        ACHIEVEMENT,
        RECOMMENDATION,
        WARNING
    }
    
    enum class InsightPriority {
        HIGH,
        MEDIUM,
        LOW
    }
    
    suspend fun generateInsights(): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        try {
            // Get current month data
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            
            // Current month range
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val monthStart = calendar.timeInMillis
            val monthEnd = System.currentTimeMillis()
            
            // Previous month range
            calendar.add(Calendar.MONTH, -1)
            val prevMonthStart = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            val prevMonthEnd = calendar.timeInMillis - 1
            
            // Get spending data
            val currentSpending = transactionRepository.getTotalSpendingInPeriod(monthStart, monthEnd)
            val previousSpending = transactionRepository.getTotalSpendingInPeriod(prevMonthStart, prevMonthEnd)
            
            // Get transactions
            val currentTransactions = transactionRepository.getTransactionsByDateRange(monthStart, monthEnd).firstOrNull() ?: emptyList()
            val previousTransactions = transactionRepository.getTransactionsByDateRange(prevMonthStart, prevMonthEnd).firstOrNull() ?: emptyList()
            
            // Get category spending
            val categorySpending = transactionRepository.getCategorySpending(monthStart, monthEnd)
            val prevCategorySpending = transactionRepository.getCategorySpending(prevMonthStart, prevMonthEnd)
            
            // Get subscriptions
            val activeSubscriptions = transactionRepository.getActiveSubscriptions().firstOrNull() ?: emptyList()
            
            // Generate spending trend insights
            insights.addAll(generateSpendingTrendInsights(currentSpending, previousSpending, currentTransactions, previousTransactions))
            
            // Generate category insights
            insights.addAll(generateCategoryInsights(categorySpending, prevCategorySpending))
            
            // Generate subscription insights
            insights.addAll(generateSubscriptionInsights(activeSubscriptions, currentSpending))
            
            // Generate daily pattern insights
            insights.addAll(generateDailyPatternInsights(currentTransactions))
            
            // Generate achievement insights
            insights.addAll(generateAchievementInsights(currentSpending, previousSpending, currentTransactions))
            
            // Sort by priority
            return insights.sortedByDescending { it.priority.ordinal }
                .take(5) // Return top 5 most relevant insights
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating insights", e)
            return listOf(
                AIInsight(
                    title = "Welcome to PennyWise AI",
                    message = "Start by scanning your SMS messages to get personalized financial insights",
                    type = InsightType.RECOMMENDATION,
                    priority = InsightPriority.MEDIUM,
                    icon = "🚀"
                )
            )
        }
    }
    
    private fun generateSpendingTrendInsights(
        currentSpending: Double,
        previousSpending: Double,
        currentTransactions: List<Transaction>,
        previousTransactions: List<Transaction>
    ): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        if (currentSpending == 0.0 && currentTransactions.isEmpty()) {
            return listOf(
                AIInsight(
                    title = "No Data Yet",
                    message = "Scan your SMS messages to start tracking your spending automatically",
                    type = InsightType.RECOMMENDATION,
                    priority = InsightPriority.HIGH,
                    icon = "📱"
                )
            )
        }
        
        // Calculate spending change
        val spendingChange = if (previousSpending > 0) {
            ((currentSpending - previousSpending) / previousSpending) * 100
        } else {
            0.0
        }
        
        // Spending trend insight
        when {
            spendingChange > 30 -> {
                insights.add(AIInsight(
                    title = "Spending Alert",
                    message = "Your spending is up ${spendingChange.toInt()}% compared to last month (₹${CurrencyFormatter.formatCompact(currentSpending - previousSpending)} more)",
                    type = InsightType.SPENDING_ALERT,
                    priority = InsightPriority.HIGH,
                    icon = "📈"
                ))
            }
            spendingChange > 10 -> {
                insights.add(AIInsight(
                    title = "Spending Increase",
                    message = "You're spending ${spendingChange.toInt()}% more than last month. Consider reviewing your expenses",
                    type = InsightType.WARNING,
                    priority = InsightPriority.MEDIUM,
                    icon = "⚠️"
                ))
            }
            spendingChange < -20 -> {
                insights.add(AIInsight(
                    title = "Great Progress!",
                    message = "You've reduced spending by ${abs(spendingChange).toInt()}% compared to last month. You saved ₹${CurrencyFormatter.formatCompact(previousSpending - currentSpending)}!",
                    type = InsightType.ACHIEVEMENT,
                    priority = InsightPriority.HIGH,
                    icon = "🎉"
                ))
            }
            spendingChange < -10 -> {
                insights.add(AIInsight(
                    title = "Good Job!",
                    message = "Your spending is down ${abs(spendingChange).toInt()}% from last month",
                    type = InsightType.ACHIEVEMENT,
                    priority = InsightPriority.MEDIUM,
                    icon = "✅"
                ))
            }
        }
        
        // Average transaction insight
        if (currentTransactions.isNotEmpty()) {
            val avgTransaction = currentSpending / currentTransactions.size
            val prevAvgTransaction = if (previousTransactions.isNotEmpty()) previousSpending / previousTransactions.size else 0.0
            
            if (avgTransaction > prevAvgTransaction * 1.5 && prevAvgTransaction > 0) {
                insights.add(AIInsight(
                    title = "Larger Purchases",
                    message = "Your average transaction size increased to ${CurrencyFormatter.formatCompact(avgTransaction)} from ${CurrencyFormatter.formatCompact(prevAvgTransaction)}",
                    type = InsightType.TREND_ANALYSIS,
                    priority = InsightPriority.MEDIUM,
                    icon = "💳"
                ))
            }
        }
        
        return insights
    }
    
    private fun generateCategoryInsights(
        categorySpending: List<CategorySpending>,
        prevCategorySpending: List<CategorySpending>
    ): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        if (categorySpending.isEmpty()) return insights
        
        // Top spending category
        val topCategory = categorySpending.maxByOrNull { abs(it.total) }
        if (topCategory != null) {
            val categoryName = topCategory.category.name.replace("_", " ").lowercase()
                .replaceFirstChar { it.uppercase() }
            val percentage = (abs(topCategory.total) / categorySpending.sumOf { abs(it.total) }) * 100
            
            insights.add(AIInsight(
                title = "Top Spending Category",
                message = "$categoryName accounts for ${percentage.toInt()}% of your spending (${CurrencyFormatter.formatCompact(abs(topCategory.total))})",
                type = InsightType.CATEGORY_INSIGHT,
                priority = InsightPriority.MEDIUM,
                icon = when(topCategory.category) {
                    TransactionCategory.FOOD_DINING -> "🍔"
                    TransactionCategory.TRANSPORTATION -> "🚗"
                    TransactionCategory.SHOPPING -> "🛍️"
                    TransactionCategory.ENTERTAINMENT -> "🎬"
                    TransactionCategory.BILLS_UTILITIES -> "💡"
                    TransactionCategory.HEALTHCARE -> "🏥"
                    TransactionCategory.EDUCATION -> "📚"
                    TransactionCategory.TRAVEL -> "✈️"
                    TransactionCategory.GROCERIES -> "🛒"
                    TransactionCategory.SUBSCRIPTION -> "📱"
                    else -> "💰"
                }
            ))
        }
        
        // Find category with biggest increase
        val categoryIncreases = categorySpending.mapNotNull { current ->
            val previous = prevCategorySpending.find { it.category == current.category }
            if (previous != null && previous.total > 0) {
                val increase = abs(current.total) - abs(previous.total)
                val percentIncrease = (increase / abs(previous.total)) * 100
                if (percentIncrease > 50) {
                    Triple(current.category, increase, percentIncrease)
                } else null
            } else null
        }
        
        categoryIncreases.maxByOrNull { it.second }?.let { (category, increase, percent) ->
            val categoryName = category.name.replace("_", " ").lowercase()
                .replaceFirstChar { it.uppercase() }
            insights.add(AIInsight(
                title = "Category Alert",
                message = "$categoryName spending up ${percent.toInt()}% (₹${CurrencyFormatter.formatCompact(increase)} more than last month)",
                type = InsightType.WARNING,
                priority = InsightPriority.HIGH,
                icon = "📊"
            ))
        }
        
        return insights
    }
    
    private fun generateSubscriptionInsights(
        subscriptions: List<Subscription>,
        monthlySpending: Double
    ): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        if (subscriptions.isNotEmpty()) {
            val activeSubscriptions = subscriptions.filter { it.active }
            val totalSubscriptionCost = activeSubscriptions.sumOf { it.amount }
            
            insights.add(AIInsight(
                title = "Active Subscriptions",
                message = "You have ${activeSubscriptions.size} active subscriptions totaling ${CurrencyFormatter.formatCompact(totalSubscriptionCost)} per month",
                type = InsightType.SUBSCRIPTION_REMINDER,
                priority = InsightPriority.MEDIUM,
                icon = "📱"
            ))
            
            // Subscription percentage of spending
            if (monthlySpending > 0) {
                val subscriptionPercent = (totalSubscriptionCost / monthlySpending) * 100
                if (subscriptionPercent > 20) {
                    insights.add(AIInsight(
                        title = "Subscription Review",
                        message = "Subscriptions make up ${subscriptionPercent.toInt()}% of your monthly spending. Consider reviewing unused services",
                        type = InsightType.SAVINGS_OPPORTUNITY,
                        priority = InsightPriority.HIGH,
                        icon = "💡"
                    ))
                }
            }
            
            // Upcoming payments
            val upcomingPayments = subscriptions.filter { 
                it.active && it.nextPaymentDate > System.currentTimeMillis() &&
                it.nextPaymentDate < System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)
            }
            
            if (upcomingPayments.isNotEmpty()) {
                val totalUpcoming = upcomingPayments.sumOf { it.amount }
                insights.add(AIInsight(
                    title = "Upcoming Payments",
                    message = "${upcomingPayments.size} subscriptions (${CurrencyFormatter.formatCompact(totalUpcoming)}) due in the next 7 days",
                    type = InsightType.SUBSCRIPTION_REMINDER,
                    priority = InsightPriority.MEDIUM,
                    icon = "📅"
                ))
            }
        }
        
        return insights
    }
    
    private fun generateDailyPatternInsights(transactions: List<Transaction>): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        if (transactions.size < 10) return insights
        
        // Group by day of week
        val dayOfWeekSpending = transactions.groupBy { 
            Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_WEEK)
        }.mapValues { entry ->
            entry.value.sumOf { abs(it.amount) }
        }
        
        // Find highest spending day
        val highestDay = dayOfWeekSpending.maxByOrNull { it.value }
        if (highestDay != null) {
            val dayName = when(highestDay.key) {
                Calendar.SUNDAY -> "Sundays"
                Calendar.MONDAY -> "Mondays"
                Calendar.TUESDAY -> "Tuesdays"
                Calendar.WEDNESDAY -> "Wednesdays"
                Calendar.THURSDAY -> "Thursdays"
                Calendar.FRIDAY -> "Fridays"
                Calendar.SATURDAY -> "Saturdays"
                else -> "Unknown"
            }
            
            val avgDailySpending = dayOfWeekSpending.values.average()
            if (highestDay.value > avgDailySpending * 1.5) {
                insights.add(AIInsight(
                    title = "Spending Pattern",
                    message = "You tend to spend more on $dayName (${CurrencyFormatter.formatCompact(highestDay.value)} on average)",
                    type = InsightType.TREND_ANALYSIS,
                    priority = InsightPriority.LOW,
                    icon = "📅"
                ))
            }
        }
        
        // Check for frequent small transactions
        val smallTransactions = transactions.filter { abs(it.amount) < 100 }
        if (smallTransactions.size > transactions.size * 0.6) {
            val totalSmall = smallTransactions.sumOf { abs(it.amount) }
            insights.add(AIInsight(
                title = "Small Purchases Add Up",
                message = "${smallTransactions.size} small transactions totaling ${CurrencyFormatter.formatCompact(totalSmall)}. Consider the 'latte factor' effect",
                type = InsightType.SAVINGS_OPPORTUNITY,
                priority = InsightPriority.MEDIUM,
                icon = "☕"
            ))
        }
        
        return insights
    }
    
    private fun generateAchievementInsights(
        currentSpending: Double,
        previousSpending: Double,
        transactions: List<Transaction>
    ): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()
        
        // No-spend days
        if (transactions.isNotEmpty()) {
            val calendar = Calendar.getInstance()
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            
            val spendingDays = transactions.map { 
                Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH)
            }.distinct().size
            
            val noSpendDays = currentDay - spendingDays
            if (noSpendDays > 5) {
                insights.add(AIInsight(
                    title = "No-Spend Champion!",
                    message = "You had $noSpendDays no-spend days this month. Great self-control!",
                    type = InsightType.ACHIEVEMENT,
                    priority = InsightPriority.MEDIUM,
                    icon = "🏆"
                ))
            }
        }
        
        // Budget goals
        when {
            currentSpending < 10000 -> {
                insights.add(AIInsight(
                    title = "Frugal Living",
                    message = "Excellent budget control! You're spending wisely",
                    type = InsightType.ACHIEVEMENT,
                    priority = InsightPriority.LOW,
                    icon = "🌟"
                ))
            }
            currentSpending < 20000 -> {
                insights.add(AIInsight(
                    title = "Smart Spender",
                    message = "You're maintaining a balanced budget. Keep it up!",
                    type = InsightType.ACHIEVEMENT,
                    priority = InsightPriority.LOW,
                    icon = "💚"
                ))
            }
        }
        
        return insights
    }
    
    fun generateQuickInsight(
        monthlySpending: Double,
        transactionCount: Int,
        topCategory: String?
    ): AIInsight {
        // Generate a single quick insight for immediate display
        return when {
            monthlySpending == 0.0 -> AIInsight(
                title = "Get Started",
                message = "Scan your SMS messages to track spending automatically with AI",
                type = InsightType.RECOMMENDATION,
                priority = InsightPriority.HIGH,
                icon = "🚀"
            )
            monthlySpending > SPENDING_THRESHOLDS["very_high"]!! -> AIInsight(
                title = "High Spending Alert",
                message = "You've spent ${CurrencyFormatter.formatCompact(monthlySpending)} this month. Review your expenses",
                type = InsightType.SPENDING_ALERT,
                priority = InsightPriority.HIGH,
                icon = "🚨"
            )
            monthlySpending > SPENDING_THRESHOLDS["high"]!! -> AIInsight(
                title = "Spending Check",
                message = "Monthly spending at ${CurrencyFormatter.formatCompact(monthlySpending)}. Consider your budget goals",
                type = InsightType.WARNING,
                priority = InsightPriority.MEDIUM,
                icon = "⚠️"
            )
            transactionCount > 50 -> AIInsight(
                title = "Active Month",
                message = "$transactionCount transactions tracked. ${topCategory?.let { "Top category: $it" } ?: ""}",
                type = InsightType.TREND_ANALYSIS,
                priority = InsightPriority.LOW,
                icon = "📊"
            )
            else -> AIInsight(
                title = "On Track",
                message = "You're doing great! ${CurrencyFormatter.formatCompact(monthlySpending)} spent across $transactionCount transactions",
                type = InsightType.ACHIEVEMENT,
                priority = InsightPriority.LOW,
                icon = "✅"
            )
        }
    }
}