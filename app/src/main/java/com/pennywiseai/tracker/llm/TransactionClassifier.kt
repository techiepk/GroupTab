package com.pennywiseai.tracker.llm

import android.content.Context
import android.util.Log
import com.pennywiseai.tracker.data.Transaction
import com.pennywiseai.tracker.data.TransactionCategory
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import java.io.File

class TransactionClassifier(private val context: Context) {
    
    private var llmInference: LlmInference? = null
    private var isInitialized = false
    private val persistentDownloader = PersistentModelDownloader(context)
    private var currentTokens = 0
    private var sessionsProcessed = 0
    
    companion object {
        private const val TAG = "TransactionClassifier"
        private const val MAX_TOKENS = 400 // More conservative limit
        private const val RESET_AFTER_SESSIONS = 15 // Reset every 15 classifications
    }
    
    fun downloadModel(): Flow<DownloadProgress> {
        return persistentDownloader.downloadModel()
    }
    
    suspend fun initialize(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                
                // Check if model is downloaded
                val modelPath = persistentDownloader.getModelPath()
                if (modelPath == null) {
                    Log.e(TAG, "❌ Model not found. Please download model first.")
                    return@withContext false
                }
                
                Log.i(TAG, "📊 Model size: ${getModelSize() / (1024 * 1024)}MB")
                
                // Initialize MediaPipe LLM with LiteRT task file
                
                try {
                    // Check available memory before attempting to load large model
                    val runtime = Runtime.getRuntime()
                    val freeMemory = runtime.freeMemory() / (1024 * 1024) // MB
                    val maxMemory = runtime.maxMemory() / (1024 * 1024) // MB
                    
                    
                    if (freeMemory < 1000) { // Less than 1GB free
                        Log.w(TAG, "⚠️ Low memory warning: ${freeMemory}MB available, model needs ~2.7GB")
                        // Try to free up memory
                        System.gc()
                        Thread.sleep(1000)
                    }
                    
                    // Use MediaPipe API compatible with 0.10.11
                    val taskOptions = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelPath)
                        .build()
                    
                    llmInference = LlmInference.createFromOptions(context, taskOptions)
                    isInitialized = true
                    Log.i(TAG, "✅ Real LLM initialized successfully!")
                    
                } catch (e: UnsatisfiedLinkError) {
                    Log.e(TAG, "❌ Native library error - likely emulator incompatibility: ${e.message}")
                    throw IllegalStateException("LLM not supported on this device/emulator", e)
                } catch (e: OutOfMemoryError) {
                    Log.e(TAG, "❌ Out of memory loading model: ${e.message}")
                    throw IllegalStateException("Insufficient memory for model", e)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to create LLM inference: ${e.message}")
                    Log.e(TAG, "❌ Exception type: ${e.javaClass.simpleName}")
                    Log.e(TAG, "❌ This might be due to:")
                    Log.e(TAG, "   - Emulator architecture incompatibility")
                    Log.e(TAG, "   - Model format not supported by MediaPipe 0.10.11")
                    Log.e(TAG, "   - Insufficient device resources")
                    throw IllegalStateException("LLM initialization failed - try on real device", e)
                }
                
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to initialize LLM: ${e.message}")
                isInitialized = false
                false
            }
        }
    }
    
    suspend fun classifyTransaction(transaction: Transaction): TransactionCategory {
        return withContext(Dispatchers.IO) {
            
            if (!isInitialized || llmInference == null) {
                Log.e(TAG, "❌ LLM not initialized. Cannot classify without model.")
                throw IllegalStateException("LLM not initialized. Please download and initialize model first.")
            }
            
            // Check if we need to reset session to prevent token overflow
            if (shouldResetSession()) {
                resetSession()
            }
            
            try {
                val prompt = createClassificationPrompt(transaction)
                val estimatedTokens = estimateTokens(prompt)
                
                
                val startTime = System.currentTimeMillis()
                val response = llmInference!!.generateResponse(prompt)
                val endTime = System.currentTimeMillis()
                
                // Update token count
                currentTokens += estimatedTokens + estimateTokens(response)
                sessionsProcessed++
                
                
                val category = parseCategory(response)
                if (category != null) {
                    return@withContext category
                } else {
                    Log.w(TAG, "⚠️ LLM returned invalid category, cannot classify")
                    throw IllegalArgumentException("LLM returned invalid category: $response")
                }
                
            } catch (e: Exception) {
                if (e.message?.contains("OUT_OF_RANGE") == true || e.message?.contains("too long") == true) {
                    Log.w(TAG, "⚠️ Token limit exceeded, resetting session and retrying...")
                    resetSession()
                    // Retry once with fresh session
                    return@withContext classifyTransaction(transaction)
                }
                Log.e(TAG, "❌ LLM Classification failed for ${transaction.merchant}: ${e.message}")
                throw e
            }
        }
    }
    
    private fun createClassificationPrompt(transaction: Transaction): String {
        return """Classify this UPI transaction into exactly one category. Respond with only the category name.

Categories: FOOD_DINING, TRANSPORTATION, SHOPPING, ENTERTAINMENT, BILLS_UTILITIES, HEALTHCARE, EDUCATION, TRAVEL, GROCERIES, SUBSCRIPTION, INVESTMENT, TRANSFER, OTHER

Transaction:
Merchant: ${transaction.merchant}
Amount: ₹${transaction.amount}
${transaction.upiId?.let { "UPI: $it" } ?: ""}

Category:"""
    }
    
    private fun shouldResetSession(): Boolean {
        // Reset every 8 classifications as a safety measure
        return sessionsProcessed > 0 && sessionsProcessed % 8 == 0
    }
    
    private suspend fun resetSession() {
        try {
            // Reinitialize the LLM to clear session context
            llmInference?.close()
            currentTokens = 0
            sessionsProcessed = 0
            
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
    
    private fun parseCategory(response: String): TransactionCategory? {
        return try {
            // Clean up the response and try to match
            val cleanResponse = response.trim().uppercase().replace(" ", "_")
            TransactionCategory.valueOf(cleanResponse)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Could not parse category from LLM response: '$response'")
            null
        }
    }
    
    fun isAvailable(): Boolean = isInitialized
    
    fun isModelDownloaded(): Boolean {
        return try {
            persistentDownloader.isModelDownloaded()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking if model is downloaded: ${e.message}")
            false
        }
    }
    
    fun getModelSize(): Long {
        return try {
            val modelFile = File(context.filesDir, "Gemma2-2B-IT_multi-prefill-seq_q8_ekv1280.task")
            if (modelFile.exists()) modelFile.length() else 0L
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting model size: ${e.message}")
            0L
        }
    }
    
    fun deleteModel() {
        try {
            persistentDownloader.deleteModel()
            isInitialized = false
            llmInference = null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting model: ${e.message}")
        }
    }
}
