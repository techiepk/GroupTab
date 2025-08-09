package com.pennywiseai.tracker.data.parser.banks

import com.pennywiseai.tracker.data.database.entity.TransactionType
import com.pennywiseai.tracker.data.parser.BankParser
import com.pennywiseai.tracker.data.parser.ParsedTransaction
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class CanaraBankParser : BankParser() {
    override fun getBankName(): String = "Canara Bank"
    
    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return normalizedSender.contains("CANBNK") || 
               normalizedSender.contains("CANARA")
    }
    
    override fun parse(smsBody: String, sender: String, timestamp: LocalDateTime): ParsedTransaction? {
        val normalizedBody = smsBody.replace("\n", " ").trim()
        
        // Handle UPI transactions with specific format
        if (normalizedBody.contains("paid thru A/C", ignoreCase = true)) {
            return parseUpiTransaction(normalizedBody, timestamp)
        }
        
        // Handle debit transactions
        if (normalizedBody.contains("DEBITED", ignoreCase = true)) {
            return parseDebitTransaction(normalizedBody, timestamp)
        }
        
        // Handle failed transactions (we don't track these)
        if (normalizedBody.contains("failed due to", ignoreCase = true)) {
            return null
        }
        
        return null
    }
    
    private fun parseUpiTransaction(body: String, timestamp: LocalDateTime): ParsedTransaction? {
        // Pattern: Rs.23.00 paid thru A/C XX1234 on 08-8-25 16:41:00 to BMTC BUS KA57F6
        val amountPattern = """Rs\.?([\d,]+\.?\d*)\s+paid""".toRegex(RegexOption.IGNORE_CASE)
        val merchantPattern = """\sto\s+([^,]+?)(?:,\s*UPI|\.|-Canara)""".toRegex(RegexOption.IGNORE_CASE)
        val dateTimePattern = """on\s+(\d{1,2}-\d{1,2}-\d{2})\s+(\d{1,2}:\d{2}:\d{2})""".toRegex()
        
        val amount = extractAmount(body, amountPattern) ?: return null
        val merchant = extractMerchant(body, merchantPattern) ?: "UPI Payment"
        
        // Try to extract date and time
        val dateTimeMatch = dateTimePattern.find(body)
        val transactionDateTime = if (dateTimeMatch != null) {
            try {
                val dateStr = dateTimeMatch.groupValues[1]
                val timeStr = dateTimeMatch.groupValues[2]
                parseCanaraDateTime(dateStr, timeStr)
            } catch (e: Exception) {
                timestamp
            }
        } else {
            timestamp
        }
        
        return ParsedTransaction(
            amount = amount,
            merchantName = merchant.trim(),
            transactionType = TransactionType.EXPENSE,
            dateTime = transactionDateTime,
            bankName = getBankName()
        )
    }
    
    private fun parseDebitTransaction(body: String, timestamp: LocalDateTime): ParsedTransaction? {
        // Pattern: An amount of INR 50.00 has been DEBITED
        val amountPattern = """INR\s+([\d,]+\.?\d*)""".toRegex(RegexOption.IGNORE_CASE)
        val datePattern = """on\s+(\d{2}/\d{2}/\d{4})""".toRegex()
        
        val amount = extractAmount(body, amountPattern) ?: return null
        
        // Try to extract date
        val dateMatch = datePattern.find(body)
        val transactionDateTime = if (dateMatch != null) {
            try {
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val date = formatter.parse(dateMatch.groupValues[1])
                LocalDateTime.from(date.query { temporal ->
                    temporal.get(java.time.temporal.ChronoField.DAY_OF_MONTH).let { day ->
                        temporal.get(java.time.temporal.ChronoField.MONTH_OF_YEAR).let { month ->
                            temporal.get(java.time.temporal.ChronoField.YEAR).let { year ->
                                LocalDateTime.of(year, month, day, timestamp.hour, timestamp.minute)
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                timestamp
            }
        } else {
            timestamp
        }
        
        return ParsedTransaction(
            amount = amount,
            merchantName = "Canara Bank Debit",
            transactionType = TransactionType.EXPENSE,
            dateTime = transactionDateTime,
            bankName = getBankName()
        )
    }
    
    private fun parseCanaraDateTime(dateStr: String, timeStr: String): LocalDateTime {
        // Parse date like "08-8-25" (DD-M-YY)
        val dateParts = dateStr.split("-")
        val day = dateParts[0].toInt()
        val month = dateParts[1].toInt()
        val year = 2000 + dateParts[2].toInt() // Assuming 20XX
        
        // Parse time like "16:41:00"
        val timeParts = timeStr.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        val second = if (timeParts.size > 2) timeParts[2].toInt() else 0
        
        return LocalDateTime.of(year, month, day, hour, minute, second)
    }
    
    override fun extractMerchant(body: String): String? {
        // Try UPI merchant pattern first
        val upiPattern = """\sto\s+([^,]+?)(?:,\s*UPI|\.|-Canara)""".toRegex(RegexOption.IGNORE_CASE)
        upiPattern.find(body)?.let { 
            return it.groupValues[1].trim()
        }
        
        // Default to generic description
        return when {
            body.contains("DEBITED", ignoreCase = true) -> "Canara Bank Debit"
            else -> null
        }
    }
}