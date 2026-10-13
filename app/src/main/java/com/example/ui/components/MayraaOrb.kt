package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.domain.model.AssistantState
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DeepViolet
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.OrbErrorGlow
import com.example.ui.theme.OrbIdleGlow
import com.example.ui.theme.OrbListeningGlow
import com.example.ui.theme.OrbSpeakingGlow
import com.example.ui.theme.OrbThinkingGlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MayraaOrb(
    state: AssistantState,
    rmsLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransitions")

    // Slow breathing animation for IDLE
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdlePulse"
    )

    // Continuous rotation for THINKING & SPEAKING
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Fast ripple for listening & speaking
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RippleScale"
    )

    val contentDesc = when (state) {
        AssistantState.IDLE -> "Mayraa AI Orb Idle - Tap to speak"
        AssistantState.LISTENING -> "Mayraa AI Listening to your voice"
        AssistantState.THINKING -> "Mayraa AI Processing query"
        AssistantState.EXECUTING -> "Executing your command"
        AssistantState.SPEAKING -> "Mayraa AI Speaking - Tap to stop"
        AssistantState.ERROR -> "Mayraa AI Connection warning"
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .semantics { contentDescription = contentDesc }
            .testTag("mayraa_central_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.58f

            when (state) {
                AssistantState.IDLE -> {
                    // Outer diffuse aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(OrbIdleGlow.copy(alpha = 0.35f), Color.Transparent),
                            center = center,
                            radius = baseRadius * 1.5f * idlePulse
                        ),
                        radius = baseRadius * 1.5f * idlePulse,
                        center = center
                    )
                    // Inner glowing cybernetic sphere
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan, ElectricViolet, DeepViolet),
                            center = Offset(center.x - 15f, center.y - 15f),
                            radius = baseRadius * idlePulse
                        ),
                        radius = baseRadius * idlePulse,
                        center = center
                    )
                    // Sleek thin cyber ring
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.6f),
                        radius = baseRadius * 1.15f * idlePulse,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                AssistantState.LISTENING -> {
                    // Audio-reactive expansion
                    val reactiveRadius = baseRadius * (1f + rmsLevel * 0.45f)

                    // Multiple neon soundwaves pulsing outward
                    drawCircle(
                        color = OrbListeningGlow.copy(alpha = (0.4f - (rippleScale - 1f)).coerceAtLeast(0.05f)),
                        radius = baseRadius * rippleScale * (1f + rmsLevel * 0.3f),
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawCircle(
                        color = CyanGlow.copy(alpha = 0.25f),
                        radius = reactiveRadius * 1.25f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Core active listening orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, NeonCyan, DeepViolet),
                            center = center,
                            radius = reactiveRadius
                        ),
                        radius = reactiveRadius,
                        center = center
                    )
                }

                AssistantState.THINKING -> {
                    // Pulsing thinking ring with gradient arcs
                    val pulse = 1f + (sin(rotation * PI.toFloat() / 180f) * 0.08f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(OrbThinkingGlow.copy(alpha = 0.3f), Color.Transparent),
                            center = center,
                            radius = baseRadius * 1.4f
                        ),
                        radius = baseRadius * 1.4f,
                        center = center
                    )

                    // Core sphere
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonMagenta, ElectricViolet, DeepViolet),
                            center = center,
                            radius = baseRadius * pulse
                        ),
                        radius = baseRadius * pulse,
                        center = center
                    )

                    // Rotating glowing arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(NeonCyan, NeonMagenta, ElectricViolet, NeonCyan),
                            center = center
                        ),
                        startAngle = rotation,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx()),
                        topLeft = Offset(center.x - baseRadius * 1.18f, center.y - baseRadius * 1.18f),
                        size = androidx.compose.ui.geometry.Size(baseRadius * 2.36f, baseRadius * 2.36f)
                    )
                }

                AssistantState.EXECUTING -> {
                    // Fast electric rotation for action execution
                    val execPulse = 1f + (sin(rotation * 2f * PI.toFloat() / 180f) * 0.12f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.4f), Color.Transparent),
                            center = center,
                            radius = baseRadius * 1.5f * execPulse
                        ),
                        radius = baseRadius * 1.5f * execPulse,
                        center = center
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, NeonCyan, ElectricViolet),
                            center = center,
                            radius = baseRadius * execPulse
                        ),
                        radius = baseRadius * execPulse,
                        center = center
                    )

                    // Dual spinning orbits
                    drawArc(
                        color = NeonCyan,
                        startAngle = rotation * 1.5f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 3.5.dp.toPx()),
                        topLeft = Offset(center.x - baseRadius * 1.25f, center.y - baseRadius * 1.25f),
                        size = androidx.compose.ui.geometry.Size(baseRadius * 2.5f, baseRadius * 2.5f)
                    )
                    drawArc(
                        color = NeonMagenta,
                        startAngle = -rotation * 1.5f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 2.5.dp.toPx()),
                        topLeft = Offset(center.x - baseRadius * 1.15f, center.y - baseRadius * 1.15f),
                        size = androidx.compose.ui.geometry.Size(baseRadius * 2.3f, baseRadius * 2.3f)
                    )
                }

                AssistantState.SPEAKING -> {
                    // Dynamic voice sound waves
                    val speakWave = 1f + (sin(rotation * 2.5f * PI.toFloat() / 180f) * 0.12f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(OrbSpeakingGlow.copy(alpha = 0.35f), Color.Transparent),
                            center = center,
                            radius = baseRadius * 1.5f * speakWave
                        ),
                        radius = baseRadius * 1.5f * speakWave,
                        center = center
                    )

                    // Central glowing orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, NeonBlue, ElectricViolet),
                            center = center,
                            radius = baseRadius * speakWave
                        ),
                        radius = baseRadius * speakWave,
                        center = center
                    )

                    // Particle points around the speaker orb
                    val numPoints = 8
                    for (i in 0 until numPoints) {
                        val angle = (rotation + (i * 360f / numPoints)) * (PI.toFloat() / 180f)
                        val dist = baseRadius * 1.25f + (sin(angle * 3) * 10.dp.toPx())
                        val px = center.x + cos(angle) * dist
                        val py = center.y + sin(angle) * dist
                        drawCircle(
                            color = NeonCyan,
                            radius = 3.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }

                AssistantState.ERROR -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(OrbErrorGlow.copy(alpha = 0.3f), Color.Transparent),
                            center = center,
                            radius = baseRadius * 1.3f
                        ),
                        radius = baseRadius * 1.3f,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(OrbErrorGlow, DeepViolet),
                            center = center,
                            radius = baseRadius
                        ),
                        radius = baseRadius,
                        center = center
                    )
                }
            }
        }
    }
}
