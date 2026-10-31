package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.tysonmakes.tvremoteapp.model.AppCategoryFilter
import com.tysonmakes.tvremoteapp.model.InstalledApp
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun AppManagerView(
    apps: List<InstalledApp>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onForceStop: (String) -> Unit,
    onClearData: (String) -> Unit,
    onUninstall: (String) -> Unit,
    onExtractApk: (InstalledApp) -> Unit,
    onOpenPlayStore: (String) -> Unit,
    onOpenRemote: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(AppCategoryFilter.ALL) }
    var selectedAppForMenu by remember { mutableStateOf<InstalledApp?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    val filteredApps = remember(apps, searchQuery, selectedFilter) {
        apps.filter { app ->
            val matchesCategory = when (selectedFilter) {
                AppCategoryFilter.USER -> !app.isSystemApp
                AppCategoryFilter.SYSTEM -> app.isSystemApp
                AppCategoryFilter.ALL -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                app.appName.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // 1. Search Bar with Filter Icon (Screenshot 3 style)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = AtvSearchBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, RemoteBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = AtvTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search", color = AtvTextSecondary, fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedTextColor = AtvTextPrimary,
                            unfocusedTextColor = AtvTextPrimary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = AtvTextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Filter toggle
                    IconButton(
                        onClick = {
                            selectedFilter = when (selectedFilter) {
                                AppCategoryFilter.ALL -> AppCategoryFilter.USER
                                AppCategoryFilter.USER -> AppCategoryFilter.SYSTEM
                                AppCategoryFilter.SYSTEM -> AppCategoryFilter.ALL
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter",
                            tint = if (selectedFilter != AppCategoryFilter.ALL) AtvAccentBlue else AtvTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Apps List (Screenshot 3)
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = AtvAccentBlue, strokeWidth = 2.dp)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No apps matching '$searchQuery'" else "No installed apps found",
                                color = AtvTextSecondary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(containerColor = AtvButtonDark)
                            ) {
                                Text("Refresh Apps", color = AtvTextPrimary)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        InstalledAppCard(
                            app = app,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLaunchApp(app.packageName)
                            },
                            onMenuClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedAppForMenu = app
                            }
                        )
                    }
                }
            }
        }

        // Floating Remote Button (Pill at bottom right, Screenshot 3)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenRemote()
                }
                .testTag("apps_floating_remote_btn"),
            shape = RoundedCornerShape(23.dp),
            color = AtvButtonDark,
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, AtvDividerLine)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsRemote,
                    contentDescription = "Remote Control",
                    tint = AtvTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Remote",
                    color = AtvTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }

    // App Options Bottom Sheet / Dialog
    selectedAppForMenu?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForMenu = null },
            containerColor = AtvSheetDark,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AtvButtonDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = app.appName.firstOrNull()?.uppercase() ?: "A",
                            color = AtvAccentBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(app.appName, color = AtvTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(app.packageName, color = AtvTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppActionItem(
                        icon = Icons.Default.PlayArrow,
                        title = "Open App",
                        subtitle = "Launch on TV display",
                        color = AtvAccentBlue,
                        onClick = {
                            onLaunchApp(app.packageName)
                            selectedAppForMenu = null
                        }
                    )
                    AppActionItem(
                        icon = Icons.Default.Close,
                        title = "Force Stop",
                        subtitle = "Terminate process immediately",
                        color = Color(0xFFF59E0B),
                        onClick = {
                            onForceStop(app.packageName)
                            selectedAppForMenu = null
                        }
                    )
                    AppActionItem(
                        icon = Icons.Default.CleaningServices,
                        title = "Clear Data & Cache",
                        subtitle = "Reset application data",
                        color = Color(0xFFF97316),
                        onClick = {
                            onClearData(app.packageName)
                            selectedAppForMenu = null
                        }
                    )
                    AppActionItem(
                        icon = Icons.Default.FileDownload,
                        title = "Extract APK to TV",
                        subtitle = "Copies APK to /sdcard/Download",
                        color = Color(0xFF38BDF8),
                        onClick = {
                            onExtractApk(app)
                            selectedAppForMenu = null
                        }
                    )
                    AppActionItem(
                        icon = Icons.Default.Shop,
                        title = "Open in Play Store",
                        subtitle = "View in TV Store",
                        color = Color(0xFF10B981),
                        onClick = {
                            onOpenPlayStore(app.packageName)
                            selectedAppForMenu = null
                        }
                    )
                    AppActionItem(
                        icon = Icons.Default.Delete,
                        title = "Uninstall App",
                        subtitle = "Remove from Android TV",
                        color = AtvPowerRed,
                        onClick = {
                            onUninstall(app.packageName)
                            selectedAppForMenu = null
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAppForMenu = null }) {
                    Text("Cancel", color = AtvTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun InstalledAppCard(
    app: InstalledApp,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("app_item_${app.packageName}"),
        shape = RoundedCornerShape(14.dp),
        color = AtvSheetDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, RemoteBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // App Logo / Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AtvButtonDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.firstOrNull()?.uppercase() ?: "A",
                        color = AtvTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        color = AtvTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${app.versionName} - ${app.sizeString}",
                        color = AtvTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "App options",
                    tint = AtvTextSecondary
                )
            }
        }
    }
}

@Composable
private fun AppActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    color: Color = AtvTextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = AtvTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = AtvTextSecondary, fontSize = 11.sp)
        }
    }
}
