package com.pennywiseai.tracker.data.parser.bank

import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.parser.ParsedTransaction
import java.math.BigDecimal

/**
 * Parser for Juspay/Amazon Pay wallet transactions.
 * Handles messages from XX-JUSPAY-X and similar senders.
 */
class JuspayParser : BankParser() {
    
    override fun getBankName() = "Amazon Pay"
    
    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()
        return upperSender.contains("JUSPAY")
    }
    
    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        // Skip non-transaction messages
        if (!isTransactionMessage(smsBody)) {
            return null
        }
        
        val amount = extractAmount(smsBody)
        if (amount == null) {
            return null
        }
        
        val type = extractTransactionType(smsBody)
        if (type == null) {
            return null
        }
        
        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = extractMerchant(smsBody, sender),
            reference = extractReference(smsBody),
            accountLast4 = null, // Wallet doesn't have account numbers
            balance = extractBalance(smsBody),
            smsBody = smsBody,
            sender = sender,
            timestamp = timestamp,
            bankName = getBankName()
        )
    }
    
    override fun extractAmount(message: String): BigDecimal? {
        // Pattern: "INR XXX.XX" or "Rs XXX.XX"
        val patterns = listOf(
            Regex("""INR\s+([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE),
            Regex("""Rs\.?\s*([0-9,]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                val amountStr = match.groupValues[1].replace(",", "")
                return try {
                    BigDecimal(amountStr)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        
        // Fall back to base class patterns
        return super.extractAmount(message)
    }
    
    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        
        return when {
            // Juspay specific keywords
            lowerMessage.contains("debited") -> TransactionType.EXPENSE
            lowerMessage.contains("credited") -> TransactionType.INCOME
            lowerMessage.contains("cashback") -> TransactionType.INCOME
            lowerMessage.contains("refund") -> TransactionType.INCOME
            lowerMessage.contains("received") -> TransactionType.INCOME
            
            // Amazon Pay wallet usually sends debit messages for payments
            lowerMessage.contains("wallet balance") && lowerMessage.contains("debit") -> TransactionType.EXPENSE
            lowerMessage.contains("apay wallet") && lowerMessage.contains("debit") -> TransactionType.EXPENSE
            
            else -> super.extractTransactionType(message)
        }
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        // For Juspay/Amazon Pay, the merchant info is usually not in the message
        // These are typically payment gateway transactions
        // The actual merchant would be in the order details
        
        // Check for common patterns
        if (message.contains("Amazon", ignoreCase = true)) {
            return "Amazon"
        }
        
        // For wallet debits, we can use a generic merchant name
        if (message.contains("wallet", ignoreCase = true)) {
            return "Amazon Pay Transaction"
        }
        
        // Fall back to base extraction
        return super.extractMerchant(message, sender) ?: "Amazon Pay"
    }
    
    override fun extractReference(message: String): String? {
        // Pattern: "Transaction Reference Number is XXXXX"
        val patterns = listOf(
            Regex("""Transaction Reference Number is\s+([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE),
            Regex("""Reference Number[:\s]+([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE),
            Regex("""Ref[:\s]+([A-Za-z0-9]+)""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                return match.groupValues[1].trim()
            }
        }
        
        return super.extractReference(message)
    }
    
    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // Skip OTP and verification messages
        if (lowerMessage.contains("otp") || 
            lowerMessage.contains("verification code") ||
            lowerMessage.contains("one time password")) {
            return false
        }
        
        // Must contain wallet or transaction keywords
        val transactionKeywords = listOf(
            "wallet", "debited", "credited", 
            "apay", "balance", "transaction"
        )
        
        return transactionKeywords.any { lowerMessage.contains(it) }
    }
}