package com.pennywiseai.tracker.parser.bank

import com.pennywiseai.tracker.parser.MerchantExtractor
import com.pennywiseai.tracker.parser.AmountExtractor
import com.pennywiseai.tracker.parser.TypeExtractor
import com.pennywiseai.tracker.parser.CategoryExtractor

/**
 * Base interface for bank-specific parsers
 */
abstract class BankParser {
    
    // Reuse existing extractors as base implementation
    protected val merchantExtractor = MerchantExtractor()
    protected val amountExtractor = AmountExtractor()
    protected val typeExtractor = TypeExtractor()
    protected val categoryExtractor = CategoryExtractor()
    
    /**
     * Extract merchant name from transaction message
     * Override this for bank-specific patterns
     */
    open fun extractMerchant(message: String, sender: String): String? {
        return merchantExtractor.extract(message, sender)
    }
    
    /**
     * Extract amount from transaction message
     * Override this for bank-specific patterns
     */
    open fun extractAmount(message: String, sender: String): AmountExtractor.AmountInfo? {
        return amountExtractor.extract(message, sender)
    }
    
    /**
     * Extract reference number from transaction message
     * Override this for bank-specific patterns
     */
    open fun extractReference(message: String): String? {
        val patterns = listOf(
            Regex("""(?:Ref|Reference|Trans|Transaction|ID|UPI|RefNo|Ref no|Ref#)\s*(?:No\.?|Number|#|:)?\s*([A-Z0-9]+)""", RegexOption.IGNORE_CASE),
            Regex("""\((?:UPI|Ref)\s+([0-9]+)\)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1].trim()
            }
        }
        return null
    }
    
    /**
     * Extract account last 4 digits
     */
    open fun extractAccountLast4(message: String): String? {
        val patterns = listOf(
            Regex("""A/c\s*(?:XX|xx|\*\*)?(\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""Account\s*(?:ending|XX|xx|\*\*)?(\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""from\s+[A-Za-z\s]*(?:Bank\s+)?(?:A/c|Account)\s*(?:XX|xx|\*\*)?(\d{4})""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1]
            }
        }
        return null
    }
    
    /**
     * Extract available balance after transaction
     */
    open fun extractAvailableBalance(message: String): Double? {
        val patterns = listOf(
            // "Avl bal INR 1,23,456.78"
            Regex("""Avl\s+bal\s+(?:INR\s+|Rs\.?\s*)?([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            // "Available balance: Rs 1,234.56"
            Regex("""Available\s+balance[:]\s*(?:INR\s+|Rs\.?\s*)?([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            // "Bal: Rs 1,234.56"
            Regex("""Bal[:]\s*(?:INR\s+|Rs\.?\s*)?([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE),
            // "Balance Rs 1,234.56"
            Regex("""Balance\s+(?:INR\s+|Rs\.?\s*)?([0-9,]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                val balanceStr = match.groupValues[1].replace(",", "")
                return balanceStr.toDoubleOrNull()
            }
        }
        return null
    }
    
    /**
     * Extract UPI VPA if present
     */
    open fun extractUpiVpa(message: String): String? {
        val patterns = listOf(
            Regex("""VPA\s*:?\s*([a-zA-Z0-9\.\-_]+@[a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE),
            Regex("""(?:to|from)\s+VPA\s+([a-zA-Z0-9\.\-_]+@[a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE),
            Regex("""([a-zA-Z0-9\.\-_]+@[a-zA-Z0-9]+)""") // Generic UPI pattern
        )
        
        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1]
            }
        }
        return null
    }
    
    /**
     * Get bank name for display
     */
    abstract fun getBankName(): String
    
    /**
     * Check if this parser can handle the given sender
     */
    abstract fun canHandle(sender: String): Boolean
}