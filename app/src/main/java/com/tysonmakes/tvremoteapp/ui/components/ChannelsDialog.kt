package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tysonmakes.tvremoteapp.ui.theme.*

import androidx.compose.runtime.*
import com.tysonmakes.tvremoteapp.model.RemoteKeycodes

data class TvInputOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val command: String
)

val DEFAULT_TV_INPUTS = listOf(
    TvInputOption("hdmi_1", "HDMI 1", "External Port 1", Icons.Default.Tv, "input keyevent 178"),
    TvInputOption("hdmi_2", "HDMI 2", "External Port 2", Icons.Default.Tv, "input keyevent 178"),
    TvInputOption("hdmi_3", "HDMI 3 (ARC/eARC)", "Audio Return Port", Icons.Default.SpeakerGroup, "input keyevent 178"),
    TvInputOption("hdmi_4", "HDMI 4", "Gaming Console", Icons.Default.SportsEsports, "input keyevent 178"),
    TvInputOption("live_tv", "Live TV / Tuner", "Broadcast Channels", Icons.Default.LiveTv, "input keyevent 170"),
    TvInputOption("av_in", "Composite / AV", "RCA Analog In", Icons.Default.SettingsInputComponent, "input keyevent 178")
)

@Composable
fun ChannelsDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSelectInput: (TvInputOption) -> Unit,
    onKeySend: (String) -> Unit = {}
) {
    if (!isOpen) return
    val haptic = LocalHapticFeedback.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Numpad & Colors, 1: Inputs

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = AtvSheetDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, RemoteBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = AtvAccentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (selectedTab == 0) "Keypad & Numbers" else "TV Inputs & Channels",
                            color = AtvTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AtvTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Segmented Tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AtvButtonDark)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTab = 0
                            },
                        color = if (selectedTab == 0) AtvAccentBlue else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Numpad & Color Keys",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) AtvTextPrimary else AtvTextSecondary
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTab = 1
                            },
                        color = if (selectedTab == 1) AtvAccentBlue else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "TV Inputs (HDMI)",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) AtvTextPrimary else AtvTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Full TV Numpad & 4 Color Program Keys
                    NumpadView(
                        onKeySend = onKeySend,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // TV Source Inputs Grid
                    Text(
                        text = "Switch TV display input source or open tuner channels:",
                        color = AtvTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(DEFAULT_TV_INPUTS, key = { it.id }) { option ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSelectInput(option)
                                        onDismiss()
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = AtvButtonDark,
                                border = androidx.compose.foundation.BorderStroke(1.dp, AtvDividerLine)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = option.title,
                                            tint = AtvTextPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = option.title,
                                            color = AtvTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = option.subtitle,
                                        color = AtvTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
