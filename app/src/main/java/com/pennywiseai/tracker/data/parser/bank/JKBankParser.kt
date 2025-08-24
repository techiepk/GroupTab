package com.pennywiseai.tracker.data.parser.bank

import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal

/**
 * Jammu & Kashmir Bank (JK Bank) specific parser.
 * Handles JK Bank's message formats including:
 * - Standard debit/credit messages
 * - UPI transactions
 * - Account number patterns
 * - Balance updates
 */
class JKBankParser : BankParser() {
    
    override fun getBankName() = "JK Bank"
    
    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()
        
        // Common JK Bank sender IDs
        val jkBankSenders = setOf(
            "JKBANK",
            "JKB",
            "JKBANKL",
            "JKBNK"
        )
        
        // Direct match
        if (upperSender in jkBankSenders) return true
        
        // DLT patterns (AD-JKBANK-S, etc.)
        val dltPatterns = listOf(
            Regex("^[A-Z]{2}-JKBANK.*$"),
            Regex("^[A-Z]{2}-JKB.*$"),
            Regex("^[A-Z]{2}-JKBNK.*$"),
            Regex("^JKBANK-[A-Z]+$"),
            Regex("^JKB-[A-Z]+$")
        )
        
        return dltPatterns.any { it.matches(upperSender) }
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        // Check for UPI transactions without specific merchant
        if (message.contains("via UPI", ignoreCase = true)) {
            // Look for UPI VPA pattern
            val vpaPattern = Regex("""to\s+([^@\s]+@[^\s]+)""", RegexOption.IGNORE_CASE)
            vpaPattern.find(message)?.let { match ->
                val vpa = match.groupValues[1].trim()
                // Extract the part before @ as merchant name
                val merchantName = vpa.substringBefore("@")
                if (merchantName.isNotEmpty() && merchantName != "upi") {
                    return cleanMerchantName(merchantName)
                }
            }
            
            // Look for merchant after "to" but before "via UPI"
            val toMerchantPattern = Regex("""to\s+([^.\n]+?)\s+via\s+UPI""", RegexOption.IGNORE_CASE)
            toMerchantPattern.find(message)?.let { match ->
                val merchant = match.groupValues[1].trim()
                if (isValidMerchantName(merchant)) {
                    return cleanMerchantName(merchant)
                }
            }
            
            // If no specific merchant found in UPI transaction, return "upi" as merchant
            // This matches the expected output from the sample
            return "upi"
        }
        
        // Check for ATM withdrawals
        if (message.contains("ATM", ignoreCase = true) || 
            message.contains("withdrawn", ignoreCase = true)) {
            return "ATM"
        }
        
