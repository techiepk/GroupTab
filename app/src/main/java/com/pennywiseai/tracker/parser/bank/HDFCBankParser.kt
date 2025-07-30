package com.pennywiseai.tracker.parser.bank

/**
 * HDFC Bank specific parser
 * Handles HDFC's unique message formats
 */
class HDFCBankParser : BankParser() {
    
    override fun getBankName() = "HDFC Bank"
    
    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()
        return upperSender.contains("HDFC") || 
               // Direct sender IDs
               upperSender == "HDFCBK" ||
               // DLT patterns for transactions (-S suffix)
               upperSender.matches(Regex("^[A-Z]{2}-HDFCBK-S$")) ||
               // Other DLT patterns (OTP, Promotional, Govt)
               upperSender.matches(Regex("^[A-Z]{2}-HDFCBK-[TPG]$")) ||
               // Legacy patterns without suffix
               upperSender.matches(Regex("^[A-Z]{2}-HDFCBK$")) ||
               upperSender.matches(Regex("^[A-Z]{2}-HDFC$"))
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        // Handle HDFC specific patterns first
        
        // Pattern 1: Salary credit - "for XXXXX-ABC-XYZ MONTH SALARY-COMPANY NAME"
        if (message.contains("SALARY", ignoreCase = true) && message.contains("deposited", ignoreCase = true)) {
            // Match pattern: "for [some-code] SALARY-[COMPANY NAME]"
            val salaryPattern = Regex("""for\s+[^-]+-[^-]+-[^-]+\s+[A-Z]+\d*\s+SALARY-([^.\n]+)""", RegexOption.IGNORE_CASE)
            salaryPattern.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
            
            // Fallback: If SALARY- pattern doesn't match, try to extract company name differently
            // Sometimes it might just be "for COMPANY NAME salary"
            val simpleSalaryPattern = Regex("""for\s+([^.\n]+?)\s*(?:salary|SALARY)""", RegexOption.IGNORE_CASE)
            simpleSalaryPattern.find(message)?.let { match ->
                val merchant = match.groupValues[1].trim()
                // Remove any transaction codes (usually alphanumeric with dashes)
                val cleaned = merchant.replace(Regex("""^[A-Z0-9]+-[A-Z0-9-]+\s*"""), "").trim()
                if (cleaned.isNotEmpty()) {
                    return cleanMerchantName(cleaned)
                }
            }
        }
        
        // Pattern 2: "Info: UPI/merchant/category" format
        if (message.contains("Info:", ignoreCase = true)) {
            val infoPattern = Regex("""Info:\s*(?:UPI/)?([^/.\n]+)""", RegexOption.IGNORE_CASE)
            infoPattern.find(message)?.let { match ->
                val merchant = match.groupValues[1].trim()
                if (merchant.isNotEmpty() && !merchant.equals("UPI", ignoreCase = true)) {
                    return cleanMerchantName(merchant)
                }
            }
        }
        
        // Pattern 2: "VPA merchant@bank (sender)" format
        if (message.contains("VPA", ignoreCase = true) && message.contains("(")) {
            val vpaPattern = Regex("""VPA\s+([^@\s]+)(?:@[^\s]+)?\s*\(([^)]+)\)""", RegexOption.IGNORE_CASE)
            vpaPattern.find(message)?.let { match ->
                // Prefer the name in parentheses if available
                val nameInParens = match.groupValues[2].trim()
                val vpaName = match.groupValues[1].trim()
                return cleanMerchantName(nameInParens.ifEmpty { vpaName })
            }
        }
        
        // Pattern 3: UPI Mandate - "To merchant name\n date"
        if (message.contains("UPI Mandate", ignoreCase = true)) {
            val mandatePattern = Regex("""To\s+([^\n]+)\s*\n\s*\d{2}/\d{2}/\d{2}""", RegexOption.IGNORE_CASE)
            mandatePattern.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
        }
        
        // Pattern 4: "spent on Card XX1234 at merchant on date"
        if (message.contains("spent on Card", ignoreCase = true)) {
            val spentPattern = Regex("""at\s+([^.\n]+?)\s+on\s+\d""", RegexOption.IGNORE_CASE)
            spentPattern.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
        }
        
