package com.pennywiseai.tracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.ChatMessage
import com.pennywiseai.tracker.data.ChatMessageType
import com.pennywiseai.tracker.database.AppDatabase
import com.pennywiseai.tracker.repository.TransactionRepository
import com.pennywiseai.tracker.llm.LLMTransactionExtractor
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import java.text.NumberFormat
import java.util.*
import java.util.Calendar

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = TransactionRepository(AppDatabase.getDatabase(application))
    private val llmExtractor = LLMTransactionExtractor(application)
    
    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val messageList = mutableListOf<ChatMessage>()
    
    data class AISettings(
        var responseStyle: String = "Concise",
        var contextMemory: String = "10",
        var autoSuggestions: Boolean = true,
        var showFinancialTips: Boolean = true
    )
    
    private val aiSettings = AISettings()
    
    init {
        // Initialize LLM extractor
        viewModelScope.launch {
            llmExtractor.initialize()
        }
        
        // Load AI settings from preferences
        val prefs = getApplication<Application>().getSharedPreferences("ai_settings", 0)
        aiSettings.responseStyle = prefs.getString("response_style", "Concise") ?: "Concise"
        aiSettings.contextMemory = prefs.getString("context_memory", "10") ?: "10"
        aiSettings.autoSuggestions = prefs.getBoolean("auto_suggestions", true)
        aiSettings.showFinancialTips = prefs.getBoolean("financial_tips", true)
    }
    
    fun addWelcomeMessage() {
        val welcomeMessage = ChatMessage(
            content = """
                👋 Welcome to your AI Finance Assistant!

                I can help you analyze your spending patterns, track subscriptions, and provide personalized financial advice based on your transaction history.

                💡 Try asking:
                - "What's my spending this month?"
                - "Show me my subscription costs"
                - "How can I save money?"
                - "What's my top spending category?"
                - "Analyze my financial trends"

                All your data stays private and secure on your device!
            """.trimIndent(),
            type = ChatMessageType.AI
        )
        
        messageList.add(welcomeMessage)
        _messages.value = messageList.toList()
    }
    
    fun sendMessage(userMessage: String) {
        // Add user message
        val userChatMessage = ChatMessage(
            content = userMessage,
            type = ChatMessageType.USER
        )
        messageList.add(userChatMessage)
        _messages.value = messageList.toList()
        
        // Add typing indicator
        val typingIndicator = ChatMessage(
            content = "",
            type = ChatMessageType.TYPING_INDICATOR
        )
        messageList.add(typingIndicator)
        _messages.value = messageList.toList()
        
        // Generate AI response
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val response = generateFinanceResponse(userMessage)
                
                // Remove typing indicator
                messageList.removeAll { it.type == ChatMessageType.TYPING_INDICATOR }
                
                val aiMessage = ChatMessage(
                    content = response,
                    type = ChatMessageType.AI
                )
                messageList.add(aiMessage)
                _messages.value = messageList.toList()
            } catch (e: Exception) {
                // Remove typing indicator
                messageList.removeAll { it.type == ChatMessageType.TYPING_INDICATOR }
                
                val errorMessage = ChatMessage(
                    content = """
                        ## ⚠️ Error Processing Request
                        
                        Sorry, I encountered an error while processing your request. 
                        
                        **Please try again** or ask a different question.
                        
                        > If the issue persists, try restarting the app or checking your device's available storage.
                    """.trimIndent(),
                    type = ChatMessageType.AI
                )
                messageList.add(errorMessage)
                _messages.value = messageList.toList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun generateFinanceResponse(userMessage: String): String {
        // Create a prompt for the LLM with user's financial data
        val prompt = createFinancePrompt(userMessage)
        
        // Use the LLM to generate response
        return try {
            val response = llmExtractor.generateFinanceAdvice(prompt)
            response ?: """
                ## 🤖 Unable to Generate Response
                
                I'm sorry, I couldn't generate a response at the moment. 
                
                **Please try again** or rephrase your question.
                
                > The AI model may be processing other requests or initializing.
            """.trimIndent()
        } catch (e: Exception) {
            """
                ## 🔧 Processing Error
                
                I'm having trouble processing your request right now. 
                
                **Please try again later** or restart the app.
                
                > This may be due to temporary model unavailability or resource constraints.
            """.trimIndent()
        }
    }
    
    private suspend fun createFinancePrompt(userMessage: String): String {
        // Get user's financial data
        val monthlySpending = getCurrentMonthSpending()
        val topCategories = getTopCategories()
        val subscriptions = getActiveSubscriptions()
        val recentTransactions = getRecentTransactions()
        
        return """You are a concise personal finance assistant. Answer briefly and directly.

Data: Monthly ₹${String.format("%.0f", monthlySpending)} | Top: ${topCategories.joinToString(", ") { "${it.first} ₹${String.format("%.0f", it.second)}" }} | ${subscriptions.size} subscriptions

Question: $userMessage

Rules:
- Max 3-4 sentences
- Use **bold** for key amounts
- Use bullet points (-) for tips
- Include 1 relevant emoji
- Be actionable and specific

Response:"""
    }
    
    private suspend fun getCurrentMonthSpending(): Double {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        val monthEnd = System.currentTimeMillis()
        
        return repository.getTotalSpendingInPeriod(monthStart, monthEnd)
    }
    
    private suspend fun getTopCategories(): List<Pair<String, Double>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.timeInMillis
        val monthEnd = System.currentTimeMillis()
        
        return repository.getCategorySpending(monthStart, monthEnd)
            .sortedByDescending { it.total }
            .take(3)
            .map { it.category.name.replace("_", " ") to it.total }
    }
    
    private suspend fun getActiveSubscriptions(): List<com.pennywiseai.tracker.data.Subscription> {
        return repository.getActiveSubscriptions().firstOrNull() ?: emptyList()
    }
    
    private suspend fun getRecentTransactions(): List<com.pennywiseai.tracker.data.Transaction> {
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        return repository.getTransactionsByDateRange(weekAgo, System.currentTimeMillis()).firstOrNull() ?: emptyList()
    }
    
    fun clearChatHistory() {
        messageList.clear()
        addWelcomeMessage()
    }
    
    fun getAISettings(): AISettings = aiSettings
    
    fun updateResponseStyle(style: String) {
        aiSettings.responseStyle = style
        // Save to preferences if needed
        val prefs = getApplication<Application>().getSharedPreferences("ai_settings", 0)
        prefs.edit().putString("response_style", style).apply()
    }
    
    fun updateContextMemory(memory: String) {
        aiSettings.contextMemory = memory
        val prefs = getApplication<Application>().getSharedPreferences("ai_settings", 0)
        prefs.edit().putString("context_memory", memory).apply()
    }
    
    fun toggleAutoSuggestions() {
        aiSettings.autoSuggestions = !aiSettings.autoSuggestions
        val prefs = getApplication<Application>().getSharedPreferences("ai_settings", 0)
        prefs.edit().putBoolean("auto_suggestions", aiSettings.autoSuggestions).apply()
    }
    
    fun toggleFinancialTips() {
        aiSettings.showFinancialTips = !aiSettings.showFinancialTips
        val prefs = getApplication<Application>().getSharedPreferences("ai_settings", 0)
        prefs.edit().putBoolean("financial_tips", aiSettings.showFinancialTips).apply()
    }
}