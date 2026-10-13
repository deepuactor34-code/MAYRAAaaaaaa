package com.example.domain.tools

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.view.KeyEvent
import com.example.data.repository.ContactRepository
import com.example.domain.model.ContactInfo
import com.example.domain.model.ToolCall
import com.example.domain.model.ToolExecutionResult
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ToolExecutionEngine(
    private val context: Context,
    private val contactRepository: ContactRepository
) {

    // Common App Package mappings
    private val knownAppPackages = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "chrome" to "com.android.chrome",
        "camera" to "com.android.camera",
        "calculator" to "com.google.android.calculator",
        "settings" to "com.android.settings",
        "maps" to "com.google.android.apps.maps",
        "gmail" to "com.google.android.gm",
        "clock" to "com.google.android.deskclock",
        "gallery" to "com.google.android.apps.photos",
        "spotify" to "com.spotify.music",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "facebook" to "com.facebook.katana",
        "telegram" to "org.telegram.messenger",
        "snapchat" to "com.snapchat.android",
        "files" to "com.google.android.documentsui",
        "play store" to "com.android.vending"
    )

    fun execute(toolCall: ToolCall): ToolExecutionResult {
        return try {
            when (toolCall) {
                is ToolCall.OpenApp -> handleOpenApp(toolCall.appName)
                is ToolCall.PrepareCall -> handlePrepareCall(toolCall.contactName, toolCall.phoneNumber)
                is ToolCall.PrepareWhatsAppMessage -> handlePrepareWhatsApp(toolCall.contactName, toolCall.phoneNumber, toolCall.message)
                is ToolCall.PrepareSMS -> handlePrepareSMS(toolCall.contactName, toolCall.phoneNumber, toolCall.message)
                is ToolCall.PrepareEmail -> handlePrepareEmail(toolCall.recipientEmail, toolCall.subject, toolCall.body)
                is ToolCall.SearchGoogle -> handleSearchGoogle(toolCall.query)
                is ToolCall.OpenYouTube -> handleOpenYouTube(toolCall.query)
                is ToolCall.SetAlarm -> handleSetAlarm(toolCall.hour, toolCall.minute, toolCall.message)
                is ToolCall.SetTimer -> handleSetTimer(toolCall.durationSeconds, toolCall.message)
                is ToolCall.AdjustVolume -> handleAdjustVolume(toolCall.direction)
                is ToolCall.OpenNavigation -> handleOpenNavigation(toolCall.destination)
                is ToolCall.ControlMedia -> handleControlMedia(toolCall.action)
            }
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                feedbackMessage = "Oops! Action execute karne mein issue aaya: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun handleOpenApp(appName: String): ToolExecutionResult {
        val normalized = appName.trim().lowercase()

        // 1. Check direct actions for camera/dialer
        if (normalized.contains("camera") || normalized == "cam") {
            val cameraIntent = Intent("android.media.action.IMAGE_CAPTURE").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (cameraIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(cameraIntent)
                return ToolExecutionResult(true, "Camera khol diya hai, Deepu! Smile karo! 📸")
            }
        }

        if (normalized.contains("dialer") || normalized.contains("phone")) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            return ToolExecutionResult(true, "Phone dialer khol diya hai! 📞")
        }

        if (normalized.contains("setting")) {
            val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            return ToolExecutionResult(true, "Settings khol di hain!")
        }

        // 2. Check known packages
        val targetPackage = knownAppPackages.entries.firstOrNull { normalized.contains(it.key) }?.value

        val pm = context.packageManager
        if (targetPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ToolExecutionResult(true, "$appName open kar diya hai! ✨")
            }
        }

        // 3. Search launchable apps
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val appList = pm.queryIntentActivities(mainIntent, 0)
        for (info in appList) {
            val label = info.loadLabel(pm).toString().lowercase()
            if (label.contains(normalized) || normalized.contains(label)) {
                val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ToolExecutionResult(true, "${info.loadLabel(pm)} open kar rahi hoon!")
                }
            }
        }

        // App not found
        return ToolExecutionResult(
            success = false,
            feedbackMessage = "$appName phone mein installed nahi mila. Kya Google Play par dhundhoon?"
        )
    }

    private fun handlePrepareCall(contactName: String, phoneNumber: String?): ToolExecutionResult {
        // Direct number provided
        if (!phoneNumber.isNullOrBlank()) {
            val cleanNumber = phoneNumber.replace("[^0-9+]".toRegex(), "")
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            return ToolExecutionResult(true, "$cleanNumber par call lagane ke liye dialer khol diya hai!")
        }

        // Search in Contacts
        if (!contactRepository.hasContactsPermission()) {
            return ToolExecutionResult(
                success = false,
                feedbackMessage = "Contacts read karne ki permission nahi hai. Please settings se allow karo!"
            )
        }

        val matches = contactRepository.searchContacts(contactName)
        return when {
            matches.isEmpty() -> {
                ToolExecutionResult(
                    success = false,
                    feedbackMessage = "'$contactName' naam se koi contact nahi mila. Naam theek se batao?"
                )
            }
            matches.size == 1 -> {
                val contact = matches.first()
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                ToolExecutionResult(
                    success = true,
                    feedbackMessage = "${contact.name} (${contact.phoneNumber}) ke liye dialer open kar diya hai!"
                )
            }
            else -> {
                // Multiple contacts matched: Disambiguation required
                ToolExecutionResult(
                    success = true,
                    feedbackMessage = "'$contactName' naam ke ${matches.size} contacts mile. Kisko call lagana hai?",
                    requiresDisambiguation = true,
                    candidateContacts = matches,
                    pendingAction = ToolCall.PrepareCall(contactName)
                )
            }
        }
    }

    private fun handlePrepareWhatsApp(contactName: String, phoneNumber: String?, message: String): ToolExecutionResult {
        val pm = context.packageManager
        val isWhatsAppInstalled = try {
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: Exception) {
            false
        }

        if (!isWhatsAppInstalled) {
            return ToolExecutionResult(
                success = false,
                feedbackMessage = "Phone mein WhatsApp installed nahi mila, Deepu!"
            )
        }

        var resolvedNumber = phoneNumber
        if (resolvedNumber.isNullOrBlank() && contactName.isNotBlank()) {
            if (!contactRepository.hasContactsPermission()) {
                return ToolExecutionResult(
                    success = false,
                    feedbackMessage = "Contacts permission required hai contact dhundhne ke liye!"
                )
            }
            val matches = contactRepository.searchContacts(contactName)
            when {
                matches.isEmpty() -> {
                    return ToolExecutionResult(
                        success = false,
                        feedbackMessage = "'$contactName' contact list mein nahi mila."
                    )
                }
                matches.size > 1 -> {
                    return ToolExecutionResult(
                        success = true,
                        feedbackMessage = "Multiple contacts mile. Kisko WhatsApp message bhejna hai?",
                        requiresDisambiguation = true,
                        candidateContacts = matches,
                        pendingAction = ToolCall.PrepareWhatsAppMessage(contactName, null, message)
                    )
                }
                else -> {
                    resolvedNumber = matches.first().phoneNumber
                }
            }
        }

        val encodedText = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
        val intent = if (!resolvedNumber.isNullOrBlank()) {
            var formatted = resolvedNumber.replace("[^0-9+]".toRegex(), "")
            if (!formatted.startsWith("+") && formatted.length == 10) {
                formatted = "91$formatted" // Default India country code for 10-digit numbers
            } else if (formatted.startsWith("+")) {
                formatted = formatted.substring(1)
            }
            Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$formatted&text=$encodedText"))
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
        }

        intent.setPackage("com.whatsapp")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
        return ToolExecutionResult(
            success = true,
            feedbackMessage = "WhatsApp khol diya hai! Message review karke send daba do: \"$message\""
        )
    }

    private fun handlePrepareSMS(contactName: String, phoneNumber: String?, message: String): ToolExecutionResult {
        var targetNumber = phoneNumber
        if (targetNumber.isNullOrBlank() && contactName.isNotBlank() && contactRepository.hasContactsPermission()) {
            val matches = contactRepository.searchContacts(contactName)
            if (matches.size == 1) {
                targetNumber = matches.first().phoneNumber
            } else if (matches.size > 1) {
                return ToolExecutionResult(
                    success = true,
                    feedbackMessage = "Kisko SMS bhejna hai choose karo:",
                    requiresDisambiguation = true,
                    candidateContacts = matches,
                    pendingAction = ToolCall.PrepareSMS(contactName, null, message)
                )
            }
        }

        val uri = if (!targetNumber.isNullOrBlank()) Uri.parse("smsto:$targetNumber") else Uri.parse("smsto:")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ToolExecutionResult(true, "SMS pre-fill karke messages app open kar diya hai!")
    }

    private fun handlePrepareEmail(recipientEmail: String, subject: String, body: String): ToolExecutionResult {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${if (recipientEmail.isNotBlank()) recipientEmail else ""}")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Email app choose karo").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return ToolExecutionResult(true, "Email draft tayar hai! Review karke send kar do.")
    }

    private fun handleSearchGoogle(query: String): ToolExecutionResult {
        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (searchIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(searchIntent)
        } else {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        }
        return ToolExecutionResult(true, "Google par search kar diya: \"$query\" 🔍")
    }

    private fun handleOpenYouTube(query: String): ToolExecutionResult {
        val intent = if (query.isBlank()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            launchIntent ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
        } else {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val ytAppIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
            }
            if (ytAppIntent.resolveActivity(context.packageManager) != null) {
                ytAppIntent
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded"))
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        val msg = if (query.isBlank()) "YouTube khol diya hai!" else "YouTube par '$query' chala diya!"
        return ToolExecutionResult(true, msg)
    }

    private fun handleSetAlarm(hour: Int, minute: Int, message: String): ToolExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        val formattedTime = String.format("%02d:%02d", hour, minute)
        return ToolExecutionResult(true, "$formattedTime baje ka alarm set kar diya hai, Deepu! ⏰")
    }

    private fun handleSetTimer(durationSeconds: Int, message: String): ToolExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        val minutes = durationSeconds / 60
        val secs = durationSeconds % 60
        val timeStr = if (minutes > 0) "$minutes minute" else "$secs seconds"
        return ToolExecutionResult(true, "$timeStr ka timer chalu kar diya hai! ⏳")
    }

    private fun handleAdjustVolume(direction: String): ToolExecutionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (direction.lowercase()) {
            "up", "raise", "increase" -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                return ToolExecutionResult(true, "Volume badha diya hai! 🔊")
            }
            "down", "lower", "decrease" -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                return ToolExecutionResult(true, "Volume kam kar diya hai! 🔉")
            }
            "mute" -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )
                return ToolExecutionResult(true, "Mute kar diya hai! 🔇")
            }
            else -> {
                return ToolExecutionResult(false, "Volume command samajh nahi aayi.")
            }
        }
    }

    private fun handleOpenNavigation(destination: String): ToolExecutionResult {
        val encoded = URLEncoder.encode(destination, "UTF-8")
        val gmmIntentUri = Uri.parse("google.navigation:q=$encoded")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(geoIntent)
        }
        return ToolExecutionResult(true, "'$destination' ke liye navigation khol diya hai! 🗺️")
    }

    private fun handleControlMedia(action: String): ToolExecutionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val keyCode = when (action.lowercase()) {
            "play", "pause", "toggle" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "prev", "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }

        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return ToolExecutionResult(true, "Media control sent: $action! 🎵")
    }
}
