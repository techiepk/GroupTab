package com.pennywiseai.tracker.parser

import android.util.Log
import com.pennywiseai.tracker.data.TransactionType

/**
 * Extracts transaction type from SMS
 */
class TypeExtractor : BaseExtractor<TransactionType>("TypeExtractor") {
    
    override fun extract(smsBody: String, sender: String?): TransactionType {
        
        val lowerSms = smsBody.lowercase()
        
        // Check for specific transaction types
        val typeScores = mutableMapOf<TransactionType, Int>()
        
        TransactionPatterns.TYPE_KEYWORDS.forEach { (type, keywords) ->
            val score = keywords.count { keyword ->
                lowerSms.contains(keyword)
            }
            if (score > 0) {
                typeScores[type] = score
            }
        }
        
        // Get type with highest score
        val detectedType = typeScores.maxByOrNull { it.value }?.key
        
        // If no specific type detected, try to infer
        val finalType = detectedType ?: inferTransactionType(smsBody)
        
        logExtraction("type", finalType, true)
        return finalType
    }
    
    private fun inferTransactionType(smsBody: String): TransactionType {
        val lowerSms = smsBody.lowercase()
        
        return when {
            // Check for refund indicators
            lowerSms.contains("refund") || lowerSms.contains("reversed") -> TransactionType.REFUND
            
            // Check for transfer indicators
            lowerSms.contains("sent to") || lowerSms.contains("received from") ||
            lowerSms.contains("transfer") -> TransactionType.TRANSFER
            
            // Check for recurring indicators
            lowerSms.contains("auto debit") || lowerSms.contains("standing instruction") -> TransactionType.RECURRING_BILL
            
            // Default to one-time transaction
            else -> TransactionType.ONE_TIME
        }
    }
}