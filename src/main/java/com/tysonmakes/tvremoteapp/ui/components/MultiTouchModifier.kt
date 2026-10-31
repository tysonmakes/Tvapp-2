package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Multi-touch, zero-latency tactile button modifier for TV remote controls.
 *
 * Solves:
 * 1. Multi-Touch Blocking: Standard Compose clickable/detectTapGestures serializes touches.
 *    Using awaitEachGesture with independent PointerId tracking allows multiple fingers to
 *    press distinct buttons simultaneously (e.g., D-Pad + OK, Gamepad A + B).
 * 2. Rapid Multi-Tap Freezing: Fires immediately upon pointer DOWN without waiting for release,
 *    eliminating double-tap detection latency and tap-swallowing.
 * 3. Smooth Non-Blocking Repeat: Long-pressing smoothly repeats without locking coroutines or UI threads.
 */
fun Modifier.multiTouchKey(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onStartRepeat: (() -> Unit)? = null,
    onStopRepeat: (() -> Unit)? = null,
    repeatDelayMs: Long = 240L,
    repeatIntervalMs: Long = 75L,
    enableScaleEffect: Boolean = true
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enableScaleEffect) 0.93f else 1.0f,
        animationSpec = tween(durationMillis = 60),
        label = "ButtonScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(onClick, onLongClick, onStartRepeat, onStopRepeat, repeatDelayMs, repeatIntervalMs) {
            awaitEachGesture {
                // Wait for any finger to touch down inside this button's bounds
                val down = awaitFirstDown(requireUnconsumed = false)
                val targetPointerId: PointerId = down.id
                down.consume()

                isPressed = true
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                // Fire immediate stroke on pointer DOWN (0ms touch-to-dispatch latency)
                onClick()

                var repeatActive = false

                // Handle repeat or long-press if held
                val supervisor = coroutineScope {
                    launch {
                        delay(repeatDelayMs)
                        if (onLongClick != null && onStartRepeat == null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        } else if (onStartRepeat != null) {
                            repeatActive = true
                            while (isActive) {
                                onStartRepeat()
                                delay(repeatIntervalMs)
                            }
                        }
                    }
                }

                // Track the specific pointer until release or cancellation
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.find { it.id == targetPointerId }

                        if (change == null || !change.pressed) {
                            change?.consume()
                            break
                        }
                    }
                } finally {
                    isPressed = false
                    supervisor.cancel()
                    if (repeatActive) {
                        onStopRepeat?.invoke()
                    }
                }
            }
        }
}
