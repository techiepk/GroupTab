package com.pennywiseai.tracker.data.parser.bank

import java.math.BigDecimal

/**
 * HDFC Bank specific parser.
 * Handles HDFC's unique message formats including:
 * - Standard debit/credit messages
 * - UPI transactions with VPA details
 * - Salary credits with company names
 * - E-Mandate notifications
 * - Card transactions
 */
class HDFCBankParser : BankParser() {
    
    override fun getBankName() = "HDFC Bank"
    
    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()
        
        // Common HDFC sender IDs
        val hdfcSenders = setOf(
            "HDFCBK",
            "HDFCBANK",
            "HDFC",
            "HDFCB"
        )
        
        // Direct match
        if (upperSender in hdfcSenders) return true
        
        // DLT patterns
        val dltPatterns = listOf(
            Regex("^[A-Z]{2}-HDFCBK.*$"),     // VK-HDFCBK, AD-HDFCBK-S, etc.
            Regex("^[A-Z]{2}-HDFC.*$"),       // VK-HDFC, AD-HDFC, etc.
            Regex("^HDFC-[A-Z]+$"),           // HDFC-INFO, etc.
            Regex("^[A-Z]{2}-HDFCB.*$")       // Some variations
        )
        
        return dltPatterns.any { it.matches(upperSender) }
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        // Try HDFC specific patterns first
        
        // Pattern 1: Salary credit - "for XXXXX-ABC-XYZ MONTH SALARY-COMPANY NAME"
        if (message.contains("SALARY", ignoreCase = true) && message.contains("deposited", ignoreCase = true)) {
            val salaryPattern = Regex("""for\s+[^-]+-[^-]+-[^-]+\s+[A-Z]+\s+SALARY-([^.\n]+)""", RegexOption.IGNORE_CASE)
            salaryPattern.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
            
            // Simpler salary pattern
            val simpleSalaryPattern = Regex("""SALARY[- ]([^.\n]+?)(?:\s+Info|$)""", RegexOption.IGNORE_CASE)
            simpleSalaryPattern.find(message)?.let { match ->
                val merchant = match.groupValues[1].trim()
                if (merchant.isNotEmpty() && !merchant.all { it.isDigit() }) {
                    return cleanMerchantName(merchant)
                }
            }
        }
        
        // Pattern 2: "Info: UPI/merchant/category" format
        if (message.contains("Info:", ignoreCase = true)) {
            val infoPattern = Regex("""Info:\s*(?:UPI/)?([^/.\n]+?)(?:/|$)""", RegexOption.IGNORE_CASE)
            infoPattern.find(message)?.let { match ->
                val merchant = match.groupValues[1].trim()
                if (merchant.isNotEmpty() && !merchant.equals("UPI", ignoreCase = true)) {
                    return cleanMerchantName(merchant)
                }
            }
        }
        
        // Pattern 3: "VPA merchant@bank (Merchant Name)" format
        if (message.contains("VPA", ignoreCase = true)) {
            // First try to get name in parentheses
            val vpaWithNamePattern = Regex("""VPA\s+[^@\s]+@[^\s]+\s*\(([^)]+)\)""", RegexOption.IGNORE_CASE)
            vpaWithNamePattern.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
            
            // Then try just the VPA username part
            val vpaPattern = Regex("""VPA\s+([^@\s]+)@""", RegexOption.IGNORE_CASE)
            vpaPattern.find(message)?.let { match ->
                val vpaName = match.groupValues[1].trim()
                if (vpaName.length > 3 && !vpaName.all { it.isDigit() }) {
                    return cleanMerchantName(vpaName)
                }
            }
        }
        
        // Pattern 4: "spent on Card XX1234 at merchant on date"
        if (message.contains("spent on Card", ignoreCase = true)) {
            val spentPattern = Regex("""at\s+([^.\n]+?)\s+on\s+\d{2}""", RegexOption.IGNORE_CASE)
            spentPattern.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
        }
        
        // Pattern 5: "debited for merchant on date"
        if (message.contains("debited for", ignoreCase = true)) {
            val debitPattern = Regex("""debited\s+for\s+([^.\n]+?)\s+on\s+\d{2}""", RegexOption.IGNORE_CASE)
            debitPattern.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
        }
        
