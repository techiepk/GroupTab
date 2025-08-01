package com.pennywiseai.tracker.data.parser

import com.pennywiseai.tracker.data.database.entity.TransactionEntity
import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Represents a parsed transaction from an SMS message.
 */
data class ParsedTransaction(
    val amount: BigDecimal,
    val type: TransactionType,
    val merchant: String?,
    val reference: String?,
    val accountLast4: String?,
    val balance: BigDecimal?,
    val smsBody: String,
    val sender: String,
    val timestamp: Long,
    val bankName: String
) {
    /**
     * Generates a unique transaction ID based on sender, amount, and timestamp.
     * This helps in duplicate detection.
     */
    fun generateTransactionId(): String {
        val normalizedAmount = amount.setScale(2, java.math.RoundingMode.HALF_UP)
        val data = "$sender|$normalizedAmount|$timestamp"
        
        return MessageDigest.getInstance("MD5")
            .digest(data.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Converts this parsed transaction to a database entity.
     */
    fun toEntity(): TransactionEntity {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
        
        return TransactionEntity(
            id = 0, // Auto-generated
            amount = amount,
            merchantName = merchant ?: "Unknown Merchant",
            category = determineCategory(),
            transactionType = type,
            dateTime = dateTime,
            description = null,
            smsBody = smsBody,
            bankName = bankName,
            accountNumber = accountLast4,
            balanceAfter = balance,
            isRecurring = false, // Will be determined later
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
    
    /**
     * Determines the category based on merchant name.
     * This is a simple implementation - can be enhanced later.
     */
    private fun determineCategory(): String {
        val merchantLower = merchant?.lowercase() ?: return "Others"
        
        return when {
            // Food & Dining
            merchantLower.contains("swiggy") || 
            merchantLower.contains("zomato") || 
            merchantLower.contains("restaurant") ||
            merchantLower.contains("cafe") ||
            merchantLower.contains("food") -> "Food & Dining"
            
            // Transportation
            merchantLower.contains("uber") || 
            merchantLower.contains("ola") || 
            merchantLower.contains("rapido") ||
            merchantLower.contains("petrol") ||
            merchantLower.contains("fuel") -> "Transportation"
            
            // Shopping
            merchantLower.contains("amazon") || 
            merchantLower.contains("flipkart") || 
            merchantLower.contains("myntra") ||
            merchantLower.contains("store") ||
            merchantLower.contains("mart") -> "Shopping"
            
            // Bills & Utilities
            merchantLower.contains("electricity") || 
            merchantLower.contains("water") || 
            merchantLower.contains("gas") ||
            merchantLower.contains("broadband") ||
            merchantLower.contains("bill") -> "Bills & Utilities"
            
            // Entertainment
            merchantLower.contains("netflix") || 
            merchantLower.contains("spotify") || 
            merchantLower.contains("prime") ||
            merchantLower.contains("hotstar") ||
            merchantLower.contains("cinema") -> "Entertainment"
            
            // Healthcare
            merchantLower.contains("pharmacy") || 
            merchantLower.contains("medical") || 
            merchantLower.contains("hospital") ||
            merchantLower.contains("clinic") ||
            merchantLower.contains("doctor") -> "Healthcare"
            
            // Income
            type == TransactionType.INCOME -> when {
                merchantLower.contains("salary") -> "Salary"
                merchantLower.contains("refund") -> "Refunds"
                else -> "Income"
            }
            
            else -> "Others"
        }
    }
}