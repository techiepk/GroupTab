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
            "security code", "passcode", "authorization code", "2fa", "two factor",
            
            // Cashback messages (as per user requirement)
            "cashback", "cash back", "reward", "points earned", "earn points",
            "loyalty points", "bonus credited", "reward points", "cashback earned",
            
            // Loan and credit offers (spam)
            "loan", "personal loan", "instant loan", "pre-approved", "pre approved",
            "eligible", "eligibility", "credit limit", "loan approved", "loan offer",
            "apply for loan", "loan amount", "emi starting", "interest rate",
            "get loan", "avail loan", "business loan", "home loan", "car loan",
            "education loan", "gold loan", "loan against", "overdraft facility",
            "credit line", "line of credit", "loan disbursed", "loan application",
            
            // Credit card offers
            "credit card offer", "lifetime free", "joining fee", "annual fee waived",
            "upgrade your card", "card upgrade", "apply for credit card",
            "credit card approved", "credit card limit", "increase limit",
            "card is ready", "ready for approval", "complete your verification",
            "verify now", "complete verification", "neucard", "tata card",
            
            // Investment and insurance spam
            "investment opportunity", "invest now", "mutual fund", "sip investment",
            "insurance policy", "term insurance", "life insurance", "health insurance",
            "policy premium", "renew policy", "insurance plan", "returns upto",
            "guaranteed returns", "tax saving", "save tax", "80c benefits",
            
            // Promotional banking offers
            "special offer", "exclusive offer", "limited period", "festive offer",
            "discount offer", "flat discount", "upto off", "% off", "percent off",
            "deal", "sale", "hurry", "last chance", "expires soon", "valid till",
            
            // Marketing and ads
            "click here", "click now", "apply now", "register now", "download app",
            "install app", "visit branch", "call us", "contact us", "toll free",
            "customer care", "helpline", "sms stop", "opt out", "unsubscribe",
            "http://", "https://", "bit.ly", "tinyurl", "goo.gl", "t.co",
            
            // Contests and luck-based
            "congratulations", "you have won", "winner", "prize", "lottery",
            "lucky draw", "contest", "giveaway", "claim your", "redeem now",
            
            // Account alerts (not transactions)
            "balance inquiry", "mini statement", "account statement", "check balance",
            "low balance", "maintain balance", "minimum balance", "quarterly average",
            "kyc update", "kyc pending", "update kyc", "complete kyc", "kyc required",
            "update pan", "update aadhaar", "link aadhaar", "mandate registration",
            
            // Service messages
            "service request", "complaint registered", "request received",
            "thank you for", "feedback", "rate us", "customer survey",
            "branch timing", "holiday notice", "system maintenance", "downtime",
            
            // Cancelled/Failed transactions
            "cancelled", "canceled", "failed", "unsuccessful", "declined", "rejected",
            "could not be processed", "transaction failed", "payment failed",
            "insufficient funds", "transaction reversed", "refund initiated",
            
            // Pending/Future transactions
            "will be debited", "will be credited", "scheduled", "pending",
            "will be charged", "upcoming payment", "auto debit scheduled",
            "due on", "payment due", "bill due", "emi due",
            
            // TDS and tax
            "total tds", "tds deducted", "tax deducted", "tds amount",
            "income tax", "gst", "tax refund", "form 26as",
            
            // Wallet notifications (not bank transactions)
            "wallet balance", "wallet credited", "wallet debited", "paytm wallet",
            "phonepe wallet", "amazon pay", "mobikwik wallet", "freecharge wallet",
            "wallet cashback", "add money", "wallet offer",
            
            // Informational
            "interest credited", "interest earned", "fixed deposit", "fd matured",
            "rd installment", "ppf account", "nomination", "beneficiary added",
            "password changed", "pin changed", "profile updated", "mobile updated"
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
        // Special handling for certain keywords that might appear in legitimate transactions
        val specialCases = mapOf(
            "loan" to listOf("loan disbursed to", "loan credited", "loan amount credited"),
            "interest" to listOf("interest paid", "interest debited", "interest charged"),
            "cashback" to listOf("cashback reversed", "cashback adjusted")
        )
        
        for (keyword in IGNORE_KEYWORDS) {
            if (lowerSms.contains(keyword)) {
                // Check if it's a special case where the keyword is part of a legitimate transaction
                val exceptions = specialCases[keyword]
                if (exceptions != null) {
                    // If any exception pattern matches, don't filter out
                    if (exceptions.any { lowerSms.contains(it) }) {
                        continue
                    }
                }
                return true
            }
        }
        return false
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
    fun getFilterReason(smsBody: String, sender: String? = null): String? {
        val lowerSms = smsBody.lowercase()
        
        // Check sender first
        if (sender != null) {
            if (BankSenderIds.isExcludedSender(sender)) {
                return "Sender is in excluded list: $sender"
            }
            
            if (!BankSenderIds.isBankSender(sender)) {
                return "Unknown sender (not in bank whitelist): $sender"
            }
        }
        
        // Check ignore keywords with details
        val detectedKeywords = IGNORE_KEYWORDS.filter { keyword ->
            lowerSms.contains(keyword)
        }
        
        if (detectedKeywords.isNotEmpty()) {
            val keywordCategories = mapOf(
                "loan" to "Loan offer/spam",
                "eligible" to "Promotional offer",
                "cashback" to "Cashback notification",
                "otp" to "OTP/Verification",
                "kyc" to "KYC update request",
                "congratulations" to "Contest/Lottery spam",
                "offer" to "Promotional offer",
                "wallet" to "Wallet notification"
            )
            
            val categorizedKeyword = detectedKeywords.firstOrNull { keyword ->
                keywordCategories.keys.any { it in keyword }
            }
            
            val category = if (categorizedKeyword != null) {
                keywordCategories.entries.firstOrNull { categorizedKeyword.contains(it.key) }?.value
            } else null
            
            return if (category != null) {
                "$category - Keywords found: ${detectedKeywords.take(3).joinToString(", ")}"
            } else {
                "Contains spam/ignored keywords: ${detectedKeywords.take(3).joinToString(", ")}"
            }
        }
        
        // Check for amount
        if (!containsAmount(smsBody)) {
            return "No amount found in message"
        }
        
        return null
    }
    
    /**
     * Check if message is likely spam based on pattern analysis
     */
    fun isLikelySpam(smsBody: String): Boolean {
        val lowerSms = smsBody.lowercase()
        
        // Strong spam indicators (any one of these = spam)
        val strongSpamIndicators = listOf(
            "congratulations" to "won",
            "eligible" to "loan",
            "pre-approved" to "credit",
            "click here" to "apply",
            "limited period" to "offer",
            "get instant" to "loan",
            "earn upto" to "cashback",
            "credit card" to "ready",
            "card" to "approval",
            "limit" to "ready",
            "verification" to "complete"
        )
        
        // Check for URL patterns (shortened URLs are almost always spam)
        val urlPatterns = listOf(
            Regex("""(http://|https://)\S+"""),
            Regex("""(bit\.ly|tinyurl|goo\.gl|t\.co|hu2\.in)/\S+""")
        )
        
        val hasUrl = urlPatterns.any { it.containsMatchIn(lowerSms) }
        
        return hasUrl || strongSpamIndicators.any { (first, second) ->
            lowerSms.contains(first) && lowerSms.contains(second)
        }
    }
}