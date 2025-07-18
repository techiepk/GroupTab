package com.pennywiseai.tracker.parser

import android.util.Log

/**
 * Filters SMS messages to determine if they should be processed as transactions
 */
class SmsFilter {
    
    companion object {
        private const val TAG = "SmsFilter"
        
        // Keywords that indicate the message should be ignored
        private val IGNORE_KEYWORDS = listOf(
            // OTP and verification messages
            "otp", "one time password", "verification code", "verify", "authentication code",
            "security code", "passcode", "authorization code",
            
            // Cashback messages (as per user requirement)
            "cashback", "cash back", "reward", "points earned",
            
            // Cancelled/Failed transactions
            "cancelled", "canceled", "failed", "unsuccessful", "declined", "rejected",
            "could not be processed", "transaction failed",
            
            // Pending/Future transactions
            "will be debited", "will be", "scheduled", "pending", "will be charged", "upcoming payment",
            "auto debit scheduled", "due on", "payment due",
            
            // TDS messages
            "total tds", "tds deducted", "tax deducted", "tds amount",
            
            // Other non-transaction messages
            "balance inquiry", "mini statement", "account statement", "check balance",
            "registration successful", "welcome to", "thank you for registering",
            "update your", "complete your kyc", "link your",
            
            // Promotional/Marketing
            "special offer", "exclusive deal", "discount", "sale", "limited time",
            "click here", "apply now", "get instant", "win", "congratulations"
        )
        
    }
    
    /**
     * Check if SMS should be processed as a transaction
     * @return true if SMS should be processed, false if it should be ignored
     */
    fun shouldProcessSms(smsBody: String): Boolean {
        val lowerSms = smsBody.lowercase()
        
        // Check for ignore keywords
        if (containsIgnoreKeywords(lowerSms)) {
            return false
        }
        
        // Check if it contains amount (basic check)
        val hasAmount = containsAmount(smsBody)
        if (!hasAmount) {
            return false
        }
        
        return true
    }
    
    private fun containsIgnoreKeywords(lowerSms: String): Boolean {
        return IGNORE_KEYWORDS.any { keyword ->
            lowerSms.contains(keyword)
        }
    }
    
    private fun containsAmount(smsBody: String): Boolean {
        // Basic check for amount patterns
        val amountPatterns = listOf(
            Regex("""(?:Rs\.?|INR|₹)\s*\d+""", RegexOption.IGNORE_CASE),
            Regex("""\d+\s*(?:Rs\.?|INR|₹)""", RegexOption.IGNORE_CASE)
        )
        
        return amountPatterns.any { pattern ->
            pattern.containsMatchIn(smsBody)
        }
    }
    
    /**
     * Get reason why SMS was filtered (for debugging)
     */
    fun getFilterReason(smsBody: String): String? {
        val lowerSms = smsBody.lowercase()
        
        // Check each ignore keyword
        for (keyword in IGNORE_KEYWORDS) {
            if (lowerSms.contains(keyword)) {
                return "Contains ignored keyword: $keyword"
            }
        }
        
        // Check for amount
        if (!containsAmount(smsBody)) {
            return "No amount found"
        }
        
        return null
    }
}