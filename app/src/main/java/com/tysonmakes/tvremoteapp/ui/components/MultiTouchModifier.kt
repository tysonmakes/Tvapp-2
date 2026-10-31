package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Modifier for ultra-low latency, multi-touch key dispatch.
 *
 * Solves:
 * 1. 0ms Dispatch Latency: Fires immediately on pointer DOWN rather than waiting for finger release (UP).
 * 2. Multi-touch Independence: Allows multiple fingers to press different buttons simultaneously
 *    without pointer theft or gesture cancellation.
 * 3. Anti-Stuck Safeguard: Resets repeat and press state reliably on any pointer release or gesture completion.
 * 4. High-frequency Button Mashing: Never queues blocking coroutines or locks UI interaction.
 */
fun Modifier.multiTouchKey(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    enableRepeat: Boolean = false,
    repeatDelayMs: Long = 250L,
    repeatIntervalMs: Long = 85L,
    onStartRepeat: (() -> Unit)? = null,
    onStopRepeat: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val currentOnClick = rememberUpdatedState(onClick)
    val currentOnStartRepeat = rememberUpdatedState(onStartRepeat)
    val currentOnStopRepeat = rememberUpdatedState(onStopRepeat)
    val scope = rememberCoroutineScope()

    pointerInput(enabled) {
        if (!enabled) return@pointerInput

        awaitEachGesture {
            // Detect down with requireUnconsumed = false so parent layouts don't swallow taps
            val down = awaitFirstDown(requireUnconsumed = false)
            val pointerId = down.id
            down.consume()

            // Instant fire on touch down
            currentOnClick.value.invoke()

            // Update interaction source for ripple / pressed visual state
            var pressInteraction: PressInteraction.Press? = null
            if (interactionSource != null) {
                val press = PressInteraction.Press(down.position)
                pressInteraction = press
                scope.launch { interactionSource.emit(press) }
            }

            var repeatJob: Job? = null
            if (onStartRepeat != null) {
                currentOnStartRepeat.value?.invoke()
            } else if (enableRepeat && repeatIntervalMs > 0) {
                repeatJob = scope.launch {
                    delay(repeatDelayMs)
                    while (isActive) {
                        currentOnClick.value.invoke()
                        delay(repeatIntervalMs)
                    }
                }
            }

            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.find { it.id == pointerId }
                    if (change == null || !change.pressed) {
                        change?.consume()
                        break
                    }
                }
            } finally {
                repeatJob?.cancel()
                currentOnStopRepeat.value?.invoke()
                if (interactionSource != null && pressInteraction != null) {
                    val release = PressInteraction.Release(pressInteraction)
                    scope.launch { interactionSource.emit(release) }
                }
            }
        }
    }
}
