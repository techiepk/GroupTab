package com.pennywiseai.tracker.data.parser.bank

import com.pennywiseai.tracker.core.CompiledPatterns
import com.pennywiseai.tracker.data.database.entity.TransactionType
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
        return CompiledPatterns.HDFC.DLT_PATTERNS.any { it.matches(upperSender) }
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        // Try HDFC specific patterns first
        
        // Pattern 1: Salary credit - "for XXXXX-ABC-XYZ MONTH SALARY-COMPANY NAME"
        if (message.contains("SALARY", ignoreCase = true) && message.contains("deposited", ignoreCase = true)) {
            CompiledPatterns.HDFC.SALARY_PATTERN.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
            
            // Simpler salary pattern
            CompiledPatterns.HDFC.SIMPLE_SALARY_PATTERN.find(message)?.let { match ->
                val merchant = match.groupValues[1].trim()
                if (merchant.isNotEmpty() && !merchant.all { it.isDigit() }) {
                    return cleanMerchantName(merchant)
                }
            }
        }
        
        // Pattern 2: "Info: UPI/merchant/category" format
        if (message.contains("Info:", ignoreCase = true)) {
            CompiledPatterns.HDFC.INFO_PATTERN.find(message)?.let { match ->
                val merchant = match.groupValues[1].trim()
                if (merchant.isNotEmpty() && !merchant.equals("UPI", ignoreCase = true)) {
                    return cleanMerchantName(merchant)
                }
            }
        }
        
        // Pattern 3: "VPA merchant@bank (Merchant Name)" format
        if (message.contains("VPA", ignoreCase = true)) {
            // First try to get name in parentheses
            CompiledPatterns.HDFC.VPA_WITH_NAME.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
            
            // Then try just the VPA username part
            CompiledPatterns.HDFC.VPA_PATTERN.find(message)?.let { match ->
                val vpaName = match.groupValues[1].trim()
                if (vpaName.length > 3 && !vpaName.all { it.isDigit() }) {
                    return cleanMerchantName(vpaName)
                }
            }
        }
        
        // Pattern 4: "spent on Card XX1234 at merchant on date"
        if (message.contains("spent on Card", ignoreCase = true)) {
            CompiledPatterns.HDFC.SPENT_PATTERN.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
        }
        
        // Pattern 5: "debited for merchant on date"
        if (message.contains("debited for", ignoreCase = true)) {
            CompiledPatterns.HDFC.DEBIT_FOR_PATTERN.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
        }
        
        // Pattern 6: "To merchant name" (for UPI mandate)
        if (message.contains("UPI Mandate", ignoreCase = true)) {
            CompiledPatterns.HDFC.MANDATE_PATTERN.find(message)?.let { match ->
                return cleanMerchantName(match.groupValues[1].trim())
            }
        }
        
        // Fall back to generic extraction
        return super.extractMerchant(message, sender)
    }
    
    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        
        return when {
            // HDFC specific: "Sent Rs.X From HDFC Bank"
            lowerMessage.contains("sent") && lowerMessage.contains("from hdfc") -> TransactionType.EXPENSE
            
            // Standard expense keywords
            lowerMessage.contains("debited") -> TransactionType.EXPENSE
            lowerMessage.contains("withdrawn") -> TransactionType.EXPENSE
            lowerMessage.contains("spent") -> TransactionType.EXPENSE
            lowerMessage.contains("charged") -> TransactionType.EXPENSE
            lowerMessage.contains("paid") -> TransactionType.EXPENSE
            lowerMessage.contains("purchase") -> TransactionType.EXPENSE
            
            // Income keywords
            lowerMessage.contains("credited") -> TransactionType.INCOME
            lowerMessage.contains("deposited") -> TransactionType.INCOME
            lowerMessage.contains("received") -> TransactionType.INCOME
            lowerMessage.contains("refund") -> TransactionType.INCOME
            lowerMessage.contains("cashback") && !lowerMessage.contains("earn cashback") -> TransactionType.INCOME
            
            else -> null
        }
    }
    
    override fun extractReference(message: String): String? {
        // HDFC specific reference patterns
        val hdfcPatterns = listOf(
            CompiledPatterns.HDFC.REF_SIMPLE,
            CompiledPatterns.HDFC.UPI_REF_NO,
            CompiledPatterns.HDFC.REF_NO,
            CompiledPatterns.HDFC.REF_END
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
            CompiledPatterns.HDFC.ACCOUNT_DEPOSITED,
            CompiledPatterns.HDFC.ACCOUNT_FROM,
            CompiledPatterns.HDFC.ACCOUNT_SIMPLE,
            CompiledPatterns.HDFC.ACCOUNT_GENERIC
        )
        
        for (pattern in hdfcPatterns) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1]
            }
        }
        
        return super.extractAccountLast4(message)
    }
    
    override fun cleanMerchantName(merchant: String): String {
        // Use parent class implementation which already uses CompiledPatterns
        return super.cleanMerchantName(merchant)
    }
    
    /**
     * Checks if this is an E-Mandate notification (not a transaction).
     */
    fun isEMandateNotification(message: String): Boolean {
        return message.contains("E-Mandate!", ignoreCase = true)
    }
    
    /**
     * Parses E-Mandate subscription information.
     */
    fun parseEMandateSubscription(message: String): EMandateInfo? {
        if (!isEMandateNotification(message)) {
            return null
        }
        
        // Extract amount
        val amount = CompiledPatterns.HDFC.AMOUNT_WILL_DEDUCT.find(message)?.let { match ->
            val amountStr = match.groupValues[1].replace(",", "")
            try {
                BigDecimal(amountStr)
            } catch (e: NumberFormatException) {
                null
            }
        } ?: return null
        
        // Extract date
        val dateStr = CompiledPatterns.HDFC.DEDUCTION_DATE.find(message)?.groupValues?.get(1)
        
        // Extract merchant name
        val merchant = CompiledPatterns.HDFC.MANDATE_MERCHANT.find(message)?.let { match ->
            cleanMerchantName(match.groupValues[1].trim())
        } ?: "Unknown Subscription"
        
        // Extract UMN (Unique Mandate Number)
        val umn = CompiledPatterns.HDFC.UMN_PATTERN.find(message)?.groupValues?.get(1)
        
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
        
        val lowerMessage = message.lowercase()
        
        // Skip credit card blocking notifications
        if (lowerMessage.contains("block cc")) {
            return false
        }
        
        // Skip credit card payment confirmations
        if (lowerMessage.contains("received towards your credit card")) {
            return false
        }
        
        // Skip credit card payment credited notifications
        if (lowerMessage.contains("payment") && 
            lowerMessage.contains("credited to your card")) {
            return false
        }
        
        // Skip OTP and promotional messages
        if (lowerMessage.contains("otp") || 
            lowerMessage.contains("one time password") ||
            lowerMessage.contains("verification code") ||
            lowerMessage.contains("offer") || 
            lowerMessage.contains("discount") ||
            lowerMessage.contains("cashback offer") ||
            lowerMessage.contains("win ")) {
            return false
        }
        
        // HDFC specific transaction keywords
        val hdfcTransactionKeywords = listOf(
            "debited", "credited", "withdrawn", "deposited",
            "spent", "received", "transferred", "paid", 
            "sent" // HDFC uses "Sent Rs.X From HDFC Bank"
        )
        
        return hdfcTransactionKeywords.any { lowerMessage.contains(it) }
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