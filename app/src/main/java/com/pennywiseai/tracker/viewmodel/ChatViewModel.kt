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
import kotlinx.coroutines.withTimeoutOrNull
import java.text.NumberFormat
import java.util.*
import java.util.Calendar
import com.pennywiseai.tracker.data.FinancialInsight
import com.pennywiseai.tracker.data.SubscriptionFrequency
import kotlin.math.abs
import android.content.Context
import com.pennywiseai.tracker.llm.PersistentModelDownloader
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log
import com.pennywiseai.tracker.database.CategorySpending

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
        
        return """Finance Assistant:
Monthly spending: ₹${String.format("%.0f", monthlySpending)}
Top category: ${topCategories.firstOrNull()?.let { "${it.first} ₹${String.format("%.0f", it.second)}" } ?: "None"}

Question: $userMessage

Answer in 2-3 sentences. Be direct."""
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
    
    fun isModelDownloaded(): Boolean {
        val modelDownloader = PersistentModelDownloader(getApplication())
        return modelDownloader.isModelDownloaded()
    }
    
    suspend fun generateFinancialInsights(days: Int): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        
        try {
            // Check if LLM is available
            if (!llmExtractor.isInitialized()) {
                // Try to initialize
                if (!llmExtractor.initialize()) {
                    // Model not downloaded
                    return listOf(
                        FinancialInsight(
                            title = "AI Model Required",
                            description = "Download the AI model to get personalized financial insights",
                            type = FinancialInsight.Type.TREND_ANALYSIS,
                            priority = FinancialInsight.Priority.HIGH,
                            actionText = "Download Model",
                            actionQuery = null
                        )
                    )
                }
            }
            
            // Get date range
            val endDate = System.currentTimeMillis()
            val startDate = endDate - (days * 24 * 60 * 60 * 1000L)
            
            // Get spending data
            val totalSpending = repository.getTotalSpendingInPeriod(startDate, endDate)
            val categorySpending = repository.getCategorySpending(startDate, endDate)
                .sortedByDescending { it.total }
            val transactions = repository.getTransactionsByDateRange(startDate, endDate).firstOrNull() ?: emptyList()
            val subscriptions = repository.getActiveSubscriptions().firstOrNull() ?: emptyList()
            
            // Previous period for comparison
            val prevEndDate = startDate
            val prevStartDate = prevEndDate - (days * 24 * 60 * 60 * 1000L)
            val prevSpending = repository.getTotalSpendingInPeriod(prevStartDate, prevEndDate)
            
            // Create prompt for LLM
            val prompt = createFinancialInsightsPrompt(
                days = days,
                totalSpending = totalSpending,
                prevSpending = prevSpending,
                categorySpending = categorySpending,
                transactions = transactions,
                subscriptions = subscriptions
            )
            
            // Get LLM insights with timeout
            val llmResponse = try {
                withTimeoutOrNull(15000) { // 15 second timeout for insights
                    llmExtractor.generateFinancialInsights(prompt)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "LLM insights generation failed", e)
                null
            }
            
            if (llmResponse != null) {
                // Parse LLM response
                insights.addAll(parseLLMInsights(llmResponse))
            } else {
                Log.w("ChatViewModel", "LLM insights timed out or failed, using fallback")
            }
            
            // If LLM didn't provide enough insights, add some basic ones
            if (insights.size < 3) {
                // Add basic spending insight
                if (totalSpending > 0) {
                    val change = totalSpending - prevSpending
                    val changePercent = if (prevSpending > 0) ((change / prevSpending) * 100) else 0.0
                    
                    insights.add(FinancialInsight(
                        title = if (change > 0) "Spending Increased" else "Spending Decreased",
                        description = "Your spending ${if (change > 0) "increased" else "decreased"} by ₹${String.format("%.0f", abs(change))} (${String.format("%.0f", abs(changePercent))}%) compared to the previous $days days",
                        type = if (change > 0) FinancialInsight.Type.SPENDING_ALERT else FinancialInsight.Type.SAVING_TIP,
                        priority = if (abs(changePercent) > 20) FinancialInsight.Priority.HIGH else FinancialInsight.Priority.MEDIUM,
                        amount = totalSpending
                    ))
                }
                
                // Add subscription insight if available
                if (subscriptions.isNotEmpty()) {
                    val activeCount = subscriptions.count { it.active }
                    insights.add(FinancialInsight(
                        title = "Active Subscriptions",
                        description = "You have $activeCount active subscriptions. Review them to save money.",
                        type = FinancialInsight.Type.SUBSCRIPTION_ALERT,
                        priority = FinancialInsight.Priority.MEDIUM,
                        actionText = "View All",
                        actionQuery = "Show me all my subscriptions"
                    ))
                }
            }
            
        } catch (e: Exception) {
            // Return error insight
            insights.add(FinancialInsight(
                title = "Analysis Error",
                description = "Unable to generate insights. Please try again.",
                type = FinancialInsight.Type.TREND_ANALYSIS,
                priority = FinancialInsight.Priority.LOW
            ))
        }
        
        // Sort by priority and limit to 5 insights
        return insights
            .sortedBy { insight -> insight.priority }
            .take(5)
    }
    
    private fun createFinancialInsightsPrompt(
        days: Int,
        totalSpending: Double,
        prevSpending: Double,
        categorySpending: List<CategorySpending>,
        transactions: List<com.pennywiseai.tracker.data.Transaction>,
        subscriptions: List<com.pennywiseai.tracker.data.Subscription>
    ): String {
        val topCategories = categorySpending.take(3).joinToString(", ") { 
            "${it.category.name.replace("_", " ")}: ₹${String.format("%.0f", it.total)}" 
        }
        
        val activeSubscriptions = subscriptions.filter { it.active }
        val subscriptionInfo = if (activeSubscriptions.isNotEmpty()) {
            "${activeSubscriptions.size} active subscriptions"
        } else {
            "No active subscriptions"
        }
        
        return """Generate 3 brief financial insights:

Spending: ₹${String.format("%.0f", totalSpending)} (last $days days)
Previous: ₹${String.format("%.0f", prevSpending)}
Top: $topCategories

Return JSON array:
[{"title":"...", "description":"...", "type":"SPENDING_ALERT", "priority":"HIGH"}]

Types: SPENDING_ALERT, SAVING_TIP, SUBSCRIPTION_ALERT
Priority: HIGH, MEDIUM, LOW
Keep descriptions under 50 words."""
    }
    
    private fun parseLLMInsights(response: String): List<FinancialInsight> {
        val insights = mutableListOf<FinancialInsight>()
        
        try {
            // Extract JSON array from response
            val jsonStart = response.indexOf('[')
            val jsonEnd = response.lastIndexOf(']')
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonString = response.substring(jsonStart, jsonEnd + 1)
                val jsonArray = JSONArray(jsonString)
                
                for (i in 0 until jsonArray.length()) {
                    val jsonInsight = jsonArray.getJSONObject(i)
                    
                    val type = try {
                        FinancialInsight.Type.valueOf(jsonInsight.getString("type"))
                    } catch (e: Exception) {
                        FinancialInsight.Type.TREND_ANALYSIS
                    }
                    
                    val priority = try {
                        FinancialInsight.Priority.valueOf(jsonInsight.getString("priority"))
                    } catch (e: Exception) {
                        FinancialInsight.Priority.MEDIUM
                    }
                    
                    insights.add(FinancialInsight(
                        title = jsonInsight.getString("title"),
                        description = jsonInsight.getString("description"),
                        type = type,
                        priority = priority,
                        actionText = jsonInsight.optString("actionText", null),
                        actionQuery = jsonInsight.optString("actionQuery", null)
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to parse LLM insights: ${e.message}")
        }
        
        return insights
    }
}