        // Pattern 6: "To merchant name" (for UPI mandate)
        if (message.contains("UPI Mandate", ignoreCase = true)) {
            val mandatePattern = Regex("""To\s+([^\n]+?)\s*(?:\n|\d{2}/\d{2})""", RegexOption.IGNORE_CASE)
            mandatePattern.find(message)?.let { match ->
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
            // "Ref No. 123456"
            Regex("""Ref\s+No\.?\s+([A-Z0-9]+)""", RegexOption.IGNORE_CASE),
            // Reference at end of message
            Regex("""(?:Ref|Reference)[:.\s]+([A-Z0-9]{6,})(?:\s*$|\s*Not\s+You)""", RegexOption.IGNORE_CASE)
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
            Regex("""deposited\s+in\s+(?:HDFC\s+Bank\s+)?A/c\s+(?:XX+)?(\d{4})""", RegexOption.IGNORE_CASE),
            // "from HDFC Bank A/c XX1234"
            Regex("""from\s+(?:HDFC\s+Bank\s+)?A/c\s+(?:XX+)?(\d{4})""", RegexOption.IGNORE_CASE),
            // "HDFC Bank A/c 1234" (without XX prefix)
            Regex("""HDFC\s+Bank\s+A/c\s+(\d{4})""", RegexOption.IGNORE_CASE),
            // Generic A/c pattern
            Regex("""A/c\s+(?:XX+)?(\d{4})""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in hdfcPatterns) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1]
            }
        }
        
        return super.extractAccountLast4(message)
    }
    
    override fun cleanMerchantName(merchant: String): String {
        // HDFC specific cleaning
        var cleaned = merchant
            .replace(Regex("""\s*\(.*?\)\s*$"""), "") // Remove trailing parentheses
            .replace(Regex("""\s+Ref\s+No.*""", RegexOption.IGNORE_CASE), "") // Remove reference numbers
            .replace(Regex("""\s+Ref\s+\d+.*""", RegexOption.IGNORE_CASE), "") // Remove Ref 123456
            .replace(Regex("""\s+on\s+\d{2}.*"""), "") // Remove dates
            .replace(Regex("""\s+UPI.*""", RegexOption.IGNORE_CASE), "") // Remove UPI IDs
            .replace(Regex("""\s+at\s+\d{2}:\d{2}.*"""), "") // Remove time
            .replace(Regex("""\s*-\s*$"""), "") // Remove trailing dash
            .trim()
        
        // Remove common HDFC specific noise
        val noisePatterns = listOf(
            Regex("""^(UPI/|VPA\s+)""", RegexOption.IGNORE_CASE),
            Regex("""(\s+PVT\.?\s*LTD\.?|\s+PRIVATE\s+LIMITED)$""", RegexOption.IGNORE_CASE),
            Regex("""(\s+LTD\.?|\s+LIMITED)$""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in noisePatterns) {
            cleaned = cleaned.replace(pattern, "")
        }
        
        return cleaned.trim()
    }
    
    /**
     * Checks if this is an E-Mandate notification (not a transaction).
     */
    fun isEMandateNotification(message: String): Boolean {
        return message.contains("E-Mandate!", ignoreCase = true) ||
               message.contains("will be deducted", ignoreCase = true)
    }
    
    /**
     * Parses E-Mandate subscription information.
     */
    fun parseEMandateSubscription(message: String): EMandateInfo? {
        if (!isEMandateNotification(message)) {
            return null
        }
        
        // Extract amount
        val amountPattern = Regex("""Rs\.?\s*([0-9,]+(?:\.\d{2})?)\s+will\s+be\s+deducted""", RegexOption.IGNORE_CASE)
        val amount = amountPattern.find(message)?.let { match ->
            val amountStr = match.groupValues[1].replace(",", "")
            try {
                BigDecimal(amountStr)
            } catch (e: NumberFormatException) {
                null
            }
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
    
    override fun isTransactionMessage(message: String): Boolean {
        // Skip E-Mandate notifications
        if (isEMandateNotification(message)) {
            return false
        }
        
        return super.isTransactionMessage(message)
    }
    
    /**
     * E-Mandate subscription information.
     */
    data class EMandateInfo(
        val amount: BigDecimal,
        val nextDeductionDate: String?,
        val merchant: String,
        val umn: String?
    )
}