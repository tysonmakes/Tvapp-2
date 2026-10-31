package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tysonmakes.tvremoteapp.model.ATV_GRID_TOOLS
import com.tysonmakes.tvremoteapp.model.TvToolAction
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun TvToolsView(
    onToolClick: (TvToolAction) -> Unit,
    onInstallApkClick: () -> Unit,
    onUploadFileClick: () -> Unit,
    onToggleNowPlaying: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenRemote: () -> Unit,
    isPlaying: Boolean,
    nowPlayingTitle: String,
    nowPlayingApp: String,
    isExecuting: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Grid of 11 Tools (Screenshot 4)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(ATV_GRID_TOOLS, key = { it.id }) { tool ->
                TvGridToolCard(
                    tool = tool,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when (tool.id) {
                            "install_apk" -> onInstallApkClick()
                            "upload_file" -> onUploadFileClick()
                            else -> onToolClick(tool)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // -------------------------------------------------------------
        // PERSISTENT NOW PLAYING / MINI MEDIA BAR (Screenshot 4)
        // -------------------------------------------------------------
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .testTag("tools_mini_player"),
            shape = RoundedCornerShape(16.dp),
            color = AtvMiniPlayerBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, RemoteBorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Video thumbnail / Album art
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF263042)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartDisplay,
                            contentDescription = null,
                            tint = AtvAccentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = nowPlayingTitle,
                            color = AtvTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = nowPlayingApp,
                            color = AtvTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Play/Pause button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleNowPlaying()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AtvAccentBlue)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play or Pause",
                        tint = AtvTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Next Track button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSkipNext()
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = AtvTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Floating Remote Button (docked inside bar for quick launch)
                Surface(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenRemote()
                        }
                        .testTag("floating_remote_btn"),
                    shape = RoundedCornerShape(12.dp),
                    color = AtvButtonDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AtvDividerLine)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsRemote,
                            contentDescription = "Remote Control",
                            tint = AtvTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvGridToolCard(
    tool: TvToolAction,
    onClick: () -> Unit
) {
    val iconVector: ImageVector = when (tool.id) {
        "install_apk" -> Icons.Default.DownloadForOffline
        "upload_file" -> Icons.Default.UploadFile
        "file_manager" -> Icons.Default.Folder
        "channels" -> Icons.Default.LiveTv
        "screen_mirror" -> Icons.Default.Cast
        "gamepad" -> Icons.Default.SportsEsports
        "screenshot" -> Icons.Default.CameraAlt
        "screen_record" -> Icons.Default.Videocam
        "clear_cache" -> Icons.Default.DeleteOutline
        "screensaver" -> Icons.Default.AutoAwesome
        "power_menu" -> Icons.Default.PowerSettingsNew
        else -> Icons.Default.Build
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("grid_tool_${tool.id}"),
        shape = RoundedCornerShape(14.dp),
        color = AtvButtonDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, AtvDividerLine)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = tool.title,
                tint = AtvTextPrimary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = tool.title,
                color = AtvTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                lineHeight = 17.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
