package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tysonmakes.tvremoteapp.model.*
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun RemoteSettingsDialog(
    isOpen: Boolean,
    settings: RemoteSettings,
    onDismiss: () -> Unit,
    onUpdateSettings: (RemoteSettings) -> Unit
) {
    if (!isOpen) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .testTag("remote_settings_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Remote & Latency Settings",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Response & Latency Mode
                    item {
                        SettingsCard(title = "ADB Response Mode", icon = Icons.Default.Speed) {
                            ResponseMode.entries.forEach { mode ->
                                val isSelected = settings.responseMode == mode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) AccentCyan.copy(alpha = 0.15f) else DarkSurfaceRaised)
                                        .border(
                                            1.dp,
                                            if (isSelected) AccentCyan else DpadBorderColor,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            onUpdateSettings(settings.copy(responseMode = mode))
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onUpdateSettings(settings.copy(responseMode = mode)) },
                                        colors = RadioButtonDefaults.colors(selectedColor = AccentCyan)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(mode.label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(mode.description, color = TextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Haptic Feedback Intensity
                    item {
                        SettingsCard(title = "Haptic Vibration Feedback", icon = Icons.Default.Vibration) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HapticIntensity.entries.forEach { intensity ->
                                    val isSelected = settings.hapticIntensity == intensity
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) AccentCyan else DarkSurfaceRaised)
                                            .border(1.dp, if (isSelected) AccentCyan else DpadBorderColor, RoundedCornerShape(10.dp))
                                            .clickable {
                                                onUpdateSettings(settings.copy(hapticIntensity = intensity))
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = intensity.label,
                                            color = if (isSelected) DarkBackground else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Auto-Repeat Speed
                    item {
                        SettingsCard(title = "Button Hold Auto-Repeat Speed", icon = Icons.Default.Repeat) {
                            val speeds = listOf(
                                "Slow (120ms)" to 120L,
                                "Normal (85ms)" to 85L,
                                "Turbo (50ms)" to 50L
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                speeds.forEach { (label, speedVal) ->
                                    val isSelected = settings.repeatSpeedMs == speedVal
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) AccentCyan else DarkSurfaceRaised)
                                            .border(1.dp, if (isSelected) AccentCyan else DpadBorderColor, RoundedCornerShape(10.dp))
                                            .clickable {
                                                onUpdateSettings(settings.copy(repeatSpeedMs = speedVal))
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label.split(" ")[0],
                                            color = if (isSelected) DarkBackground else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Auto-Connect Preference
                    item {
                        SettingsCard(title = "Startup Behavior", icon = Icons.Default.Power) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-reconnect to last TV", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Automatically connects when app opens", color = TextMuted, fontSize = 11.sp)
                                }
                                Switch(
                                    checked = settings.autoConnectLastDevice,
                                    onCheckedChange = { onUpdateSettings(settings.copy(autoConnectLastDevice = it)) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBackground,
                                        checkedTrackColor = AccentCyan
                                    )
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = DarkBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save & Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceRaised),
        modifier = Modifier.border(1.dp, DpadBorderColor, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
