package com.pennywiseai.tracker.data

enum class TransactionSortOrder(val displayName: String) {
    DATE_DESC("Date (Newest First)"),
    DATE_ASC("Date (Oldest First)"),
    AMOUNT_DESC("Amount (Highest First)"),
    AMOUNT_ASC("Amount (Lowest First)"),
    MERCHANT_ASC("Merchant (A-Z)"),
    MERCHANT_DESC("Merchant (Z-A)"),
    CATEGORY("Category");
    
    companion object {
        fun getDefault() = DATE_DESC
    }
}