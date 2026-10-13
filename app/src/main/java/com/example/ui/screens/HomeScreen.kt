package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.domain.model.AssistantState
import com.example.ui.components.ConversationBubble
import com.example.ui.components.DisambiguationDialog
import com.example.ui.components.MayraaOrb
import com.example.ui.components.QuickActionChips
import com.example.ui.components.RealtimeWaveformVisualizer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToActivity: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    var textInput by remember { mutableStateOf("") }

    // Permission Request Launcher for Microphone
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startListening()
        } else {
            onNavigateToPermissions()
        }
    }

    // Safe in-app voice trigger function
    val startVoiceCommand: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            if (uiState.isMuted) {
                // If muted, unmute immediately so user can give command
                viewModel.setMuteState(false)
            }
            if (uiState.isMicActive) {
                viewModel.stopListening()
            } else {
                viewModel.startListening()
            }
        }
    }

    // Scroll to latest message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Disambiguation dialog
    uiState.disambiguationResult?.let { result ->
        DisambiguationDialog(
            candidates = result.candidateContacts,
            promptMessage = result.feedbackMessage,
            onSelectContact = { viewModel.selectDisambiguatedContact(it) },
            onDismiss = { viewModel.dismissDisambiguation() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Mayraa AI",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Deepu Prankstar badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DeepViolet.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Deepu Edition",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (uiState.isMuted) "Mic Muted 🔇 (Tap un-mute to speak)"
                            else if (uiState.isFallbackMode) "Voice Assistant • Active"
                            else "Gemini Voice Assistant • Ready",
                            color = if (uiState.isMuted) Color(0xFFF59E0B) else NeonCyan,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    // Quick Mute / Unmute Top Bar Action
                    IconButton(
                        onClick = { viewModel.toggleMute() },
                        modifier = Modifier.testTag("top_bar_mute_toggle")
                    ) {
                        Icon(
                            imageVector = if (uiState.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (uiState.isMuted) "Unmute Mayraa" else "Mute Mayraa",
                            tint = if (uiState.isMuted) Color(0xFFF59E0B) else NeonCyan
                        )
                    }

                    IconButton(
                        onClick = onNavigateToActivity,
                        modifier = Modifier.testTag("nav_activity_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Recent Activity",
                            tint = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onNavigateToPermissions,
                        modifier = Modifier.testTag("nav_permissions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Permissions",
                            tint = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("nav_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
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
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Error / Feedback Banner
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.errorMessage?.let { errorText ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.isMuted) Color(0x33F59E0B) else Color(0x33EF4444),
                        border = BorderStroke(1.dp, if (uiState.isMuted) Color(0xFFF59E0B) else Color(0xFFEF4444)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = errorText,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { viewModel.clearError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Central Orb & Voice Status Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MayraaOrb(
                        state = if (uiState.isMuted) AssistantState.IDLE else uiState.assistantState,
                        rmsLevel = if (uiState.isMuted) 0f else uiState.rmsLevel,
                        onClick = {
                            if (uiState.assistantState == AssistantState.SPEAKING) {
                                viewModel.stopSpeaking()
                            } else {
                                startVoiceCommand()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Assistant State & Mute status pill
                    val statusText = when {
                        uiState.isMuted -> "🔇 Muted • Tap mic or un-mute to speak"
                        uiState.assistantState == AssistantState.LISTENING -> "MAYRAA is listening..."
                        uiState.assistantState == AssistantState.THINKING -> "MAYRAA is thinking..."
                        uiState.assistantState == AssistantState.EXECUTING -> "Executing your command..."
                        uiState.assistantState == AssistantState.SPEAKING -> "MAYRAA is speaking..."
                        uiState.assistantState == AssistantState.ERROR -> "⚠️ Issue occurred • Tap to retry"
                        userSettings.wakeWordEnabled -> "MAYRAA is waiting • Say 'Mayraa' or Tap Mic"
                        else -> "MAYRAA is waiting for you • Tap Mic to speak"
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.isMuted) DarkSurfaceVariant.copy(alpha = 0.95f) else DarkSurfaceVariant.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, if (uiState.isMuted) Color(0xFFF59E0B).copy(alpha = 0.6f) else GlassBorder),
                        modifier = Modifier.clickable {
                            if (uiState.isMuted) viewModel.toggleMute() else startVoiceCommand()
                        }
                    ) {
                        Text(
                            text = statusText,
                            color = when {
                                uiState.isMuted -> Color(0xFFF59E0B)
                                uiState.assistantState == AssistantState.LISTENING -> NeonCyan
                                uiState.assistantState == AssistantState.SPEAKING -> NeonBlue
                                uiState.assistantState == AssistantState.THINKING -> NeonMagenta
                                uiState.assistantState == AssistantState.EXECUTING -> ElectricViolet
                                uiState.assistantState == AssistantState.ERROR -> Color(0xFFEF4444)
                                else -> TextSecondary
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    // Partial spoken transcript preview
                    AnimatedVisibility(visible = uiState.partialVoiceText.isNotBlank()) {
                        Text(
                            text = "\"${uiState.partialVoiceText}\"",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Real-time audio waveform visualizer
            RealtimeWaveformVisualizer(
                waveform = uiState.waveform,
                isCapturing = uiState.isMicActive,
                isMuted = uiState.isMuted,
                onVisualizerClick = {
                    if (uiState.isMuted) viewModel.toggleMute() else startVoiceCommand()
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Quick action voice chips
            QuickActionChips(
                onActionClick = { query ->
                    viewModel.handleUserQuery(query)
                }
            )

            // Conversation history list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    ConversationBubble(message = msg)
                }
            }

            // Bottom Input Controls: Mute Toggle + Mic Button + Text Input
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    // Mute / Unmute Button right by the Mic
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { viewModel.toggleMute() },
                            shape = CircleShape,
                            containerColor = if (uiState.isMuted) Color(0xFFEF4444).copy(alpha = 0.2f) else DeepViolet,
                            contentColor = if (uiState.isMuted) Color(0xFFEF4444) else NeonCyan,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("mute_unmute_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (uiState.isMuted) "Unmute Mayraa" else "Mute Mayraa",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = if (uiState.isMuted) "Unmute" else "Mute",
                            color = if (uiState.isMuted) Color(0xFFEF4444) else TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Primary Glowing Mic Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { startVoiceCommand() },
                            shape = CircleShape,
                            containerColor = when {
                                uiState.isMicActive -> NeonCyan
                                uiState.isMuted -> Color(0xFFEF4444)
                                else -> ElectricViolet
                            },
                            contentColor = when {
                                uiState.isMicActive -> Color.Black
                                uiState.isMuted -> Color.White
                                else -> Color.White
                            },
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("main_mic_button")
                        ) {
                            Icon(
                                imageVector = when {
                                    uiState.isMicActive -> Icons.Default.Close
                                    uiState.isMuted -> Icons.Default.MicOff
                                    else -> Icons.Default.Mic
                                },
                                contentDescription = if (uiState.isMicActive) "Stop listening" else "Start listening",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = when {
                                uiState.isMicActive -> "Listening"
                                uiState.isMuted -> "Tap to Speak"
                                else -> "Speak"
                            },
                            color = when {
                                uiState.isMicActive -> NeonCyan
                                uiState.isMuted -> Color(0xFFEF4444)
                                else -> NeonBlue
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Secondary text query input
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text("Bol nahi sakte? Yahan likho...", color = TextTertiary, fontSize = 13.sp)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = NeonCyan,
                            unfocusedIndicatorColor = GlassBorder,
                            cursorColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    viewModel.handleUserQuery(textInput)
                                    textInput = ""
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("text_command_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send text button
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.handleUserQuery(textInput)
                                textInput = ""
                                focusManager.clearFocus()
                            }
                        },
                        enabled = textInput.isNotBlank(),
                        modifier = Modifier.testTag("send_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Command",
                            tint = if (textInput.isNotBlank()) NeonCyan else TextTertiary
                        )
                    }
                }
            }
        }
    }
}
