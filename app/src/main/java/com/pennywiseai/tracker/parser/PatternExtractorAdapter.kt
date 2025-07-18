package com.pennywiseai.tracker.parser

import com.pennywiseai.tracker.data.Transaction

/**
 * Adapter to use PatternTransactionParser with TransactionExtractor interface
 */
class PatternExtractorAdapter : TransactionExtractor {
    
    private val parser = PatternTransactionParser()
    
    override suspend fun extractTransaction(
        smsBody: String, 
        sender: String, 
        timestamp: Long
    ): Transaction? {
        return parser.parseTransaction(smsBody, sender, timestamp)
    }
    
    override fun isAvailable(): Boolean = true // Always available
    
    override suspend fun initialize(): Boolean = true // No initialization needed
    
    override fun getExtractorType() = TransactionExtractor.ExtractorType.PATTERN_BASED
}