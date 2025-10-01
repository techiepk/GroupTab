package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.TransactionType
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
            Regex(
                """for\s+([A-Z]{3})\s+([0-9,]+(?:\.\d{2})?)""",
                RegexOption.IGNORE_CASE
            ),  // Add this line
            Regex(
                """([A-Z]{3})\s+\*([0-9,]+(?:\.\d{2})?)""",
                RegexOption.IGNORE_CASE
            ),  // Explicit asterisk pattern
            Regex(
                """([A-Z]{3})\s+([0-9*,]+(?:\.\d{2})?)""",
                RegexOption.IGNORE_CASE
            ),   // General pattern with asterisks
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
            Regex(
                """Amount\s+([A-Z]{3})\s+[0-9,]+(?:\.\d{2})?""",
                RegexOption.IGNORE_CASE
            ), // After "Amount"
            Regex(
                """[A-Z]{3}\s+\*?[0-9,]+(?:\.\d{2})?""",
                RegexOption.IGNORE_CASE
            )  // With optional asterisk
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
            // Credit card transactions (always expenses)
            lowerMessage.contains("credit card purchase") -> TransactionType.CREDIT
            lowerMessage.contains("debit card purchase") -> TransactionType.EXPENSE
            lowerMessage.contains("card purchase") -> TransactionType.EXPENSE

            //Cheque transactions
            lowerMessage.contains("cheque credited") -> TransactionType.INCOME
            lowerMessage.contains("cheque returned") -> TransactionType.EXPENSE

            // ATM withdrawals are expenses
            lowerMessage.contains("atm cash withdrawal") -> TransactionType.EXPENSE

            // Income transactions
            lowerMessage.contains("inward remittance") -> TransactionType.INCOME
            lowerMessage.contains("cash deposit") -> TransactionType.INCOME
            lowerMessage.contains("has been credited to your fab account") -> TransactionType.INCOME

            // Outward remittance and payment instructions are expenses
            lowerMessage.contains("outward remittance") -> TransactionType.EXPENSE
            lowerMessage.contains("payment instructions") -> TransactionType.EXPENSE
            lowerMessage.contains("has been processed") -> TransactionType.EXPENSE

            // Standard keywords - but be more careful with context
            lowerMessage.contains("credit") && !lowerMessage.contains("credit card") &&
                    !lowerMessage.contains("debit") &&
                    !lowerMessage.contains("purchase") &&
                    !lowerMessage.contains("payment") -> TransactionType.INCOME

            lowerMessage.contains("debit") && !lowerMessage.contains("credit") -> TransactionType.EXPENSE
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
        // Pattern 1: Credit/Debit card - merchant on third line after amount
        // "Card No XXXX\nTHB ###.##\nWWW.GRAB.COM BANGKOK TH"
        if (containsCardPurchase(message)) {
            val lines = message.split("\n")

            // Find the line with currency amount (AED, THB, USD, etc.)
            val currencyLineIndex = lines.indexOfFirst {
                it.matches(
                    Regex(
                        ".*[A-Z]{3}\\s+[0-9,]+(?:\\.\\d{2})?.*",
                        RegexOption.IGNORE_CASE
                    )
                )
            }
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
                        !merchantLine.matches(Regex("""\d{2}/\d{2}/\d{2}\s+\d{2}:\d{2}"""))
                    ) {
                        return cleanMerchantName(merchantLine)
                    }
                }
            }

            // Fallback: Look for merchant pattern directly (website names, etc.)
            val merchantPattern =
                Regex("""([A-Z]+\.(?:COM|NET|ORG|IN)[^\n]*)""", RegexOption.IGNORE_CASE)
            merchantPattern.find(message)?.let { match ->
                val merchant = match.groupValues[1].trim()
                if (merchant.isNotEmpty()) {
                    return cleanMerchantName(merchant)
                }
            }
        }

