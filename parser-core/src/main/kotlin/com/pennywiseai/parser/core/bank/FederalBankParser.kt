package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
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
        
        // Pattern 2: Rs 34.51 debited via UPI
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
        // Pattern 1: Credit card transactions - "at <merchant> on date"
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
                """to\s+VPA\s+([^.\s]+)""",
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
        
        // Pattern 4: "from <sender name>" (for credits)
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
            // Ride-hailing
            cleanVPA.contains("uber") -> "Uber"
            cleanVPA.contains("ola") -> "Ola"
            cleanVPA.contains("rapido") -> "Rapido"
            
            // Payment apps
            cleanVPA.contains("paytm") -> "Paytm"
            cleanVPA.contains("bharatpe") -> "BharatPe"
            cleanVPA.contains("phonepe") -> "PhonePe"
            cleanVPA.contains("googlepay") || cleanVPA.contains("gpay") -> "Google Pay"
            
            // E-commerce
            cleanVPA.contains("amazon") -> "Amazon"
            cleanVPA.contains("flipkart") -> "Flipkart"
            cleanVPA.contains("myntra") -> "Myntra"
            cleanVPA.contains("meesho") -> "Meesho"
            
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
            
            // Default - try to extract meaningful name
            else -> {
                val parts = cleanVPA.split(".", "-", "_")
                parts.firstOrNull { it.length > 3 && !it.all { char -> char.isDigit() } }
                    ?.replaceFirstChar { it.uppercase() } ?: "Merchant"
            }
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
        
        // Check for Federal Bank specific transaction keywords
        val federalKeywords = listOf(
            "sent via upi",
            "debited via upi",
            "credited",
            "withdrawn",
            "received",
            "transferred",
            "spent on your credit card"
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
        
        // Fall back to base class patterns for regular account numbers
        return super.extractAccountLast4(message)
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
}