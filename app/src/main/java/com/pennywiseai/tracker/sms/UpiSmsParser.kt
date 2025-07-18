package com.pennywiseai.tracker.sms

import android.util.Log
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionCategory
import java.util.UUID
import java.util.regex.Pattern

class UpiSmsParser {
    
    companion object {
        private const val TAG = "UpiSmsParser"
        // Common UPI SMS patterns from different banks - more flexible patterns
        private val UPI_PATTERNS = listOf(
            // Amount first, then merchant - covers most cases
            Pattern.compile(
                "(?i).*Rs\\.?\\s*(\\d+(?:,\\d+)*(?:\\.\\d{2})?).*(?:debited|paid|sent|transferred).*?(?:to|via|from|at)\\s*([^\\n.]+?)(?:\\s+(?:on|at|via|UPI|Ref|A/c).*)?",
                Pattern.DOTALL
            ),
            // UPI specific pattern with merchant and reference
            Pattern.compile(
                "(?i).*(?:debited|paid|sent).*Rs\\.?\\s*(\\d+(?:,\\d+)*(?:\\.\\d{2})?).*?(?:to|via)\\s*([^\\n.]+?).*?(?:UPI|VPA|Ref)",
                Pattern.DOTALL
            ),
            // Generic transaction pattern
            Pattern.compile(
                "(?i).*Rs\\.?\\s*(\\d+(?:,\\d+)*(?:\\.\\d{2})?).*?(?:paid|debited|sent).*?(?:to|at|via)\\s*([A-Za-z0-9@][^\\n.]+?)(?:\\s|\\.|$)",
                Pattern.DOTALL
            ),
            // Simple amount and merchant extraction
            Pattern.compile(
                "(?i)Rs\\.?\\s*(\\d+(?:,\\d+)*(?:\\.\\d{2})?).*?([A-Za-z][A-Za-z0-9\\s@.-]{2,20}?)(?:\\s+(?:payment|transaction|UPI|via|A/c|on))",
                Pattern.DOTALL
            )
        )
        
        
        // Merchant name cleanup patterns
        private val MERCHANT_CLEANUP_PATTERNS = mapOf(
            Pattern.compile("(?i)\\s*(pvt\\s*ltd|private\\s*limited|ltd|inc|corp)\\s*", Pattern.CASE_INSENSITIVE) to "",
            Pattern.compile("(?i)\\s*payment\\s*", Pattern.CASE_INSENSITIVE) to "",
            Pattern.compile("[^a-zA-Z0-9\\s]") to " ",
            Pattern.compile("\\s+") to " "
        )
    }
    
    fun parseUpiTransaction(smsBody: String, sender: String, timestamp: Long): Transaction? {
        
        // Let LLM handle all intelligence - just try to parse any SMS with amount
        
        
        for ((index, pattern) in UPI_PATTERNS.withIndex()) {
            val matcher = pattern.matcher(smsBody)
            if (matcher.find()) {
                try {
                    val amountStr = matcher.group(1) ?: "0"
                    val merchantStr = matcher.group(2) ?: "Unknown"
                    
                    val amount = parseAmount(amountStr)
                    val merchant = cleanMerchantName(merchantStr)
                    val upiId = if (matcher.groupCount() >= 3) matcher.group(3) else null
                    
                    Log.i(TAG, "🎯 Successfully parsed transaction:")
                    
                    return Transaction(
                        id = UUID.randomUUID().toString(),
                        amount = amount,
                        merchant = merchant,
                        category = TransactionCategory.OTHER, // Will be classified by LLM
                        date = timestamp,
                        rawSms = smsBody,
                        upiId = upiId,
                        subscription = false,
                        confidence = 0.8f
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Pattern #${index + 1} matched but parsing failed: ${e.message}")
                    continue
                }
            }
        }
        
        Log.w(TAG, "❌ No patterns matched for UPI SMS")
        return null
    }
    
    
    private fun parseAmount(amountStr: String): Double {
        return amountStr.replace(",", "").toDouble()
    }
    
    private fun cleanMerchantName(rawMerchant: String): String {
        var cleaned = rawMerchant.trim()
        
        // Apply cleanup patterns
        MERCHANT_CLEANUP_PATTERNS.forEach { (pattern, replacement) ->
            cleaned = pattern.matcher(cleaned).replaceAll(replacement)
        }
        
        return cleaned.trim().takeIf { it.isNotEmpty() } ?: "Unknown Merchant"
    }
    
    fun extractMerchantFromUpiId(upiId: String?): String? {
        if (upiId == null) return null
        
        // Extract merchant name from UPI ID (e.g., merchant@paytm -> merchant)
        return upiId.split("@").firstOrNull()?.takeIf { it.isNotEmpty() }
    }
}