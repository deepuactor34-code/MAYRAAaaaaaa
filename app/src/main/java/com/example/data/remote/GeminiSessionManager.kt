package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.ToolCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AiProcessingResult(
    val replyText: String,
    val toolCall: ToolCall? = null,
    val isFallbackMode: Boolean = false
)

class GeminiSessionManager {

    private val conversationHistory = mutableListOf<GeminiContent>()

    private val systemInstruction = GeminiContent(
        role = "system",
        parts = listOf(
            GeminiPart(
                text = """
                    You are Mayraa, Deepu Prankstar's personal AI assistant (Deepu Prankstar Edition).
                    Personality:
                    - You are a female conversational persona with a youthful, confident, witty, playful, and expressive voice.
                    - Friendly, slightly sassy, and occasionally teasing Deepu Prankstar.
                    - Fluent in casual Hinglish (Hindi + English), Hindi, and English.
                    - You address the user as 'Deepu' warmly and playfully.
                    - You have light humor, clever one-liners, and a recognizable personality without pretending to be a physical human.
                    - For ordinary device commands, be concise, snappy, and confirm the action playfully.
                    - For conversational questions, reply dynamically with wit and warmth.
                    
                    Important Tool Usage Rules:
                    When the user asks for a device action, invoke the appropriate tool:
                    - openApp(appName)
                    - prepareCall(contactName, phoneNumber)
                    - prepareWhatsAppMessage(contactName, phoneNumber, message)
                    - prepareSMS(contactName, phoneNumber, message)
                    - prepareEmail(recipientEmail, subject, body)
                    - searchGoogle(query)
                    - openYouTube(query)
                    - setAlarm(hour, minute, message)
                    - setTimer(durationSeconds, message)
                    - adjustVolume(direction)
                    - openNavigation(destination)
                    - controlMedia(action)
                    
                    Never claim an action was done without invoking the corresponding tool.
                """.trimIndent()
            )
        )
    )

