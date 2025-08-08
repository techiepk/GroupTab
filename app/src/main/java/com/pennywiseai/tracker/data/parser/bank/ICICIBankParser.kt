package com.pennywiseai.tracker.data.parser.bank

import com.pennywiseai.tracker.data.database.entity.TransactionType
import java.math.BigDecimal

/**
 * Parser for ICICI Bank SMS messages
 * 
 * Supported formats:
 * - Debit: "Your account has been successfully debited with Rs xxx.00"
 * - Credit: "Acct XXxxx is credited with Rs xxx.00"
 * - UPI: "ICICI Bank Acct XXxxx debited for Rs xxx.00"
 * - Cash Deposit: "Cash deposit transaction of Rs xxx in ICICI Bank Account 1234XXXX1234 has been completed"
 * - AutoPay transactions
 * 
 * Common senders: XX-ICICIB-S, ICICIB, ICICIBANK
 */
class ICICIBankParser : BankParser() {
    
    override fun getBankName() = "ICICI Bank"
    
    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("ICICI") ||
               normalizedSender.contains("ICICIB") ||
               // DLT patterns for transactions (-S suffix)
               normalizedSender.matches(Regex("^[A-Z]{2}-ICICIB-S$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-ICICI-S$")) ||
               // Other DLT patterns
               normalizedSender.matches(Regex("^[A-Z]{2}-ICICIB-[TPG]$")) ||
               // Legacy patterns
               normalizedSender.matches(Regex("^[A-Z]{2}-ICICIB$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-ICICI$")) ||
               // Direct sender IDs
               normalizedSender == "ICICIB" ||
               normalizedSender == "ICICIBANK"
    }
    
    override fun extractAmount(message: String): BigDecimal? {
        // Pattern 1: "debited with Rs xxx.00"
        val debitWithPattern = Regex(
            """debited\s+with\s+Rs\.?\s*([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        debitWithPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 2: "debited for Rs xxx.00"
        val debitForPattern = Regex(
            """debited\s+for\s+Rs\.?\s*([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        debitForPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 3: "credited with Rs xxx.00"
        val creditWithPattern = Regex(
            """credited\s+with\s+Rs\.?\s*([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        creditWithPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(amount)
            } catch (e: NumberFormatException) {
                null
            }
        }
        
        // Pattern 4: "Cash deposit transaction of Rs xxx" (must have "has been completed")
        if (message.contains("Cash deposit transaction", ignoreCase = true) && 
            message.contains("has been completed", ignoreCase = true)) {
            val cashDepositPattern = Regex(
                """Cash\s+deposit\s+transaction\s+of\s+Rs\.?\s*([0-9,]+(?:\.\d{2})?)""",
                RegexOption.IGNORE_CASE
            )
            cashDepositPattern.find(message)?.let { match ->
                val amount = match.groupValues[1].replace(",", "")
                return try {
                    BigDecimal(amount)
                } catch (e: NumberFormatException) {
                    null
                }
            }
        }
        
        // Fall back to base class patterns
        return super.extractAmount(message)
    }
    
    override fun extractMerchant(message: String, sender: String): String? {
        // Pattern 1: "towards <merchant> for"
        val towardsPattern = Regex(
            """towards\s+([^.\n]+?)\s+for""",
            RegexOption.IGNORE_CASE
        )
        towardsPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        // Pattern 2: "from <name>. UPI"
        val fromUpiPattern = Regex(
            """from\s+([^.\n]+?)\.\s*UPI""",
            RegexOption.IGNORE_CASE
        )
        fromUpiPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        // Pattern 3: "; <name> credited. UPI"
        val creditedPattern = Regex(
            """;\s*([^.\n]+?)\s+credited\.\s*UPI""",
            RegexOption.IGNORE_CASE
        )
        creditedPattern.find(message)?.let { match ->
            val merchant = cleanMerchantName(match.groupValues[1].trim())
            if (isValidMerchantName(merchant)) {
                return merchant
            }
        }
        
        // Pattern 4: Cash deposit (only if completed)
        if (message.contains("Cash deposit transaction", ignoreCase = true) && 
            message.contains("has been completed", ignoreCase = true)) {
            return "Cash Deposit"
        }
        
        // Pattern 5: AutoPay specific - extract service name
        if (message.contains("AutoPay", ignoreCase = true)) {
            // Look for common AutoPay services
            val lowerMessage = message.lowercase()
            return when {
                lowerMessage.contains("google play") -> "Google Play Store"
                lowerMessage.contains("netflix") -> "Netflix"
                lowerMessage.contains("spotify") -> "Spotify"
                lowerMessage.contains("amazon prime") -> "Amazon Prime"
                lowerMessage.contains("disney") || lowerMessage.contains("hotstar") -> "Disney+ Hotstar"
                lowerMessage.contains("youtube") -> "YouTube Premium"
                else -> "AutoPay Subscription"
            }
        }
        
        // Fall back to base class patterns
        return super.extractMerchant(message, sender)
    }
    
    override fun extractAccountLast4(message: String): String? {
        // Pattern 1: "Acct XXxxx"
        val acctPattern = Regex(
            """Acct\s+(?:XX|X\*+)?(\d{3,4})""",
            RegexOption.IGNORE_CASE
        )
        acctPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Pattern 2: "ICICI Bank Acct XXxxx"
        val bankAcctPattern = Regex(
            """ICICI\s+Bank\s+Acct\s+(?:XX|X\*+)?(\d{3,4})""",
            RegexOption.IGNORE_CASE
        )
        bankAcctPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Pattern 3: "ICICI Bank Account 1234XXXX1234" - extract last 4 visible digits
        val accountFullPattern = Regex(
            """ICICI\s+Bank\s+Account\s+\d+X+(\d{4})""",
            RegexOption.IGNORE_CASE
        )
        accountFullPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Fall back to base class
        return super.extractAccountLast4(message)
    }
    
    override fun extractReference(message: String): String? {
        // Pattern 1: "RRN 1xxxxx3xxxxx"
        val rrnPattern = Regex(
            """RRN\s+([A-Za-z0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        rrnPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Pattern 2: "UPI:5xxxxx8xxxxx"
        val upiPattern = Regex(
            """UPI:([A-Za-z0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        upiPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Pattern 3: "transaction reference no.MCDA001746000000"
        val txnRefPattern = Regex(
            """transaction\s+reference\s+no\.?([A-Z0-9]+)""",
            RegexOption.IGNORE_CASE
        )
        txnRefPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Fall back to base class
        return super.extractReference(message)
    }
    
    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()
        
        // Skip credit card related messages
        if (lowerMessage.contains("icici bank credit card")) {
            return false
        }
        
        // Skip SMS BLOCK instructions (not a transaction)
        if (lowerMessage.contains("sms block") && lowerMessage.contains("to 9215676766")) {
            // This is just instruction text at the end of transaction messages
            // Don't skip the entire message, just ignore this part
        }
        
        // Check for ICICI-specific transaction keywords
        val iciciKeywords = listOf(
            "debited with",
            "debited for",
            "credited with",
            "autopay",
            "your account has been"
        )
        
        // Special check for cash deposit - must be completed
        if (lowerMessage.contains("cash deposit transaction") && 
            lowerMessage.contains("has been completed")) {
            return true
        }
        
        // If any ICICI-specific pattern is found, it's likely a transaction
        if (iciciKeywords.any { lowerMessage.contains(it) }) {
            return true
        }
        
        // Fall back to base class for standard checks
        return super.isTransactionMessage(message)
    }
    
    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()
        
        // Cash deposit is income (only if completed)
        if (lowerMessage.contains("cash deposit transaction") && 
            lowerMessage.contains("has been completed")) {
            return TransactionType.INCOME
        }
        
        // Fall back to base class for standard checks
        return super.extractTransactionType(message)
    }
}