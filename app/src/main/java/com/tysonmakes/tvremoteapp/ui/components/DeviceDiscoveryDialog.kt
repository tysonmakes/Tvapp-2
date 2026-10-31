package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tysonmakes.tvremoteapp.adb.ScanProgress
import com.tysonmakes.tvremoteapp.model.TvDevice
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun DeviceDiscoveryDialog(
    isOpen: Boolean,
    isScanning: Boolean,
    scanProgress: ScanProgress?,
    savedDevices: List<TvDevice>,
    onDismiss: () -> Unit,
    onStartScan: (String) -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (String, Int, String) -> Unit,
    onDeleteDevice: (String) -> Unit,
    onPairDevice: ((String, Int, String) -> Unit)? = null
) {
    if (!isOpen) return

    var manualIp by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("5555") }
    var manualName by remember { mutableStateOf("My TV") }
    var subnetPrefix by remember { mutableStateOf(com.tysonmakes.tvremoteapp.adb.DeviceScanner.getLocalSubnetPrefix()) }
    var showHelpGuide by remember { mutableStateOf(false) }

    // Wireless Debugging Pairing
    var pairingIp by remember { mutableStateOf("") }
    var pairingPort by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .testTag("device_discovery_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Connect TV Device",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_discovery_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Local Network Scanner
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceRaised),
                            modifier = Modifier.border(1.dp, DpadBorderColor, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Auto Scan Network (ADB Port 5555)",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = subnetPrefix,
                                        onValueChange = { subnetPrefix = it },
                                        label = { Text("Subnet Base", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("subnet_prefix_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentCyan,
                                            unfocusedBorderColor = DpadBorderColor,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (isScanning) {
                                                onStopScan()
                                            } else {
                                                onStartScan(subnetPrefix)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isScanning) AccentRed else AccentCyan,
                                            contentColor = DarkBackground
                                        ),
                                        modifier = Modifier.testTag("scan_network_button")
                                    ) {
                                        if (isScanning) {
                                            Text("Stop", fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Scan", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (isScanning && scanProgress != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LinearProgressIndicator(
                                        progress = { scanProgress.scannedCount.toFloat() / scanProgress.totalCount.coerceAtLeast(1) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = AccentCyan,
                                        trackColor = DarkBackground,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Scanning ${scanProgress.currentIp} (${scanProgress.scannedCount}/${scanProgress.totalCount})",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                if (scanProgress != null && scanProgress.foundDevices.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Found Devices (${scanProgress.foundDevices.size}):",
                                        color = AccentGreen,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    scanProgress.foundDevices.forEach { foundIp ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 6.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(DarkBackground)
                                                .clickable {
                                                    onConnectDevice(foundIp, 5555, "Android TV")
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(AccentGreen)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(foundIp, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                            }
                                            Text("Connect", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Manual IP Connection
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceRaised),
                            modifier = Modifier.border(1.dp, DpadBorderColor, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Manual Connection",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = manualIp,
                                        onValueChange = { manualIp = it },
                                        label = { Text("TV IP Address", fontSize = 11.sp) },
                                        placeholder = { Text("192.168.1.150", fontSize = 11.sp, color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(2f)
                                            .testTag("manual_ip_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentCyan,
                                            unfocusedBorderColor = DpadBorderColor,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = manualPort,
                                        onValueChange = { manualPort = it },
                                        label = { Text("Port", fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("manual_port_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentCyan,
                                            unfocusedBorderColor = DpadBorderColor,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val portInt = manualPort.toIntOrNull() ?: 5555
                                        if (manualIp.isNotBlank()) {
                                            onConnectDevice(manualIp.trim(), portInt, manualName.ifBlank { "Android TV" })
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("manual_connect_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentCyan,
                                        contentColor = DarkBackground
                                    )
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Connect via ADB", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 3. Wireless Debugging Pairing (Android 11+ / Fire TV)
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceRaised),
                            modifier = Modifier.border(1.dp, DpadBorderColor, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pair with 6-Digit Code (Android 11+)",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AccentCyan.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Wireless Debug", color = AccentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "TV Settings → Developer Options → Wireless Debugging → Pair device with pairing code",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = if (pairingIp.isNotBlank()) pairingIp else manualIp,
                                    onValueChange = { pairingIp = it },
                                    label = { Text("TV IP Address", fontSize = 11.sp) },
                                    placeholder = { Text("192.168.1.150", fontSize = 11.sp, color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentCyan,
                                        unfocusedBorderColor = DpadBorderColor,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = pairingPort,
                                        onValueChange = { pairingPort = it },
                                        label = { Text("Pairing Port", fontSize = 11.sp) },
                                        placeholder = { Text("e.g. 37821", fontSize = 11.sp, color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentCyan,
                                            unfocusedBorderColor = DpadBorderColor,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = pairingCode,
                                        onValueChange = { pairingCode = it },
                                        label = { Text("6-Digit Code", fontSize = 11.sp) },
                                        placeholder = { Text("123456", fontSize = 11.sp, color = TextMuted) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentCyan,
                                            unfocusedBorderColor = DpadBorderColor,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val targetIp = (if (pairingIp.isNotBlank()) pairingIp else manualIp).trim()
                                        val portInt = pairingPort.toIntOrNull() ?: 0
                                        if (targetIp.isNotBlank() && portInt > 0 && pairingCode.isNotBlank()) {
                                            onPairDevice?.invoke(targetIp, portInt, pairingCode.trim())
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentGreen,
                                        contentColor = DarkBackground
                                    )
                                ) {
                                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pair Device", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 4. Saved Devices
                    if (savedDevices.isNotEmpty()) {
                        item {
                            Text(
                                text = "Saved & Recent TVs",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        items(savedDevices) { dev ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceRaised)
                                    .border(1.dp, DpadBorderColor, RoundedCornerShape(12.dp))
                                    .clickable {
                                        onConnectDevice(dev.ip, dev.port, dev.name)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(dev.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("${dev.ip}:${dev.port}", color = TextSecondary, fontSize = 12.sp)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = { onConnectDevice(dev.ip, dev.port, dev.name) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan.copy(alpha = 0.15f), contentColor = AccentCyan),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Connect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = { onDeleteDevice(dev.ip) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 4. Pairing Guide / Help Accordion
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkBackground),
                            modifier = Modifier
                                .clickable { showHelpGuide = !showHelpGuide }
                                .border(1.dp, DpadBorderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("How to enable ADB on Android TV / Fire TV", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Icon(
                                        imageVector = if (showHelpGuide) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = TextMuted
                                    )
                                }

                                AnimatedVisibility(visible = showHelpGuide) {
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        Text(
                                            text = "1. On TV, go to Settings → About (or Device Info).\n" +
                                                   "2. Click 'Build Number' 7 times until Developer Mode is unlocked.\n" +
                                                   "3. Open Settings → Developer Options → Enable 'USB / Network Debugging' (Port 5555).\n" +
                                                   "4. When connecting, check TV screen and select 'Always allow from this computer'.",
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
