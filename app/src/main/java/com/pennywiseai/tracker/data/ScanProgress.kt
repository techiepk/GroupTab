package com.pennywiseai.tracker.data

data class ScanProgress(
    val currentMessage: Int,
    val totalMessages: Int,
    val transactionsFound: Int,
    val isComplete: Boolean = false,
    val errorMessage: String? = null
) {
    val progressPercentage: Int
        get() = if (totalMessages > 0) (currentMessage * 100) / totalMessages else 0
}