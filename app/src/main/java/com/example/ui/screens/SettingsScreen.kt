package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TtsVoiceInfo
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val userSettings by viewModel.userSettings.collectAsState()
    val availableVoices by viewModel.availableTtsVoices.collectAsState()

    var apiKeyText by remember { mutableStateOf(userSettings.apiKeyOverride) }
    var apiKeySavedFeedback by remember { mutableStateOf(false) }
    var showVoiceSelectorDialog by remember { mutableStateOf(false) }

    // TTS Voice Selector Dialog
    if (showVoiceSelectorDialog) {
        VoiceSelectorDialog(
            voices = availableVoices,
            selectedVoiceName = userSettings.selectedVoiceName,
            onSelectVoice = { voiceName ->
                viewModel.setSelectedVoice(voiceName)
            },
            onTestVoice = { voiceName ->
                viewModel.testVoice(voiceName)
            },
            onDismiss = { showVoiceSelectorDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Voice & Persona Settings
            SettingsSectionHeader(title = "Voice & Persona Settings")

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceVariant,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Primary Language",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val languages = listOf("hinglish" to "Hinglish (Recommended)", "hindi" to "Hindi", "english" to "English")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languages.forEach { (code, label) ->
                            val isSelected = userSettings.language.equals(code, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) ElectricViolet else DarkSurface,
                                border = BorderStroke(1.dp, if (isSelected) NeonCyan else GlassBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setLanguage(code) }
                                    .testTag("lang_option_$code")
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // TTS Voice Selector Card
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "TTS Voice",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "System TTS Voice",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                val currentVoiceDisplay = if (userSettings.selectedVoiceName.isNotBlank()) {
                                    val matched = availableVoices.firstOrNull { it.name == userSettings.selectedVoiceName }
                                    matched?.displayName ?: userSettings.selectedVoiceName.substringAfterLast("/")
                                } else {
                                    "System Default Auto Voice"
                                }
                                Text(
                                    text = currentVoiceDisplay,
                                    color = NeonBlue,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                            onClick = { showVoiceSelectorDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("choose_voice_button")
                        ) {
                            Text("Choose Voice", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Voice Pitch Slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Tone",
                            tint = NeonBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mayraa Voice Pitch (${String.format("%.2f", userSettings.speechPitch)})",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    Slider(
                        value = userSettings.speechPitch,
                        onValueChange = { viewModel.setSpeechPitch(it) },
                        valueRange = 0.8f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = ElectricViolet
                        ),
                        modifier = Modifier.testTag("pitch_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Speech Rate Slider
                    Text(
                        text = "Speech Speed (${String.format("%.2f", userSettings.speechRate)}x)",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Slider(
                        value = userSettings.speechRate,
                        onValueChange = { viewModel.setSpeechRate(it) },
                        valueRange = 0.7f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = ElectricViolet
                        ),
                        modifier = Modifier.testTag("rate_slider")
                    )
                }
            }

            // 'Always-on' & Hands-Free Listening Section
            SettingsSectionHeader(title = "Hands-Free & Always-On Listening")

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceVariant,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Always-on Listening Mode Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = "Always-on Mode",
                                tint = if (userSettings.alwaysOnListeningEnabled) NeonCyan else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "'Always-on' Listening Mode",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (userSettings.alwaysOnListeningEnabled) NeonCyan.copy(alpha = 0.2f) else DarkSurface
                                    ) {
                                        Text(
                                            text = if (userSettings.alwaysOnListeningEnabled) "LIVE" else "OFF",
                                            color = if (userSettings.alwaysOnListeningEnabled) NeonCyan else TextSecondary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Microphone hamesha active rahega bina button dabaye commands lene ke liye.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        Switch(
                            checked = userSettings.alwaysOnListeningEnabled,
                            onCheckedChange = { viewModel.setAlwaysOnListening(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = ElectricViolet
                            ),
                            modifier = Modifier.testTag("always_on_listening_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Wake-Word "Mayraa" Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wake-Word 'Mayraa'",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Bol kar activate karo ('Mayraa')",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = userSettings.wakeWordEnabled,
                            onCheckedChange = { viewModel.setWakeWordEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = ElectricViolet
                            ),
                            modifier = Modifier.testTag("wake_word_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Background Service Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Background Listening Service",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "App background mein hote hue bhi listening active rakho (Persistent notification ke sath).",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = userSettings.backgroundListeningEnabled,
                            onCheckedChange = { viewModel.setBackgroundListeningEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = ElectricViolet
                            ),
                            modifier = Modifier.testTag("background_service_switch")
                        )
                    }
                }
            }

            // AI Backend & API Key Section
            SettingsSectionHeader(title = "Gemini AI Connection")

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceVariant,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API Key",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini API Key (Optional Override)",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "By default, Mayraa uses project BuildConfig / Secrets. Custom key yahan enter kar sakte ho.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    OutlinedTextField(
                        value = apiKeyText,
                        onValueChange = {
                            apiKeyText = it
                            apiKeySavedFeedback = false
                        },
                        placeholder = { Text("AIzaSy...", color = Color.Gray, fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = NeonCyan,
                            unfocusedIndicatorColor = GlassBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("api_key_override_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.setApiKeyOverride(apiKeyText.trim())
                                apiKeySavedFeedback = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_api_key_button")
                        ) {
                            Text("Save Key", color = Color.White)
                        }

                        if (apiKeySavedFeedback) {
                            Text("Saved! ✅", color = NeonCyan, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Maintenance & History
            SettingsSectionHeader(title = "Data & History")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.clearChat() },
                    border = BorderStroke(1.dp, GlassBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("clear_chat_button")
                ) {
                    Text("Clear Chat", color = TextPrimary, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.clearActivityLogs() },
                    border = BorderStroke(1.dp, GlassBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("clear_logs_button")
                ) {
                    Text("Clear Activity", color = TextPrimary, fontSize = 12.sp)
                }
            }

            // About Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurfaceVariant,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAbout() }
                    .testTag("about_mayraa_item")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "About Mayraa AI",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Deepu Prankstar Edition • Personal Voice Assistant",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun VoiceSelectorDialog(
    voices: List<TtsVoiceInfo>,
    selectedVoiceName: String,
    onSelectVoice: (String) -> Unit,
    onTestVoice: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = NeonCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select TTS Voice",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Available Text-To-Speech voices installed on your device:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Option 1: System Auto Default
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedVoiceName.isBlank()) DeepViolet.copy(alpha = 0.5f) else DarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (selectedVoiceName.isBlank()) NeonCyan else GlassBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectVoice("") }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        RadioButton(
                            selected = selectedVoiceName.isBlank(),
                            onClick = { onSelectVoice("") },
                            colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "System Default Auto Voice",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Automatic selection based on chosen language",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (voices.isEmpty()) {
                    Text(
                        text = "System voices list loading or system default voice active.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        items(voices) { voice ->
                            val isSelected = selectedVoiceName == voice.name
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) DeepViolet.copy(alpha = 0.5f) else DarkSurfaceVariant,
                                border = BorderStroke(1.dp, if (isSelected) NeonCyan else GlassBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSelectVoice(voice.name) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onSelectVoice(voice.name) },
                                        colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = voice.displayName,
                                            color = TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "${voice.localeTag} ${if (voice.isNetworkRequired) "• Cloud" else "• Offline"}",
                                            color = if (isSelected) NeonBlue else TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { onTestVoice(voice.name) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Test voice",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Done", color = Color.White)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = NeonCyan,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

