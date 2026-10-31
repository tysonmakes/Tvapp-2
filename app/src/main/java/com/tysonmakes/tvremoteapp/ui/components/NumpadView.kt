package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tysonmakes.tvremoteapp.model.RemoteKeycodes
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun NumpadView(
    onKeySend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    fun clickKey(keycode: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onKeySend(keycode)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Colored Smart TV Buttons (Red, Green, Yellow, Blue)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ColorButton(label = "Red", color = ColorKeyRed, onClick = { clickKey(RemoteKeycodes.PROG_RED) }, testTag = "color_red")
            ColorButton(label = "Green", color = ColorKeyGreen, onClick = { clickKey(RemoteKeycodes.PROG_GREEN) }, testTag = "color_green")
            ColorButton(label = "Yellow", color = ColorKeyYellow, onClick = { clickKey(RemoteKeycodes.PROG_YELLOW) }, testTag = "color_yellow")
            ColorButton(label = "Blue", color = ColorKeyBlue, onClick = { clickKey(RemoteKeycodes.PROG_BLUE) }, testTag = "color_blue")
        }

        // 2. Numeric Grid (1-9, Info, 0, Enter)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            NumKey(text = "1", onClick = { clickKey(RemoteKeycodes.NUM_1) })
            NumKey(text = "2", onClick = { clickKey(RemoteKeycodes.NUM_2) })
            NumKey(text = "3", onClick = { clickKey(RemoteKeycodes.NUM_3) })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            NumKey(text = "4", onClick = { clickKey(RemoteKeycodes.NUM_4) })
            NumKey(text = "5", onClick = { clickKey(RemoteKeycodes.NUM_5) })
            NumKey(text = "6", onClick = { clickKey(RemoteKeycodes.NUM_6) })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            NumKey(text = "7", onClick = { clickKey(RemoteKeycodes.NUM_7) })
            NumKey(text = "8", onClick = { clickKey(RemoteKeycodes.NUM_8) })
            NumKey(text = "9", onClick = { clickKey(RemoteKeycodes.NUM_9) })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            // Guide / Info
            IconButtonKey(
                icon = Icons.Default.Info,
                label = "INFO",
                onClick = { clickKey(RemoteKeycodes.MENU) },
                testTag = "num_info"
            )
            NumKey(text = "0", onClick = { clickKey(RemoteKeycodes.NUM_0) })
            // Enter / OK
            IconButtonKey(
                icon = Icons.AutoMirrored.Filled.KeyboardReturn,
                label = "ENTER",
                onClick = { clickKey(RemoteKeycodes.DPAD_CENTER) },
                testTag = "num_enter"
            )
        }
    }
}

@Composable
private fun ColorButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .multiTouchKey(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun NumKey(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceRaised)
            .border(1.dp, DpadBorderColor, RoundedCornerShape(16.dp))
            .multiTouchKey(onClick = onClick)
            .testTag("num_key_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun IconButtonKey(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceRaised)
            .border(1.dp, DpadBorderColor, RoundedCornerShape(16.dp))
            .multiTouchKey(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = AccentCyan,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
