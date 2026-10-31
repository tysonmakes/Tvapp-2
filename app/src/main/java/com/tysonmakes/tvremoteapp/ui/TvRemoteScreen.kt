package com.tysonmakes.tvremoteapp.ui

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tysonmakes.tvremoteapp.model.RemoteKeycodes
import com.tysonmakes.tvremoteapp.model.RemoteTab
import com.tysonmakes.tvremoteapp.ui.components.*
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun TvRemoteScreen(
    viewModel: TvRemoteViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var showOverflowMenu by remember { mutableStateOf(false) }

    val isConnected = uiState.connectionStatus is ConnectionStatus.Connected

    // Phone File Picker for APK Installation
    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            var fileName = "app_${System.currentTimeMillis()}.apk"
            try {
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            cursor.getString(nameIndex)?.let { name -> fileName = name }
                        }
                    }
                }
            } catch (_: Exception) {}
            viewModel.installApkFromUri(it, fileName)
        }
    }

    // Phone File Picker for General File Upload
    val fileUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            var fileName = "file_${System.currentTimeMillis()}"
            try {
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            cursor.getString(nameIndex)?.let { name -> fileName = name }
                        }
                    }
                }
            } catch (_: Exception) {}
            val targetFolder = uiState.currentTvPath.ifEmpty { "/sdcard/Download" }
            viewModel.uploadFileFromUri(it, fileName, targetFolder)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AtvCanvasDark,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            // Authentic atvTools Top Bar: Back, TV Name, Status, Three-Dots Menu
            AtvTopAppBar(
                tvName = uiState.connectedDevice?.name ?: "Family Room TV",
                statusText = when (uiState.connectionStatus) {
                    is ConnectionStatus.Connected -> "CONNECTED"
                    is ConnectionStatus.Connecting -> "CONNECTING..."
                    is ConnectionStatus.Error -> "ERROR"
                    ConnectionStatus.Disconnected -> "DISCONNECTED"
                },
                statusColor = when (uiState.connectionStatus) {
                    is ConnectionStatus.Connected -> Color(0xFF10B981)
                    is ConnectionStatus.Connecting -> Color(0xFFFF9100)
                    is ConnectionStatus.Error -> Color(0xFFEF4444)
                    ConnectionStatus.Disconnected -> AtvTextSecondary
                },
                onBackClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (uiState.isRemoteSheetOpen) {
                        viewModel.setRemoteSheetOpen(false)
                    } else if (uiState.currentTab == RemoteTab.FILES || uiState.currentTab == RemoteTab.GAMEPAD) {
                        viewModel.setTab(RemoteTab.TOOLS)
                    } else {
                        viewModel.sendKey(RemoteKeycodes.BACK)
                    }
                },
                onMenuClick = { showOverflowMenu = true },
                isMenuExpanded = showOverflowMenu,
                onDismissMenu = { showOverflowMenu = false },
                onSelectDeviceDiscovery = {
                    showOverflowMenu = false
                    viewModel.setDiscoveryOpen(true)
                },
                onSelectSettings = {
                    showOverflowMenu = false
                    viewModel.setSettingsOpen(true)
                },
                onSelectChannels = {
                    showOverflowMenu = false
                    viewModel.setChannelsDialogOpen(true)
                },
                onSelectPowerMenu = {
                    showOverflowMenu = false
                    viewModel.setPowerMenuOpen(true)
                },
                onDisconnect = {
                    showOverflowMenu = false
                    viewModel.disconnect()
                }
            )
        },
        bottomBar = {
            // Bottom 4-Tab Navigation Bar (Only visible when Remote sheet is closed)
            if (!uiState.isRemoteSheetOpen && uiState.currentTab != RemoteTab.FILES && uiState.currentTab != RemoteTab.GAMEPAD) {
                AtvBottomNavigationBar(
                    selectedTab = when (uiState.currentTab) {
                        RemoteTab.TOOLS -> 0
                        RemoteTab.APPS -> 1
                        RemoteTab.TERMINAL -> 2
                        RemoteTab.INFO -> 3
                        else -> 0
                    },
                    onTabSelected = { index ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        when (index) {
                            0 -> viewModel.setTab(RemoteTab.TOOLS)
                            1 -> viewModel.setTab(RemoteTab.APPS)
                            2 -> viewModel.setTab(RemoteTab.TERMINAL)
                            3 -> viewModel.setTab(RemoteTab.INFO)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.isRemoteSheetOpen,
                transitionSpec = {
                    slideInVertically { height -> height } + fadeIn() togetherWith
                            slideOutVertically { height -> height } + fadeOut()
                },
                label = "MainRemoteSheetTransition"
            ) { isRemoteSheet ->
                if (isRemoteSheet) {
                    // Modern TV Remote Control View with zero-latency touch
                    ModernTvRemoteView(
                        onKeySend = { viewModel.sendKey(it) },
                        onKeyStartRepeat = { viewModel.startKeyRepeat(it) },
                        onKeyStopRepeat = { viewModel.stopKeyRepeat() },
                        latencyMs = uiState.latencyMs,
                        isConnected = uiState.isConnected,
                        onReconnect = { viewModel.connectLastDevice() },
                        onOpenNumpad = { viewModel.setChannelsDialogOpen(true) },
                        onOpenTextInput = { viewModel.setTextInputDialogOpen(true) },
                        onOpenPowerMenu = { viewModel.setPowerMenuOpen(true) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Selected Tab Content View
                    when (uiState.currentTab) {
                        RemoteTab.TOOLS -> {
                            TvToolsView(
                                onToolClick = { viewModel.handleGridToolClick(it) },
                                onInstallApkClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                                onUploadFileClick = { fileUploadLauncher.launch("*/*") },
                                onToggleNowPlaying = { viewModel.toggleNowPlayingMedia() },
                                onSkipNext = { viewModel.skipNextMedia() },
                                onOpenRemote = { viewModel.setRemoteSheetOpen(true) },
                                isPlaying = uiState.isNowPlayingPlaying,
                                nowPlayingTitle = uiState.nowPlayingTitle,
                                nowPlayingApp = uiState.nowPlayingApp,
                                isExecuting = uiState.isExecutingTool
                            )
                        }
                        RemoteTab.APPS -> {
                            AppManagerView(
                                apps = uiState.installedApps,
                                isLoading = uiState.isAppsLoading,
                                onRefresh = { viewModel.fetchInstalledApps() },
                                onLaunchApp = { viewModel.launchApp(it) },
                                onForceStop = { viewModel.forceStopApp(it) },
                                onClearData = { viewModel.clearAppData(it) },
                                onUninstall = { viewModel.uninstallApp(it) },
                                onExtractApk = { viewModel.extractApk(it) },
                                onOpenPlayStore = { viewModel.openPlayStore(it) },
                                onOpenRemote = { viewModel.setRemoteSheetOpen(true) }
                            )
                        }
                        RemoteTab.TERMINAL -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AdbTerminalView(
                                    outputLogs = uiState.consoleLogs,
                                    onExecuteCommand = { viewModel.executeShell(it) },
                                    onClearLogs = { viewModel.clearLogs() }
                                )
                                // Floating Remote Button
                                FloatingRemoteButton(
                                    onClick = { viewModel.setRemoteSheetOpen(true) },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(20.dp)
                                )
                            }
                        }
                        RemoteTab.INFO -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                DeviceInfoView(
                                    telemetry = uiState.telemetry,
                                    onRefresh = { viewModel.fetchDeviceTelemetry() }
                                )
                                // Floating Remote Button
                                FloatingRemoteButton(
                                    onClick = { viewModel.setRemoteSheetOpen(true) },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(20.dp)
                                )
                            }
                        }
                        RemoteTab.FILES -> {
                            FileManagerView(
                                currentPath = uiState.currentTvPath.ifEmpty { "/sdcard" },
                                files = uiState.tvFiles,
                                isLoading = uiState.isFilesLoading,
                                onNavigate = { viewModel.fetchTvFiles(it) },
                                onDeleteFile = { viewModel.deleteTvFile(it) },
                                onOpenFileOnTv = { viewModel.openFileOnTv(it) },
                                onInstallApkFile = { viewModel.installApkFromTvPath(it) },
                                onUploadClick = { fileUploadLauncher.launch("*/*") },
                                onCreateFolder = { viewModel.createTvFolder(it) },
                                onRefresh = { viewModel.fetchTvFiles(uiState.currentTvPath.ifEmpty { "/sdcard" }) }
                            )
                        }
                        RemoteTab.GAMEPAD -> {
                            GamepadControl(
                                onKeySend = { viewModel.sendKey(it) }
                            )
                        }
                        else -> {
                            TvToolsView(
                                onToolClick = { viewModel.handleGridToolClick(it) },
                                onInstallApkClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                                onUploadFileClick = { fileUploadLauncher.launch("*/*") },
                                onToggleNowPlaying = { viewModel.toggleNowPlayingMedia() },
                                onSkipNext = { viewModel.skipNextMedia() },
                                onOpenRemote = { viewModel.setRemoteSheetOpen(true) },
                                isPlaying = uiState.isNowPlayingPlaying,
                                nowPlayingTitle = uiState.nowPlayingTitle,
                                nowPlayingApp = uiState.nowPlayingApp,
                                isExecuting = uiState.isExecutingTool
                            )
                        }
                    }
                }
            }

            // Real-time status feedback pill (positioned safely at bottom center, auto-dismissed, never blocks controls)
            AnimatedVisibility(
                visible = uiState.statusMessage.isNotBlank(),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (uiState.isRemoteSheetOpen) 24.dp else 16.dp, start = 20.dp, end = 20.dp)
            ) {
                val isError = uiState.statusMessage.contains("failed", ignoreCase = true) ||
                        uiState.statusMessage.contains("error", ignoreCase = true) ||
                        uiState.statusMessage.contains("unauthorized", ignoreCase = true) ||
                        uiState.statusMessage.contains("timed out", ignoreCase = true)

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = if (isError) Color(0xFFEF4444).copy(alpha = 0.96f) else Color(0xFF1E2430).copy(alpha = 0.96f),
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isError) Color(0xFFDC2626) else AtvAccentBlue.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isError) Color.White else AtvAccentBlue)
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Text(
                            text = uiState.statusMessage,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // DIALOGS & OVERLAYS
    // -------------------------------------------------------------

    // Channels / TV Input & Numpad Dialog
    ChannelsDialog(
        isOpen = uiState.isChannelsDialogOpen,
        onDismiss = { viewModel.setChannelsDialogOpen(false) },
        onSelectInput = { option ->
            viewModel.executeShell(option.command)
        },
        onKeySend = { keycode ->
            viewModel.sendKey(keycode)
        }
    )

    // Text & Keyboard Input Dialog
    TextInputDialog(
        isOpen = uiState.isTextInputDialogOpen,
        onDismiss = { viewModel.setTextInputDialogOpen(false) },
        onSendText = { text ->
            viewModel.sendText(text)
        }
    )

    // File Transfer / APK Install Progress Dialog
    FileTransferDialog(
        transferState = uiState.fileTransferState,
        onDismiss = { viewModel.dismissFileTransfer() }
    )

    // Network Scan & Manual Discovery Dialog
    DeviceDiscoveryDialog(
        isOpen = uiState.isDiscoveryOpen,
        isScanning = uiState.isScanning,
        scanProgress = uiState.scanProgress,
        savedDevices = uiState.savedDevices,
        onDismiss = { viewModel.setDiscoveryOpen(false) },
        onStartScan = { viewModel.startScan(it) },
        onStopScan = { viewModel.stopScan() },
        onConnectDevice = { ip, port, name -> viewModel.connectDevice(ip, port, name) },
        onDeleteDevice = { viewModel.deleteDevice(it) },
        onPairDevice = { ip, port, code -> viewModel.pairDevice(ip, port, code) }
    )

    // Preferences & Settings Dialog
    RemoteSettingsDialog(
        isOpen = uiState.isSettingsOpen,
        settings = uiState.settings,
        onDismiss = { viewModel.setSettingsOpen(false) },
        onUpdateSettings = { viewModel.updateSettings(it) }
    )

    // Power Menu Dialog
    if (uiState.isPowerMenuOpen) {
        PowerMenuDialog(
            onDismiss = { viewModel.setPowerMenuOpen(false) },
            onSleep = { viewModel.sendKey(RemoteKeycodes.SLEEP) },
            onWake = { viewModel.sendKey(RemoteKeycodes.WAKEUP) },
            onSoftReboot = {
                viewModel.executeTvTool(
                    com.tysonmakes.tvremoteapp.model.TvToolAction("soft_reboot", "Soft Reboot", "", "", "setprop ctl.restart zygote || am restart")
                )
            },
            onFullReboot = {
                viewModel.executeTvTool(
                    com.tysonmakes.tvremoteapp.model.TvToolAction("reboot", "Full Reboot", "", "", "reboot")
                )
            },
            onPowerOff = { viewModel.sendKey(RemoteKeycodes.POWER) }
        )
    }

    // Screenshot Preview Dialog
    if (uiState.isScreenshotDialogOpen) {
        ScreenshotDialog(
            screenshotPath = uiState.lastScreenshotPath,
            onDismiss = { viewModel.setScreenshotDialogOpen(false) }
        )
    }
}