        // Standard patterns for merchant extraction
        val merchantPatterns = listOf(
            // Pattern for "to MERCHANT via"
            Regex("""to\s+([^.\n]+?)\s+via""", RegexOption.IGNORE_CASE),
            // Pattern for "from MERCHANT"
            Regex("""from\s+([^.\n]+?)(?:\s+on|\s+Ref|$)""", RegexOption.IGNORE_CASE),
            // Pattern for "at MERCHANT"
            Regex("""at\s+([^.\n]+?)(?:\s+on|\s+Ref|$)""", RegexOption.IGNORE_CASE),
            // Pattern for "for MERCHANT"
            Regex("""for\s+([^.\n]+?)(?:\s+on|\s+Ref|$)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in merchantPatterns) {
            pattern.find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) {
                    return merchant
                }
            }
        }
        
        // Fall back to base extraction
        return super.extractMerchant(message, sender)
    }
    
    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        
        return when {
            // JK Bank specific patterns
            lowerMessage.contains("has been debited") -> TransactionType.EXPENSE
            lowerMessage.contains("has been credited") -> TransactionType.INCOME
            
            // Standard expense keywords
            lowerMessage.contains("debited") -> TransactionType.EXPENSE
            lowerMessage.contains("withdrawn") -> TransactionType.EXPENSE
            lowerMessage.contains("spent") -> TransactionType.EXPENSE
            lowerMessage.contains("charged") -> TransactionType.EXPENSE
            lowerMessage.contains("paid") -> TransactionType.EXPENSE
            lowerMessage.contains("purchase") -> TransactionType.EXPENSE
            lowerMessage.contains("transferred") -> TransactionType.EXPENSE
            
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
        // JK Bank specific reference patterns
        val jkBankPatterns = listOf(
            // UPI Ref: 115458170728
            Regex("""UPI\s+Ref[:\s]+(\d+)""", RegexOption.IGNORE_CASE),
            // txn Ref: XXXXX
            Regex("""txn\s+Ref[:\s]+([A-Z0-9]+)""", RegexOption.IGNORE_CASE),
            // Reference: XXXXX
            Regex("""Reference[:\s]+([A-Z0-9]+)""", RegexOption.IGNORE_CASE),
            // Ref No: XXXXX
            Regex("""Ref\s+No[:\s]+([A-Z0-9]+)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in jkBankPatterns) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1].trim()
            }
        }
        
        // Fall back to base extraction
        return super.extractReference(message)
    }
    
    override fun extractAccountLast4(message: String): String? {
        // JK Bank specific account patterns
        val jkBankPatterns = listOf(
            // Your A/c XXXXXXXX1111
            Regex("""Your\s+A\/c\s+[X]+(\d{4})""", RegexOption.IGNORE_CASE),
            // A/c XXXXXXXX1111 or A/c XX1111
            Regex("""A\/c\s+[X]*(\d{4})""", RegexOption.IGNORE_CASE),
            // Account XXXXXXXX1111
            Regex("""Account\s+[X]+(\d{4})""", RegexOption.IGNORE_CASE),
            // from A/c ending 1111
            Regex("""A\/c\s+ending\s+(\d{4})""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in jkBankPatterns) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1]
            }
        }
        
        // Fall back to base extraction
        return super.extractAccountLast4(message)
    }
    
    override fun extractBalance(message: String): BigDecimal? {
        // JK Bank specific balance patterns
        val balancePatterns = listOf(
            // Avl Bal: Rs.1234.56
            Regex("""Avl\s+Bal[:\s]+Rs\.?\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            // Balance: Rs.1234.56
            Regex("""Balance[:\s]+Rs\.?\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            // Bal Rs.1234.56
            Regex("""Bal\s+Rs\.?\s*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in balancePatterns) {
            pattern.find(message)?.let { match ->
                val balanceStr = match.groupValues[1].replace(",", "")
                return try {
                    BigDecimal(balanceStr)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        
        // Fall back to base extraction
        return super.extractBalance(message)
    }
    
    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // Skip OTP and verification messages
        if (lowerMessage.contains("otp") || 
            lowerMessage.contains("one time password") ||
            lowerMessage.contains("verification code")) {
            return false
        }
        
        // Skip promotional messages
        if (lowerMessage.contains("offer") || 
            lowerMessage.contains("discount") ||
            lowerMessage.contains("cashback offer") ||
            lowerMessage.contains("win ")) {
            return false
        }
        
        // Skip payment request messages
        if (lowerMessage.contains("has requested") || 
            lowerMessage.contains("payment request") ||
            lowerMessage.contains("collect request") ||
            lowerMessage.contains("requesting payment")) {
            return false
        }
        
        // Skip messages asking to report fraud
        // But make sure the transaction keywords are present
        if (lowerMessage.contains("if not done by you") || 
            lowerMessage.contains("report immediately")) {
            // These are usually part of transaction messages, so check for transaction keywords
            val transactionKeywords = listOf(
                "debited", "credited", "withdrawn", "deposited",
                "spent", "received", "transferred", "paid"
            )
            return transactionKeywords.any { lowerMessage.contains(it) }
        }
        
        // JK Bank specific transaction keywords
        val jkBankTransactionKeywords = listOf(
            "has been debited",
            "has been credited",
            "debited",
            "credited",
            "withdrawn",
            "deposited",
            "spent",
            "received",
            "transferred",
            "paid"
        )
        
        return jkBankTransactionKeywords.any { lowerMessage.contains(it) }
    }
}