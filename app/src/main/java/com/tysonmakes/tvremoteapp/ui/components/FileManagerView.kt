package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tysonmakes.tvremoteapp.model.TvFileItem
import com.tysonmakes.tvremoteapp.model.TvFileType
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun FileManagerView(
    currentPath: String,
    files: List<TvFileItem>,
    isLoading: Boolean,
    onNavigate: (String) -> Unit,
    onDeleteFile: (TvFileItem) -> Unit,
    onOpenFileOnTv: (TvFileItem) -> Unit,
    onInstallApkFile: (TvFileItem) -> Unit,
    onUploadClick: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    var selectedFileForMenu by remember { mutableStateOf<TvFileItem?>(null) }
    var fileToDelete by remember { mutableStateOf<TvFileItem?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val quickShortcuts = remember {
        listOf(
            Pair("Internal Storage", "/sdcard"),
            Pair("Downloads", "/sdcard/Download"),
            Pair("Movies", "/sdcard/Movies"),
            Pair("Pictures", "/sdcard/Pictures"),
            Pair("DCIM", "/sdcard/DCIM"),
            Pair("Root (/)", "/")
        )
    }

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val breadcrumbs = remember(currentPath) {
        val normalized = currentPath.trimEnd('/')
        if (normalized.isEmpty() || normalized == "/") {
            listOf(Pair("/", "/"))
        } else {
            val parts = normalized.split('/').filter { it.isNotEmpty() }
            val list = mutableListOf(Pair("Root", "/"))
            var acc = ""
            for (p in parts) {
                acc += "/$p"
                list.add(Pair(p, acc))
            }
            list
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // 1. Top Action Toolbar: Upload Button, New Folder Button, Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Upload to TV Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUploadClick()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = DarkBackground,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Upload to TV",
                    color = DarkBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // New Folder Button
                IconButton(
                    onClick = {
                        newFolderName = ""
                        showNewFolderDialog = true
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                        .border(1.dp, DpadBorderColor, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "New Folder",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Refresh Button
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRefresh()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                        .border(1.dp, DpadBorderColor, RoundedCornerShape(10.dp))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AccentCyan, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentCyan, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Interactive Breadcrumbs Navigation Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DpadBorderColor)
        ) {
            val breadcrumbScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(breadcrumbScrollState)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentPath != "/" && currentPath != "/sdcard") {
                    IconButton(
                        onClick = {
                            val parent = if (currentPath.contains('/')) {
                                val up = currentPath.substringBeforeLast('/')
                                if (up.isEmpty()) "/" else up
                            } else "/"
                            onNavigate(parent)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go up",
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))

                breadcrumbs.forEachIndexed { index, pair ->
                    val isLast = index == breadcrumbs.size - 1
                    Text(
                        text = pair.first,
                        color = if (isLast) TextPrimary else AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = !isLast) { onNavigate(pair.second) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    if (!isLast) {
                        Text(
                            text = "/",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Quick Folder Shortcuts Chips (Downloads, Movies, Internal Storage)
        val shortcutsScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(shortcutsScrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickShortcuts.forEach { shortcut ->
                val isCurrent = currentPath == shortcut.second || (shortcut.second == "/sdcard" && currentPath == "/storage/emulated/0")
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigate(shortcut.second) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCurrent) AccentCyan.copy(alpha = 0.15f) else DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isCurrent) AccentCyan else DpadBorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (shortcut.first) {
                                "Downloads" -> Icons.Default.Download
                                "Movies" -> Icons.Default.Movie
                                "Pictures", "DCIM" -> Icons.Default.Image
                                "Root (/)" -> Icons.Default.Storage
                                else -> Icons.Default.Folder
                            },
                            contentDescription = null,
                            tint = if (isCurrent) AccentCyan else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = shortcut.first,
                            color = if (isCurrent) AccentCyan else TextMuted,
                            fontSize = 11.5.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Search Bar for Current Directory
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            placeholder = { Text("Filter files in this folder...", color = TextMuted, fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = DpadBorderColor,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Interactive Files & Folders List Area
        if (isLoading && files.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentCyan, strokeWidth = 2.dp)
            }
        } else if (filteredFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No files match '$searchQuery'" else "This folder is empty",
                        color = TextMuted,
                        fontSize = 13.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onUploadClick,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceRaised),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload a file here", color = AccentCyan, fontSize = 12.5.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredFiles, key = { it.path + it.name }) { fileItem ->
                    InteractiveFileRow(
                        item = fileItem,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (fileItem.isDirectory) {
                                onNavigate(fileItem.path)
                            } else {
                                selectedFileForMenu = fileItem
                            }
                        },
                        onMenuClick = {
                            selectedFileForMenu = fileItem
                        }
                    )
                }
            }
        }
    }

    // 6. Action Menu Dialog for Selected File / Folder
    selectedFileForMenu?.let { fileItem ->
        AlertDialog(
            onDismissRequest = { selectedFileForMenu = null },
            containerColor = DarkSurfaceRaised,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(getFileColor(fileItem).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getFileIcon(fileItem),
                            contentDescription = null,
                            tint = getFileColor(fileItem),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = fileItem.name,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (fileItem.isDirectory) "Directory" else "${fileItem.size.ifEmpty { "File" }} • ${fileItem.path}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Open / Explore
                    if (fileItem.isDirectory) {
                        FileActionMenuItem(
                            icon = Icons.Default.FolderOpen,
                            title = "Open Directory",
                            subtitle = "Browse contents of this folder",
                            color = Color(0xFFFBBF24),
                            onClick = {
                                onNavigate(fileItem.path)
                                selectedFileForMenu = null
                            }
                        )
                    } else if (fileItem.fileType == TvFileType.APK) {
                        // Install APK option
                        FileActionMenuItem(
                            icon = Icons.Default.InstallMobile,
                            title = "Install APK on TV",
                            subtitle = "Run package manager to install this APK",
                            color = AccentCyan,
                            onClick = {
                                onInstallApkFile(fileItem)
                                selectedFileForMenu = null
                            }
                        )
                    } else {
                        // Open with default TV app / intent
                        FileActionMenuItem(
                            icon = Icons.Default.PlayArrow,
                            title = "Open / View on TV",
                            subtitle = "Launch file using TV default handler",
                            color = AccentCyan,
                            onClick = {
                                onOpenFileOnTv(fileItem)
                                selectedFileForMenu = null
                            }
                        )
                    }

                    // Copy Full TV Path
                    FileActionMenuItem(
                        icon = Icons.Default.ContentCopy,
                        title = "Copy TV Path",
                        subtitle = fileItem.path,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(fileItem.path))
                            selectedFileForMenu = null
                        }
                    )

                    // Delete File / Folder
                    FileActionMenuItem(
                        icon = Icons.Default.Delete,
                        title = if (fileItem.isDirectory) "Delete Folder" else "Delete File",
                        subtitle = "Permanently remove from TV storage",
                        color = Color(0xFFEF4444),
                        onClick = {
                            fileToDelete = fileItem
                            selectedFileForMenu = null
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedFileForMenu = null }) {
                    Text("Close", color = TextMuted)
                }
            }
        )
    }

    // 7. Confirm Delete Dialog
    fileToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            containerColor = DarkSurfaceRaised,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Delete ${if (item.isDirectory) "Folder" else "File"}?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${item.name}' from TV internal storage? This action cannot be undone.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFile(item)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // 8. New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = DarkSurfaceRaised,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(text = "Create New Folder", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Folder will be created inside:\n$currentPath",
                        color = TextMuted,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        placeholder = { Text("Folder Name", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = DpadBorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateFolder(newFolderName.trim())
                            showNewFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Create", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun InteractiveFileRow(
    item: TvFileItem,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val fileColor = getFileColor(item)
    val fileIcon = getFileIcon(item)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("file_row_${item.name}"),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DpadBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(fileColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = fileIcon,
                        contentDescription = null,
                        tint = fileColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        color = TextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.size.isNotEmpty()) {
                            Text(
                                text = item.size,
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (item.lastModified.isNotEmpty()) {
                                Text(
                                    text = " • ${item.lastModified}",
                                    color = TextMuted.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        } else {
                            Text(
                                text = if (item.isDirectory) "Folder" else "File",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Options 3-dots Menu
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FileActionMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color = TextPrimary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, color = color, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = TextMuted, fontSize = 10.5.sp, maxLines = 1)
            }
        }
    }
}

private fun getFileColor(item: TvFileItem): Color {
    if (item.isDirectory) return Color(0xFFFBBF24)
    return when (item.fileType) {
        TvFileType.APK -> AccentCyan
        TvFileType.VIDEO -> Color(0xFFF97316)
        TvFileType.AUDIO -> Color(0xFFA855F7)
        TvFileType.IMAGE -> Color(0xFF34D399)
        TvFileType.DOCUMENT -> Color(0xFF60A5FA)
        TvFileType.ARCHIVE -> Color(0xFFEC4899)
        TvFileType.OTHER, TvFileType.DIRECTORY -> Color(0xFF94A3B8)
    }
}

private fun getFileIcon(item: TvFileItem): ImageVector {
    if (item.isDirectory) return Icons.Default.Folder
    return when (item.fileType) {
        TvFileType.APK -> Icons.Default.Android
        TvFileType.VIDEO -> Icons.Default.Movie
        TvFileType.AUDIO -> Icons.Default.MusicNote
        TvFileType.IMAGE -> Icons.Default.Image
        TvFileType.DOCUMENT -> Icons.Default.Description
        TvFileType.ARCHIVE -> Icons.Default.Archive
        TvFileType.OTHER, TvFileType.DIRECTORY -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}
