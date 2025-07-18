package com.pennywiseai.tracker.data

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val type: ChatMessageType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ChatMessageType {
    USER,
    AI,
    TYPING_INDICATOR
}