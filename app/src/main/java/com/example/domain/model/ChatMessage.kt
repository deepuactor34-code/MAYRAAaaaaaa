package com.example.domain.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isToolExecuted: Boolean = false,
    val toolName: String? = null,
    val toolDetail: String? = null
)

enum class MessageSender {
    USER,
    MAYRAA,
    SYSTEM
}
