package com.pennywiseai.tracker.parser

import android.util.Log

/**
 * Extracts transaction amount and direction (debit/credit) from SMS
 */
class AmountExtractor : BaseExtractor<AmountExtractor.AmountInfo>("AmountExtractor") {
    
    data class AmountInfo(
        val amount: Double,
        val isCredit: Boolean
    )
    
    override fun extract(smsBody: String, sender: String?): AmountInfo? {
        
        // First, determine transaction direction
        val isCredit = determineDirection(smsBody)
        
        // Extract amount
        val amount = extractAmount(smsBody)
        if (amount == null) {
            logExtraction("amount", null, false)
            return null
        }
        
        val amountInfo = AmountInfo(
            amount = if (isCredit) amount else -amount,
            isCredit = isCredit
        )
        
        logExtraction("amount", "${if (isCredit) "+" else "-"}₹$amount", true)
        return amountInfo
    }
    
    private fun extractAmount(smsBody: String): Double? {
        // Try each pattern
        for (pattern in TransactionPatterns.AMOUNT_PATTERNS) {
            val match = pattern.find(smsBody)
            if (match != null) {
                val amountStr = match.groupValues[1]
                val amount = parseAmount(amountStr)
                if (amount != null && amount > 0) {
                    return amount
                }
            }
        }
        
        return null
    }
    
    private fun determineDirection(smsBody: String): Boolean {
        val lowerSms = smsBody.lowercase()
        
        // Count debit and credit keywords
        val debitCount = TransactionPatterns.DEBIT_KEYWORDS.count { keyword ->
            lowerSms.contains(keyword)
        }
        
        val creditCount = TransactionPatterns.CREDIT_KEYWORDS.count { keyword ->
            lowerSms.contains(keyword)
        }
        
        
        // If more credit keywords, it's a credit
        // Default to debit if equal or no keywords found
        return creditCount > debitCount
    }
}