package com.example.domain.model

data class ActivityLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val actionType: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
)
