package com.pennywiseai.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to track the latest balance for each bank account
 */
@Entity(tableName = "account_balances")
data class AccountBalance(
    @PrimaryKey 
    val accountId: String, // Format: "BANK_LAST4" e.g., "HDFC_1234"
    val balance: Double,
    val lastUpdated: Long, // Timestamp when balance was last updated
    val bankName: String, // e.g., "HDFC Bank"
    val last4Digits: String // e.g., "1234"
)