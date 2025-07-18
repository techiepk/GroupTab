package com.pennywiseai.tracker.parser

import android.util.Log
import com.pennywiseai.tracker.data.TransactionCategory

/**
 * Extracts transaction category based on keywords and patterns
 */
class CategoryExtractor : BaseExtractor<TransactionCategory>("CategoryExtractor") {
    
    override fun extract(smsBody: String, sender: String?): TransactionCategory? {
        
        // Check for ATM withdrawal first
        if (isAtmWithdrawal(smsBody)) {
            return TransactionCategory.TRANSFER
        }
        
        // First check sender-based categorization
        sender?.let {
            val senderCategory = extractFromSender(it)
            if (senderCategory != null) {
                logExtraction("category", senderCategory, true)
                return senderCategory
            }
        }
        
        // Then check keywords in SMS body
        val category = extractFromKeywords(smsBody) ?: TransactionCategory.OTHER
        
        logExtraction("category", category, true)
        return category
    }
    
    private fun isAtmWithdrawal(smsBody: String): Boolean {
        val lowerSms = smsBody.lowercase()
        return (lowerSms.contains("withdrawn") || lowerSms.contains("withdrawal")) &&
               (lowerSms.contains("atm") || lowerSms.contains("cash"))
    }
    
    private fun extractFromSender(sender: String): TransactionCategory? {
        return TransactionPatterns.SENDER_CATEGORY_MAP[sender.uppercase()]
    }
    
    private fun extractFromKeywords(smsBody: String): TransactionCategory? {
        val lowerSms = smsBody.lowercase()
        val categoryScores = mutableMapOf<TransactionCategory, Int>()
        
        // Score each category based on keyword matches
        TransactionPatterns.CATEGORY_KEYWORDS.forEach { (category, keywords) ->
            val score = keywords.count { keyword ->
                lowerSms.contains(keyword)
            }
            if (score > 0) {
                categoryScores[category] = score
            }
        }
        
        // Return category with highest score
        return categoryScores.maxByOrNull { it.value }?.key
    }
}