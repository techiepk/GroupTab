package com.pennywiseai.tracker.parser

import android.util.Log
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.data.TransactionType
import com.pennywiseai.tracker.logging.LogStreamManager
import com.pennywiseai.tracker.parser.bank.BankParserFactory
import java.security.MessageDigest

/**
 * Pattern-based transaction parser that extracts transaction information
 * from SMS using regex patterns instead of LLM
 */
class PatternTransactionParser {
    
    companion object {
        private const val TAG = "PatternTransactionParser"
    }
    
    // Initialize extractors
    private val smsFilter = SmsFilter()
    private val amountExtractor = AmountExtractor()
    private val merchantExtractor = MerchantExtractor()
    private val categoryExtractor = CategoryExtractor()
    private val typeExtractor = TypeExtractor()
    private val upiExtractor = UpiExtractor()
    
    /**
     * Parse SMS and extract transaction information
     * @return Transaction object if SMS contains valid transaction, null otherwise
     */
    fun parseTransaction(smsBody: String, sender: String, timestamp: Long): Transaction? {
        
        LogStreamManager.log(
            LogStreamManager.LogCategory.SMS_PROCESSING,
            "🔍 Pattern-based parsing SMS from $sender",
            LogStreamManager.LogLevel.DEBUG
        )
        
        // Apply SMS filters first
        if (!smsFilter.shouldProcessSms(smsBody, sender)) {
            val reason = smsFilter.getFilterReason(smsBody, sender)
            LogStreamManager.log(
                LogStreamManager.LogCategory.SMS_PROCESSING,
                "🚫 SMS filtered: $reason",
                LogStreamManager.LogLevel.DEBUG
            )
            return null
        }
        
        // Additional spam check
        if (smsFilter.isLikelySpam(smsBody)) {
            LogStreamManager.log(
                LogStreamManager.LogCategory.SMS_PROCESSING,
                "🚫 SMS detected as spam based on pattern analysis",
                LogStreamManager.LogLevel.DEBUG
            )
            return null
        }
        
        try {
            // Get bank-specific parser
            val bankParser = BankParserFactory.getParser(sender)
            
            // Skip E-Mandate notifications - they will be processed after scanning
            if (smsBody.contains("E-Mandate!", ignoreCase = true)) {
                LogStreamManager.log(
                    LogStreamManager.LogCategory.SMS_PROCESSING,
                    "📅 E-Mandate notification detected, will process after scan completes",
                    LogStreamManager.LogLevel.INFO
                )
                return null
            }
            
            // Extract amount (required)
            val amountInfo = amountExtractor.extract(smsBody, sender)
            if (amountInfo == null) {
                LogStreamManager.log(
                    LogStreamManager.LogCategory.SMS_PROCESSING,
                    "❌ No amount found in SMS, skipping",
                    LogStreamManager.LogLevel.DEBUG
                )
                return null
            }
            
            // Extract merchant using bank-specific parser
            val merchant = bankParser.extractMerchant(smsBody, sender) ?: "Unknown Merchant"
            
            // Extract category
            val category = categoryExtractor.extract(smsBody, sender) ?: TransactionCategory.OTHER
            
            // Extract transaction type
            val transactionType = typeExtractor.extract(smsBody, sender)
            
            // Extract UPI ID (optional)
            val upiId = bankParser.extractUpiVpa(smsBody) ?: upiExtractor.extract(smsBody, sender)
            
            // Extract additional bank-specific information
            val accountLast4 = bankParser.extractAccountLast4(smsBody)
            val reference = bankParser.extractReference(smsBody)
            val availableBalance = bankParser.extractAvailableBalance(smsBody)
            
            // Check if it's a subscription
            val isSubscription = transactionType == TransactionType.SUBSCRIPTION ||
                                smsBody.lowercase().contains("subscription") ||
                                smsBody.lowercase().contains("auto debit")
            
            // Generate unique ID
            val transactionId = generateTransactionId(smsBody, sender, timestamp)
            
            // Create transaction object
            val transaction = Transaction(
                id = transactionId,
                amount = amountInfo.amount,
                merchant = merchant,
                category = category,
                date = timestamp,
                rawSms = smsBody,
                upiId = upiId,
                transactionType = transactionType,
                confidence = 0.7f, // Lower confidence for pattern-based parsing
                subscription = isSubscription,
                sender = sender
            )
            
            Log.i(TAG, "✅ Successfully parsed transaction:")
            Log.i(TAG, "   Bank: ${bankParser.getBankName()}")
            if (accountLast4 != null) Log.i(TAG, "   Account: ****$accountLast4")
            if (reference != null) Log.i(TAG, "   Reference: $reference")
            if (availableBalance != null) Log.i(TAG, "   Available Balance: ₹$availableBalance")
            
            // Store account balance information for later processing
            transaction.accountLast4 = accountLast4
            transaction.availableBalance = availableBalance
            transaction.reference = reference
            
            LogStreamManager.log(
                LogStreamManager.LogCategory.SMS_PROCESSING,
                "✅ Pattern parser extracted: ${if (amountInfo.isCredit) "+" else "-"}₹${kotlin.math.abs(amountInfo.amount)} at $merchant",
                LogStreamManager.LogLevel.INFO,
                null,
                mapOf(
                    "amount" to amountInfo.amount,
                    "merchant" to merchant,
                    "category" to category.toString(),
                    "type" to transactionType.toString(),
                    "bank" to bankParser.getBankName(),
                    "accountLast4" to (accountLast4 ?: ""),
                    "reference" to (reference ?: ""),
                    "availableBalance" to (availableBalance?.toString() ?: "")
                )
            )
            
            return transaction
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing transaction: ${e.message}")
            e.printStackTrace()
            LogStreamManager.log(
                LogStreamManager.LogCategory.SMS_PROCESSING,
                "❌ Pattern parser error: ${e.message}",
                LogStreamManager.LogLevel.ERROR
            )
            return null
        }
    }
    
    /**
     * Check if SMS is likely a transaction message
     */
    fun isTransactionSms(smsBody: String): Boolean {
        val lowerSms = smsBody.lowercase()
        
        // Check for transaction indicators
        val hasAmount = TransactionPatterns.AMOUNT_PATTERNS.any { pattern ->
            pattern.containsMatchIn(smsBody)
        }
        
        val hasTransactionKeyword = (TransactionPatterns.DEBIT_KEYWORDS + TransactionPatterns.CREDIT_KEYWORDS)
            .any { keyword -> lowerSms.contains(keyword) }
        
        return hasAmount && hasTransactionKeyword
    }
    
    private fun generateTransactionId(smsBody: String, sender: String, timestamp: Long): String {
        val content = "${sender}_${smsBody}_${timestamp}"
        val md5 = MessageDigest.getInstance("MD5")
        val hashBytes = md5.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}