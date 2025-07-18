package com.pennywiseai.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val historicalScanDays: Int = 30,
    val lastScanTimestamp: Long = 0,
    val autoCategorizationEnabled: Boolean = true,
    val subscriptionDetectionEnabled: Boolean = true,
    val hasCompletedInitialScan: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val onboardingCompletedAt: Long = 0
)

enum class ScanTimeframe(val days: Int, val displayName: String) {
    ONE_DAY(1, "1 Day"),
    SEVEN_DAYS(7, "7 Days"),
    FOURTEEN_DAYS(14, "14 Days"),
    THIRTY_DAYS(30, "30 Days")
}