// Pattern 2: Payment instructions and funds transfer - extract recipient account
if (message.contains("payment instructions", ignoreCase = true) ||
    message.contains("funds transfer request", ignoreCase = true)
) {
    // Pattern: "to account XXXX0002 has been processed" - extract just the account number
    // CHANGED LINE: Modified regex to extract just the account number and format as "Transfer to XXX" where XXX are the last 3 digits
    val toPattern = Regex("""to\s+(?:IBAN/Account/Card\s+)?account\s+([X\d]{4,})""", RegexOption.IGNORE_CASE)
    toPattern.find(message)?.let { match ->
        val recipient = match.groupValues[1]
        // Extract just the last 3 digits of the account number
        val lastThreeDigits = recipient.takeLast(3)
        return "Transfer to $lastThreeDigits"
    }
    
    // Fallback pattern: "to IBAN/Account/Card XXXX0002" without "has been processed"
    val fallbackPattern = Regex("""to\s+(?:IBAN/Account/Card\s+)?([X\d]{4,})""", RegexOption.IGNORE_CASE)
    fallbackPattern.find(message)?.let { match ->
        val recipient = match.groupValues[1]
        // Extract just the last 3 digits of the account number
        val lastThreeDigits = recipient.takeLast(3)
        return "Transfer to $lastThreeDigits"
    }
}

        if (message.contains("has been credited to your fab account", ignoreCase = true) &&
            !message.contains("unsuccessful transaction", ignoreCase = true)) {
            return "Account Credited"
        }

        // Patterns for specific transaction types that act as merchants
        val transactionTypeMerchants = mapOf(
            "ATM Cash withdrawal" to "ATM Withdrawal",
            "Inward Remittance" to "Inward Remittance",
            "Outward Remittance" to "Outward Remittance",
            "Cash Deposit" to "Cash Deposit",
            "Cheque Credited" to "Cheque Credited",
            "Cheque Returned" to "Cheque Returned",
            "Cash withdrawal" to "Cash Withdrawal",
            "unsuccessful transaction" to "Refund" // unsuccessful transaction of AED xx.xx has been credited to your account XXXX ,this only happens during a refund of a failed transaction
        )

        for ((keyword, merchantName) in transactionTypeMerchants) {
            if (message.contains(keyword, ignoreCase = true)) {
                return merchantName
            }
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
        val balancePattern = Regex(
            """(?:Available|available)\s+[Bb]alance\s+(?:is\s+)?([A-Z]{3})\s*\*{0,}([0-9*,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
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
        val valueDatePattern =
            Regex("""Value\s+Date\s+(\d{2}/\d{2}/\d{4})""", RegexOption.IGNORE_CASE)
        valueDatePattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return super.extractReference(message)
    }

    //Added public getter for test case
    fun shouldParseTransactionMessage(message: String): Boolean {
        return isTransactionMessage(message);
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()

        // Skip administrative and non-transaction messages
        val nonTransactionKeywords = listOf(
            "declined due to insufficient balance",
            "transaction has been declined",
            "address update request",
            "statement request",
            "stamped statement",
            "cannot process your",
            "amazing rate",
            "conditions apply",
            "bit.ly",
            "instalments at 0% interest",
            "request has been logged",
            "reference number",
            "beneficiary creation/modification request",
            "funds transfer request is under process",
            "has been resolved",
            "funds transfer request has failed",
            "card has been successfully activated",
            "temporarily blocked",
            "never share credit/debit card",
            "debit card.*replacement request",  // Card replacement requests
            "card will be ready for dispatch",  // Card delivery notifications
            "replacement request has been registered",  // Card replacement confirmations
            "otp",
            "activation",
            "thank you for activating",
            "do not disclose your otp",
            "atyourservice@bankfab.com",
            "has been blocked on"  // Email-only messages
        )

        if (nonTransactionKeywords.any { keyword ->
                lowerMessage.contains(Regex(keyword, RegexOption.IGNORE_CASE))
            }) {
            return false
        }

        // Skip promotional messages
        if (lowerMessage.contains("bit.ly") ||
            lowerMessage.contains("conditions apply") ||
            lowerMessage.contains("instalments at 0% interest")
        ) {
            // But still process if it has transaction info
            if (!lowerMessage.contains("purchase") &&
                !lowerMessage.contains("payment instructions") &&
                !lowerMessage.contains("remittance")
            ) {
                return false
            }
        }

        // FAB specific transaction keywords - only actual completed transactions
        val fabTransactionKeywords = listOf(
            "credit card purchase",
            "debit card purchase",
            "inward remittance",
            "outward remittance",
            "atm cash withdrawal",
            "payment instructions",
            "has been processed",
            "has been credited to your fab account",
            "cash deposit",
            "cheque credited",
            "cheque returned"
        )

        // Special handling for funds transfer - only completed ones
        if (lowerMessage.contains("funds transfer request of")) {
            // Only allow if it's been processed successfully (not pending)
            if (lowerMessage.contains("has been processed successfully")) {
                return true
            }
        }

        if (fabTransactionKeywords.any { lowerMessage.contains(it) }) {
            return true
        }

        // Only check for these if they contain actual transaction amounts
        if ((lowerMessage.contains("credit") && !lowerMessage.contains("credit card")) ||
            lowerMessage.contains("debit") ||
            lowerMessage.contains("remittance") ||
            lowerMessage.contains("available balance")
        ) {

            // Only return true if there's a currency amount pattern
            val amountPattern = Regex("""[A-Z]{3}\s+[0-9,]+(?:\.\d{2})?""", RegexOption.IGNORE_CASE)
            return amountPattern.containsMatchIn(message)
        }

        return super.isTransactionMessage(message)
    }
}