package com.pennywiseai.tracker.ui.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.database.entity.ChatMessage
import com.pennywiseai.tracker.data.repository.LlmRepository
import com.pennywiseai.tracker.data.repository.ModelRepository
import com.pennywiseai.tracker.data.repository.ModelState
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import com.pennywiseai.tracker.utils.TokenUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val llmRepository: LlmRepository,
    private val modelRepository: ModelRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    val messages: StateFlow<List<ChatMessage>> = llmRepository.getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val modelState: StateFlow<ModelState> = modelRepository.modelState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = if (modelRepository.isModelDownloaded()) ModelState.READY else ModelState.NOT_DOWNLOADED
        )
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private val _currentResponse = MutableStateFlow("")
    val currentResponse: StateFlow<String> = _currentResponse.asStateFlow()
    
    val isDeveloperModeEnabled = userPreferencesRepository.isDeveloperModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    // Get all messages including system for accurate token count
    private val allMessagesIncludingSystem = llmRepository.getAllMessagesIncludingSystem()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Chat statistics for developer mode
    val chatStats = allMessagesIncludingSystem.combine(currentResponse) { allMsgs, current ->
        // Calculate system prompt tokens separately
        val systemPromptText = allMsgs.filter { it.isSystemPrompt }.joinToString(" ") { it.message }
        val systemPromptTokens = if (systemPromptText.isNotEmpty()) {
            TokenUtils.estimateTokens(systemPromptText)
        } else {
            0
        }
        
        // Calculate total tokens
        val allText = allMsgs.joinToString(" ") { it.message } + " " + current
        val totalChars = allText.length
        val estimatedTokens = TokenUtils.estimateTokens(allText)
        val maxTokens = 8192 // Gemma 2B context window
        
        // Count only visible messages for UI
        val visibleCount = allMsgs.count { !it.isSystemPrompt }
        
        ChatStats(
            messageCount = visibleCount,
            totalCharacters = totalChars,
            estimatedTokens = estimatedTokens,
            systemPromptTokens = systemPromptTokens,
            maxTokens = maxTokens,
            contextUsagePercent = TokenUtils.calculateContextUsagePercent(estimatedTokens, maxTokens)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatStats()
    )
    
    init {
        Log.d("ChatViewModel", "Initializing ChatViewModel")
        modelRepository.checkModelState()
        
        // Log initial state
        val isDownloaded = modelRepository.isModelDownloaded()
        Log.d("ChatViewModel", "Initial model downloaded check: $isDownloaded")
        
        // Also observe state changes
        viewModelScope.launch {
            modelRepository.modelState.collect { state ->
                Log.d("ChatViewModel", "Model state changed to: $state")
            }
        }
    }
    
    fun sendMessage(message: String) {
        if (message.isBlank() || _uiState.value.isLoading) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            _currentResponse.value = ""
            
            try {
                // Use streaming for better UX
                llmRepository.sendMessageStream(message)
                    .catch { error ->
                        Log.e("ChatViewModel", "Error in stream", error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                    .collect { partialResponse ->
                        _currentResponse.value += partialResponse
                    }
                
                // Stream completed successfully
                Log.d("ChatViewModel", "Stream completed, resetting state")
                _uiState.value = _uiState.value.copy(isLoading = false)
                _currentResponse.value = ""
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Exception in sendMessage", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
                _currentResponse.value = ""
            }
        }
    }
    
    fun clearChat() {
        viewModelScope.launch {
            llmRepository.deleteAllMessages()
            _uiState.value = _uiState.value.copy(
                error = null
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ChatStats(
    val messageCount: Int = 0,
    val totalCharacters: Int = 0,
    val estimatedTokens: Int = 0,
    val systemPromptTokens: Int = 0,
    val maxTokens: Int = 8192,
    val contextUsagePercent: Int = 0
)