        // Pattern 5: "debited for merchant on date"
        if (message.contains("debited for", ignoreCase = true)) {
            val debitPattern = Regex("""debited\s+for\s+([^.\n]+?)\s+on\s+\d""", RegexOption.IGNORE_CASE)
            debitPattern.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
        }
        
        // Fall back to generic extraction
        return super.extractMerchant(message, sender)
    }
    
    override fun extractReference(message: String): String? {
        // HDFC specific reference patterns
        val hdfcPatterns = listOf(
            // "Ref 123456789" without colon
            Regex("""Ref\s+(\d{9,12})""", RegexOption.IGNORE_CASE),
            // "UPI Ref No 123456789012"
            Regex("""UPI\s+Ref\s+No\s+(\d{12})""", RegexOption.IGNORE_CASE),
            // Reference at end of message after all text
            Regex("""(?:Ref|Reference)\s*[:.]?\s*([A-Z0-9]{8,})(?:\s*$|\s*Not\s+You)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in hdfcPatterns) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1].trim()
            }
        }
        
        // Fall back to generic extraction
        return super.extractReference(message)
    }
    
    override fun extractAccountLast4(message: String): String? {
        // HDFC specific patterns
        val hdfcPatterns = listOf(
            // "deposited in HDFC Bank A/c XX1234"
            Regex("""(?:deposited\s+in|from|to)\s+(?:HDFC\s+Bank\s+)?A/c\s+(?:XX)?(\d{4})""", RegexOption.IGNORE_CASE),
            // "HDFC Bank A/c 1234" (without XX prefix)
            Regex("""HDFC\s+Bank\s+A/c\s+(\d{4})""", RegexOption.IGNORE_CASE),
            // Generic A/c pattern
            Regex("""A/c\s+(?:XX)?(\d{4})""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in hdfcPatterns) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1]
            }
        }
        
        return super.extractAccountLast4(message)
    }
    
    private fun cleanMerchantName(merchant: String): String {
        // Remove common HDFC specific noise
        return merchant
            .replace(Regex("""\s*\(.*?\)\s*$"""), "") // Remove trailing parentheses
            .replace(Regex("""\s+Ref\s+No.*""", RegexOption.IGNORE_CASE), "") // Remove reference numbers
            .replace(Regex("""\s+on\s+\d{2}.*"""), "") // Remove dates
            .trim()
            .takeIf { it.isNotEmpty() } ?: merchant
    }
    

    fun parseEMandateSubscription(message: String): EMandateInfo? {
        if (!message.contains("E-Mandate!", ignoreCase = true)) {
            return null
        }
        
        // Extract amount
        val amountPattern = Regex("""Rs\.?(\d+(?:\.\d{2})?)\s+will\s+be\s+deducted""", RegexOption.IGNORE_CASE)
        val amount = amountPattern.find(message)?.let { match ->
            match.groupValues[1].toDoubleOrNull()
        } ?: return null
        
        // Extract date
        val datePattern = Regex("""deducted\s+on\s+(\d{2}/\d{2}/\d{2})""", RegexOption.IGNORE_CASE)
        val dateStr = datePattern.find(message)?.groupValues?.get(1)
        
        // Extract merchant name
        val merchantPattern = Regex("""For\s+([^\n]+?)\s+mandate""", RegexOption.IGNORE_CASE)
        val merchant = merchantPattern.find(message)?.let { match ->
            cleanMerchantName(match.groupValues[1].trim())
        } ?: "Unknown Subscription"
        
        // Extract UMN (Unique Mandate Number)
        val umnPattern = Regex("""UMN\s+([a-zA-Z0-9@]+)""", RegexOption.IGNORE_CASE)
        val umn = umnPattern.find(message)?.groupValues?.get(1)
        
        return EMandateInfo(
            amount = amount,
            nextDeductionDate = dateStr,
            merchant = merchant,
            umn = umn
        )
    }
    
    data class EMandateInfo(
        val amount: Double,
        val nextDeductionDate: String?,
        val merchant: String,
        val umn: String?
    )
}
