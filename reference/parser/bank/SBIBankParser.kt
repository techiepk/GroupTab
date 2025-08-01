package com.pennywiseai.tracker.parser.bank

import com.pennywiseai.tracker.parser.AmountExtractor

/**
 * Parser for State Bank of India (SBI) SMS messages
 */
class SBIBankParser : BankParser() {
    
    override fun getBankName() = "State Bank of India"
    
    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("SBI") || 
               normalizedSender.contains("SBIINB") ||
               normalizedSender.contains("SBIUPI") ||
               normalizedSender.contains("SBICRD") ||
               // Direct sender IDs
               normalizedSender == "SBIBK" ||
               normalizedSender == "SBIBNK" ||
               // DLT patterns for transactions (-S suffix)
               normalizedSender.matches(Regex("^[A-Z]{2}-SBIBK-S$")) ||
               // Other DLT patterns (OTP, Promotional, Govt)
               normalizedSender.matches(Regex("^[A-Z]{2}-SBIBK-[TPG]$")) ||
               // Legacy patterns without suffix
               normalizedSender.matches(Regex("^[A-Z]{2}-SBIBK$")) ||
               normalizedSender.matches(Regex("^[A-Z]{2}-SBI$"))
    }
    
    override fun extractAmount(message: String, sender: String): AmountExtractor.AmountInfo? {
        // Pattern 1: Rs 500 debited
        val debitPattern1 = Regex("""Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)\s+(?:has\s+been\s+)?debited""", RegexOption.IGNORE_CASE)
        debitPattern1.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
            if (amount != null) {
                return AmountExtractor.AmountInfo(amount, false)
            }
        }
        
        // Pattern 2: INR 500 debited
        val debitPattern2 = Regex("""INR\s*(\d+(?:,\d{3})*(?:\.\d{2})?)\s+(?:has\s+been\s+)?debited""", RegexOption.IGNORE_CASE)
        debitPattern2.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
            if (amount != null) {
                return AmountExtractor.AmountInfo(amount, false)
            }
        }
        
        // Pattern 3: Rs 500 credited
        val creditPattern1 = Regex("""Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)\s+(?:has\s+been\s+)?credited""", RegexOption.IGNORE_CASE)
        creditPattern1.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
            if (amount != null) {
                return AmountExtractor.AmountInfo(amount, true)
            }
        }
        
        // Pattern 4: INR 500 credited
        val creditPattern2 = Regex("""INR\s*(\d+(?:,\d{3})*(?:\.\d{2})?)\s+(?:has\s+been\s+)?credited""", RegexOption.IGNORE_CASE)
        creditPattern2.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
            if (amount != null) {
                return AmountExtractor.AmountInfo(amount, true)
            }
        }
        
        // Pattern 5: withdrawn Rs 500
        val withdrawPattern = Regex("""withdrawn\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        withdrawPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
            if (amount != null) {
                return AmountExtractor.AmountInfo(amount, false)
            }
        }
        
        // Pattern 6: transferred Rs 500
        val transferPattern = Regex("""transferred\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        transferPattern.find(message)?.let { match ->
            val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()
            if (amount != null) {
                return AmountExtractor.AmountInfo(amount, false)
            }
        }
        
        return null
    }
    
    override fun extractAccountLast4(message: String): String? {
        // Pattern 1: A/c XX1234
        val pattern1 = Regex("""A/c\s+(?:XX|X\*+)?(\d{4})""", RegexOption.IGNORE_CASE)
        pattern1.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Pattern 2: from A/c ending 1234
        val pattern2 = Regex("""A/c\s+ending\s+(\d{4})""", RegexOption.IGNORE_CASE)
        pattern2.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Pattern 3: a/c no. XX1234
        val pattern3 = Regex("""a/c\s+no\.?\s+(?:XX|X\*+)?(\d{4})""", RegexOption.IGNORE_CASE)
        pattern3.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        return null
    }
    
    override fun extractAvailableBalance(message: String): Double? {
        // Pattern 1: Avl Bal Rs 1000.00
        val pattern1 = Regex("""Avl\s+Bal\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        pattern1.find(message)?.let { match ->
            return match.groupValues[1].replace(",", "").toDoubleOrNull()
        }
        
        // Pattern 2: Available Balance: Rs 1000
        val pattern2 = Regex("""Available\s+Balance:?\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        pattern2.find(message)?.let { match ->
            return match.groupValues[1].replace(",", "").toDoubleOrNull()
        }
        
        // Pattern 3: Bal: Rs 1000
        val pattern3 = Regex("""Bal:?\s+Rs\.?\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""", RegexOption.IGNORE_CASE)
        pattern3.find(message)?.let { match ->
            return match.groupValues[1].replace(",", "").toDoubleOrNull()
        }
        
        return null
    }
    
    override fun extractReference(message: String): String? {
        // Pattern 1: Ref No 123456789
        val pattern1 = Regex("""Ref\s+No\.?\s*(\w+)""", RegexOption.IGNORE_CASE)
        pattern1.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Pattern 2: Txn# 123456
        val pattern2 = Regex("""Txn#\s*(\w+)""", RegexOption.IGNORE_CASE)
        pattern2.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        // Pattern 3: transaction ID 123456
        val pattern3 = Regex("""transaction\s+ID:?\s*(\w+)""", RegexOption.IGNORE_CASE)
        pattern3.find(message)?.let { match ->
            return match.groupValues[1]
        }
        
        return null
    }
    
}