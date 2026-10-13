package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary

@Composable
fun QuickActionChips(
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickActions = listOf(
        "YouTube par song chalao" to "🎵 YouTube",
        "WhatsApp par message bhejo" to "💬 WhatsApp",
        "Papa ko call karo" to "📞 Call",
        "10 minute ka timer lagao" to "⏳ Timer",
        "Mayraa, motivate karo" to "🔥 Motivate",
        "Camera kholo" to "📸 Camera",
        "Volume badhao" to "🔊 Volume",
        "Instagram open karo" to "📱 Instagram",
        "Google par search karo" to "🔍 Search"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickActions.forEachIndexed { index, (query, label) ->
            Surface(
                onClick = { onActionClick(query) },
                shape = RoundedCornerShape(20.dp),
                color = DarkSurfaceVariant.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.testTag("quick_action_chip_$index")
            ) {
                Text(
                    text = label,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
