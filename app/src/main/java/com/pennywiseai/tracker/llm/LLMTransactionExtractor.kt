package com.pennywiseai.tracker.llm

import android.content.Context
import android.util.Log
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionCategory
import com.pennywiseai.tracker.data.TransactionType
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import com.pennywiseai.tracker.logging.LogStreamManager

class LLMTransactionExtractor(private val context: Context) {
    
    companion object {
        private const val TAG = "LLMTransactionExtractor"
        private const val MAX_TOKENS = 400 // More conservative limit
        private const val RESET_AFTER_EXTRACTIONS = 15 // Reset every 15 extractions
    }
    
    private var llmInference: LlmInference? = null
    private var isInitialized = false
    private var currentTokens = 0
    private var extractionsProcessed = 0
    
    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "🚀 Initializing LLM Transaction Extractor...")
                
                val modelDownloader = ModelDownloader(context)
                val modelPath = modelDownloader.getModelPath()
                
                if (modelPath == null) {
                    Log.e(TAG, "❌ No model available for transaction extraction")
                    return@withContext false
                }
                
                
                // Create LLM inference 
                val llmInferenceOptions = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .build()
                llmInference = LlmInference.createFromOptions(context, llmInferenceOptions)
                
                isInitialized = true
                Log.i(TAG, "✅ LLM Transaction Extractor initialized successfully!")
                return@withContext true
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to initialize LLM Transaction Extractor: ${e.message}")
                Log.e(TAG, "❌ Exception: ${e.javaClass.simpleName}")
                e.printStackTrace()
                return@withContext false
            }
        }
    }
    
    suspend fun extractTransaction(smsBody: String, sender: String, timestamp: Long): Transaction? {
        return withContext(Dispatchers.IO) {
            if (!isInitialized || llmInference == null) {
                Log.e(TAG, "❌ Extractor not initialized")
                return@withContext null
            }
            
            // Check if we need to reset session to prevent token overflow
            if (shouldResetSession()) {
                resetSession()
            }
            
            
            try {
                val prompt = createExtractionPrompt(smsBody, sender)
                val estimatedTokens = estimateTokens(prompt)
                
                
                // Check if single message is too long
                if (estimatedTokens > 200) {
                    Log.w(TAG, "⚠️ Single message is very long (${estimatedTokens} tokens), might cause issues")
                    Log.w(TAG, "📱 Long SMS: ${smsBody.take(200)}...")
                }
                
                val startTime = System.currentTimeMillis()
                val response = llmInference!!.generateResponse(prompt)
                val endTime = System.currentTimeMillis()
                
                // Update token count
                currentTokens += estimatedTokens + estimateTokens(response)
                extractionsProcessed++
                
                
                // Parse the structured response
                val transaction = parseTransactionResponse(response, smsBody, sender, timestamp)
                
                if (transaction != null) {
                    Log.i(TAG, "✅ Successfully extracted transaction: ${transaction.merchant} - ${if (transaction.amount >= 0) "+" else ""}₹${kotlin.math.abs(transaction.amount)}")
                } else {
                }
                
                return@withContext transaction
                
            } catch (e: Exception) {
                if (e.message?.contains("OUT_OF_RANGE") == true || e.message?.contains("too long") == true) {
                    Log.w(TAG, "⚠️ Token limit exceeded, resetting session and retrying...")
                    resetSession()
                    // Retry once with fresh session
                    return@withContext extractTransaction(smsBody, sender, timestamp)
                }
                Log.e(TAG, "❌ Error extracting transaction: ${e.message}")
                e.printStackTrace()
                return@withContext null
            }
        }
    }
    
    private fun createExtractionPrompt(smsBody: String, sender: String): String {
        return """Extract transaction from SMS. Reply format:
TRANSACTION:YES or NO
DIRECTION:DEBIT or CREDIT
AMOUNT:123.45
MERCHANT:Zomato
CATEGORY:FOOD_DINING
TYPE:ONE_TIME
UPI_ID:merchant@paytm

DIRECTION:
- DEBIT: Money OUT (paid, sent, debited, charged TO merchant)
- CREDIT: Money IN (received, credited, refund FROM someone)
- Payment TO any merchant = DEBIT
- Refunds/cashback = CREDIT

Categories: FOOD_DINING, TRANSPORTATION, SHOPPING, ENTERTAINMENT, BILLS_UTILITIES, HEALTHCARE, EDUCATION, TRAVEL, GROCERIES, SUBSCRIPTION, INVESTMENT, TRANSFER, OTHER

Types: ONE_TIME, SUBSCRIPTION, RECURRING_BILL, TRANSFER, REFUND, INVESTMENT

SMS: $smsBody
Sender: $sender

Response:""".trimIndent()
    }
    
    private fun parseTransactionResponse(response: String, smsBody: String, sender: String, timestamp: Long): Transaction? {
        try {
            LogStreamManager.log(
                LogStreamManager.LogCategory.LLM_ANALYSIS,
                "🔧 Parsing LLM response...",
                LogStreamManager.LogLevel.DEBUG
            )
            
            val lines = response.trim().split("\n").map { it.trim() }
            val data = mutableMapOf<String, String>()
            
            for (line in lines) {
                if (line.contains(":")) {
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        data[parts[0].trim()] = parts[1].trim()
                    }
                }
            }
            
            val isTransaction = data["TRANSACTION"]?.uppercase() == "YES"
            
            if (!isTransaction) {
                LogStreamManager.log(
                    LogStreamManager.LogCategory.LLM_ANALYSIS,
                    "📭 LLM determined this is not a transaction",
                    LogStreamManager.LogLevel.DEBUG
                )
                return null
            }
            
            val baseAmount = data["AMOUNT"]?.toDoubleOrNull() ?: 0.0
            val direction = data["DIRECTION"]?.uppercase() ?: "DEBIT"
            val amount = if (direction == "CREDIT") baseAmount else -baseAmount
            val merchant = data["MERCHANT"] ?: "Unknown"
            val categoryStr = data["CATEGORY"] ?: "OTHER"
            val typeStr = data["TYPE"] ?: "UNKNOWN"
            val upiId = data["UPI_ID"]?.takeIf { it.isNotEmpty() && it != "null" }
            val subscription = data["SUBSCRIPTION"]?.uppercase() == "YES"
            val confidence = data["CONFIDENCE"]?.toFloatOrNull() ?: 0.9f
            
            
            // Enhanced logging for debugging
            LogStreamManager.log(
                LogStreamManager.LogCategory.LLM_ANALYSIS,
                "🎯 Extracted transaction details - Amount: ${if (direction == "CREDIT") "+" else "-"}₹${kotlin.math.abs(amount)}, Merchant: $merchant, Category: $categoryStr",
                LogStreamManager.LogLevel.DEBUG,
                mapOf(
                    "amount" to amount,
                    "direction" to direction,
                    "merchant" to merchant,
                    "category" to categoryStr,
                    "type" to typeStr,
                    "confidence" to confidence
                )
            )
            
            // Map category string to enum
            val categoryEnum = try {
                TransactionCategory.valueOf(categoryStr.uppercase())
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Unknown category '$categoryStr', using OTHER")
                TransactionCategory.OTHER
            }
            
            // Map transaction type string to enum
            val transactionType = try {
                TransactionType.valueOf(typeStr.uppercase())
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Unknown transaction type '$typeStr', using UNKNOWN")
                TransactionType.UNKNOWN
            }
            
            // Generate unique ID
            val uniqueId = generateTransactionId(smsBody, sender, timestamp)
            
            return Transaction(
                id = uniqueId,
                amount = amount,
                merchant = merchant,
                category = categoryEnum,
                date = timestamp,
                rawSms = smsBody,
                upiId = upiId,
                transactionType = transactionType,
                confidence = confidence,
                subscription = subscription
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing LLM response: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    private fun generateTransactionId(smsBody: String, sender: String, timestamp: Long): String {
        val content = "${sender}_${smsBody}_${timestamp}"
        val md5 = MessageDigest.getInstance("MD5")
        val hashBytes = md5.digest(content.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun shouldResetSession(): Boolean {
        // Reset every 8 extractions as a safety measure
        return extractionsProcessed > 0 && extractionsProcessed % 8 == 0
    }
    
    private suspend fun resetSession() {
        try {
            // Reinitialize the LLM to clear session context
            llmInference?.close()
            currentTokens = 0
            extractionsProcessed = 0
            
            // Reinitialize 
            if (!initialize()) {
                Log.e(TAG, "❌ Failed to reinitialize LLM after session reset")
                throw IllegalStateException("Failed to reset LLM session")
            }
            Log.i(TAG, "✅ LLM session reset successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error resetting LLM session: ${e.message}")
            throw e
        }
    }
    
    private fun estimateTokens(text: String): Int {
        // More accurate estimation: ~3 characters per token for typical SMS content
        // Add extra padding for safety
        return ((text.length / 3) + 10).coerceAtLeast(1)
    }
    
    fun isAvailable(): Boolean = isInitialized
    
    suspend fun generateFinanceAdvice(prompt: String): String? {
        return withContext(Dispatchers.IO) {
            if (!isInitialized || llmInference == null) {
                Log.e(TAG, "❌ Extractor not initialized for finance advice")
                return@withContext null
            }
            
            
            try {
                val response = llmInference!!.generateResponse(prompt)
                
                // Clean up response (remove any parsing artifacts)
                val cleanedResponse = cleanFinanceResponse(response)
                
                return@withContext cleanedResponse
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error generating finance advice: ${e.message}")
                e.printStackTrace()
                return@withContext null
            }
        }
    }
    
    private fun cleanFinanceResponse(response: String): String {
        // Remove any structured parsing artifacts and clean up response
        return response
            .replace(Regex("^Response:\\s*"), "")
            .replace(Regex("^Answer:\\s*"), "")
            .trim()
    }
}