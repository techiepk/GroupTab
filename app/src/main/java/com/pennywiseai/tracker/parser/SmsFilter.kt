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
            "click here", "apply now", "get instant", "win", "congratulations",
            "worth rs", "worth inr", "worth ₹", "free gift", "prize", "lottery",
            
            // Wallet notifications (not bank transactions)
            "wallet balance", "wallet credited", "wallet debited", "paytm wallet",
            "phonepe wallet", "amazon pay", "mobikwik wallet", "freecharge wallet"
        )
        
    }
    
    /**
     * Check if SMS should be processed as a transaction
     * @param smsBody The SMS text content
     * @param sender The SMS sender ID
     * @return true if SMS should be processed, false if it should be ignored
     */
    fun shouldProcessSms(smsBody: String, sender: String? = null): Boolean {
        val lowerSms = smsBody.lowercase()
        
        // First check sender if provided
        if (sender != null) {
            Log.d(TAG, "Checking sender: $sender")
            
            // Exclude known non-bank senders
            if (BankSenderIds.isExcludedSender(sender)) {
                Log.d(TAG, "Sender $sender is in excluded list")
                return false
            }
            
            // If it's a known bank sender, do lighter validation
            if (BankSenderIds.isBankSender(sender)) {
                Log.d(TAG, "Sender $sender is recognized as bank")
                // Just check for amount and basic keywords
                return containsAmount(smsBody) && !containsIgnoreKeywords(lowerSms)
            }
            
            Log.d(TAG, "Sender $sender is NOT recognized as bank")
        }
        
        // For unknown senders, REJECT by default
        // We only want messages from known banks and UPI providers
        
        // Check for ignore keywords
        if (containsIgnoreKeywords(lowerSms)) {
            return false
        }
        
        // Must have amount
        if (!containsAmount(smsBody)) {
            return false
        }
        
        // STRICT MODE: Only accept messages from known banks/UPI providers
        // NO EXCEPTIONS - even if they have banking keywords
        if (sender == null) {
            // For legacy data without sender info, reject by default
            Log.d(TAG, "No sender info - rejecting")
            return false
        } else if (!BankSenderIds.isBankSender(sender)) {
            // Unknown sender = automatic reject
            Log.w(TAG, "REJECTED: Unknown sender '$sender' - add to whitelist if this is your bank")
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