package com.example.domain.model

sealed class ToolCall {
    data class OpenApp(val appName: String) : ToolCall()
    data class PrepareCall(val contactName: String, val phoneNumber: String? = null) : ToolCall()
    data class PrepareWhatsAppMessage(val contactName: String, val phoneNumber: String? = null, val message: String) : ToolCall()
    data class PrepareSMS(val contactName: String, val phoneNumber: String? = null, val message: String) : ToolCall()
    data class PrepareEmail(val recipientEmail: String, val subject: String, val body: String) : ToolCall()
    data class SearchGoogle(val query: String) : ToolCall()
    data class OpenYouTube(val query: String) : ToolCall()
    data class SetAlarm(val hour: Int, val minute: Int, val message: String = "Mayraa Alarm") : ToolCall()
    data class SetTimer(val durationSeconds: Int, val message: String = "Mayraa Timer") : ToolCall()
    data class AdjustVolume(val direction: String) : ToolCall() // "up", "down", "mute"
    data class OpenNavigation(val destination: String) : ToolCall()
    data class ControlMedia(val action: String) : ToolCall() // "play", "pause", "next", "previous"
}

data class ToolExecutionResult(
    val success: Boolean,
    val feedbackMessage: String,
    val requiresDisambiguation: Boolean = false,
    val candidateContacts: List<ContactInfo> = emptyList(),
    val pendingAction: ToolCall? = null
)
