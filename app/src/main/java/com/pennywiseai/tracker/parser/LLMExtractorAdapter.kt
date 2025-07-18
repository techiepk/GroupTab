package com.pennywiseai.tracker.parser

import android.content.Context
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.llm.LLMTransactionExtractor

/**
 * Adapter to use LLMTransactionExtractor with TransactionExtractor interface
 */
class LLMExtractorAdapter(private val context: Context) : TransactionExtractor {
    
    private val llmExtractor = LLMTransactionExtractor(context)
    
    override suspend fun extractTransaction(
        smsBody: String, 
        sender: String, 
        timestamp: Long
    ): Transaction? {
        return llmExtractor.extractTransaction(smsBody, sender, timestamp)
    }
    
    override fun isAvailable(): Boolean = llmExtractor.isAvailable()
    
    override suspend fun initialize(): Boolean = llmExtractor.initialize()
    
    override fun getExtractorType() = TransactionExtractor.ExtractorType.LLM_BASED
}