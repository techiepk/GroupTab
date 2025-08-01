package com.pennywiseai.tracker.parser

import android.util.Log

/**
 * Extracts merchant name from SMS
 */
class MerchantExtractor : BaseExtractor<String>("MerchantExtractor") {
    
    override fun extract(smsBody: String, sender: String?): String? {
        
        // Check for ATM withdrawal first
        if (isAtmWithdrawal(smsBody)) {
            return "ATM Withdrawal"
        }
        
        // Try to extract from SMS patterns
        var merchant = extractFromPatterns(smsBody)
        
        // Clean up merchant name if found
        merchant = merchant?.let { cleanMerchantName(it) }
        
        // Only use sender as fallback if it's a known merchant sender
        if (merchant == null && sender != null && isKnownMerchantSender(sender)) {
            merchant = deriveMerchantFromSender(sender)
        }
        
        // If still no merchant, return "Unknown Merchant" instead of sender
        if (merchant == null) {
            merchant = "Unknown Merchant"
        }
        
        logExtraction("merchant", merchant, merchant != null)
        return merchant
    }
    
    private fun isAtmWithdrawal(smsBody: String): Boolean {
        val lowerSms = smsBody.lowercase()
        // Check for ATM withdrawal patterns
        return (lowerSms.contains("withdrawn") || lowerSms.contains("withdrawal")) &&
               (lowerSms.contains("atm") || lowerSms.contains("cash"))
    }
    
    private fun isKnownMerchantSender(sender: String): Boolean {
        // Only treat these senders as actual merchants
        val merchantSenders = setOf(
            "AMAZON", "FLIPKART", "UBER", "OLA", "SWIGGY", "ZOMATO",
            "NETFLIX", "SPOTIFY", "HOTSTAR", "PRIME"
        )
        return sender.uppercase() in merchantSenders
    }
    
    private fun extractFromPatterns(smsBody: String): String? {
        // Try each merchant pattern
        for (pattern in TransactionPatterns.MERCHANT_PATTERNS) {
            val match = pattern.find(smsBody)
            if (match != null && match.groups.size > 1) {
                val merchantName = match.groupValues[1].trim()
                if (isValidMerchantName(merchantName)) {
                    return merchantName
                }
            }
        }
        
        // Try to extract from UPI ID
        val upiMatch = TransactionPatterns.UPI_PATTERNS
            .firstOrNull { it.find(smsBody) != null }
            ?.find(smsBody)
        
        if (upiMatch != null) {
            val upiId = upiMatch.value
            val merchantFromUpi = upiId.substringBefore("@")
            if (merchantFromUpi.isNotEmpty() && merchantFromUpi.length > 2) {
                return merchantFromUpi
            }
        }
        
        return null
    }
    
    private fun isValidMerchantName(name: String): Boolean {
        // Merchant name should:
        // - Be at least 2 characters long
        // - Contain at least one letter
        // - Not be just a common word like "USING", "VIA", etc.
        val commonWords = setOf("USING", "VIA", "THROUGH", "BY", "WITH", "FOR", "TO", "FROM", "AT")
        
        return name.length >= 2 && 
               name.any { it.isLetter() } && 
               name.uppercase() !in commonWords &&
               !name.all { it.isDigit() }
    }
    
    private fun deriveMerchantFromSender(sender: String): String {
        // Map common senders to readable names
        return when (sender.uppercase()) {
            "HDFCBK" -> "HDFC Bank"
            "ICICIB" -> "ICICI Bank"
            "SBIINB", "SBIPSG" -> "SBI"
            "PAYTM" -> "Paytm"
            "PHONEPE" -> "PhonePe"
            "GPAY" -> "Google Pay"
            "AMAZON" -> "Amazon"
            "FLIPKART" -> "Flipkart"
            "UBER" -> "Uber"
            "OLA" -> "Ola"
            "SWIGGY" -> "Swiggy"
            "ZOMATO" -> "Zomato"
            else -> sender
        }
    }
    
    private fun cleanMerchantName(merchant: String): String {
        // Common words to exclude that are not merchant names
        val excludeWords = setOf(
            "using", "via", "through", "by", "from", "to", "at", "on", "for",
            "with", "your", "account", "card", "upi", "ref", "txn", "transaction",
            "payment", "transfer", "amount", "rs", "inr", "rupees"
        )
        
        // Company suffixes to remove
        val companySuffixes = listOf(
            "LIMITED", "LTD", "PRIVATE", "PVT", "INC", "INCORPORATED", 
            "CORP", "CORPORATION", "COM", "ENT", "ENTERPRISE", "ENTERPRISES",
            "INDIA", "SYSTEMS", "SOLUTIONS", "SERVICES", "TECHNOLOGIES",
            "RETAIL", "ONLINE", "DIGITAL"
            // Note: Removed "PAYMENTS" as it's meaningful for services like "Amazon Pay"
        )
        
        var cleaned = merchant
            .replace(Regex("^VPA\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ")
            .replace("_", " ")
            .trim()
        
        // Remove common date patterns that might be captured
        cleaned = cleaned
            .replace(Regex("\\s+on\\s+\\d{1,2}-\\d{1,2}-\\d{2,4}.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+dated\\s+\\d{1,2}-\\d{1,2}-\\d{2,4}.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+dt\\s+\\d{1,2}-\\d{1,2}-\\d{2,4}.*", RegexOption.IGNORE_CASE), "")
            .trim()
        
        // Remove trailing punctuation
        cleaned = cleaned.trimEnd('.', ',', ';', ':')
        
        // Split into words
        val words = cleaned.split(" ")
            .filter { it.isNotEmpty() }
            .map { it.uppercase() }
        
        // Remove company suffixes from the end
        val cleanedWords = mutableListOf<String>()
        var foundMeaningfulWord = false
        
        for (word in words) {
            if (word !in companySuffixes && word.lowercase() !in excludeWords) {
                cleanedWords.add(word)
                foundMeaningfulWord = true
            } else if (!foundMeaningfulWord && word in companySuffixes) {
                // Keep company suffix if no meaningful word found yet
                cleanedWords.add(word)
            }
        }
        
        // Always take only the first word
        val merchantName = when {
            // Take only first meaningful word
            cleanedWords.isNotEmpty() -> cleanedWords.first()
            
            // If no words left after cleaning, use original first word
            words.isNotEmpty() -> words.first()
            
            else -> merchant
        }
        
        // Apply special mappings for known merchants
        return applyMerchantMappings(merchantName)
    }
    
    private fun applyMerchantMappings(merchant: String): String {
        // Known merchant mappings
        val mappings = mapOf(
            "BIGTREE" to "BookMyShow",
            "INOX" to "PVR",
            "RELIANCE" to "Reliance",
            "NETFLIX" to "Netflix",
            "SPOTIFY" to "Spotify",
            "SWIGGY" to "Swiggy",
            "ZOMATO" to "Zomato",
            "UBER" to "Uber",
            "OLA" to "Ola",
            "AMAZON" to "Amazon",
            "FLIPKART" to "Flipkart",
            "JIOHOTSTAR" to "JioHotstar",
            "HOTSTAR" to "Hotstar",
            "JIO" to "Jio",
            "MANI" to "Mani"
        )
        
        val upperMerchant = merchant.uppercase()
        
        // Check if merchant starts with any known mapping
        for ((key, value) in mappings) {
            if (upperMerchant.startsWith(key)) {
                return value
            }
        }
        
        // Default: capitalize properly
        return merchant.lowercase().replaceFirstChar { it.uppercase() }
    }
}