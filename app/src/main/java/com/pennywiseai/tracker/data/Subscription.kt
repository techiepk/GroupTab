package com.pennywiseai.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey val id: String,
    val merchantName: String,
    val amount: Double,
    val frequency: SubscriptionFrequency,
    val nextPaymentDate: Long,
    val lastPaymentDate: Long,
    val active: Boolean = true,
    val transactionIds: List<String> = emptyList(),
    // Enhanced lifecycle fields
    val startDate: Long,
    val endDate: Long? = null,
    val cancellationDate: Long? = null,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val category: TransactionCategory = TransactionCategory.SUBSCRIPTION,
    val description: String? = null,
    val paymentCount: Int = 0,
    val totalPaid: Double = 0.0,
    val lastAmountPaid: Double = amount,
    val averageAmount: Double = amount,
    val isEMandate: Boolean = false // Flag to identify E-Mandate based subscriptions
)

enum class SubscriptionFrequency(val days: Int) {
    WEEKLY(7),
    MONTHLY(30),
    QUARTERLY(90),
    YEARLY(365)
}

enum class SubscriptionStatus {
    ACTIVE,         // Currently active subscription
    PAUSED,         // Temporarily paused
    CANCELLED,      // User cancelled
    EXPIRED,        // Natural expiry
    FAILED,         // Payment failed/suspended
    TRIAL           // In trial period
}