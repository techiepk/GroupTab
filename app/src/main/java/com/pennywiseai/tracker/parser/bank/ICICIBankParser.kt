package com.pennywiseai.tracker.parser.bank

/**
 * ICICI Bank specific parser
 * Handles ICICI's unique message formats
 */
class ICICIBankParser : BankParser() {
    
    override fun getBankName() = "ICICI Bank"
    
    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()
        return upperSender.contains("ICICI") || upperSender == "ICICIB" || 
               upperSender.matches(Regex("^[A-Z]{2}-ICICI.*"))
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        // ICICI specific patterns
        
        // Pattern 1: "Your ICICI Bank Credit Card XX1234 has been used for Rs.1,299.00 at Amazon"
        if (message.contains("has been used for", ignoreCase = true)) {
            val usedPattern = Regex("""at\s+([^.\n]+?)(?:\s+on\s+|\.|$)""", RegexOption.IGNORE_CASE)
            usedPattern.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
        }
        
        // Pattern 2: "Acct XX123 debited with Rs.500.00 on 27-Jul-24 & credited to merchant"
        if (message.contains("credited to", ignoreCase = true)) {
            val creditPattern = Regex("""credited\s+to\s+([^.\n]+?)(?:\.|$)""", RegexOption.IGNORE_CASE)
            creditPattern.find(message)?.let { match ->
                val merchant = match.groupValues[1].trim()
                // Skip if it's just "account" or similar generic terms
                if (!merchant.contains("account", ignoreCase = true)) {
                    return cleanMerchantName(merchant)
                }
            }
        }
        
        // Pattern 3: "Payment of Rs.500 done using Credit Card XX1234 to merchant"
        if (message.contains("Payment of", ignoreCase = true) && message.contains("to", ignoreCase = true)) {
            val paymentPattern = Regex("""to\s+([^.\n]+?)(?:\.|$)""", RegexOption.IGNORE_CASE)
            paymentPattern.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
        }
        
        // Pattern 4: ICICI UPI format - often includes merchant in a specific way
        if (message.contains("UPI", ignoreCase = true)) {
            // Try to extract from UPI ID first
            val upiPattern = Regex("""(?:to|from)\s+([a-zA-Z0-9\.\-_]+)@[a-zA-Z0-9]+""", RegexOption.IGNORE_CASE)
            upiPattern.find(message)?.let { match ->
                val upiMerchant = match.groupValues[1].trim()
                if (upiMerchant.isNotEmpty()) {
                    return cleanMerchantName(upiMerchant)
                }
            }
        }
        
        // Fall back to generic extraction
        return super.extractMerchant(message, sender)
    }
    
    override fun extractReference(message: String): String? {
        // ICICI specific reference patterns
        val iciciPatterns = listOf(
            // "Txn ref no. 123456789012"
            Regex("""Txn\s+ref\s+no\.\s*(\d+)""", RegexOption.IGNORE_CASE),
            // "Ref No: 123456789"
            Regex("""Ref\s+No:\s*([A-Z0-9]+)""", RegexOption.IGNORE_CASE),
            // UPI reference in ICICI format
            Regex("""UPI/(\d{12})""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in iciciPatterns) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1].trim()
            }
        }
        
        return super.extractReference(message)
    }
    
    override fun extractAccountLast4(message: String): String? {
        // ICICI uses "Acct XX123" or "A/c ...1234" format
        val iciciPatterns = listOf(
            Regex("""Acct\s+(?:XX)?(\d{3,4})""", RegexOption.IGNORE_CASE),
            Regex("""A/c\s+\.\.\.(\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""Account\s+(?:Number\s+)?(?:ending\s+)?(\d{4})""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in iciciPatterns) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1]
            }
        }
        
        return super.extractAccountLast4(message)
    }
    
    private fun cleanMerchantName(merchant: String): String {
        return merchant
            .replace(Regex("""Linked.*""", RegexOption.IGNORE_CASE), "") // Remove "Linked to mobile" etc
            .replace(Regex("""Info.*""", RegexOption.IGNORE_CASE), "") // Remove info text
            .replace(Regex("""\s+Ref.*""", RegexOption.IGNORE_CASE), "") // Remove reference
            .replace(Regex("""\s+on\s+\d{2}-"""), "") // Remove dates
            .trim()
            .takeIf { it.isNotEmpty() && it.length > 2 } ?: merchant
    }
}