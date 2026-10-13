package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.WakeWordDetector

class MayraaVoiceService : Service() {

    private var wakeWordDetector: WakeWordDetector? = null

    companion object {
        const val CHANNEL_ID = "mayraa_voice_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_WAKE_WORD_TRIGGERED = "com.example.service.WAKE_WORD_TRIGGERED"

        fun startService(context: Context) {
            val intent = Intent(context, MayraaVoiceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MayraaVoiceService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startInForeground()

        wakeWordDetector = WakeWordDetector(this) {
            // Wake word detected! Launch MainActivity to listen
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                action = ACTION_WAKE_WORD_TRIGGERED
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(launchIntent)
        }
        wakeWordDetector?.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startInForeground() {
        val stopIntent = Intent(this, MayraaVoiceService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = Intent(this, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mayraa AI — Deepu Prankstar Edition")
            .setContentText("Hands-free wake-word active. Say 'Mayraa' to speak.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(appPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Listening", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mayraa Voice Wake-Word",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous listening for 'Mayraa' hands-free activation"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        wakeWordDetector?.stop()
        wakeWordDetector = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
