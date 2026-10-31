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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tysonmakes.tvremoteapp.model.RemoteKeycodes
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun GamepadControl(
    onKeySend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Shoulder Triggers (L1, Select, Start, R1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // L1 Trigger
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceRaised)
                    .border(1.dp, DpadBorderColor, RoundedCornerShape(12.dp))
                    .multiTouchKey(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onKeySend(RemoteKeycodes.BUTTON_L1)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("L1", fontWeight = FontWeight.Bold, color = AccentCyan)
            }

            // Select & Start
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                        .border(1.dp, DpadBorderColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .multiTouchKey(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onKeySend(RemoteKeycodes.BUTTON_SELECT)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SELECT", fontSize = 10.sp, color = TextMuted)
                }

                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                        .border(1.dp, DpadBorderColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .multiTouchKey(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onKeySend(RemoteKeycodes.BUTTON_START)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("START", fontSize = 10.sp, color = TextPrimary)
                }
            }

            // R1 Trigger
            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceRaised)
                    .border(1.dp, DpadBorderColor, RoundedCornerShape(12.dp))
                    .multiTouchKey(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onKeySend(RemoteKeycodes.BUTTON_R1)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("R1", fontWeight = FontWeight.Bold, color = AccentCyan)
            }
        }

        // Center Split: Left D-Pad + Right Action Cluster (A/B/X/Y)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left D-Pad Cross
            GamepadDpadCross(
                onKeySend = onKeySend
            )

            // Right Action Cluster (X, Y, A, B)
            GamepadAbxyCluster(
                onKeySend = onKeySend
            )
        }
    }
}

@Composable
private fun GamepadDpadCross(
    onKeySend: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(DarkSurfaceRaised)
            .border(1.dp, DpadBorderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // UP
        GamepadDirectionBtn(
            modifier = Modifier.align(Alignment.TopCenter),
            icon = Icons.Default.KeyboardArrowUp,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onKeySend(RemoteKeycodes.DPAD_UP)
            }
        )
        // DOWN
        GamepadDirectionBtn(
            modifier = Modifier.align(Alignment.BottomCenter),
            icon = Icons.Default.KeyboardArrowDown,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onKeySend(RemoteKeycodes.DPAD_DOWN)
            }
        )
        // LEFT
        GamepadDirectionBtn(
            modifier = Modifier.align(Alignment.CenterStart),
            icon = Icons.Default.KeyboardArrowLeft,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onKeySend(RemoteKeycodes.DPAD_LEFT)
            }
        )
        // RIGHT
        GamepadDirectionBtn(
            modifier = Modifier.align(Alignment.CenterEnd),
            icon = Icons.Default.KeyboardArrowRight,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onKeySend(RemoteKeycodes.DPAD_RIGHT)
            }
        )
        // Center
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(DarkBackground)
                .border(1.dp, DpadBorderColor, CircleShape)
                .multiTouchKey(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onKeySend(RemoteKeycodes.DPAD_CENTER)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("OK", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GamepadDirectionBtn(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .multiTouchKey(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun GamepadAbxyCluster(
    onKeySend: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(DarkSurfaceRaised)
            .border(1.dp, DpadBorderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // TOP: Y (Yellow)
        GamepadActionBtn(
            modifier = Modifier.align(Alignment.TopCenter),
            label = "Y",
            color = Color(0xFFFBBF24),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onKeySend(RemoteKeycodes.BUTTON_Y)
            }
        )

        // BOTTOM: A (Green)
        GamepadActionBtn(
            modifier = Modifier.align(Alignment.BottomCenter),
            label = "A",
            color = Color(0xFF10B981),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onKeySend(RemoteKeycodes.BUTTON_A)
            }
        )

        // LEFT: X (Blue)
        GamepadActionBtn(
            modifier = Modifier.align(Alignment.CenterStart),
            label = "X",
            color = Color(0xFF38BDF8),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onKeySend(RemoteKeycodes.BUTTON_X)
            }
        )

        // RIGHT: B (Red)
        GamepadActionBtn(
            modifier = Modifier.align(Alignment.CenterEnd),
            label = "B",
            color = Color(0xFFEF4444),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onKeySend(RemoteKeycodes.BUTTON_B)
            }
        )
    }
}

@Composable
private fun GamepadActionBtn(
    modifier: Modifier = Modifier,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(6.dp)
            .size(42.dp)
            .shadow(4.dp, CircleShape, spotColor = color)
            .clip(CircleShape)
            .background(DarkSurface)
            .border(1.5.dp, color, CircleShape)
            .multiTouchKey(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
    }
}
