package com.pennywiseai.tracker.parser

import com.pennywiseai.tracker.data.Transaction

/**
 * Common interface for transaction extractors
 */
interface TransactionExtractor {
    
    /**
     * Extract transaction from SMS
     * @return Transaction if successfully extracted, null otherwise
     */
    suspend fun extractTransaction(
        smsBody: String, 
        sender: String, 
        timestamp: Long
    ): Transaction?
    
    /**
     * Check if extractor is available and ready
     */
    fun isAvailable(): Boolean
    
    /**
     * Initialize the extractor
     * @return true if initialization successful
     */
    suspend fun initialize(): Boolean
    
    /**
     * Get extractor type for logging/UI
     */
    fun getExtractorType(): ExtractorType
    
    enum class ExtractorType {
        PATTERN_BASED,
        LLM_BASED
    }
}