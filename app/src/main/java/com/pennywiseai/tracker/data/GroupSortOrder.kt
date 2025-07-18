package com.pennywiseai.tracker.data

enum class GroupSortOrder(val displayName: String) {
    TRANSACTION_COUNT_DESC("Transaction Count (High to Low)"),
    TRANSACTION_COUNT_ASC("Transaction Count (Low to High)"),
    TOTAL_AMOUNT_DESC("Total Amount (High to Low)"),
    TOTAL_AMOUNT_ASC("Total Amount (Low to High)"),
    NAME_ASC("Group Name (A-Z)"),
    NAME_DESC("Group Name (Z-A)"),
    LAST_ACTIVITY_DESC("Last Activity (Recent First)"),
    LAST_ACTIVITY_ASC("Last Activity (Oldest First)");
    
    companion object {
        fun getDefault() = TRANSACTION_COUNT_DESC
    }
}