@Composable
private fun AtvTopAppBar(
    tvName: String,
    statusText: String,
    statusColor: Color,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    isMenuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onSelectDeviceDiscovery: () -> Unit,
    onSelectSettings: () -> Unit,
    onSelectChannels: () -> Unit,
    onSelectPowerMenu: () -> Unit,
    onDisconnect: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AtvCanvasDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Back Arrow
            IconButton(onClick = onBackClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AtvTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Center: TV Name & Subtitle Status (e.g. CONNECTED) - tap to switch/connect device
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelectDeviceDiscovery() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = tvName,
                    color = AtvTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.1.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Right: Three dots menu
            Box {
                IconButton(onClick = onMenuClick, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = AtvTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = onDismissMenu,
                    modifier = Modifier.background(AtvSheetDark)
                ) {
                    DropdownMenuItem(
                        text = { Text("Select TV / Devices", color = AtvTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Tv, contentDescription = null, tint = AtvAccentBlue) },
                        onClick = onSelectDeviceDiscovery
                    )
                    DropdownMenuItem(
                        text = { Text("Channels & Inputs", color = AtvTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.LiveTv, contentDescription = null, tint = AtvAccentBlue) },
                        onClick = onSelectChannels
                    )
                    DropdownMenuItem(
                        text = { Text("Settings & Preferences", color = AtvTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = AtvTextSecondary) },
                        onClick = onSelectSettings
                    )
                    DropdownMenuItem(
                        text = { Text("Power Options", color = AtvPowerRed) },
                        leadingIcon = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = AtvPowerRed) },
                        onClick = onSelectPowerMenu
                    )
                    DropdownMenuItem(
                        text = { Text("Disconnect", color = AtvTextSecondary) },
                        leadingIcon = { Icon(Icons.Default.LinkOff, contentDescription = null, tint = AtvTextSecondary) },
                        onClick = onDisconnect
                    )
                }
            }
        }
    }
}

@Composable
private fun AtvBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        Pair("Tools", Icons.Default.Build),
        Pair("Apps", Icons.Default.Apps),
        Pair("Shell", Icons.Default.Android),
        Pair("Info", Icons.Default.Info)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        color = AtvCanvasDark,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, AtvDividerLine.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            tabs.forEachIndexed { index, pair ->
                val isSelected = selectedTab == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(index) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = pair.second,
                        contentDescription = pair.first,
                        tint = if (isSelected) AtvAccentBlue else AtvTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = pair.first,
                        color = if (isSelected) AtvAccentBlue else AtvTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingRemoteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
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
