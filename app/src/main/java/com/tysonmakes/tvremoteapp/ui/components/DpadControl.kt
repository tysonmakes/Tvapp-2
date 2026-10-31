package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tysonmakes.tvremoteapp.model.HapticIntensity
import com.tysonmakes.tvremoteapp.model.RemoteKeycodes
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun DpadControl(
    onKeySend: (String) -> Unit,
    onStartRepeat: (String) -> Unit,
    onStopRepeat: () -> Unit,
    hapticLevel: HapticIntensity = HapticIntensity.MEDIUM,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    fun triggerHaptic() {
        when (hapticLevel) {
            HapticIntensity.OFF -> {}
            HapticIntensity.SUBTLE -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            HapticIntensity.MEDIUM -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            HapticIntensity.STRONG -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Main Controller Arena: Volume Rocker | D-PAD Compass | Channel Rocker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Rocker: Volume (+, Mute, -)
            TactileRocker(
                topIcon = Icons.AutoMirrored.Filled.VolumeUp,
                topLabel = "VOL +",
                topKeycode = RemoteKeycodes.VOLUME_UP,
                centerIcon = Icons.AutoMirrored.Filled.VolumeMute,
                centerLabel = "MUTE",
                centerKeycode = RemoteKeycodes.VOLUME_MUTE,
                bottomIcon = Icons.AutoMirrored.Filled.VolumeDown,
                bottomLabel = "VOL -",
                bottomKeycode = RemoteKeycodes.VOLUME_DOWN,
                onStartRepeat = {
                    triggerHaptic()
                    onStartRepeat(it)
                },
                onStopRepeat = onStopRepeat,
                testTagPrefix = "volume_rocker"
            )

            // Center Compass: Circular High-Precision D-Pad
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .shadow(16.dp, CircleShape, spotColor = AccentCyan.copy(alpha = 0.25f))
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                DpadRimGradientTop,
                                DpadRimGradientBottom,
                                Color(0xFF090D10)
                            )
                        )
                    )
                    .border(2.dp, DpadBorderColor, CircleShape)
                    .testTag("dpad_container"),
                contentAlignment = Alignment.Center
            ) {
                // UP Button
                DpadDirectionButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    keycode = RemoteKeycodes.DPAD_UP,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .testTag("dpad_up"),
                    onStartRepeat = {
                        triggerHaptic()
                        onStartRepeat(it)
                    },
                    onStopRepeat = onStopRepeat
                )

                // DOWN Button
                DpadDirectionButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    keycode = RemoteKeycodes.DPAD_DOWN,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .testTag("dpad_down"),
                    onStartRepeat = {
                        triggerHaptic()
                        onStartRepeat(it)
                    },
                    onStopRepeat = onStopRepeat
                )

                // LEFT Button
                DpadDirectionButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    keycode = RemoteKeycodes.DPAD_LEFT,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp)
                        .testTag("dpad_left"),
                    onStartRepeat = {
                        triggerHaptic()
                        onStartRepeat(it)
                    },
                    onStopRepeat = onStopRepeat
                )

                // RIGHT Button
                DpadDirectionButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    keycode = RemoteKeycodes.DPAD_RIGHT,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp)
                        .testTag("dpad_right"),
                    onStartRepeat = {
                        triggerHaptic()
                        onStartRepeat(it)
                    },
                    onStopRepeat = onStopRepeat
                )

                // Center OK Button
                CenterOkButton(
                    onPress = {
                        triggerHaptic()
                        onKeySend(RemoteKeycodes.DPAD_CENTER)
                    },
                    modifier = Modifier.testTag("dpad_ok")
                )
            }

            // Right Rocker: Channel (CH+, Guide, CH-)
            TactileRocker(
                topIcon = Icons.Default.KeyboardArrowUp,
                topLabel = "CH +",
                topKeycode = RemoteKeycodes.CHANNEL_UP,
                centerIcon = Icons.Default.Tv,
                centerLabel = "INPUT",
                centerKeycode = RemoteKeycodes.TV_INPUT,
                bottomIcon = Icons.Default.KeyboardArrowDown,
                bottomLabel = "CH -",
                bottomKeycode = RemoteKeycodes.CHANNEL_DOWN,
                onStartRepeat = {
                    triggerHaptic()
                    onStartRepeat(it)
                },
                onStopRepeat = onStopRepeat,
                testTagPrefix = "channel_rocker"
            )
        }

        // Media Player Deck
        Row(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceRaised)
                .border(1.dp, DpadBorderColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    triggerHaptic()
                    onKeySend(RemoteKeycodes.MEDIA_REWIND)
                },
                modifier = Modifier.testTag("media_rewind")
            ) {
                Icon(Icons.Default.FastRewind, contentDescription = "Rewind", tint = TextPrimary, modifier = Modifier.size(24.dp))
            }

            // Play / Pause Button with Accent Highlight
            Button(
                onClick = {
                    triggerHaptic()
                    onKeySend(RemoteKeycodes.MEDIA_PLAY_PAUSE)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = DarkBackground
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(46.dp)
                    .shadow(8.dp, CircleShape, spotColor = AccentCyan.copy(alpha = 0.5f))
                    .testTag("media_play_pause")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause", modifier = Modifier.size(26.dp))
            }

            IconButton(
                onClick = {
                    triggerHaptic()
                    onKeySend(RemoteKeycodes.MEDIA_FAST_FORWARD)
                },
                modifier = Modifier.testTag("media_fast_forward")
            ) {
                Icon(Icons.Default.FastForward, contentDescription = "Fast Forward", tint = TextPrimary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun DpadDirectionButton(
    icon: ImageVector,
    keycode: String,
    modifier: Modifier = Modifier,
    onStartRepeat: (String) -> Unit,
    onStopRepeat: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.88f else 1.0f, label = "dpad_scale")

    Box(
        modifier = modifier
            .size(52.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isPressed) AccentCyan.copy(alpha = 0.25f) else Color.Transparent)
            .multiTouchKey(
                onStartRepeat = {
                    isPressed = true
                    onStartRepeat(keycode)
                },
                onStopRepeat = {
                    isPressed = false
                    onStopRepeat()
                },
                onClick = {
                    isPressed = true
                    onStartRepeat(keycode)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = keycode,
            tint = if (isPressed) AccentCyan else TextPrimary,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
private fun CenterOkButton(
    onPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1.0f, label = "ok_scale")

    Box(
        modifier = modifier
            .size(76.dp)
            .scale(scale)
            .shadow(12.dp, CircleShape, spotColor = AccentCyan.copy(alpha = 0.4f))
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = if (isPressed) listOf(AccentCyan.copy(alpha = 0.8f), AccentCyanDim)
                    else listOf(Color(0xFF2B3A48), Color(0xFF151C22))
                )
            )
            .border(
                width = 2.dp,
                color = if (isPressed) AccentCyan else AccentCyan.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .multiTouchKey(
                onStartRepeat = { isPressed = true },
                onStopRepeat = { isPressed = false },
                onClick = {
                    isPressed = true
                    onPress()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "OK",
            color = if (isPressed) DarkBackground else AccentCyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun TactileRocker(
    topIcon: ImageVector,
    topLabel: String,
    topKeycode: String,
    centerIcon: ImageVector,
    centerLabel: String,
    centerKeycode: String,
    bottomIcon: ImageVector,
    bottomLabel: String,
    bottomKeycode: String,
    onStartRepeat: (String) -> Unit,
    onStopRepeat: () -> Unit,
    testTagPrefix: String
) {
    Column(
        modifier = Modifier
            .width(58.dp)
            .height(180.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(DarkSurfaceRaised)
            .border(1.5.dp, DpadBorderColor, RoundedCornerShape(24.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Action
        RockerSegment(
            icon = topIcon,
            label = topLabel,
            keycode = topKeycode,
            isTop = true,
            onStartRepeat = onStartRepeat,
            onStopRepeat = onStopRepeat,
            modifier = Modifier
                .weight(1f)
                .testTag("${testTagPrefix}_top")
        )

        // Center Action
        RockerCenterSegment(
            icon = centerIcon,
            label = centerLabel,
            keycode = centerKeycode,
            onStartRepeat = onStartRepeat,
            onStopRepeat = onStopRepeat,
            modifier = Modifier.testTag("${testTagPrefix}_center")
        )

        // Bottom Action
        RockerSegment(
            icon = bottomIcon,
            label = bottomLabel,
            keycode = bottomKeycode,
            isTop = false,
            onStartRepeat = onStartRepeat,
            onStopRepeat = onStopRepeat,
            modifier = Modifier
                .weight(1f)
                .testTag("${testTagPrefix}_bottom")
        )
    }
}

@Composable
private fun RockerSegment(
    icon: ImageVector,
    label: String,
    keycode: String,
    isTop: Boolean,
    onStartRepeat: (String) -> Unit,
    onStopRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isPressed) AccentCyan.copy(alpha = 0.2f) else Color.Transparent)
            .multiTouchKey(
                onStartRepeat = {
                    isPressed = true
                    onStartRepeat(keycode)
                },
                onStopRepeat = {
                    isPressed = false
                    onStopRepeat()
                },
                onClick = {
                    isPressed = true
                    onStartRepeat(keycode)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPressed) AccentCyan else TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = if (isTop) "+" else "−",
                color = if (isPressed) AccentCyan else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RockerCenterSegment(
    icon: ImageVector,
    label: String,
    keycode: String,
    onStartRepeat: (String) -> Unit,
    onStopRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(if (isPressed) AccentCyan.copy(alpha = 0.25f) else DarkBackground.copy(alpha = 0.6f))
            .multiTouchKey(
                onStartRepeat = {
                    isPressed = true
                    onStartRepeat(keycode)
                },
                onStopRepeat = {
                    isPressed = false
                    onStopRepeat()
                },
                onClick = {
                    isPressed = true
                    onStartRepeat(keycode)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isPressed) AccentCyan else TextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}
