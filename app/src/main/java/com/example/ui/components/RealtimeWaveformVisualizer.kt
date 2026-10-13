package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun RealtimeWaveformVisualizer(
    waveform: List<Float>,
    isCapturing: Boolean,
    isMuted: Boolean,
    onVisualizerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformIdleTransition")

    // Idle breathing wave animation when standby
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "IdleWavePhase"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isMuted) Color(0xFFF59E0B).copy(alpha = 0.5f)
            else if (isCapturing) NeonCyan.copy(alpha = 0.5f)
            else GlassBorder
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onVisualizerClick() }
            .testTag("realtime_waveform_visualizer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header: Live status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isMuted -> Color(0xFFF59E0B)
                                    isCapturing -> NeonCyan
                                    else -> TextSecondary
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isMuted -> "HARDWARE MIC MUTED"
                            isCapturing -> "LIVE AUDIO CAPTURE • 16 kHz PCM"
                            else -> "AUDIO STANDBY • TAP TO CAPTURE"
                        },
                        color = when {
                            isMuted -> Color(0xFFF59E0B)
                            isCapturing -> NeonCyan
                            else -> TextSecondary
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = if (isMuted) "0.0 dB" else if (isCapturing) "ACTIVE" else "READY",
                    color = if (isMuted) Color(0xFFF59E0B) else if (isCapturing) NeonBlue else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Waveform Bars
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val centerY = height / 2f
                    val barCount = waveform.size.coerceAtLeast(1)
                    val totalSpacing = width * 0.15f
                    val availableBarWidth = (width - totalSpacing) / barCount
                    val barWidth = availableBarWidth.coerceIn(2.dp.toPx(), 8.dp.toPx())
                    val gap = (width - (barCount * barWidth)) / (barCount + 1)

                    if (isMuted) {
                        // Muted flatline with dashed look
                        drawLine(
                            color = Color(0xFFF59E0B).copy(alpha = 0.6f),
                            start = Offset(0f, centerY),
                            end = Offset(width, centerY),
                            strokeWidth = 2.dp.toPx()
                        )
                    } else {
                        // Center baseline glow line
                        drawLine(
                            color = if (isCapturing) NeonCyan.copy(alpha = 0.25f) else Color(0x228B5CF6),
                            start = Offset(0f, centerY),
                            end = Offset(width, centerY),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Symmetrical waveform bars
                        val barBrush = Brush.verticalGradient(
                            colors = listOf(
                                NeonCyan,
                                ElectricViolet,
                                NeonBlue
                            )
                        )

                        val idleBrush = Brush.verticalGradient(
                            colors = listOf(
                                ElectricViolet.copy(alpha = 0.5f),
                                DeepViolet.copy(alpha = 0.3f)
                            )
                        )

                        for (i in 0 until barCount) {
                            val x = gap + i * (barWidth + gap)

                            val normalizedAmp = if (isCapturing) {
                                waveform[i].coerceIn(0.06f, 1.0f)
                            } else {
                                // Subtle undulating idle wave
                                val waveOffset = sin(idlePhase + (i * 0.25f))
                                (0.08f + (waveOffset * 0.04f)).coerceIn(0.04f, 0.16f)
                            }

                            val barHeight = (height * 0.90f * normalizedAmp).coerceAtLeast(3.dp.toPx())
                            val topY = centerY - (barHeight / 2f)

                            drawRoundRect(
                                brush = if (isCapturing) barBrush else idleBrush,
                                topLeft = Offset(x, topY),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }
                }
            }
        }
    }
}
