package com.example.data.repository

import com.example.domain.model.ActivityLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ActivityLogRepository {

    private val _logs = MutableStateFlow<List<ActivityLog>>(
        listOf(
            ActivityLog(
                actionType = "System Initialized",
                description = "Mayraa AI system online. Deepu Prankstar Edition ready for voice commands.",
                isSuccess = true
            )
        )
    )
    val logs: StateFlow<List<ActivityLog>> = _logs.asStateFlow()

    fun logAction(actionType: String, description: String, isSuccess: Boolean = true) {
        val newEntry = ActivityLog(
            actionType = actionType,
            description = description,
            isSuccess = isSuccess
        )
        val updated = listOf(newEntry) + _logs.value
        _logs.value = updated.take(50) // keep latest 50
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}
