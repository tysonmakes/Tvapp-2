package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PanToolAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tysonmakes.tvremoteapp.model.RemoteKeycodes
import com.tysonmakes.tvremoteapp.ui.theme.*
import kotlin.math.abs

@Composable
fun TrackpadControl(
    onKeySend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var lastGestureFeedback by remember { mutableStateOf("Swipe to navigate • Tap to select (OK)") }
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    var sensitivity by remember { mutableFloatStateOf(40f) } // Lower = more sensitive

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = AccentCyan.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DarkSurfaceRaised,
                            DarkSurface,
                            DarkBackground
                        )
                    )
                )
                .border(1.5.dp, DpadBorderColor, RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            touchPosition = offset
                            tryAwaitRelease()
                            touchPosition = null
                        },
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            lastGestureFeedback = "Tap: Select (OK)"
                            onKeySend(RemoteKeycodes.DPAD_CENTER)
                        }
                    )
                }
                .pointerInput(sensitivity) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            touchPosition = offset
                            totalDragX = 0f
                            totalDragY = 0f
                        },
                        onDragEnd = {
                            touchPosition = null
                        },
                        onDragCancel = {
                            touchPosition = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            touchPosition = change.position
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y

                            if (abs(totalDragX) > sensitivity || abs(totalDragY) > sensitivity) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (abs(totalDragX) > abs(totalDragY)) {
                                    if (totalDragX > 0) {
                                        lastGestureFeedback = "Swipe Right →"
                                        onKeySend(RemoteKeycodes.DPAD_RIGHT)
                                    } else {
                                        lastGestureFeedback = "← Swipe Left"
                                        onKeySend(RemoteKeycodes.DPAD_LEFT)
                                    }
                                } else {
                                    if (totalDragY > 0) {
                                        lastGestureFeedback = "Swipe Down ↓"
                                        onKeySend(RemoteKeycodes.DPAD_DOWN)
                                    } else {
                                        lastGestureFeedback = "Swipe Up ↑"
                                        onKeySend(RemoteKeycodes.DPAD_UP)
                                    }
                                }
                                totalDragX = 0f
                                totalDragY = 0f
                            }
                        }
                    )
                }
                .testTag("trackpad_surface"),
            contentAlignment = Alignment.Center
        ) {
            // Live Cursor Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                touchPosition?.let { pos ->
                    drawCircle(
                        color = AccentCyan.copy(alpha = 0.25f),
                        radius = 48.dp.toPx(),
                        center = pos
                    )
                    drawCircle(
                        color = AccentCyan,
                        radius = 8.dp.toPx(),
                        center = pos
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PanToolAlt,
                    contentDescription = "Trackpad",
                    tint = AccentCyan.copy(alpha = 0.85f),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "High-Precision Trackpad",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastGestureFeedback,
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sensitivity adjustment
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Sensitivity", color = TextSecondary, fontSize = 12.sp)
            Slider(
                value = 80f - sensitivity,
                onValueChange = { sensitivity = 80f - it },
                valueRange = 10f..60f,
                modifier = Modifier
                    .width(160.dp)
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = AccentCyan,
                    activeTrackColor = AccentCyan,
                    inactiveTrackColor = DarkSurfaceRaised
                )
            )
            Text("Fast", color = TextSecondary, fontSize = 12.sp)
        }
    }
}
