package com.pennywiseai.tracker.data.repository

import android.util.Log
import com.pennywiseai.tracker.data.database.dao.ChatDao
import com.pennywiseai.tracker.data.database.entity.ChatMessage
import com.pennywiseai.tracker.domain.service.LlmService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LlmRepository @Inject constructor(
    private val llmService: LlmService,
    private val chatDao: ChatDao,
    private val modelRepository: ModelRepository
) {
    private var currentSessionId: String = UUID.randomUUID().toString()
    
    fun getAllMessages(): Flow<List<ChatMessage>> = chatDao.getAllMessages()
    
    fun getSessionMessages(sessionId: String): Flow<List<ChatMessage>> = 
        chatDao.getMessagesBySession(sessionId)
    
    fun getCurrentSessionMessages(): Flow<List<ChatMessage>> = 
        chatDao.getMessagesBySession(currentSessionId)
    
    suspend fun startNewSession() {
        currentSessionId = UUID.randomUUID().toString()
    }
    
    suspend fun sendMessage(userMessage: String): Result<String> {
        // Save user message
        val userChatMessage = ChatMessage(
            message = userMessage,
            isUser = true,
            sessionId = currentSessionId
        )
        chatDao.insertMessage(userChatMessage)
        
        // Initialize LLM if needed
        if (!llmService.isInitialized()) {
            val modelFile = modelRepository.getModelFile()
            if (!modelFile.exists()) {
                return Result.failure(Exception("Model not downloaded"))
            }
            
            val initResult = llmService.initialize(modelFile.absolutePath)
            if (initResult.isFailure) {
                return Result.failure(initResult.exceptionOrNull() ?: Exception("Failed to initialize LLM"))
            }
        }
        
        // Generate response
        val responseResult = llmService.generateResponse(userMessage)
        
        return if (responseResult.isSuccess) {
            val response = responseResult.getOrNull() ?: ""
            
            // Save AI response
            val aiChatMessage = ChatMessage(
                message = response,
                isUser = false,
                sessionId = currentSessionId
            )
            chatDao.insertMessage(aiChatMessage)
            
            Result.success(response)
        } else {
            Result.failure(responseResult.exceptionOrNull() ?: Exception("Failed to generate response"))
        }
    }
    
    fun sendMessageStream(userMessage: String): Flow<String> = flow {
        // Save user message
        val userChatMessage = ChatMessage(
            message = userMessage,
            isUser = true
        )
        Log.d("LlmRepository", "Saving user message: ${userMessage.take(50)}...")
        chatDao.insertMessage(userChatMessage)
        Log.d("LlmRepository", "User message saved")
        
        // Initialize LLM if needed
        if (!llmService.isInitialized()) {
            val modelFile = modelRepository.getModelFile()
            if (!modelFile.exists()) {
                throw Exception("Model not downloaded")
            }
            
            val initResult = llmService.initialize(modelFile.absolutePath)
            if (initResult.isFailure) {
                throw initResult.exceptionOrNull() ?: Exception("Failed to initialize LLM")
            }
        }
        
        // Get conversation history
        val recentMessages = chatDao.getRecentMessages(10) // Get last 10 messages
        val conversationContext = buildConversationContext(recentMessages, userMessage)
        
        // Stream response and accumulate for saving
        val responseBuilder = StringBuilder()
        var messageInserted = false
        
        llmService.generateResponseStream(conversationContext)
            .collect { partialResponse ->
                responseBuilder.append(partialResponse)
                emit(partialResponse)
            }
        
        // Save the complete AI response at the end
        val finalResponse = responseBuilder.toString()
        Log.d("LlmRepository", "Saving AI response: ${finalResponse.take(50)}...")
        val aiMessage = ChatMessage(
            message = finalResponse,
            isUser = false
        )
        chatDao.insertMessage(aiMessage)
        Log.d("LlmRepository", "AI response saved")
    }
    
    suspend fun deleteAllMessages() {
        chatDao.deleteAllMessages()
    }
    
    suspend fun deleteOldMessages(beforeTimestamp: Long) {
        chatDao.deleteMessagesBefore(beforeTimestamp)
    }
    
    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteSession(sessionId)
    }
    
    suspend fun getAllSessions(): List<String> = chatDao.getAllSessions()
    
    suspend fun getMessageCount(): Int = chatDao.getMessageCount()
    
    private suspend fun buildConversationContext(history: List<ChatMessage>, currentMessage: String): String {
        val contextBuilder = StringBuilder()
        
        // Add conversation history (reverse since we got them in DESC order)
        if (history.isNotEmpty()) {
            contextBuilder.append("Previous conversation:\n")
            history.reversed().forEach { msg ->
                val role = if (msg.isUser) "User" else "Assistant"
                contextBuilder.append("$role: ${msg.message}\n")
            }
            contextBuilder.append("\n")
        }
        
        // Add current message
        contextBuilder.append("User: $currentMessage\n")
        contextBuilder.append("Assistant:")
        
        return contextBuilder.toString()
    }
}