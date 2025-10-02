package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.MandateInfo
import java.math.BigDecimal

/**
 * Parser for Federal Bank SMS messages
 * 
 * Supported formats:
 * - UPI transactions: "Rs 34.51 debited via UPI on 08-05-2025 13:48:03 to VPA ..."
 * - Card transactions
 * - ATM withdrawals
 * - NEFT/IMPS transfers
 * 
 * Sender patterns: AD-FEDBNK-S, JM-FEDBNK-S, etc.
 */
class FederalBankParser : BankParser() {
    
    override fun getBankName() = "Federal Bank"
    
    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("FEDBNK") ||
               normalizedSender.contains("FEDERAL") ||
               normalizedSender.contains("FEDFIB") ||
               // DLT patterns for transactions (-S suffix)
               normalizedSender.matches(Regex("^[A-Z]{2}-FEDBNK-S$")) ||
               // FedFiB patterns
               normalizedSender.matches(Regex("^[A-Z]{2}-FedFiB-[A-Z]$")) ||
               // Other DLT patterns
               normalizedSender.matches(Regex("^[A-Z]{2}-FEDBNK-[TPG]$")) ||
               // Legacy patterns
               normalizedSender.matches(Regex("^[A-Z]{2}-FEDBNK$"))
    }
    
    override fun extractAmount(message: String): BigDecimal? {
        // Pattern 1: INR 506.52 spent (credit card format)
        val inrSpentPattern = Regex(
            """INR\s+([0-9,]+(?:\.\d{2})?)\s+spent""",
            RegexOption.IGNORE_CASE
        )
        inrSpentPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 2: "you've received INR 10,509.09"
        val receivedPattern = Regex(
            """you've received INR\s+([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        receivedPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }

        // Pattern 3: Rs 34.51 debited via UPI
        val debitPattern = Regex(
            """Rs\s+([0-9,]+(?:\.\d{2})?)\s+debited""",
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
        
        // Pattern 3: Rs 70.00 sent via UPI
        val sentPattern = Regex(
            """Rs\s+([0-9,]+(?:\.\d{2})?)\s+sent""",
            RegexOption.IGNORE_CASE
        )
        sentPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 4: Rs 500.00 credited
        val creditPattern = Regex(
            """Rs\s+([0-9,]+(?:\.\d{2})?)\s+credited""",
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
        
        // Pattern 5: withdrawn Rs 500
        val withdrawnPattern = Regex(
            """withdrawn\s+Rs\s+([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        withdrawnPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Fall back to base class patterns
        return super.extractAmount(message)
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        // Priority 1: IMPS credits - show "IMPS Credit" instead of parsing description
        if (message.contains("credited to your A/c", ignoreCase = true) &&
            message.contains("via IMPS", ignoreCase = true)) {
            return "IMPS Credit"
        }

        // Pattern 1: E-mandate transactions
        if (message.contains("e-mandate", ignoreCase = true) || message.contains("payment of", ignoreCase = true)) {
            // Extract merchant from "payment of INR amount for MERCHANT via e-mandate"
            val emandatePattern = Regex(
                """payment of\s+[^.]+?\s+for\s+([^.\n]+?)\s+via\s+e-mandate""",
                RegexOption.IGNORE_CASE
            )
            emandatePattern.find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) {
                    return merchant
                }
            }

            // Extract merchant from "payment via e-mandate declined for ID: id on Federal Bank Debit Card"
            val emandateDeclinedPattern = Regex(
                """payment via e-mandate\s+declined\s+for\s+ID:\s*[^.]+?\s+on\s+Federal Bank\s+Debit Card\s+\d+""",
                RegexOption.IGNORE_CASE
            )
            if (emandateDeclinedPattern.find(message) != null) {
                return "E-Mandate Declined"
            }
        }

        // Pattern 2: Credit card transactions - "at <merchant> on date"
        if (message.contains("credit card", ignoreCase = true) && message.contains(" at ", ignoreCase = true)) {
            val creditCardPattern = Regex(
                """at\s+([^.\n]+?)\s+on\s+\d""",
                RegexOption.IGNORE_CASE
            )
            creditCardPattern.find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) {
                    // Remove common suffixes like "limited", "ltd", "pvt ltd"
                    val cleanedMerchant = merchant
                        .replace(Regex("""\s+(limited|ltd|pvt\s+ltd|private\s+limited)$""", RegexOption.IGNORE_CASE), "")
                        .trim()
                    return cleanedMerchant.ifEmpty { merchant }
                }
            }
        }
        
        // Pattern 2: UPI transactions - "to VPA merchant@bank"
        if (message.contains("VPA", ignoreCase = true)) {
            val vpaPattern = Regex(
                """to\s+VPA\s+([^\s]+?)(?:\.\s*Ref\s+No|\s*Ref\s+No|$)""",
                RegexOption.IGNORE_CASE
            )
            vpaPattern.find(message)?.let { match ->
                val vpa = match.groupValues[1].trim()
                return parseUPIMerchant(vpa)
            }
        }
        
        // Pattern 3: "to <merchant name>" (general)
        val toPattern = Regex(
            """to\s+([^.\n]+?)(?:\.\s*Ref|Ref\s+No|$)""",
            RegexOption.IGNORE_CASE
        )
        toPattern.find(message)?.let { match ->
            val merchant = match.groupValues[1].trim()
            // Skip if it's VPA (already handled above)
            if (!merchant.contains("VPA", ignoreCase = true)) {
                val cleaned = cleanMerchantName(merchant)
                if (isValidMerchantName(cleaned)) {
                    return cleaned
                }
            }
        }
        
        // Pattern 4: "you've received INR" transactions - extract sender
        if (message.contains("you've received", ignoreCase = true)) {
            val sentByPattern = Regex(
                """It was sent by\s+([^.\n]+?)(?:\s+on|$)""",
                RegexOption.IGNORE_CASE
            )
            sentByPattern.find(message)?.let { match ->
                val sender = match.groupValues[1].trim()
                // If sender is 0000 or similar numbers, categorize as Bank Transfer/Refund
                if (sender.matches(Regex("^0+$")) || sender.length <= 4) {
                    return "Bank Transfer"
                }
                val merchant = cleanMerchantName(sender)
                if (isValidMerchantName(merchant)) {
                    return merchant
                }
            }
        }

        // Pattern 5: "from <sender name>" (for credits)
        val fromPattern = Regex(
            """from\s+([^.\n]+?)(?:\.\s*|$)""",
            RegexOption.IGNORE_CASE
        )
        fromPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        // Pattern 5: ATM transactions
        if (message.contains("ATM", ignoreCase = true) || 
            message.contains("withdrawn", ignoreCase = true)) {
            return "ATM Withdrawal"
        }
        
        // Fall back to base class extraction
        return super.extractMerchant(message, sender)
    }
    
    private fun parseUPIMerchant(vpa: String): String {
        // Extract merchant name from VPA
        val cleanVPA = vpa.split("@")[0].lowercase()
        
        return when {
            // Airlines & Travel (check before payment apps)
            cleanVPA.contains("indigo") -> "Indigo"
            cleanVPA.contains("spicejet") -> "SpiceJet"
            cleanVPA.contains("airasia") -> "AirAsia"
            cleanVPA.contains("vistara") -> "Vistara"
            cleanVPA.contains("airindia") -> "Air India"

            // Ride-hailing
            cleanVPA.contains("uber") -> "Uber"
            cleanVPA.contains("ola") -> "Ola"
            cleanVPA.contains("rapido") -> "Rapido"

            // E-commerce
            cleanVPA.contains("amazon") -> "Amazon"
            cleanVPA.contains("flipkart") -> "Flipkart"
            cleanVPA.contains("myntra") -> "Myntra"
            cleanVPA.contains("meesho") -> "Meesho"

            // Payment apps (check after specific brands)
            cleanVPA.contains("paytm") -> "Paytm"
            cleanVPA.contains("bharatpe") -> "BharatPe"
            cleanVPA.contains("phonepe") -> "PhonePe"
            cleanVPA.contains("googlepay") || cleanVPA.contains("gpay") -> "Google Pay"
            
            // Food delivery
            cleanVPA.contains("swiggy") -> "Swiggy"
            cleanVPA.contains("zomato") -> "Zomato"
            
            // Entertainment
            cleanVPA.contains("netflix") -> "Netflix"
            cleanVPA.contains("spotify") -> "Spotify"
            cleanVPA.contains("hotstar") || cleanVPA.contains("disney") -> "Disney+ Hotstar"
            cleanVPA.contains("prime") -> "Amazon Prime"
            cleanVPA.contains("pvr") || cleanVPA.contains("inox") -> "PVR Inox"
            cleanVPA.contains("bookmyshow") || cleanVPA.contains("bms") -> "BookMyShow"
            
            // Telecom
            cleanVPA.contains("jio") -> "Jio"
            cleanVPA.contains("airtel") -> "Airtel"
            cleanVPA.contains("vodafone") || cleanVPA.contains("vi") -> "Vi"
            cleanVPA.contains("bsnl") -> "BSNL"
            
            // Travel
            cleanVPA.contains("irctc") -> "IRCTC"
            cleanVPA.contains("redbus") -> "RedBus"
            cleanVPA.contains("makemytrip") || cleanVPA.contains("mmt") -> "MakeMyTrip"
            cleanVPA.contains("goibibo") -> "Goibibo"
            cleanVPA.contains("oyo") -> "OYO"
            cleanVPA.contains("airbnb") -> "Airbnb"
            
            // Payment gateways - try to extract actual merchant
            cleanVPA.contains("razorpay") || cleanVPA.contains("razorp") || cleanVPA.contains("rzp") -> {
                when {
                    cleanVPA.contains("pvr") -> "PVR"
                    cleanVPA.contains("inox") -> "PVR Inox"
                    cleanVPA.contains("swiggy") -> "Swiggy"
                    cleanVPA.contains("zomato") -> "Zomato"
                    else -> "Online Payment"
                }
            }
            cleanVPA.contains("payu") || cleanVPA.contains("billdesk") || cleanVPA.contains("ccavenue") -> "Online Payment"
            
            // Individual transfers (just numbers)
            cleanVPA.matches(Regex("\\d+")) -> "Individual"
            
            // Default - return full VPA when no clear pattern is detected
            else -> vpa.trim()
        }
    }
    
    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // Skip OTP and promotional messages
        if (lowerMessage.contains("otp") || 
            lowerMessage.contains("one time password") ||
            lowerMessage.contains("verification code")) {
            return false
        }
        
        // Skip mandate creation notifications and declined payments (not actual transactions)
        if (isMandateCreationNotification(message) || isDeclinedMandatePayment(message)) {
            return false
        }

        // Check for Federal Bank specific transaction keywords
        val federalKeywords = listOf(
            "sent via upi",
            "debited via upi",
            "credited",
            "withdrawn",
            "received",
            "transferred",
            "spent on your credit card",
            "payment of", // Actual mandate payments, not creation notifications
            "payment via e-mandate" // Actual mandate payments
        )
        
        // If any Federal Bank specific pattern is found, it's likely a transaction
        if (federalKeywords.any { lowerMessage.contains(it) }) {
            return true
        }
        
        // Fall back to base class for standard checks
        return super.isTransactionMessage(message)
    }
    
    override fun extractAccountLast4(message: String): String? {
        // Pattern for credit card: "credit card ending with 1234"
        val creditCardPattern = Regex(
            """credit\s+card\s+ending\s+with\s+(\d{4})""",
            RegexOption.IGNORE_CASE
        )
        creditCardPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        // Pattern for debit card transactions: "card XX**9747"
        val debitCardPattern = Regex(
            """card\s+XX\*\*?(\d{4})""",
            RegexOption.IGNORE_CASE
        )
        debitCardPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        // Fall back to base class patterns for regular account numbers
        val accountNumber = super.extractAccountLast4(message)
        if (accountNumber != null) {
            return accountNumber
        }

        // For UPI transactions that don't specify account number,
        // we can't determine the account from the message alone
        // Return null and let the application handle it at a higher level
        return null
    }
    
    override fun extractBalance(message: String): BigDecimal? {
        // Don't extract credit limit as balance - that's handled separately
        // Fall back to base class patterns for actual account balances
        return super.extractBalance(message)
    }
    
    // The base class now handles available limit extraction for credit card transactions
    // No need to override parse() or extractAvailableLimit() as the base implementation covers our patterns
    
    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        
        return when {
            // Credit card transactions
            lowerMessage.contains("credit card") && lowerMessage.contains("spent") -> TransactionType.CREDIT

            // E-mandate payments (only successful ones, not declined)
            (lowerMessage.contains("e-mandate") || lowerMessage.contains("payment of")) &&
            lowerMessage.contains("processed successfully") -> TransactionType.EXPENSE

            // Expense keywords - including "sent"
            lowerMessage.contains("sent via upi") -> TransactionType.EXPENSE
            lowerMessage.contains("debited") -> TransactionType.EXPENSE
            lowerMessage.contains("withdrawn") -> TransactionType.EXPENSE
            lowerMessage.contains("spent") && !lowerMessage.contains("credit card") -> TransactionType.EXPENSE
            lowerMessage.contains("paid") -> TransactionType.EXPENSE

            // Income keywords
            lowerMessage.contains("credited") -> TransactionType.INCOME
            lowerMessage.contains("received") -> TransactionType.INCOME
            lowerMessage.contains("deposited") -> TransactionType.INCOME
            lowerMessage.contains("refund") -> TransactionType.INCOME

            else -> super.extractTransactionType(message)
        }
    }

    /**
     * Checks if this is a mandate creation notification (not an actual transaction).
     * Follows the same pattern as HDFC and SBI parsers.
     */
    fun isMandateCreationNotification(message: String): Boolean {
        val lowerMessage = message.lowercase()

        // Federal Bank mandate creation patterns
        return (lowerMessage.contains("mandate") || lowerMessage.contains("e-mandate")) &&
               (lowerMessage.contains("successfully created a mandate") ||
                lowerMessage.contains("you have successfully created") ||
                lowerMessage.contains("successfully created") ||
                lowerMessage.contains("has been initiated") ||
                lowerMessage.contains("registration has been initiated"))
    }

    /**
     * Checks if this is a declined e-mandate payment (not an actual expense).
     */
    fun isDeclinedMandatePayment(message: String): Boolean {
        val lowerMessage = message.lowercase()

        return (lowerMessage.contains("e-mandate") || lowerMessage.contains("payment of")) &&
               lowerMessage.contains("declined")
    }

    /**
     * Parses E-Mandate subscription information for Federal Bank.
     * Compatible with HDFC's EMandateInfo structure for consistency.
     */
    fun parseEMandateSubscription(message: String): EMandateInfo? {
        if (!isMandateCreationNotification(message)) {
            return null
        }

        // Extract amount - "maximum amount of Rs 119.00" or "for a maximum amount of Rs 119.00"
        val amountPattern = Regex("""(?:for\s+a\s+)?maximum\s+amount\s+of\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        val amount = amountPattern.find(message)?.let { match ->
            val amountStr = match.groupValues[1].replace(",", "")
            try {
                BigDecimal(amountStr)
            } catch (e: NumberFormatException) {
                null
            }
        } ?: return null

        // Extract start date - "starting from 11-06-2025"
        val datePattern = Regex("""starting\s+from\s+(\d{2}-\d{2}-\d{4})""", RegexOption.IGNORE_CASE)
        val startDate = datePattern.find(message)?.groupValues?.get(1)

        // Extract merchant - "created a mandate on Spotify India" or "mandate on Spotify India"
        val merchantPattern = Regex("""(?:created\s+a\s+mandate\s+on|mandate\s+on)\s+([^.\n]+?)(?:\s+for|\s*$)""", RegexOption.IGNORE_CASE)
        val merchant = merchantPattern.find(message)?.let { match ->
            cleanMerchantName(match.groupValues[1].trim())
        } ?: "Unknown Subscription"

        // Extract UMN - "Mandate Ref No- 52c3f1f40cf0435a90988fe85381d2ff@fifederal" or "Mandate Ref No- 52c3f1f40cf0435a90988fe85381d2ff@fifederal"
        val umnPattern = Regex("""Mandate\s+Ref\s+No-?\s*([^.\s]+)""", RegexOption.IGNORE_CASE)
        val umn = umnPattern.find(message)?.groupValues?.get(1)

        return EMandateInfo(
            amount = amount,
            nextDeductionDate = startDate,
            merchant = merchant,
            umn = umn
        )
    }

    /**
     * Parses future debit notifications for subscription tracking.
     * For payment due messages.
     */
    fun parseFutureDebit(message: String): EMandateInfo? {
        val lowerMessage = message.lowercase()

        // Check if this is a payment due notification
        if (!lowerMessage.contains("payment due") || !lowerMessage.contains("will be processed")) {
            return null
        }

        // Extract amount - "INR 119.00"
        val amountPattern = Regex("""INR\s+(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        val amount = amountPattern.find(message)?.let { match ->
            val amountStr = match.groupValues[1].replace(",", "")
            try {
                BigDecimal(amountStr)
            } catch (e: NumberFormatException) {
                null
            }
        } ?: return null

        // Extract due date - "on 06/08/2024"
        val datePattern = Regex("""on\s+(\d{2}/\d{2}/\d{4})""", RegexOption.IGNORE_CASE)
        val dueDate = datePattern.find(message)?.groupValues?.get(1)?.let { dateStr ->
            // Convert DD/MM/YYYY to DD/MM/YY for consistency
            try {
                val parts = dateStr.split("/")
                if (parts.size == 3) {
                    "${parts[0]}/${parts[1]}/${parts[2].takeLast(2)}"
                } else {
                    dateStr
                }
            } catch (e: Exception) {
                dateStr
            }
        }

        // Extract merchant - "for Spotify"
        val merchantPattern = Regex("""for\s+([^.\n]+?)\s*,\s*INR""", RegexOption.IGNORE_CASE)
        val merchant = merchantPattern.find(message)?.let { match ->
            cleanMerchantName(match.groupValues[1].trim())
        } ?: "Unknown Subscription"

        return EMandateInfo(
            amount = amount,
            nextDeductionDate = dueDate,
            merchant = merchant,
            umn = null // Payment due notifications don't have UMN
        )
    }

    /**
     * E-Mandate subscription information for Federal Bank.
     * Implements the common MandateInfo interface for standardized handling.
     */
    data class EMandateInfo(
        override val amount: BigDecimal,
        override val nextDeductionDate: String?,
        override val merchant: String,
        override val umn: String?
    ) : MandateInfo {
        // Federal Bank uses dd-MM-yyyy format for mandate creation dates
        override val dateFormat = "dd-MM-yyyy"
    }

    // Wrapper function for testing - exposes protected isTransactionMessage method
    fun isTransactionMessageForTesting(message: String): Boolean = isTransactionMessage(message)
}