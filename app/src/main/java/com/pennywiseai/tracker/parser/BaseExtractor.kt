package com.pennywiseai.tracker.parser

import android.util.Log

/**
 * Base class for all extractors to ensure consistent structure
 */
abstract class BaseExtractor<T>(protected val tag: String) {
    
    /**
     * Extract information from SMS text
     * @param smsBody The SMS text to extract from
     * @param sender The SMS sender (optional, can help with extraction)
     * @return Extracted value or null if not found
     */
    abstract fun extract(smsBody: String, sender: String? = null): T?
    
    /**
     * Clean extracted text by removing extra spaces and trimming
     */
    protected fun cleanText(text: String): String {
        return text.trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-zA-Z0-9\\s\\-_@.]"), "")
            .trim()
    }
    
    /**
     * Parse amount string to Double, handling Indian number format
     */
    protected fun parseAmount(amountStr: String): Double? {
        return try {
            // Remove commas and parse
            amountStr.replace(",", "").toDouble()
        } catch (e: Exception) {
            Log.w(tag, "Failed to parse amount: $amountStr")
            null
        }
    }
    
    /**
     * Log extraction attempt for debugging
     */
    protected fun logExtraction(field: String, value: Any?, success: Boolean) {
        if (success) {
        } else {
            Log.d(tag, "❌ Failed to extract $field")
        }
    }
}