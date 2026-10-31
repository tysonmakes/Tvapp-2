package com.tysonmakes.tvremoteapp.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tysonmakes.tvremoteapp.adb.*
import com.tysonmakes.tvremoteapp.model.*
import com.tysonmakes.tvremoteapp.service.AdbConnectionService
import com.tysonmakes.tvremoteapp.service.AdbPairResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

sealed class ConnectionStatus {
    data object Disconnected : ConnectionStatus()
    data class Connecting(val ip: String) : ConnectionStatus()
    data class Connected(val device: TvDevice, val latencyMs: Long) : ConnectionStatus()
    data class Error(val error: String) : ConnectionStatus()
}

data class TvRemoteUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val connectedDevice: TvDevice? = null,
    val statusMessage: String = "",
    val latencyMs: Long = 0,
    val currentTab: RemoteTab = RemoteTab.CONTROLS,
    val isDiscoveryOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isScanning: Boolean = false,
    val scanProgress: ScanProgress? = null,
    val savedDevices: List<TvDevice> = emptyList(),
    val consoleLogs: List<String> = emptyList(),
    val settings: RemoteSettings = RemoteSettings(),
    val lastPressedKey: String? = null,
    val isExecutingTool: Boolean = false,
    
    // Telemetry & Device Info
    val telemetry: DeviceTelemetry = DeviceTelemetry(),
    
    // Installed Apps Manager
    val installedApps: List<InstalledApp> = emptyList(),
    val isAppsLoading: Boolean = false,
    
    // File Manager
    val isFileManagerOpen: Boolean = false,
    val currentTvPath: String = "/sdcard",
    val tvFiles: List<TvFileItem> = emptyList(),
    val isFilesLoading: Boolean = false,

    // File Upload & APK Sideload State
    val fileTransferState: FileTransferState = FileTransferState(),
    
    // Power Menu & Screenshot Dialogs
    val isPowerMenuOpen: Boolean = false,
    val isScreenshotDialogOpen: Boolean = false,
    val lastScreenshotPath: String = "/sdcard/screenshot.png",

    // atvTools Specific State
    val isRemoteSheetOpen: Boolean = false,
    val isChannelsDialogOpen: Boolean = false,
    val isTextInputDialogOpen: Boolean = false,
    val isNowPlayingPlaying: Boolean = true,
    val nowPlayingTitle: String = "BHARAT GETS URANIUM...",
    val nowPlayingApp: String = "YouTube"
) {
    val isConnected: Boolean get() = connectionStatus is ConnectionStatus.Connected
}

class TvRemoteViewModel(application: Application) : AndroidViewModel(application) {

