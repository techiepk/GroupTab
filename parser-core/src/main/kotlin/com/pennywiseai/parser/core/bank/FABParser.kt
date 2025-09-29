package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import java.math.BigDecimal

/**
 * Parser for First Abu Dhabi Bank (FAB) - UAE's largest bank
 * Handles AED currency transactions
 */
class FABParser : BankParser() {

    override fun getBankName() = "First Abu Dhabi Bank"

    override fun getCurrency() = "AED"  // UAE Dirham

    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()
        return upperSender == "FAB" ||
               upperSender.contains("FABBANK") ||
               upperSender.contains("ADFAB") ||
               // DLT patterns for UAE might be different
               upperSender.matches(Regex("^[A-Z]{2}-FAB-[A-Z]$"))
    }

    override fun extractAmount(message: String): BigDecimal? {
        // FAB patterns: "AED 8.00", "AED *0.00", etc.
        val patterns = listOf(
            Regex("""AED\s*\*?([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""Amount\s*AED\s*\*?([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""payment.*?AED\s*\*?([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                val amountStr = match.groupValues[1].replace(",", "").replace("*", "")
                return try {
                    BigDecimal(amountStr)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }

        return super.extractAmount(message)
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()

        return when {
            // Credit card transactions
            lowerMessage.contains("credit card purchase") -> TransactionType.EXPENSE
            lowerMessage.contains("debit card purchase") -> TransactionType.EXPENSE
            lowerMessage.contains("card purchase") -> TransactionType.EXPENSE

            // Inward remittance is income
            lowerMessage.contains("inward remittance") -> TransactionType.INCOME

            // Payment instructions are expenses
            lowerMessage.contains("payment instructions") -> TransactionType.EXPENSE
            lowerMessage.contains("has been processed") -> TransactionType.EXPENSE

            // Standard keywords
            lowerMessage.contains("credit") && !lowerMessage.contains("credit card") -> TransactionType.INCOME
            lowerMessage.contains("debit") -> TransactionType.EXPENSE
            lowerMessage.contains("purchase") -> TransactionType.EXPENSE
            lowerMessage.contains("payment") -> TransactionType.EXPENSE

            else -> null
        }
    }
    
    // centralized function to reduce repeated code for card purchase check
    private fun containsCardPurchase(message: String): Boolean {
        return message.contains(Regex("(Credit|Debit) Card Purchase", RegexOption.IGNORE_CASE))
    }

    override fun extractMerchant(message: String, sender: String): String? {
        // Pattern 1: Credit card - merchant on third line after amount
        // "Card No XXXX\nAED 8.00\nT*** R** DUBAI ARE"
        if (containsCardPurchase(message)) {
            val lines = message.split("\n")
            // Find the line with AED amount
            val aedLineIndex = lines.indexOfFirst { it.contains("AED", ignoreCase = true) }
            if (aedLineIndex != -1 && aedLineIndex + 1 < lines.size) {
                val merchantLine = lines[aedLineIndex + 1].trim()
                // Clean up asterisks but keep the text
                val cleanedMerchant = merchantLine.replace("*", "").trim()
                if (cleanedMerchant.isNotEmpty() && !cleanedMerchant.contains("/")) {
                    return cleanMerchantName(cleanedMerchant)
                }
            }
        }

        // Pattern 2: Payment instructions - extract recipient
        // "payment instructions of AED *.00 to 5xxx**1xxx"
        if (message.contains("payment instructions", ignoreCase = true)) {
            val toPattern = Regex("""to\s+([^\s]+)""", RegexOption.IGNORE_CASE)
            toPattern.find(message)?.let { match ->
                val recipient = match.groupValues[1].replace("*", "").trim()
                if (recipient.isNotEmpty()) {
                    return cleanMerchantName(recipient)
                }
            }
        }

        // Pattern 3: Inward remittance
        if (message.contains("Inward Remittance", ignoreCase = true)) {
            return "Inward Remittance"
        }

        return super.extractMerchant(message, sender)
    }

    override fun extractAccountLast4(message: String): String? {
        // Pattern: "Card No XXXX" or "Account XXXX**"
        val patterns = listOf(
            Regex("""Card\s+No\s+([X\d]{4})""", RegexOption.IGNORE_CASE),
            Regex("""Account\s+([X\d]{4})\*{0,2}""", RegexOption.IGNORE_CASE),
            Regex("""Account\s+[X\*]+(\d{4})""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                val accountStr = match.groupValues[1].replace("X", "")
                if (accountStr.isNotEmpty()) {
                    return accountStr
                }
            }
        }

        return super.extractAccountLast4(message)
    }

    override fun extractBalance(message: String): BigDecimal? {
        // Pattern: "Available Balance AED **30.16"
        val balancePattern = Regex("""Available\s+Balance\s+AED\s*\*{0,2}([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        balancePattern.find(message)?.let { match ->
            val balanceStr = match.groupValues[1].replace(",", "").replace("*", "")
            return try {
                BigDecimal(balanceStr)
            } catch (e: NumberFormatException) {
                null
            }
        }

        return super.extractBalance(message)
    }

    override fun extractReference(message: String): String? {
        // Look for date/time as reference (23/09/25 16:17)
        val dateTimePattern = Regex("""(\d{2}/\d{2}/\d{2}\s+\d{2}:\d{2})""")
        dateTimePattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        // Value Date for remittances
        val valueDatePattern = Regex("""Value\s+Date\s+(\d{2}/\d{2}/\d{4})""", RegexOption.IGNORE_CASE)
        valueDatePattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return super.extractReference(message)
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()

        // Skip promotional messages
        if (lowerMessage.contains("bit.ly") ||
            lowerMessage.contains("conditions apply") ||
            lowerMessage.contains("instalments at 0% interest")) {
            // But still process if it has transaction info
            if (!lowerMessage.contains("purchase") &&
                !lowerMessage.contains("payment instructions") &&
                !lowerMessage.contains("remittance")) {
                return false
            }
        }

        // FAB specific transaction keywords
        val fabTransactionKeywords = listOf(
            "credit card purchase",
            "debit card purchase",
            "inward remittance",
            "payment instructions",
            "has been processed",
            "available balance aed"
        )

        if (fabTransactionKeywords.any { lowerMessage.contains(it) }) {
            return true
        }

        return super.isTransactionMessage(message)
    }
}