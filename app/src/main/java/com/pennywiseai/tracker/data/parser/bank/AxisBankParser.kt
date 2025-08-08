package com.pennywiseai.tracker.data.parser.bank

import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal

/**
 * Parser for Axis Bank SMS messages
 */
class AxisBankParser : BankParser() {
    
    override fun getBankName() = "Axis Bank"
    
    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("AXIS BANK") ||
               normalizedSender.contains("AXISBANK") ||
               normalizedSender.contains("AXISBK") ||
               normalizedSender.contains("AXISB") ||
               // DLT patterns for transactions (-S suffix)
               normalizedSender.matches(Regex("^[A-Z]{2}-AXISBK-S$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-AXISBANK-S$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-AXIS-S$")) ||
               // Legacy patterns
               normalizedSender.matches(Regex("^[A-Z]{2}-AXISBK$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-AXIS$")) ||
               // Direct sender IDs
               normalizedSender == "AXISBK" ||
               normalizedSender == "AXISBANK" ||
               normalizedSender == "AXIS"
    }
    
    override fun extractAmount(message: String): BigDecimal? {
        val inrDebitPattern = Regex(
            """INR\s+([0-9,]+(?:\.\d{2})?)\s+debited""",
            RegexOption.IGNORE_CASE
        )
        inrDebitPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        val inrCreditPattern = Regex(
            """INR\s+([0-9,]+(?:\.\d{2})?)\s+credited""",
            RegexOption.IGNORE_CASE
        )
        inrCreditPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        val paymentPattern = Regex(
            """Payment\s+of\s+INR\s+([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        paymentPattern.find(message)?.let { match ->
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
        val upiMerchantPattern = Regex(
            """UPI/[^/]+/[^/]+/([^\n]+?)(?:\s*Not you|\s*$)""",
            RegexOption.IGNORE_CASE
        )
        upiMerchantPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        val upiPersonPattern = Regex(
            """UPI/P2A/[^/]+/([^\n]+?)(?:\s*Not you|\s*$)""",
            RegexOption.IGNORE_CASE
        )
        upiPersonPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        val infoPattern = Regex(
            """Info\s*[-–]\s*([^.\n]+?)(?:\.\s*Chk|\s*$)""",
            RegexOption.IGNORE_CASE
        )
        infoPattern.find(message)?.let { match ->
            val info = match.groupValues[1].trim()
            return when {
                info.contains("SALARY", ignoreCase = true) -> "Salary"
                else -> cleanMerchantName(info)
            }
        }
        
        // Fall back to base class patterns
        return super.extractMerchant(message, sender)
    }
    
    override fun extractAccountLast4(message: String): String? {
        val acNoPattern = Regex(
            """A/c\s+no\.\s+(?:XX|X\*+)?(\d{4,5})""",
            RegexOption.IGNORE_CASE
        )
        acNoPattern.find(message)?.let { match ->
            val digits = match.groupValues[1]
            return if (digits.length > 4) digits.takeLast(4) else digits
        }
        
        val creditCardPattern = Regex(
            """Credit\s+Card\s+(?:XX|X\*+)?(\d{4})""",
            RegexOption.IGNORE_CASE
        )
        creditCardPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        return super.extractAccountLast4(message)
    }
    
    override fun extractReference(message: String): String? {
        val upiRefPattern = Regex(
            """UPI/[^/]+/([0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        upiRefPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        return super.extractReference(message)
    }
    
    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        if (lowerMessage.contains("payment") && 
            lowerMessage.contains("has been received") && 
            lowerMessage.contains("towards your axis bank credit card")) {
            return false
        }
        
        return super.isTransactionMessage(message)
    }
}