package com.pennywiseai.tracker.data

data class FinancialInsight(
    val title: String,
    val description: String,
    val type: Type,
    val priority: Priority = Priority.MEDIUM,
    val actionText: String? = null,
    val actionQuery: String? = null,
    val amount: Double? = null
) {
    enum class Type {
        SPENDING_ALERT,
        SAVING_TIP,
        SUBSCRIPTION_ALERT,
        TREND_ANALYSIS,
        BUDGET_RECOMMENDATION
    }
    
    enum class Priority {
        HIGH, MEDIUM, LOW
    }
}