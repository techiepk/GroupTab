package com.pennywiseai.tracker.parser.bank

/**
 * Factory for creating bank-specific parsers
 */
object BankParserFactory {
    
    // List of available bank parsers
    private val bankParsers = listOf(
        HDFCBankParser(),
        ICICIBankParser(),
        SBIBankParser(),
        IndusIndBankParser(),
        UnionBankParser(),
        IndianBankParser()
        // Add more bank parsers as needed
    )
    
    // Generic parser for banks without specific implementation
    private val genericParser = GenericBankParser()
    
    /**
     * Get appropriate parser for the given sender
     */
    fun getParser(sender: String): BankParser {
        // Find the first parser that can handle this sender
        return bankParsers.firstOrNull { it.canHandle(sender) } ?: genericParser
    }
    
    /**
     * Get bank name from sender ID
     */
    fun getBankName(sender: String): String {
        return getParser(sender).getBankName()
    }
}

/**
 * Generic parser for banks without specific implementation
 */
class GenericBankParser : BankParser() {
    override fun getBankName() = "Bank"
    override fun canHandle(sender: String) = true // Accepts any sender
}