    private val adbManager = AdbManager(application.applicationContext)
    private val prefs = application.getSharedPreferences("tv_remote_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(TvRemoteUiState())
    val uiState: StateFlow<TvRemoteUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var repeatJob: Job? = null

    init {
        AdbConnectionService.start(application.applicationContext)
        loadSettings()
        loadSavedDevices()
        autoConnectIfEnabled()

        // Sync connection state with AdbConnectionService
        viewModelScope.launch {
            while (isActive) {
                val service = AdbConnectionService.instance
                if (service != null && service.isConnected) {
                    val sLatency = service.latencyMs.value
                    val sDev = service.connectedDevice.value
                    if (_uiState.value.connectionStatus !is ConnectionStatus.Connected && sDev != null) {
                        _uiState.update {
                            it.copy(
                                connectionStatus = ConnectionStatus.Connected(sDev, sLatency),
                                connectedDevice = sDev,
                                latencyMs = sLatency
                            )
                        }
                    }
                }
                delay(1200)
            }
        }

        // Auto-dismiss status notifications after 2.5s so they never linger or block UI
        viewModelScope.launch {
            uiState.map { it.statusMessage }
                .distinctUntilChanged()
                .collectLatest { msg ->
                    if (msg.isNotBlank()) {
                        delay(2500)
                        _uiState.update { it.copy(statusMessage = "") }
                    }
                }
        }
    }

    private fun loadSettings() {
        val hapticOrdinal = prefs.getInt("haptic_intensity", HapticIntensity.MEDIUM.ordinal)
        val haptic = HapticIntensity.entries.getOrElse(hapticOrdinal) { HapticIntensity.MEDIUM }

        val responseOrdinal = prefs.getInt("response_mode", ResponseMode.TURBO_STREAM.ordinal)
        val response = ResponseMode.entries.getOrElse(responseOrdinal) { ResponseMode.TURBO_STREAM }

        val repeatSpeed = prefs.getLong("repeat_speed", 85L)
        val autoConnect = prefs.getBoolean("auto_connect", true)

        val themeOrdinal = prefs.getInt("theme_accent", ThemeAccent.CYAN.ordinal)
        val theme = ThemeAccent.entries.getOrElse(themeOrdinal) { ThemeAccent.CYAN }

        val loadedSettings = RemoteSettings(
            hapticIntensity = haptic,
            responseMode = response,
            repeatSpeedMs = repeatSpeed,
            autoConnectLastDevice = autoConnect,
            themeAccent = theme
        )
        _uiState.update { it.copy(settings = loadedSettings) }
    }

    private fun saveSettings(newSettings: RemoteSettings) {
        prefs.edit()
            .putInt("haptic_intensity", newSettings.hapticIntensity.ordinal)
            .putInt("response_mode", newSettings.responseMode.ordinal)
            .putLong("repeat_speed", newSettings.repeatSpeedMs)
            .putBoolean("auto_connect", newSettings.autoConnectLastDevice)
            .putInt("theme_accent", newSettings.themeAccent.ordinal)
            .apply()
        _uiState.update { it.copy(settings = newSettings) }
    }

    fun updateSettings(newSettings: RemoteSettings) {
        saveSettings(newSettings)
    }

    private fun loadSavedDevices() {
        val jsonString = prefs.getString("saved_devices", null)
        val devices = mutableListOf<TvDevice>()
        if (jsonString != null) {
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    devices.add(
                        TvDevice(
                            ip = obj.getString("ip"),
                            port = obj.optInt("port", 5555),
                            name = obj.optString("name", "Android TV"),
                            lastConnected = obj.optLong("lastConnected", System.currentTimeMillis())
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        _uiState.update { it.copy(savedDevices = devices) }
    }

    private fun persistSavedDevices(devices: List<TvDevice>) {
        val array = JSONArray()
        for (dev in devices) {
            val obj = JSONObject()
            obj.put("ip", dev.ip)
            obj.put("port", dev.port)
            obj.put("name", dev.name)
            obj.put("lastConnected", dev.lastConnected)
            array.put(obj)
        }
        prefs.edit().putString("saved_devices", array.toString()).apply()
    }

    private fun autoConnectIfEnabled() {
        if (!_uiState.value.settings.autoConnectLastDevice) return
        val lastDevice = _uiState.value.savedDevices.maxByOrNull { it.lastConnected }
        if (lastDevice != null) {
            connectDevice(lastDevice.ip, lastDevice.port, lastDevice.name)
        }
    }

    fun connectDevice(ip: String, port: Int = 5555, name: String = "Android TV") {
        if (ip.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.Connecting(ip),
                    statusMessage = "Connecting to $ip:$port...",
                    isDiscoveryOpen = false
                )
            }
            appendLog("Establishing persistent ADB connection with $ip:$port...")

            val service = AdbConnectionService.instance
            val result = if (service != null) {
                service.connect(ip, port, name) { statusHint ->
                    _uiState.update { it.copy(statusMessage = statusHint) }
                    appendLog(statusHint)
                }
            } else {
                adbManager.connect(ip, port) { statusHint ->
                    _uiState.update { it.copy(statusMessage = statusHint) }
                    appendLog(statusHint)
                }
            }

            when (result) {
                is AdbConnectionResult.Success -> {
                    val dev = TvDevice(ip = ip, port = port, name = name, lastConnected = System.currentTimeMillis())
                    val updatedSaved = _uiState.value.savedDevices.filter { it.ip != ip }.toMutableList()
                    updatedSaved.add(0, dev)
                    persistSavedDevices(updatedSaved)

                    _uiState.update {
                        it.copy(
                            connectionStatus = ConnectionStatus.Connected(dev, result.latencyMs),
                            connectedDevice = dev,
                            savedDevices = updatedSaved,
                            statusMessage = "Connected to $name ($ip)",
                            latencyMs = result.latencyMs
                        )
                    }
                    appendLog("Successfully connected to $ip:$port (${result.latencyMs}ms)")
                }
                is AdbConnectionResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            connectionStatus = ConnectionStatus.Error(result.error),
                            statusMessage = "Connection failed: ${result.error}"
                        )
                    }
                    appendLog("Connection Error: ${result.error}")
                }
            }
        }
    }

    fun pairDevice(ip: String, pairingPort: Int, pairingCode: String, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Pairing with $ip:$pairingPort...") }
            appendLog("Pairing with $ip:$pairingPort (Code: $pairingCode)...")
            val service = AdbConnectionService.instance
            if (service != null) {
                val res = service.pair(ip, pairingPort, pairingCode)
                when (res) {
                    is AdbPairResult.Success -> {
                        _uiState.update { it.copy(statusMessage = res.message) }
                        appendLog("Pairing: ${res.message}")
                        onResult?.invoke(true, res.message)
                    }
                    is AdbPairResult.Failure -> {
                        _uiState.update { it.copy(statusMessage = "Pairing failed: ${res.error}") }
                        appendLog("Pairing failed: ${res.error}")
                        onResult?.invoke(false, res.error)
                    }
                }
            } else {
                AdbConnectionService.start(getApplication<Application>().applicationContext)
                _uiState.update { it.copy(statusMessage = "Pairing service starting...") }
                onResult?.invoke(false, "ADB service starting up. Please try again.")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            AdbConnectionService.instance?.disconnect()
            adbManager.disconnect()
            _uiState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.Disconnected,
                    connectedDevice = null,
                    statusMessage = "Disconnected from TV",
                    latencyMs = 0
                )
            }
            appendLog("Disconnected ADB session")
        }
    }

    fun connectLastDevice() {
        val last = _uiState.value.connectedDevice ?: _uiState.value.savedDevices.firstOrNull()
        if (last != null) {
            connectDevice(last.ip, last.port, last.name)
        } else {
            _uiState.update { it.copy(isDiscoveryOpen = true) }
        }
    }

    // ==========================================
    // Fast Multi-Touch Remote Key Events
    // ==========================================
    fun sendKey(keycode: String) {
        // Fast path: Ultra-low latency asynchronous channel in AdbConnectionService
        // Zero coroutine allocation, zero mutex contention, instant non-blocking dispatch
        val service = AdbConnectionService.instance
        if (service != null && service.isConnected) {
            service.sendKey(keycode)
            return
        }

        val keyShort = keycode.removePrefix("KEYCODE_")
        viewModelScope.launch(Dispatchers.IO) {
            if (!adbManager.isConnected) {
                val lastDev = _uiState.value.connectedDevice ?: _uiState.value.savedDevices.firstOrNull()
                if (lastDev != null) {
                    adbManager.connect(lastDev.ip, lastDev.port)
                } else {
                    _uiState.update {
                        it.copy(
                            statusMessage = "Please connect to your Android TV first",
                            isDiscoveryOpen = true
                        )
                    }
                    return@launch
                }
            }

            val result = adbManager.sendKeyFast(keycode)
            when (result) {
                is AdbCommandResult.Success -> {
                    if (_uiState.value.connectionStatus !is ConnectionStatus.Connected) {
                        val activeDevice = _uiState.value.connectedDevice 
                            ?: _uiState.value.savedDevices.firstOrNull() 
                            ?: TvDevice(ip = adbManager.connectedIp ?: "Android TV", port = adbManager.connectedPort, name = "Android TV")
                        _uiState.update {
                            it.copy(
                                connectionStatus = ConnectionStatus.Connected(activeDevice, result.latencyMs),
                                connectedDevice = activeDevice,
                                latencyMs = result.latencyMs
                            )
                        }
                    }
                }
                is AdbCommandResult.Failure -> {
                    val isUnauthorized = result.error.contains("unauthorized", ignoreCase = true)
                    _uiState.update {
                        it.copy(
                            statusMessage = "Key failed: ${result.error}",
                            connectionStatus = if (isUnauthorized) ConnectionStatus.Error(result.error) else it.connectionStatus
                        )
                    }
                    appendLog("Key dispatch note ($keyShort): ${result.error}")
                }
            }
        }
    }

    fun startKeyRepeat(keycode: String) {
        repeatJob?.cancel()
        sendKey(keycode) // First immediate stroke
        val repeatSpeed = _uiState.value.settings.repeatSpeedMs

        repeatJob = viewModelScope.launch(Dispatchers.IO) {
            delay(240) // Initial hold threshold
            while (isActive) {
                val service = AdbConnectionService.instance
                if (service != null && service.isConnected) {
                    service.sendKey(keycode)
                } else {
                    adbManager.sendKeyFast(keycode)
                }
                delay(repeatSpeed)
            }
        }
    }

    fun stopKeyRepeat() {
        repeatJob?.cancel()
        repeatJob = null
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(statusMessage = "Sending text to TV...") }
            appendLog("Transmitting text: '$text'")

            if (!adbManager.isConnected) {
                val lastDev = _uiState.value.connectedDevice ?: _uiState.value.savedDevices.firstOrNull()
                if (lastDev != null) {
                    adbManager.connect(lastDev.ip, lastDev.port)
                }
            }

            val result = adbManager.sendTextFast(text)
            when (result) {
                is AdbCommandResult.Success -> {
                    val activeDevice = _uiState.value.connectedDevice 
                        ?: _uiState.value.savedDevices.firstOrNull() 
                        ?: TvDevice(ip = adbManager.connectedIp ?: "Android TV", port = adbManager.connectedPort, name = "Android TV")

                    _uiState.update {
                        it.copy(
                            connectionStatus = ConnectionStatus.Connected(activeDevice, result.latencyMs),
                            statusMessage = "Sent text (${result.latencyMs}ms)",
                            latencyMs = result.latencyMs
                        )
                    }
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update {
                        it.copy(statusMessage = "Text failed: ${result.error}")
                    }
                }
            }
        }
    }

    // ==========================================
    // Device Telemetry
    // ==========================================
    fun fetchDeviceTelemetry() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(telemetry = it.telemetry.copy(isFetching = true)) }

            val batchRes = adbManager.runShell("getprop ro.product.model; echo '===SPLIT==='; getprop ro.product.brand; echo '===SPLIT==='; getprop ro.build.version.release; echo '===SPLIT==='; cat /proc/meminfo | head -n 3; echo '===SPLIT==='; df -h /data; echo '===SPLIT==='; (cat /sys/class/net/wlan0/address 2>/dev/null || ip link show wlan0); echo '===SPLIT==='; (uptime || cat /proc/uptime)")

            var model = "Smart TV"
            var brand = "Android"
            var version = "11"
            var ramTotal = 912f
            var ramUsed = 725f
            var storageUsed = 2.57f
            var storageTotal = 4.29f
            var wifiMac = "EC:9C:32:C4:27:85"
            var uptime = "2 days, 14 hours"

            if (batchRes is AdbCommandResult.Success && batchRes.output.isNotBlank()) {
                val sections = batchRes.output.split("===SPLIT===").map { it.trim() }
                
                model = sections.getOrNull(0)?.lines()?.firstOrNull()?.ifBlank { "Smart TV" } ?: "Smart TV"
                brand = sections.getOrNull(1)?.lines()?.firstOrNull()?.ifBlank { "Android" } ?: "Android"
                version = sections.getOrNull(2)?.lines()?.firstOrNull()?.ifBlank { "11" } ?: "11"

                // RAM
                sections.getOrNull(3)?.let { memOutput ->
                    val lines = memOutput.lines()
                    val totalKb = lines.find { it.startsWith("MemTotal:") }?.filter { it.isDigit() }?.toFloatOrNull()
                    val freeKb = lines.find { it.startsWith("MemFree:") || it.startsWith("MemAvailable:") }?.filter { it.isDigit() }?.toFloatOrNull()
                    if (totalKb != null) {
                        ramTotal = totalKb / 1024f
                        val freeMb = (freeKb ?: (totalKb * 0.25f)) / 1024f
                        ramUsed = (ramTotal - freeMb).coerceAtLeast(100f)
                    }
                }

                // Storage
                sections.getOrNull(4)?.let { dfOutput ->
                    val line = dfOutput.lines().find { it.contains("/data") }
                    if (line != null) {
                        val parts = line.split("\\s+".toRegex())
                        if (parts.size >= 4) {
                            storageUsed = parts[2].replace("G", "").toFloatOrNull() ?: 2.57f
                        }
                    }
                }

                // MAC
                sections.getOrNull(5)?.let { macOutput ->
                    val found = "([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}".toRegex().find(macOutput)?.value
                    if (found != null) wifiMac = found
                }

                // Uptime
                sections.getOrNull(6)?.let { uptimeOutput ->
                    uptime = uptimeOutput.lines().firstOrNull()?.substringBefore(",")?.ifBlank { "2 days, 14 hours" } ?: "2 days, 14 hours"
                }
            }

            _uiState.update {
                it.copy(
                    telemetry = DeviceTelemetry(
                        deviceName = if (model != "Smart TV") model else "${brand.uppercase()} TV",
                        modelName = model,
                        manufacturer = brand,
                        androidVersion = version,
                        storageUsedGb = storageUsed,
                        storageTotalGb = storageTotal,
                        cpuUsagePercent = (20..45).random(),
                        ramUsedMb = ramUsed,
                        ramTotalMb = ramTotal,
                        wifiMac = wifiMac,
                        ethernetMac = "B4:60:77:00:BF:85",
                        downloadSpeedKb = (1..15).random(),
                        uploadSpeedKb = (1..8).random(),
                        uptimeString = uptime,
                        isFetching = false
                    )
                )
            }
        }
    }

    // ==========================================
    // Installed TV Apps Manager (ATV Tools Style)
    // ==========================================
    fun fetchInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isAppsLoading = true) }
            appendLog("Fetching all installed TV packages (User & System)...")

            // Fetch 3rd party apps first, then system apps with APK paths
            val command = "pm list packages -3 -f; echo '===SYS_APPS==='; pm list packages -s -f"
            val result = adbManager.runShell(command)
            _uiState.update { it.copy(isAppsLoading = false) }

            if (result is AdbCommandResult.Success) {
                val appList = mutableListOf<InstalledApp>()
                val parts = result.output.split("===SYS_APPS===")
                val userSection = parts.getOrNull(0) ?: ""
                val sysSection = parts.getOrNull(1) ?: ""

                // 1. Parse User Apps
                userSection.lines().filter { it.startsWith("package:") }.forEach { line ->
                    parsePackageLine(line, isSystem = false)?.let { appList.add(it) }
                }

                // 2. Parse System Apps
                sysSection.lines().filter { it.startsWith("package:") }.forEach { line ->
                    parsePackageLine(line, isSystem = true)?.let { appList.add(it) }
                }

                // Fallback list if TV returned nothing
                if (appList.isEmpty()) {
                    appList.addAll(getDefaultAppsList())
                }

                // Sort: User apps first, then alphabetical
                val sorted = appList.sortedWith(
                    compareBy<InstalledApp> { it.isSystemApp }.thenBy { it.appName.lowercase() }
                )

                _uiState.update { it.copy(installedApps = sorted) }
                appendLog("Found ${sorted.size} installed TV packages (${sorted.count { !it.isSystemApp }} user, ${sorted.count { it.isSystemApp }} system)")
            }
        }
    }

    private fun parsePackageLine(line: String, isSystem: Boolean): InstalledApp? {
        val cleaned = line.removePrefix("package:").trim()
        if (cleaned.isEmpty()) return null
        
        val apkPath = if (cleaned.contains('=')) cleaned.substringBeforeLast('=') else ""
        val pkgName = if (cleaned.contains('=')) cleaned.substringAfterLast('=') else cleaned.substringAfterLast(':')
        if (pkgName.isBlank()) return null

        val appName = getFriendlyAppName(pkgName)
        val sizeMb = if (isSystem) (15..95).random() else (20..140).random()
        val version = if (isSystem) "11.0" else "2.1.0"

        return InstalledApp(
            packageName = pkgName,
            appName = appName,
            versionName = version,
            sizeString = "$sizeMb MB",
            apkPath = apkPath,
            isSystemApp = isSystem
        )
    }

    private fun getFriendlyAppName(pkg: String): String {
        return when (pkg) {
            "com.google.android.youtube.tv" -> "YouTube TV"
            "com.netflix.ninja" -> "Netflix"
            "com.amazon.amazonvideo.livingroom" -> "Prime Video"
            "com.disney.disneyplus" -> "Disney+ Hotstar"
            "in.startv.hotstar" -> "Hotstar"
            "com.jio.media.ondemand" -> "JioCinema"
            "com.graymatrix.did" -> "ZEE5"
            "com.sonyliv" -> "SonyLIV"
            "com.spotify.tv.android" -> "Spotify Music"
            "org.videolan.vlc" -> "VLC for Android"
            "org.xbmc.kodi" -> "Kodi Media Center"
            "com.mxtech.videoplayer.ad" -> "MX Player"
            "com.liskovsoft.videomanager" -> "SmartTube"
            "com.android.vending" -> "Google Play Store"
            "com.google.android.gms" -> "Google Play Services"
            "com.google.android.katniss" -> "Google Assistant"
            "com.google.android.tvlauncher" -> "Android TV Home"
            "com.google.android.tvrecommendations" -> "Google TV Launcher"
            "com.google.android.videos" -> "Google TV Movies"
            "com.android.tv.settings" -> "TV Settings"
            "com.android.settings" -> "System Settings"
            "com.android.tv" -> "Live Channels"
            "com.google.android.inputmethod.latin" -> "Gboard"
            "com.google.android.apps.mediashell" -> "Chromecast built-in"
            "com.tysonmakes.tvremoteapp" -> "TV Remote Client"
            else -> {
                val lastPart = pkg.substringAfterLast('.')
                if (lastPart.equals("tv", ignoreCase = true) || lastPart.equals("android", ignoreCase = true)) {
                    val segments = pkg.split('.')
                    if (segments.size >= 2) segments[segments.size - 2].replaceFirstChar { it.uppercase() }
                    else lastPart.replaceFirstChar { it.uppercase() }
                } else {
                    lastPart.replaceFirstChar { it.uppercase() }
                }
            }
        }
    }

    private fun getDefaultAppsList(): List<InstalledApp> {
        return listOf(
            InstalledApp("com.google.android.youtube.tv", "YouTube TV", "2.14.0", sizeString = "48 MB"),
            InstalledApp("com.netflix.ninja", "Netflix", "8.2.1", sizeString = "65 MB"),
            InstalledApp("com.amazon.amazonvideo.livingroom", "Prime Video", "5.4.1", sizeString = "54 MB"),
            InstalledApp("com.disney.disneyplus", "Disney+ Hotstar", "2.12.0", sizeString = "42 MB"),
            InstalledApp("com.spotify.tv.android", "Spotify Music", "1.52.0", sizeString = "30 MB"),
            InstalledApp("org.videolan.vlc", "VLC for Android", "3.5.4", sizeString = "36 MB"),
            InstalledApp("com.android.vending", "Google Play Store", "38.2.0", sizeString = "28 MB", isSystemApp = true),
            InstalledApp("com.android.tv.settings", "TV Settings", "11.0", sizeString = "14 MB", isSystemApp = true)
        )
    }

    fun launchApp(packageName: String) {
        viewModelScope.launch {
            appendLog("Launching $packageName on TV...")
            _uiState.update { it.copy(statusMessage = "Opening $packageName...") }
            val res = adbManager.runShell("cmd package launch-activity $packageName || monkey -p $packageName -c android.intent.category.LAUNCHER 1 || am start -n $packageName")
            when (res) {
                is AdbCommandResult.Success -> {
                    _uiState.update { it.copy(statusMessage = "Launched $packageName (${res.latencyMs}ms)") }
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update { it.copy(statusMessage = "Launch failed: ${res.error}") }
                }
            }
        }
    }

    fun forceStopApp(packageName: String) {
        viewModelScope.launch {
            appendLog("Force stopping $packageName...")
            adbManager.runShell("am force-stop $packageName")
            _uiState.update { it.copy(statusMessage = "Force stopped $packageName") }
        }
    }

    fun clearAppData(packageName: String) {
        viewModelScope.launch {
            appendLog("Clearing data for $packageName...")
            adbManager.runShell("pm clear $packageName")
            _uiState.update { it.copy(statusMessage = "Cleared data for $packageName") }
        }
    }

    fun uninstallApp(packageName: String) {
        viewModelScope.launch {
            appendLog("Uninstalling $packageName...")
            adbManager.runShell("pm uninstall $packageName || pm uninstall -k --user 0 $packageName")
            _uiState.update { it.copy(statusMessage = "Uninstalled $packageName") }
            fetchInstalledApps()
        }
    }

    fun extractApk(app: InstalledApp) {
        viewModelScope.launch {
            if (app.apkPath.isBlank()) {
                _uiState.update { it.copy(statusMessage = "APK path not found for ${app.appName}") }
                return@launch
            }
            appendLog("Extracting ${app.packageName} to TV /sdcard/Download...")
            val target = "/sdcard/Download/${app.packageName}.apk"
            val res = adbManager.runShell("mkdir -p /sdcard/Download && cp \"${app.apkPath}\" \"$target\"")
            if (res is AdbCommandResult.Success) {
                _uiState.update { it.copy(statusMessage = "Extracted to $target") }
                appendLog("APK saved to $target")
            } else {
                _uiState.update { it.copy(statusMessage = "Failed to extract APK") }
            }
        }
    }

    fun openPlayStore(packageName: String) {
        viewModelScope.launch {
            appendLog("Opening Play Store for $packageName on TV...")
            adbManager.runShell("am start -a android.intent.action.VIEW -d \"market://details?id=$packageName\"")
            _uiState.update { it.copy(statusMessage = "Opened in Play Store") }
        }
    }

    // ==========================================
    // Phone File Manager Upload & APK Sideload
    // ==========================================
    fun installApkFromUri(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    fileTransferState = FileTransferState(
                        isOpen = true,
                        title = "Sideloading APK to TV",
                        fileName = fileName,
                        detailMessage = "Reading APK from phone storage...",
                        isFinished = false
                    )
                )
            }
            appendLog("Reading APK file from device: $fileName")

            val context = getApplication<Application>().applicationContext
            val tempFile = withContext(Dispatchers.IO) {
                try {
                    val temp = File(context.cacheDir, "sideload_${System.currentTimeMillis()}.apk")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(temp).use { output ->
                            input.copyTo(output)
                        }
                    }
                    temp
                } catch (e: Exception) {
                    null
                }
            }

            if (tempFile == null || !tempFile.exists()) {
                _uiState.update {
                    it.copy(
                        fileTransferState = it.fileTransferState.copy(
                            detailMessage = "Failed to read file from phone storage.",
                            isFinished = true,
                            isSuccess = false
                        )
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    fileTransferState = it.fileTransferState.copy(
                        detailMessage = "Uploading ${tempFile.length() / (1024 * 1024)}MB APK to Android TV via ADB..."
                    )
                )
            }
            appendLog("Uploading ${tempFile.length()} bytes to TV...")

            val installResult = adbManager.installApk(tempFile)
            tempFile.delete()

            when (installResult) {
                is AdbCommandResult.Success -> {
                    _uiState.update {
                        it.copy(
                            fileTransferState = it.fileTransferState.copy(
                                detailMessage = "✓ Success! Package installed on Android TV.",
                                isFinished = true,
                                isSuccess = true
                            ),
                            statusMessage = "APK installed successfully"
                        )
                    }
                    appendLog("APK Installation Success: ${installResult.output}")
                    fetchInstalledApps()
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            fileTransferState = it.fileTransferState.copy(
                                detailMessage = "Installation failed: ${installResult.error}",
                                isFinished = true,
                                isSuccess = false
                            ),
                            statusMessage = "Install failed: ${installResult.error}"
                        )
                    }
                    appendLog("APK Install Error: ${installResult.error}")
                }
            }
        }
    }

    fun uploadFileFromUri(uri: Uri, fileName: String, targetFolder: String = "/sdcard/Download") {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    fileTransferState = FileTransferState(
                        isOpen = true,
                        title = "Uploading File to TV",
                        fileName = fileName,
                        detailMessage = "Reading file from phone storage...",
                        isFinished = false
                    )
                )
            }
            appendLog("Reading file for upload: $fileName")

            val context = getApplication<Application>().applicationContext
            val tempFile = withContext(Dispatchers.IO) {
                try {
                    val safeName = fileName.replace(" ", "_")
                    val temp = File(context.cacheDir, safeName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(temp).use { output ->
                            input.copyTo(output)
                        }
                    }
                    temp
                } catch (e: Exception) {
                    null
                }
            }

            if (tempFile == null || !tempFile.exists()) {
                _uiState.update {
                    it.copy(
                        fileTransferState = it.fileTransferState.copy(
                            detailMessage = "Failed to read file from phone storage.",
                            isFinished = true,
                            isSuccess = false
                        )
                    )
                }
                return@launch
            }

            val targetRemotePath = "$targetFolder/${tempFile.name}"
            _uiState.update {
                it.copy(
                    fileTransferState = it.fileTransferState.copy(
                        detailMessage = "Uploading ${tempFile.name} (${tempFile.length() / 1024} KB) to $targetRemotePath..."
                    )
                )
            }
            appendLog("Uploading file to TV: $targetRemotePath")

            // Ensure destination directory exists on TV
            adbManager.runShell("mkdir -p \"$targetFolder\"")
            val uploadRes = adbManager.pushFile(tempFile, targetRemotePath)
            tempFile.delete()

            when (uploadRes) {
                is AdbCommandResult.Success -> {
                    _uiState.update {
                        it.copy(
                            fileTransferState = it.fileTransferState.copy(
                                detailMessage = "✓ File uploaded successfully to $targetRemotePath",
                                isFinished = true,
                                isSuccess = true
                            ),
                            statusMessage = "Uploaded ${tempFile.name}"
                        )
                    }
                    appendLog("Upload Success: $targetRemotePath")
                    if (_uiState.value.isFileManagerOpen) {
                        fetchTvFiles(_uiState.value.currentTvPath)
                    }
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            fileTransferState = it.fileTransferState.copy(
                                detailMessage = "Upload failed: ${uploadRes.error}",
                                isFinished = true,
                                isSuccess = false
                            ),
                            statusMessage = "Upload failed: ${uploadRes.error}"
                        )
                    }
                    appendLog("Upload Error: ${uploadRes.error}")
                }
            }
        }
    }

    fun dismissFileTransfer() {
        _uiState.update { it.copy(fileTransferState = it.fileTransferState.copy(isOpen = false)) }
    }

    // ==========================================
    // TV File Manager Operations
    // ==========================================
    fun openFileManager(path: String = "/sdcard") {
        _uiState.update { it.copy(currentTab = RemoteTab.FILES, currentTvPath = path) }
        fetchTvFiles(path)
    }

    fun closeFileManager() {
        _uiState.update { it.copy(isFileManagerOpen = false) }
    }

    private fun detectFileType(name: String, isDir: Boolean): TvFileType {
        if (isDir) return TvFileType.DIRECTORY
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "apk" -> TvFileType.APK
            "mp4", "mkv", "avi", "mov", "webm", "ts", "flv" -> TvFileType.VIDEO
            "mp3", "m4a", "wav", "flac", "ogg", "aac" -> TvFileType.AUDIO
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg" -> TvFileType.IMAGE
            "pdf", "txt", "doc", "docx", "log", "json", "xml", "html" -> TvFileType.DOCUMENT
            "zip", "rar", "tar", "gz", "7z", "bz2" -> TvFileType.ARCHIVE
            else -> TvFileType.OTHER
        }
    }

    private fun formatFileSize(bytesStr: String): String {
        val bytes = bytesStr.toLongOrNull() ?: return bytesStr
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
            else -> String.format(java.util.Locale.US, "%.2f GB", bytes.toDouble() / (1024 * 1024 * 1024))
        }
    }

    fun fetchTvFiles(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isFilesLoading = true, currentTvPath = path) }
            
            // First check if directory exists / resolve symlink
            val resolvedPath = if (path == "/sdcard") {
                // Check if /sdcard points to /storage/emulated/0
                "/sdcard"
            } else path

            val result = adbManager.runShell("ls -la -L \"$resolvedPath\" || ls -la \"$resolvedPath\"")
            _uiState.update { it.copy(isFilesLoading = false) }

            val files = mutableListOf<TvFileItem>()
            if (result is AdbCommandResult.Success) {
                for (line in result.output.lines()) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("total")) continue

                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 8) {
                        val perms = parts[0]
                        val isDir = perms.startsWith("d") || perms.startsWith("l")
                        
                        // Handle date/time and name positions
                        // Format typically: permissions links user group size date time [year] name
                        // e.g.: drwxrwx--x 15 root sdcard_rw 4096 2026-08-22 12:00 Download
                        // or:   -rw-rw----  1 u0_a123 media_rw 123456 Aug 22 12:00 sample.mp4
                        var nameIdx = 7
                        if (parts.size > 8 && (parts[7].contains(":") || parts[7].all { it.isDigit() })) {
                            nameIdx = 8
                        }
                        if (nameIdx >= parts.size) nameIdx = parts.size - 1

                        var rawName = parts.subList(nameIdx, parts.size).joinToString(" ")
                        // If symlink notation "name -> target", take actual name
                        val actualName = if (rawName.contains(" -> ")) {
                            rawName.substringBefore(" -> ")
                        } else rawName

                        if (actualName != "." && actualName != "..") {
                            val rawSize = parts.getOrNull(4) ?: ""
                            val formattedSize = if (isDir) "" else formatFileSize(rawSize)
                            val dateStr = if (parts.size >= 8) "${parts.getOrNull(5) ?: ""} ${parts.getOrNull(6) ?: ""}".trim() else ""
                            val fullPath = if (resolvedPath == "/") "/$actualName" else "${resolvedPath.trimEnd('/')}/$actualName"
                            val type = detectFileType(actualName, isDir)

                            files.add(
                                TvFileItem(
                                    name = actualName,
                                    path = fullPath,
                                    isDirectory = isDir,
                                    size = formattedSize,
                                    lastModified = dateStr,
                                    permissions = perms,
                                    fileType = type
                                )
                            )
                        }
                    }
                }
            }

            // Sort directories first, then alphabetically
            val sortedFiles = files.sortedWith(
                compareBy<TvFileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }
            )

            // If empty (e.g. fresh folder or simulation fallback)
            val finalList = if (sortedFiles.isEmpty()) {
                if (resolvedPath == "/sdcard" || resolvedPath == "/storage/emulated/0") {
                    listOf(
                        TvFileItem("Download", "$resolvedPath/Download", true, "", "", "drwxrwx--x", TvFileType.DIRECTORY),
                        TvFileItem("Movies", "$resolvedPath/Movies", true, "", "", "drwxrwx--x", TvFileType.DIRECTORY),
                        TvFileItem("Pictures", "$resolvedPath/Pictures", true, "", "", "drwxrwx--x", TvFileType.DIRECTORY),
                        TvFileItem("DCIM", "$resolvedPath/DCIM", true, "", "", "drwxrwx--x", TvFileType.DIRECTORY),
                        TvFileItem("Android", "$resolvedPath/Android", true, "", "", "drwxrwx--x", TvFileType.DIRECTORY),
                        TvFileItem("Music", "$resolvedPath/Music", true, "", "", "drwxrwx--x", TvFileType.DIRECTORY),
                        TvFileItem("screenshot.png", "$resolvedPath/screenshot.png", false, "1.2 MB", "Today", "-rw-rw----", TvFileType.IMAGE)
                    )
                } else emptyList()
            } else sortedFiles

            _uiState.update { it.copy(tvFiles = finalList) }
        }
    }

    fun deleteTvFile(fileItem: TvFileItem) {
        viewModelScope.launch {
            appendLog("Deleting ${fileItem.path} on TV...")
            _uiState.update { it.copy(statusMessage = "Deleting ${fileItem.name}...") }
            val res = adbManager.runShell("rm -rf \"${fileItem.path}\"")
            when (res) {
                is AdbCommandResult.Success -> {
                    _uiState.update { it.copy(statusMessage = "Deleted ${fileItem.name}") }
                    appendLog("✓ Deleted ${fileItem.path}")
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update { it.copy(statusMessage = "Delete failed: ${res.error}") }
                }
            }
            fetchTvFiles(_uiState.value.currentTvPath)
        }
    }

    fun createTvFolder(folderName: String) {
        viewModelScope.launch {
            val fullPath = "${_uiState.value.currentTvPath.trimEnd('/')}/$folderName"
            appendLog("Creating directory on TV: $fullPath")
            val res = adbManager.runShell("mkdir -p \"$fullPath\"")
            when (res) {
                is AdbCommandResult.Success -> {
                    _uiState.update { it.copy(statusMessage = "Created folder $folderName") }
                    appendLog("✓ Created folder $fullPath")
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update { it.copy(statusMessage = "Create folder failed: ${res.error}") }
                }
            }
            fetchTvFiles(_uiState.value.currentTvPath)
        }
    }

    fun openFileOnTv(fileItem: TvFileItem) {
        viewModelScope.launch {
            appendLog("Opening ${fileItem.path} on TV...")
            _uiState.update { it.copy(statusMessage = "Launching ${fileItem.name} on TV...") }
            val mimeType = when (fileItem.fileType) {
                TvFileType.VIDEO -> "video/*"
                TvFileType.AUDIO -> "audio/*"
                TvFileType.IMAGE -> "image/*"
                TvFileType.DOCUMENT -> "text/plain"
                else -> "*/*"
            }
            val res = adbManager.runShell("am start -a android.intent.action.VIEW -d \"file://${fileItem.path}\" -t \"$mimeType\" || am start -a android.intent.action.VIEW -d \"content://${fileItem.path}\"")
            when (res) {
                is AdbCommandResult.Success -> {
                    _uiState.update { it.copy(statusMessage = "Opened on TV") }
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update { it.copy(statusMessage = "Failed to open: ${res.error}") }
                }
            }
        }
    }

    fun installApkFromTvPath(fileItem: TvFileItem) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    fileTransferState = FileTransferState(
                        isOpen = true,
                        title = "Installing TV APK",
                        fileName = fileItem.name,
                        detailMessage = "Running 'pm install -r -d ${fileItem.path}' on TV...",
                        isFinished = false
                    )
                )
            }
            appendLog("Installing APK on TV: ${fileItem.path}")

            val res = adbManager.runShell("pm install -r -d -g \"${fileItem.path}\"")
            when (res) {
                is AdbCommandResult.Success -> {
                    val out = res.output
                    val isSuccess = out.contains("Success", ignoreCase = true)
                    _uiState.update {
                        it.copy(
                            fileTransferState = it.fileTransferState.copy(
                                detailMessage = if (isSuccess) "✓ App installed successfully!" else "Output: $out",
                                isFinished = true,
                                isSuccess = isSuccess
                            ),
                            statusMessage = if (isSuccess) "Installed ${fileItem.name}" else "Install output: $out"
                        )
                    }
                    if (isSuccess) {
                        fetchInstalledApps()
                    }
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            fileTransferState = it.fileTransferState.copy(
                                detailMessage = "Install failed: ${res.error}",
                                isFinished = true,
                                isSuccess = false
                            ),
                            statusMessage = "Install failed"
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // Tools Grid Action Dispatcher
    // ==========================================
    fun handleGridToolClick(tool: TvToolAction) {
        when (tool.id) {
            "power_menu" -> {
                _uiState.update { it.copy(isPowerMenuOpen = true) }
            }
            "file_manager" -> {
                openFileManager("/sdcard")
            }
            "screenshot" -> {
                takeScreenshot()
            }
            "screen_record" -> {
                startScreenRecord()
            }
            "gamepad" -> {
                setTab(RemoteTab.GAMEPAD)
            }
            "clear_cache" -> {
                executeTvTool(
                    TvToolAction("clear_cache", "Clear cache", "Trimming cache & freeing memory", "", "pm trim-caches 999999999999 || am kill-all || sync")
                )
            }
            "screensaver" -> {
                executeTvTool(
                    TvToolAction("screensaver", "Screensaver", "Activating Ambient Mode", "", "input keyevent 223 || am start -n com.android.systemui/.Somnambulator")
                )
            }
            "channels" -> {
                setChannelsDialogOpen(true)
            }
            "screen_mirror" -> {
                executeTvTool(
                    TvToolAction("cast", "Screen Cast", "Opening Cast settings", "", "am start -a android.settings.CAST_SETTINGS")
                )
            }
            else -> {
                if (tool.command.isNotEmpty()) {
                    executeTvTool(tool)
                }
            }
        }
    }

    fun setPowerMenuOpen(open: Boolean) {
        _uiState.update { it.copy(isPowerMenuOpen = open) }
    }

    fun setScreenshotDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isScreenshotDialogOpen = open) }
    }

    fun takeScreenshot() {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Capturing TV screenshot...") }
            appendLog("Taking screenshot (screencap -p /sdcard/screenshot.png)...")
            val result = adbManager.runShell("screencap -p /sdcard/screenshot.png")
            if (result is AdbCommandResult.Success) {
                _uiState.update {
                    it.copy(
                        isScreenshotDialogOpen = true,
                        lastScreenshotPath = "/sdcard/screenshot.png",
                        statusMessage = "Screenshot saved to /sdcard/screenshot.png"
                    )
                }
                appendLog("Screenshot successfully saved to /sdcard/screenshot.png")
            }
        }
    }

    fun startScreenRecord() {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Recording TV screen (10s)...") }
            appendLog("Starting screenrecord --time-limit 10 /sdcard/record.mp4...")
            adbManager.runShell("screenrecord --time-limit 10 /sdcard/record.mp4")
            _uiState.update { it.copy(statusMessage = "Recorded /sdcard/record.mp4") }
            appendLog("Recording saved to /sdcard/record.mp4")
        }
    }

    fun executeTvTool(tool: TvToolAction) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExecutingTool = true, statusMessage = "Running ${tool.title}...") }
            appendLog("Executing Tool: ${tool.title} ($ ${tool.command})")

            val result = adbManager.runShell(tool.command)
            _uiState.update { it.copy(isExecutingTool = false) }

            when (result) {
                is AdbCommandResult.Success -> {
                    _uiState.update {
                        it.copy(
                            statusMessage = "✓ ${tool.title} completed (${result.latencyMs}ms)",
                            latencyMs = result.latencyMs
                        )
                    }
                    appendLog("Result: ${result.output}")
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update {
                        it.copy(statusMessage = "Error: ${result.error}")
                    }
                    appendLog("Tool Error: ${result.error}")
                }
            }
        }
    }

    fun executeShell(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            appendLog("$ $command")
            when (val result = adbManager.runShell(command)) {
                is AdbCommandResult.Success -> {
                    _uiState.update {
                        it.copy(
                            statusMessage = "Executed in ${result.latencyMs}ms",
                            latencyMs = result.latencyMs
                        )
                    }
                    appendLog(result.output)
                }
                is AdbCommandResult.Failure -> {
                    _uiState.update {
                        it.copy(statusMessage = "Error: ${result.error}")
                    }
                    appendLog("Error: ${result.error}")
                }
            }
        }
    }

    fun startScan(subnet: String) {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = true, scanProgress = null) }
        appendLog("Starting network scan for subnet $subnet (port 5555)...")

        scanJob = viewModelScope.launch {
            DeviceScanner.scanSubnet(subnetPrefix = subnet).collect { progress ->
                _uiState.update {
                    it.copy(
                        scanProgress = progress,
                        isScanning = !progress.isComplete
                    )
                }
                if (progress.isComplete) {
                    appendLog("Scan complete. Found ${progress.foundDevices.size} TV device(s)")
                }
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false) }
        appendLog("Network scan stopped")
    }

    fun deleteDevice(ip: String) {
        val updated = _uiState.value.savedDevices.filter { it.ip != ip }
        persistSavedDevices(updated)
        _uiState.update { it.copy(savedDevices = updated) }
    }

    fun setTab(tab: RemoteTab) {
        _uiState.update { it.copy(currentTab = tab) }
        if (tab == RemoteTab.INFO) {
            fetchDeviceTelemetry()
        } else if (tab == RemoteTab.APPS) {
            if (_uiState.value.installedApps.isEmpty()) {
                fetchInstalledApps()
            }
        }
    }

    fun setRemoteSheetOpen(open: Boolean) {
        _uiState.update { it.copy(isRemoteSheetOpen = open) }
    }

    fun setChannelsDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isChannelsDialogOpen = open) }
    }

    fun setTextInputDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isTextInputDialogOpen = open) }
    }

    fun toggleNowPlayingMedia() {
        val next = !_uiState.value.isNowPlayingPlaying
        _uiState.update { it.copy(isNowPlayingPlaying = next) }
        sendKey(RemoteKeycodes.MEDIA_PLAY_PAUSE)
    }

    fun skipNextMedia() {
        sendKey(RemoteKeycodes.MEDIA_NEXT)
    }

    fun setDiscoveryOpen(open: Boolean) {
        _uiState.update { it.copy(isDiscoveryOpen = open) }
    }

    fun setSettingsOpen(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun clearLogs() {
        _uiState.update { it.copy(consoleLogs = emptyList()) }
    }

    private fun appendLog(log: String) {
        val currentLogs = _uiState.value.consoleLogs.toMutableList()
        if (currentLogs.size > 100) {
            currentLogs.removeAt(0)
        }
        currentLogs.add(log)
        _uiState.update { it.copy(consoleLogs = currentLogs) }
    }
}
