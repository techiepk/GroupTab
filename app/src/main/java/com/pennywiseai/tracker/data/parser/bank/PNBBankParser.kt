package com.pennywiseai.tracker.data.parser.bank

import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal

/**
 * Parser for Punjab National Bank (PNB) SMS messages
 */
class PNBBankParser : BankParser() {
    
    override fun getBankName() = "Punjab National Bank"
    
    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("PNBBNK") ||
               normalizedSender.contains("PNB") ||
               normalizedSender.contains("PUNBN") ||
               normalizedSender.matches(Regex("^[A-Z]{2}-PNBBNK-S$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-PNB-S$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-PNBBNK$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-PNB$")) ||
               normalizedSender == "PNBBNK" ||
               normalizedSender == "PNB"
    }
    
    override fun extractAmount(message: String): BigDecimal? {
        val debitPattern = Regex(
            """debited\s+INR\s+([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        debitPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        val creditPattern = Regex(
            """INR\s+([0-9,]+(?:\.\d{2})?)\s+has\s+been\s+credited""",
            RegexOption.IGNORE_CASE
        )
        creditPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        val balCreditPattern = Regex(
            """Bal\s+Rs\.?([0-9,]+(?:\.\d{2})?)\s+CR""",
            RegexOption.IGNORE_CASE
        )
        balCreditPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        return super.extractAmount(message)
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        val fromPattern = Regex(
            """From\s+([^/]+)/""",
            RegexOption.IGNORE_CASE
        )
        fromPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        if (message.contains("NEFT", ignoreCase = true)) {
            return "NEFT Transfer"
        }
        
        if (message.contains("UPI", ignoreCase = true)) {
            return "UPI Transaction"
        }
        
        return super.extractMerchant(message, sender)
    }
    
    override fun extractAccountLast4(message: String): String? {
        val acPattern = Regex(
            """A/c\s+(?:XX|X\*+)?(\d{4})""",
            RegexOption.IGNORE_CASE
        )
        acPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        return super.extractAccountLast4(message)
    }
    
    override fun extractReference(message: String): String? {
        val neftRefPattern = Regex(
            """ref\s+no\.\s+([A-Z0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        neftRefPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        val upiRefPattern = Regex(
            """UPI:\s*([0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        upiRefPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        return super.extractReference(message)
    }
    
    override fun extractBalance(message: String): BigDecimal? {
        val balPattern = Regex(
            """Bal\s+(?:INR\s+|Rs\.?)([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        balPattern.find(message)?.let { match ->
            val balanceStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(balanceStr)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        return super.extractBalance(message)
    }
    
    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        if (lowerMessage.contains("register for e-statement")) {
            return true
        }
        
        return super.isTransactionMessage(message)
    }
}