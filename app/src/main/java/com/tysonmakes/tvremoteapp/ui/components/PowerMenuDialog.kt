package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun PowerMenuDialog(
    onDismiss: () -> Unit,
    onSleep: () -> Unit,
    onWake: () -> Unit,
    onSoftReboot: () -> Unit,
    onFullReboot: () -> Unit,
    onPowerOff: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceRaised,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "TV Power Menu",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PowerOptionItem(
                    icon = Icons.Default.Bedtime,
                    title = "Sleep Display (Standby)",
                    subtitle = "Turns off screen while keeping TV ready",
                    color = Color(0xFF818CF8),
                    onClick = {
                        onSleep()
                        onDismiss()
                    }
                )

                PowerOptionItem(
                    icon = Icons.Default.WbSunny,
                    title = "Wake Up TV",
                    subtitle = "Powers on screen and HDMI-CEC inputs",
                    color = Color(0xFFFBBF24),
                    onClick = {
                        onWake()
                        onDismiss()
                    }
                )

                PowerOptionItem(
                    icon = Icons.Default.RestartAlt,
                    title = "Fast Soft Reboot",
                    subtitle = "Restarts Android System UI without full boot cycle",
                    color = Color(0xFFF97316),
                    onClick = {
                        onSoftReboot()
                        onDismiss()
                    }
                )

                PowerOptionItem(
                    icon = Icons.Default.PowerSettingsNew,
                    title = "Full TV Restart",
                    subtitle = "Complete hardware reboot cycle",
                    color = Color(0xFFEF4444),
                    onClick = {
                        onFullReboot()
                        onDismiss()
                    }
                )

                PowerOptionItem(
                    icon = Icons.Default.PowerOff,
                    title = "Power Off TV",
                    subtitle = "Shuts down Android TV completely",
                    color = Color(0xFFDC2626),
                    onClick = {
                        onPowerOff()
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
private fun PowerOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DpadBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = TextMuted, fontSize = 11.sp)
            }
        }
    }
}
