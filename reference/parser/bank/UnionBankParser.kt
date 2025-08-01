package com.pennywiseai.tracker.parser.bank

/**
 * Parser for Union Bank of India
 * 
 * Common sender patterns:
 * - Service Implicit (transactions): XX-UNIONB-S (e.g., BV-UNIONB-S, AX-UNIONB-S)
 * - OTP: XX-UNIONB-T
 * - Promotional: XX-UNIONB-P
 * - Direct: UNIONB, UNIONBK
 */
class UnionBankParser : BankParser() {
    override fun getBankName() = "Union Bank of India"
    
    override fun canHandle(sender: String): Boolean {
        val normalized = sender.uppercase()
        return normalized.contains("UNION") ||
               // Match DLT patterns for transactions (-S suffix)
               normalized.matches(Regex("^[A-Z]{2}-UNIONB-S$")) ||
               // Also handle other patterns for completeness
               normalized.matches(Regex("^[A-Z]{2}-UNIONB-[TPG]$")) ||
               // Legacy patterns without suffix
               normalized.matches(Regex("^[A-Z]{2}-UNIONB$")) ||
               // Direct sender IDs
               normalized == "UNIONB" ||
               normalized == "UNIONBK" ||
               normalized == "UNNINB"
    }
}