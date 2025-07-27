package com.pennywiseai.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val amount: Double,
    val merchant: String,
    val category: TransactionCategory,
    val date: Long,
    val rawSms: String,
    val upiId: String?,
    val transactionType: TransactionType = TransactionType.UNKNOWN,
    val confidence: Float = 0.0f,
    val sender: String? = null, // SMS sender ID (e.g., HDFCBK, ICICIT)
    // Keep subscription for backward compatibility during migration
    @Deprecated("Use transactionType instead") 
    val subscription: Boolean = false
)

enum class TransactionCategory {
    FOOD_DINING,
    TRANSPORTATION,
    SHOPPING,
    ENTERTAINMENT,
    BILLS_UTILITIES,
    HEALTHCARE,
    EDUCATION,
    TRAVEL,
    GROCERIES,
    SUBSCRIPTION,
    INVESTMENT,
    TRANSFER,
    OTHER
}

enum class TransactionType {
    ONE_TIME,           // Regular purchases
    SUBSCRIPTION,       // Recurring subscriptions (Netflix, Spotify)
    RECURRING_BILL,     // Utilities, rent, insurance
    TRANSFER,           // Money transfers
    REFUND,            // Refunds/cashbacks
    INVESTMENT,        // SIP, mutual funds
    UNKNOWN            // Fallback when type cannot be determined
}

// Data class to hold transaction with its group information
data class TransactionWithGroup(
    val transaction: Transaction,
    val groupId: String? = null,
    val groupName: String? = null
)