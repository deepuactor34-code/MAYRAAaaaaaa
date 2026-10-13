# Mayraa AI — Personal Voice Assistant (Deepu Prankstar Edition)

[![GitHub Repository](https://img.shields.io/badge/GitHub-deepuactor34--code%2FMayraa.Ai-blue?logo=github)](https://github.com/deepuactor34-code/Mayraa.Ai)
[![Android](https://img.shields.io/badge/Platform-Android%2014+-green?logo=android)](https://github.com/deepuactor34-code/Mayraa.Ai)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%20Compose-purple?logo=kotlin)](https://github.com/deepuactor34-code/Mayraa.Ai)

A native Android voice assistant application developed and branded by **Deepu Prankstar**. Mayraa combines real-time voice conversations with genuine native Android device control, wrapped in a futuristic cyberpunk interface.

**Official Repository:** [https://github.com/deepuactor34-code/Mayraa.Ai](https://github.com/deepuactor34-code/Mayraa.Ai)

---

## 🌟 Key Features

1. **Futuristic Neon Cyberpunk UI**
   - Deep obsidian background (`#070A12`) with glowing neon cyan, electric violet, and neon blue accents.
   - Central animated 5-state reactive Orb (Idle breathing glow, Listening sound wave ripple, Thinking gradient pulse, Speaking voice visualizer, and Error alert).
   - Audio amplitude reactivity that dynamically expands and ripples with microphone input.

2. **Distinctive Mayraa Persona**
   - Youthful, witty, playful, and expressive female voice persona.
   - Natural Hinglish, Hindi, and English understanding.
   - Addresses Deepu playfully with light humor and sharp one-liners.
   - Examples:
     - *"Mayraa, kya kar rahi ho?"* → *"Tumhara intezaar, Deepu! 😏 Batao, aaj phone se kya karwana hai?"*
     - *"YouTube par song chalao."* → *"Done! YouTube khol rahi hoon. Ab batao, romantic mood hai ya full DJ mode?"*
     - *"Mayraa, mujhe motivate karo."* → *"Deepu, ideas sochne se kuch nahi hota. Ek idea choose karo aur usko reality banao. Chalo, aaj kuch solid banate hain! 🚀"*

3. **Native Android Device Control (`ToolExecutionEngine`)**
   - **Open Applications**: YouTube, WhatsApp, Instagram, Chrome, Camera, Calculator, Settings, Maps, Gallery, etc.
   - **Phone Calls & Contacts**: Resolves contacts from Android Contacts Provider, with interactive contact disambiguation when multiple matches exist.
   - **WhatsApp Messaging**: Prefills and opens WhatsApp chats directly with selected contacts.
   - **SMS & Email**: Composes and prepares SMS messages and emails with zero manual typing.
   - **Alarms & Timers**: Sets system clock alarms and countdown timers hands-free.
   - **Volume & Media**: Adjusts media volume up, down, or mute; media playback controls.
   - **Google Maps Navigation**: Launches navigation to requested destinations.
   - **Web & YouTube Search**: Searches videos and queries on Google and YouTube.

4. **Speech & Audio Engine**
   - Android `SpeechRecognizer` for live voice input with RMS audio level tracking.
   - Android `TextToSpeech` with adjustable pitch, speed, and language voices.
   - Audio interruption handling: speaking automatically stops whenever you interrupt or tap the orb.
   - Hands-free wake-word detection for **"Mayraa"** with background foreground service.

5. **AI Integration with Dual-Engine Resilience**
   - **Gemini Live AI**: Direct REST integration with function calling.
   - **Local Intent Engine Fallback**: If offline or if the API key is not yet configured, Mayraa's local intent understanding kicks in instantly so all Android device commands continue working 100% reliably.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Asynchronous**: Kotlin Coroutines & Flow
- **Data Persistence**: Jetpack DataStore Preferences
- **Networking**: Retrofit 2 + Moshi + OkHttp
- **Testing**: JUnit 4 + Robolectric (JVM-based testing)

---

## 🚀 How to Build and Run

### 1. Open the Project
1. Open **Android Studio** (Koala or newer) or **AndroidIDE** on Android.
2. Select **File > Open** and choose this project root folder.
3. Allow Gradle to sync.

### 2. Configure Gemini API Key
- In Google AI Studio: Open the **Secrets panel** and configure `GEMINI_API_KEY`.
- Or in `.env` / in the app's **Settings > Gemini API Key**: Paste your Gemini API key.
- Note: Even without an API key, Mayraa operates seamlessly via her built-in Local Intent Engine!

### 3. Build the Debug APK
In Android Studio:
- Click **Build > Build Bundle(s) / APK(s) > Build APK(s)**
- Or via terminal:
  ```bash
  gradle :app:assembleDebug
  ```

### 4. Locate the Generated APK
The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 5. Install and Run on Android
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🛡️ Permissions Onboarding

When you first open Mayraa AI:
1. **Microphone (`RECORD_AUDIO`)**: Granted for voice input and orb reactivity.
2. **Contacts (`READ_CONTACTS`)**: Granted for contact search when placing calls or sending WhatsApp messages.
3. **Notifications (`POST_NOTIFICATIONS`)**: Required for the background wake-word service on Android 13+.
4. **Phone (`CALL_PHONE`)**: Optional for direct calling.

---

## 📱 Developer Credits
- **Application**: Mayraa AI (Personal Voice Assistant)
- **Developer**: Deepu Prankstar
- **Edition**: Deepu Prankstar Edition
- **Package**: `com.aistudio.mayraaai.deepu`