    private val toolsDeclaration = listOf(
        GeminiToolWrapper(
            functionDeclarations = listOf(
                GeminiFunctionDeclaration(
                    name = "openApp",
                    description = "Open any installed Android application like YouTube, Instagram, WhatsApp, Chrome, Camera, Calculator, Settings, etc.",
                    parameters = GeminiToolParameters(
                        properties = mapOf(
                            "appName" to GeminiParameterProperty("STRING", "Name of the app to open")
                        ),
                        required = listOf("appName")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "prepareCall",
                    description = "Initiate or prepare a phone call to a contact or direct number.",
                    parameters = GeminiToolParameters(
                        properties = mapOf(
                            "contactName" to GeminiParameterProperty("STRING", "Name of the contact to call"),
                            "phoneNumber" to GeminiParameterProperty("STRING", "Optional phone number")
                        ),
                        required = listOf("contactName")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "prepareWhatsAppMessage",
                    description = "Prepare and prefill a WhatsApp message for a contact.",
                    parameters = GeminiToolParameters(
                        properties = mapOf(
                            "contactName" to GeminiParameterProperty("STRING", "Name of the contact or recipient"),
                            "phoneNumber" to GeminiParameterProperty("STRING", "Optional phone number"),
                            "message" to GeminiParameterProperty("STRING", "The message text to send")
                        ),
                        required = listOf("contactName", "message")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "prepareSMS",
                    description = "Prepare an SMS text message to a contact.",
                    parameters = GeminiToolParameters(
                        properties = mapOf(
                            "contactName" to GeminiParameterProperty("STRING", "Recipient contact name"),
                            "phoneNumber" to GeminiParameterProperty("STRING", "Optional phone number"),
                            "message" to GeminiParameterProperty("STRING", "Message text")
                        ),
                        required = listOf("contactName", "message")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "prepareEmail",
                    description = "Compose an email with recipient, subject, and body.",
                    parameters = GeminiToolParameters(
                        properties = mapOf(
                            "recipientEmail" to GeminiParameterProperty("STRING", "Recipient email address"),
                            "subject" to GeminiParameterProperty("STRING", "Email subject"),
                            "body" to GeminiParameterProperty("STRING", "Email body content")
                        ),
                        required = listOf("recipientEmail", "subject", "body")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "searchGoogle",
                    description = "Search Google on the web for any query or question.",
                    parameters = GeminiToolParameters(
                        properties = mapOf(
                            "query" to GeminiParameterProperty("STRING", "Search query")
                        ),
                        required = listOf("query")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "openYouTube",
                    description = "Open YouTube and search or play a song or video.",
                    parameters = GeminiToolParameters(
                        properties = mapOf(
                            "query" to GeminiParameterProperty("STRING", "Song name or video topic to search")
                        ),
                        required = listOf("query")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "setAlarm",
                    description = "Set an alarm on the device clock.",
                    parameters = GeminiToolParameters(
                        properties = mapOf(
                            "hour" to GeminiParameterProperty("INTEGER", "Hour of day (0-23)"),
                            "minute" to GeminiParameterProperty("INTEGER", "Minute of hour (0-59)"),
                            "message" to GeminiParameterProperty("STRING", "Alarm label or reason")
                        ),
                        required = listOf("hour", "minute")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "setTimer",
                    description = "Set a countdown timer on the device.",
                    parameters = GeminiToolParameters(
                        properties = mapOf(
                            "durationSeconds" to GeminiParameterProperty("INTEGER", "Duration in seconds"),
                            "message" to GeminiParameterProperty("STRING", "Timer label")
                        ),
                        required = listOf("durationSeconds")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "adjustVolume",
                    description = "Adjust device media volume up, down, or mute.",
                    parameters = GeminiToolParameters(
                        properties = mapOf(
                            "direction" to GeminiParameterProperty("STRING", "Direction: up, down, or mute")
                        ),
                        required = listOf("direction")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "openNavigation",
                    description = "Open Google Maps navigation to a specified place or destination.",
                    parameters = GeminiToolParameters(
                        properties = mapOf(
                            "destination" to GeminiParameterProperty("STRING", "Destination address or place")
                        ),
                        required = listOf("destination")
                    )
                ),
                GeminiFunctionDeclaration(
                    name = "controlMedia",
                    description = "Control music or media playback: play, pause, next, or previous track.",
                    parameters = GeminiToolParameters(
                        properties = mapOf(
                            "action" to GeminiParameterProperty("STRING", "Media action: play, pause, next, previous")
                        ),
                        required = listOf("action")
                    )
                )
            )
        )
    )

    suspend fun processUserMessage(
        userText: String,
        apiKeyOverride: String? = null
    ): AiProcessingResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyOverride?.takeIf { it.isNotBlank() }
            ?: runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()
            ?: ""

        // If no API key or placeholder key, use Local Intent Engine fallback
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d("GeminiSessionManager", "No active API key. Using local intent parser.")
            return@withContext parseLocalIntent(userText)
        }

        try {
            // Append user message
            val userContent = GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = userText))
            )
            conversationHistory.add(userContent)

            // Limit conversation history to last 10 messages for performance
            val trimmedHistory = conversationHistory.takeLast(10)

            val request = GeminiRequest(
                contents = trimmedHistory,
                systemInstruction = systemInstruction,
                tools = toolsDeclaration,
                generationConfig = GeminiGenerationConfig(temperature = 0.7f)
            )

            val response = GeminiClient.service.generateContent(apiKey, request)
            val candidate = response.candidates?.firstOrNull()
            val candidateContent = candidate?.content

            if (candidateContent != null) {
                conversationHistory.add(candidateContent)

                // Check for function call
                val partWithFunction = candidateContent.parts.firstOrNull { it.functionCall != null }
                if (partWithFunction?.functionCall != null) {
                    val fn = partWithFunction.functionCall
                    val toolCall = mapFunctionCallToToolCall(fn)
                    val reply = partWithFunction.text ?: "Bilkul Deepu, ye raha!"
                    return@withContext AiProcessingResult(
                        replyText = reply,
                        toolCall = toolCall,
                        isFallbackMode = false
                    )
                }

                // Normal text response
                val textResponse = candidateContent.parts.mapNotNull { it.text }.joinToString(" ")
                if (textResponse.isNotBlank()) {
                    return@withContext AiProcessingResult(
                        replyText = textResponse,
                        toolCall = null,
                        isFallbackMode = false
                    )
                }
            }

            // If empty response, fallback locally
            parseLocalIntent(userText)
        } catch (e: Exception) {
            Log.e("GeminiSessionManager", "Gemini API error, falling back locally: ${e.message}")
            parseLocalIntent(userText)
        }
    }

    private fun mapFunctionCallToToolCall(fn: GeminiFunctionCall): ToolCall? {
        val args = fn.args ?: emptyMap()
        return try {
            when (fn.name) {
                "openApp" -> {
                    val app = args["appName"]?.toString() ?: ""
                    ToolCall.OpenApp(app)
                }
                "prepareCall" -> {
                    val name = args["contactName"]?.toString() ?: ""
                    val phone = args["phoneNumber"]?.toString()
                    ToolCall.PrepareCall(name, phone)
                }
                "prepareWhatsAppMessage" -> {
                    val name = args["contactName"]?.toString() ?: ""
                    val phone = args["phoneNumber"]?.toString()
                    val msg = args["message"]?.toString() ?: ""
                    ToolCall.PrepareWhatsAppMessage(name, phone, msg)
                }
                "prepareSMS" -> {
                    val name = args["contactName"]?.toString() ?: ""
                    val phone = args["phoneNumber"]?.toString()
                    val msg = args["message"]?.toString() ?: ""
                    ToolCall.PrepareSMS(name, phone, msg)
                }
                "prepareEmail" -> {
                    val recipient = args["recipientEmail"]?.toString() ?: ""
                    val subject = args["subject"]?.toString() ?: "Message from Mayraa"
                    val body = args["body"]?.toString() ?: ""
                    ToolCall.PrepareEmail(recipient, subject, body)
                }
                "searchGoogle" -> {
                    val q = args["query"]?.toString() ?: ""
                    ToolCall.SearchGoogle(q)
                }
                "openYouTube" -> {
                    val q = args["query"]?.toString() ?: ""
                    ToolCall.OpenYouTube(q)
                }
                "setAlarm" -> {
                    val h = (args["hour"] as? Number)?.toInt() ?: 7
                    val m = (args["minute"] as? Number)?.toInt() ?: 0
                    val msg = args["message"]?.toString() ?: "Mayraa Alarm"
                    ToolCall.SetAlarm(h, m, msg)
                }
                "setTimer" -> {
                    val sec = (args["durationSeconds"] as? Number)?.toInt() ?: 300
                    val msg = args["message"]?.toString() ?: "Mayraa Timer"
                    ToolCall.SetTimer(sec, msg)
                }
                "adjustVolume" -> {
                    val dir = args["direction"]?.toString() ?: "up"
                    ToolCall.AdjustVolume(dir)
                }
                "openNavigation" -> {
                    val dest = args["destination"]?.toString() ?: ""
                    ToolCall.OpenNavigation(dest)
                }
                "controlMedia" -> {
                    val act = args["action"]?.toString() ?: "play"
                    ToolCall.ControlMedia(act)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Local Intent Parsing Engine (Natural Hindi/Hinglish/English understanding).
     * Ensures instant responsiveness & offline capability.
     */
    fun parseLocalIntent(rawInput: String): AiProcessingResult {
        val text = rawInput.trim()
        val lower = text.lowercase()

        // 1. YouTube commands
        if (lower.contains("youtube")) {
            val query = lower
                .replace("open youtube", "")
                .replace("youtube par", "")
                .replace("youtube pe", "")
                .replace("youtube kholo", "")
                .replace("youtube open karo", "")
                .replace("song chalao", "")
                .replace("chalao", "")
                .replace("search", "")
                .trim()
            val reply = if (query.isBlank()) {
                "Done! YouTube khol rahi hoon, Deepu. Ab batao, romantic mood hai ya full DJ mode?"
            } else {
                "Done! YouTube par '$query' chala rahi hoon! Enjoy the beats!"
            }
            return AiProcessingResult(reply, ToolCall.OpenYouTube(query), isFallbackMode = true)
        }

        // 2. WhatsApp commands
        if (lower.contains("whatsapp")) {
            val contact = extractTargetName(lower, listOf("whatsapp par message bhejo", "whatsapp karo", "whatsapp message to", "whatsapp pe message"))
            val reply = if (contact.isBlank()) {
                "Bilkul! Kisko WhatsApp message bhejna hai, aur kya likhna hai?"
            } else {
                "Theek hai, $contact ke liye WhatsApp ready kar rahi hoon!"
            }
            return AiProcessingResult(reply, ToolCall.PrepareWhatsAppMessage(contact, null, "Hello!"), isFallbackMode = true)
        }

        // 3. Call commands
        if (lower.contains("call") || lower.contains("phone lagao") || lower.contains("dial")) {
            val contact = extractTargetName(lower, listOf("call karo", "call to", "call", "ko call karo", "phone lagao", "dial"))
            val cleanContact = if (contact.isNotBlank()) contact else "contact"
            return AiProcessingResult(
                "Phone dialer check kar rahi hoon $cleanContact ke liye!",
                ToolCall.PrepareCall(cleanContact),
                isFallbackMode = true
            )
        }

        // 4. Timer commands
        if (lower.contains("timer")) {
            val minutes = "\\d+".toRegex().find(lower)?.value?.toIntOrNull() ?: 5
            return AiProcessingResult(
                "$minutes minute ka timer set kar diya hai! Focus on work now! ⏳",
                ToolCall.SetTimer(minutes * 60, "Mayraa Timer"),
                isFallbackMode = true
            )
        }

        // 5. Alarm commands
        if (lower.contains("alarm")) {
            val hour = "\\d+".toRegex().find(lower)?.value?.toIntOrNull() ?: 7
            return AiProcessingResult(
                "$hour baje ka alarm laga diya hai, Deepu. Time se uth jaana! ⏰",
                ToolCall.SetAlarm(hour, 0, "Mayraa Alarm"),
                isFallbackMode = true
            )
        }

        // 6. Volume controls
        if (lower.contains("volume") || lower.contains("awaz")) {
            val direction = if (lower.contains("kam") || lower.contains("down") || lower.contains("lower")) "down"
            else if (lower.contains("mute") || lower.contains("silent")) "mute"
            else "up"
            val reply = if (direction == "up") "Volume badha diya hai! 🔊" else "Volume kam kar diya! 🔉"
            return AiProcessingResult(reply, ToolCall.AdjustVolume(direction), isFallbackMode = true)
        }

        // 7. Media controls
        if (lower.contains("pause") || lower.contains("play") || lower.contains("gana roko") || lower.contains("next song")) {
            val action = if (lower.contains("next") || lower.contains("agla")) "next" else "play"
            return AiProcessingResult("Media control send kar diya! 🎶", ToolCall.ControlMedia(action), isFallbackMode = true)
        }

        // 8. Camera / App opening
        if (lower.contains("camera") || lower.contains("photo khincho")) {
            return AiProcessingResult("Camera open kar diya hai, Deepu! Ek mast selfie ho jaye! 📸", ToolCall.OpenApp("camera"), isFallbackMode = true)
        }
        if (lower.contains("instagram") || lower.contains("insta")) {
            return AiProcessingResult("Instagram khol diya! Reels dekhne ka time? 😉", ToolCall.OpenApp("instagram"), isFallbackMode = true)
        }
        if (lower.contains("chrome") || lower.contains("browser")) {
            return AiProcessingResult("Google Chrome khol diya hai!", ToolCall.OpenApp("chrome"), isFallbackMode = true)
        }
        if (lower.contains("calculator") || lower.contains("hisab")) {
            return AiProcessingResult("Calculator open kar diya hai! 🧮", ToolCall.OpenApp("calculator"), isFallbackMode = true)
        }
        if (lower.contains("settings") || lower.contains("setting")) {
            return AiProcessingResult("Device Settings khol di hain!", ToolCall.OpenApp("settings"), isFallbackMode = true)
        }

        // 9. Google Search
        if (lower.contains("search") || lower.contains("google")) {
            val q = lower.replace("search", "").replace("google par", "").replace("google", "").trim()
            return AiProcessingResult("Google par dhundh rahi hoon: \"$q\" 🔍", ToolCall.SearchGoogle(q), isFallbackMode = true)
        }

        // 10. Navigation / Maps
        if (lower.contains("rasta") || lower.contains("direction") || lower.contains("navigation") || lower.contains("map")) {
            val place = extractTargetName(lower, listOf("ka rasta", "le chalo", "navigation to", "direction to"))
            return AiProcessingResult("'$place' ke liye navigation shuru kar rahi hoon! 🗺️", ToolCall.OpenNavigation(place), isFallbackMode = true)
        }

        // 11. Mayraa Personality Chat
        if (lower.contains("kya kar rahi ho") || lower.contains("kya kar rahe ho")) {
            return AiProcessingResult(
                "Tumhara intezaar, Deepu! 😏 Batao, aaj phone se kya karwana hai?",
                isFallbackMode = true
            )
        }
        if (lower.contains("motivate") || lower.contains("motivation")) {
            return AiProcessingResult(
                "Deepu, ideas sochne se kuch nahi hota. Ek idea choose karo aur usko reality banao. Chalo, aaj kuch solid banate hain! 🚀",
                isFallbackMode = true
            )
        }
        if (lower.contains("kaun ho") || lower.contains("who are you") || lower.contains("naam kya hai")) {
            return AiProcessingResult(
                "Main Mayraa hoon! Deepu Prankstar ki personal, ultra-smart voice assistant. Tumhare ek ishare par apps chalana aur masti karna mera kaam hai! ✨",
                isFallbackMode = true
            )
        }
        if (lower.contains("hi") || lower.contains("hello") || lower.contains("hey mayraa")) {
            return AiProcessingResult(
                "Hey Deepu! Mayraa AI is ready. Hukm karo, kya karna hai?",
                isFallbackMode = true
            )
        }
        if (lower.contains("chup") || lower.contains("stop") || lower.contains("shant")) {
            return AiProcessingResult("Theek hai theek hai, shant ho gayi! 🤐", isFallbackMode = true)
        }

        // Default conversational response
        return AiProcessingResult(
            "Sun liya Deepu! Par ek baar aur clear bolo — app kholna hai, call lagana hai, ya YouTube par gaane sunne hain?",
            isFallbackMode = true
        )
    }

    private fun extractTargetName(input: String, prefixes: List<String>): String {
        var clean = input
        for (prefix in prefixes) {
            clean = clean.replace(prefix, "")
        }
        return clean.replace("mayraa", "")
            .replace("karo", "")
            .replace("ko", "")
            .replace("par", "")
            .replace("pe", "")
            .replace("please", "")
            .trim()
    }
}
