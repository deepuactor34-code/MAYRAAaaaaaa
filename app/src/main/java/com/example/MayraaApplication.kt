package com.example

import android.app.Application
import com.example.data.repository.ActivityLogRepository
import com.example.data.repository.ContactRepository
import com.example.data.repository.SettingsRepository
import com.example.data.remote.GeminiSessionManager
import com.example.domain.tools.ToolExecutionEngine

class MayraaApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var contactRepository: ContactRepository
        private set

    lateinit var activityLogRepository: ActivityLogRepository
        private set

    lateinit var toolExecutionEngine: ToolExecutionEngine
        private set

    lateinit var geminiSessionManager: GeminiSessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        contactRepository = ContactRepository(this)
        activityLogRepository = ActivityLogRepository()
        toolExecutionEngine = ToolExecutionEngine(this, contactRepository)
        geminiSessionManager = GeminiSessionManager()
    }
}
