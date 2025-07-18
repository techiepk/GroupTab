package com.pennywiseai.tracker.parser

import android.util.Log

/**
 * Extracts UPI ID from SMS
 */
class UpiExtractor : BaseExtractor<String>("UpiExtractor") {
    
    override fun extract(smsBody: String, sender: String?): String? {
        
        // Try each UPI pattern
        for (pattern in TransactionPatterns.UPI_PATTERNS) {
            val match = pattern.find(smsBody)
            if (match != null) {
                val upiId = if (match.groups.size > 1) {
                    match.groupValues[1]
                } else {
                    match.value
                }
                
                // Validate UPI ID format
                if (isValidUpiId(upiId)) {
                    logExtraction("UPI ID", upiId, true)
                    return upiId
                }
            }
        }
        
        logExtraction("UPI ID", null, false)
        return null
    }
    
    private fun isValidUpiId(upiId: String): Boolean {
        // Basic UPI ID validation
        return upiId.matches(Regex("[a-zA-Z0-9\\.\\-_]+@[a-zA-Z0-9]+")) &&
               upiId.length > 5 && // Minimum meaningful length
               !upiId.contains(" ") // No spaces allowed
    }
}