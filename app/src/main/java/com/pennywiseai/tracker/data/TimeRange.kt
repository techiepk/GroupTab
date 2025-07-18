package com.pennywiseai.tracker.data

enum class TimeRange {
    SEVEN_DAYS,
    THIRTY_DAYS,
    THIS_MONTH,
    ALL_TIME;
    
    fun getDaysCount(): Int? {
        return when (this) {
            SEVEN_DAYS -> 7
            THIRTY_DAYS -> 30
            THIS_MONTH -> null // Calculate dynamically
            ALL_TIME -> null // No limit
        }
    }
    
    fun getDisplayName(): String {
        return when (this) {
            SEVEN_DAYS -> "7D"
            THIRTY_DAYS -> "30D"
            THIS_MONTH -> "This Month"
            ALL_TIME -> "All Time"
        }
    }
}