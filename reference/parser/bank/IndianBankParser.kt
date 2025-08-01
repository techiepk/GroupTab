package com.pennywiseai.tracker.parser.bank

/**
 * Parser for Indian Bank
 * 
 * Common sender patterns:
 * - Service Implicit (transactions): XX-INDBMK-S (e.g., BV-INDBMK-S, AX-INDBMK-S)
 * - OTP: XX-INDBMK-T
 * - Promotional: XX-INDBMK-P
 * - Direct: INDBMK, INDIANBK
 */
class IndianBankParser : BankParser() {
    override fun getBankName() = "Indian Bank"
    
    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase()
        return normalized.contains("INDIAN BANK") ||
               normalized.contains("INDIANBANK") ||
               // Match DLT patterns for transactions (-S suffix)
               normalized.matches(Regex("^[A-Z]{2}-INDBMK-S$")) ||
               normalized.matches(Regex("^[A-Z]{2}-INDIANBK-S$")) ||
               // Also handle other patterns for completeness
               normalized.matches(Regex("^[A-Z]{2}-INDBMK-[TPG]$")) ||
               normalized.matches(Regex("^[A-Z]{2}-INDIANBK-[TPG]$")) ||
               // Legacy patterns without suffix
               normalized.matches(Regex("^[A-Z]{2}-INDBMK$")) ||
               normalized.matches(Regex("^[A-Z]{2}-INDIANBK$")) ||
               // Direct sender IDs
               normalized == "INDBMK" ||
               normalized == "INDIANBK" ||
               normalized == "INDIAN"
    }
}