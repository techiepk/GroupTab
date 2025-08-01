package com.pennywiseai.tracker.parser.bank

/**
 * Parser for IndusInd Bank
 * 
 * Common sender patterns:
 * - Service Implicit (transactions): XX-INDBK-S (e.g., BV-INDBK-S, AX-INDBK-S)
 * - OTP: XX-INDBK-T
 * - Promotional: XX-INDBK-P
 */
class IndusIndBankParser : BankParser() {
    override fun getBankName() = "IndusInd Bank"
    
    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase()
        return normalized.contains("INDUSIND") ||
               normalized.contains("INDUSLB") ||
               // Match DLT patterns for transactions (-S suffix)
               normalized.matches(Regex("^[A-Z]{2}-INDBK-S$")) ||
               normalized.matches(Regex("^[A-Z]{2}-INDBNK-S$")) ||
               // Also handle other patterns for completeness
               normalized.matches(Regex("^[A-Z]{2}-INDBK-[TPG]$")) ||
               normalized.matches(Regex("^[A-Z]{2}-INDBNK-[TPG]$")) ||
               // Legacy patterns without suffix
               normalized.matches(Regex("^[A-Z]{2}-INDBK$")) ||
               normalized.matches(Regex("^[A-Z]{2}-INDBNK$")) ||
               // Direct sender IDs
               normalized == "INDBK" ||
               normalized == "INDBNK"
    }
}