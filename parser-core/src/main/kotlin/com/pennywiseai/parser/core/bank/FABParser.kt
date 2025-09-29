package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.ParsedTransaction
import java.math.BigDecimal

/**
 * Parser for First Abu Dhabi Bank (FAB) - UAE's largest bank
 * Handles AED currency transactions and global currencies for international transactions
 */
class FABParser : BankParser() {

    override fun getBankName() = "First Abu Dhabi Bank"

    override fun getCurrency() = "AED"  // UAE Dirham (default)

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

        // Extract currency from message or use default AED
        val currency = extractCurrency(smsBody) ?: "AED"

        // Extract available limit for credit card transactions
        val availableLimit = if (type == TransactionType.CREDIT) {
            val limit = extractAvailableLimit(smsBody)
            limit
        } else {
            null
        }

        return ParsedTransaction(
            amount = amount,
            type = type,
            merchant = extractMerchant(smsBody, sender),
            reference = extractReference(smsBody),
            accountLast4 = extractAccountLast4(smsBody),
            balance = extractBalance(smsBody),
            creditLimit = availableLimit,  // TODO: This is actually available limit, will be fixed in SmsReaderWorker
            smsBody = smsBody,
            sender = sender,
            timestamp = timestamp,
            bankName = getBankName(),
            isFromCard = detectIsCard(smsBody),
            currency = currency
        )
    }

    override fun canHandle(sender: String): Boolean {
        val upperSender = sender.uppercase()
        return upperSender == "FAB" ||
               upperSender.contains("FABBANK") ||
               upperSender.contains("ADFAB") ||
               // DLT patterns for UAE might be different
               upperSender.matches(Regex("^[A-Z]{2}-FAB-[A-Z]$"))
    }

    override fun extractAmount(message: String): BigDecimal? {
        // FAB patterns: Support global currencies - "AED 8.00", "THB ###.##", "USD 10.00", etc.
        val patterns = listOf(
            Regex("""([A-Z]{3})\s+\*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),  // Explicit asterisk pattern
            Regex("""([A-Z]{3})\s+([0-9*,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),   // General pattern with asterisks
            Regex("""Amount\s*([A-Z]{3})\s+\*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""Amount\s*([A-Z]{3})\s+([0-9*,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""payment.*?([A-Z]{3})\s+\*([0-9,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE),
            Regex("""payment.*?([A-Z]{3})\s+([0-9*,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            pattern.find(message)?.let { match ->
                val currencyCode = match.groupValues[1].uppercase()
                var amountStr = match.groupValues[2].replace(",", "")

                // Handle asterisks as placeholders - replace with 0s or treat as missing amount
                if (amountStr.contains("*")) {
                    // If it's a pattern like *0.00 or *50.00, extract the numeric part
                    if (amountStr.matches(Regex("""\*\d+(?:\.\d{2})?"""))) {
                        amountStr = amountStr.substring(1) // Remove the asterisk
                    } else if (amountStr.matches(Regex("""\*+\.\d{2}"""))) {
                        // If it's all asterisks before decimal, treat as 0
                        amountStr = "0" + amountStr.substring(amountStr.indexOf('.'))
                    } else {
                        // Try to extract any numeric pattern from the asterisk string
                        val numericMatch = Regex("""(\d+(?:\.\d{2})?)""").find(amountStr)
                        if (numericMatch != null) {
                            amountStr = numericMatch.value
                        } else {
                            // If it's all asterisks or other patterns, return null to let base class handle it
                            return super.extractAmount(message)
                        }
                    }
                }

                return try {
                    BigDecimal(amountStr)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }

        return super.extractAmount(message)
    }

    override fun extractCurrency(message: String): String? {
        // Extract currency code from transaction message
        // Look for currency pattern at the beginning of a line or with specific formatting
        val currencyPatterns = listOf(
            Regex("""^[A-Z]{3}\s+[0-9,]+(?:\.\d{2})?""", RegexOption.IGNORE_CASE),  // Start of line
            Regex("""\n[A-Z]{3}\s+[0-9,]+(?:\.\d{2})?""", RegexOption.IGNORE_CASE), // After newline
            Regex("""Amount\s+([A-Z]{3})\s+[0-9,]+(?:\.\d{2})?""", RegexOption.IGNORE_CASE), // After "Amount"
            Regex("""[A-Z]{3}\s+\*?[0-9,]+(?:\.\d{2})?""", RegexOption.IGNORE_CASE)  // With optional asterisk
        )

        for (pattern in currencyPatterns) {
            pattern.find(message)?.let { match ->
                val currencyMatch = Regex("""([A-Z]{3})""").find(match.value)
                currencyMatch?.let {
                    return it.groupValues[1].uppercase()
                }
            }
        }

        // Fallback to AED if no currency found in message
        return "AED"
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()

        return when {
            // Credit card transactions
            lowerMessage.contains("credit card purchase") -> TransactionType.EXPENSE
            lowerMessage.contains("debit card purchase") -> TransactionType.EXPENSE
            lowerMessage.contains("card purchase") -> TransactionType.EXPENSE
            
            lowerMessage.contains("atm cash withdrawal") -> TransactionType.EXPENSE

            // Inward remittance is income
            lowerMessage.contains("inward remittance") -> TransactionType.INCOME
            //cash deposit is income
            lowerMessage.contains("cash deposit") -> TransactionType.INCOME
            lowerMessage.contains("has been credited to your fab account") -> TransactionType.INCOME

            // Outward remittance is expense
            lowerMessage.contains("outward remittance") -> TransactionType.EXPENSE

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
        // "Card No XXXX\nTHB ###.##\nWWW.GRAB.COM BANGKOK TH"
        if (containsCardPurchase(message)) {
            val lines = message.split("\n")

            // Find the line with currency amount (AED, THB, USD, etc.)
            val currencyLineIndex = lines.indexOfFirst { it.matches(Regex(".*[A-Z]{3}\\s+[0-9,]+(?:\\.\\d{2})?.*", RegexOption.IGNORE_CASE)) }
            if (currencyLineIndex != -1 && currencyLineIndex + 1 < lines.size) {
                val merchantLine = lines[currencyLineIndex + 1].trim()
                // Clean up asterisks but keep the text
                val cleanedMerchant = merchantLine.replace("*", "").trim()
                if (cleanedMerchant.isNotEmpty() && !cleanedMerchant.contains("/")) {
                    return cleanMerchantName(cleanedMerchant)
                }
            }

            // Fallback: Look for merchant after card line (for specific FAB format)
            // Format: "Card XXXX2865" -> next line "THB 283.00" -> next line "WWW.GRAB.COM..."
            val cardPattern = Regex("""Card\s+[X\*]+(\d{4})""", RegexOption.IGNORE_CASE)
            val cardMatch = cardPattern.find(message)
            if (cardMatch != null) {
                val cardLineIndex = lines.indexOfFirst { it.contains(cardMatch.value) }
                if (cardLineIndex != -1 && cardLineIndex + 2 < lines.size) {
                    val merchantLine = lines[cardLineIndex + 2].trim()
                    if (merchantLine.isNotEmpty() &&
                        !merchantLine.contains("Available Balance") &&
                        !merchantLine.matches(Regex("""\d{2}/\d{2}/\d{2}\s+\d{2}:\d{2}"""))) {
                        return cleanMerchantName(merchantLine)
                    }
                }
            }

            // Fallback: Look for merchant pattern directly (website names, etc.)
            val merchantPattern = Regex("""([A-Z]+\.(?:COM|NET|ORG|IN)[^\n]*)""", RegexOption.IGNORE_CASE)
            merchantPattern.find(message)?.let { match ->
                val merchant = match.groupValues[1].trim()
                if (merchant.isNotEmpty()) {
                    return cleanMerchantName(merchant)
                }
            }
        }

        // Pattern 2: ATM Cash withdrawal - no merchant, return "ATM Withdrawal"
        if (message.contains("ATM Cash withdrawal", ignoreCase = true)) {
            return "ATM Withdrawal"
        }

        // Pattern 3: Payment instructions - extract recipient
        // "payment instructions of [CURRENCY] *.00 to 5xxx**1xxx"
        if (message.contains("payment instructions", ignoreCase = true)) {
            val toPattern = Regex("""to\s+([^\s]+)""", RegexOption.IGNORE_CASE)
            toPattern.find(message)?.let { match ->
                val recipient = match.groupValues[1].replace("*", "").trim()
                if (recipient.isNotEmpty()) {
                    return cleanMerchantName(recipient)
                }
            }
        }

        // Pattern 4: Inward remittance
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
        // Pattern: "Available Balance [CURRENCY] **30.16" or "Available Balance AED ***0.00"
        val balancePattern = Regex("""Available\s+Balance\s+([A-Z]{3})\s*\*{0,}([0-9*,]+(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        balancePattern.find(message)?.let { match ->
            var balanceStr = match.groupValues[2].replace(",", "")

            // Handle masked balances like ***0.00
            if (balanceStr.contains("*")) {
                if (balanceStr.matches(Regex("""\*+\d+(?:\.\d{2})?"""))) {
                    // Pattern like ***0.00 - extract the numeric part
                    val numericPart = balanceStr.replace("*", "")
                    balanceStr = numericPart
                } else if (balanceStr.matches(Regex("""\*+\.\d{2}"""))) {
                    // Pattern like ***.00 - treat as 0
                    balanceStr = "0" + balanceStr.substring(balanceStr.indexOf('.'))
                } else {
                    // Other masked patterns - return null
                    return null
                }
            }

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
            "outward remittance",
            "atm cash withdrawal",
            "payment instructions",
            "has been processed",
            "available balance"  // Remove hardcoded "aed" to support all currencies
        )

        if (fabTransactionKeywords.any { lowerMessage.contains(it) }) {
            return true
        }

        // FAB specific: Also check for "credit" (not in base keywords) and other patterns
        if (lowerMessage.contains("credit") ||
            lowerMessage.contains("debit") ||
            lowerMessage.contains("remittance") ||
            lowerMessage.contains("available balance")) {
            return true
        }

        return super.isTransactionMessage(message)
    }
}