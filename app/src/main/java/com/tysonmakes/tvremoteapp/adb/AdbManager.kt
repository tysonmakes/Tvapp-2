package com.tysonmakes.tvremoteapp.adb

import android.content.Context
import com.tysonmakes.tvremoteapp.model.KeycodeMapper
import com.tysonmakes.tvremoteapp.model.RemoteKeycodes
import dadb.AdbKeyPair
import dadb.AdbShellStream
import dadb.Dadb
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.IOException

sealed class AdbConnectionResult {
    data class Success(val message: String, val latencyMs: Long) : AdbConnectionResult()
    data class Failure(val error: String) : AdbConnectionResult()
}

sealed class AdbCommandResult {
    data class Success(val output: String, val latencyMs: Long) : AdbCommandResult()
    data class Failure(val error: String) : AdbCommandResult()
}

sealed class AdbSessionState {
    data object Disconnected : AdbSessionState()
    data class Connecting(val ip: String, val port: Int) : AdbSessionState()
    data class Connected(val ip: String, val port: Int, val latencyMs: Long) : AdbSessionState()
    data class Error(val message: String) : AdbSessionState()
}

/**
 * AdbManager handles TCP-based ADB connections to Android TV and Fire TV devices,
 * maintains active session keep-alive heartbeats to prevent connection drops,
 * and provides high-level helper functions to send key codes and commands.
 */
class AdbManager(private val context: Context) {
    @Volatile
    private var dadb: Dadb? = null
    private val adbMutex = Mutex()

    // Persistent interactive shell stream for instant, near-zero latency key/text injection
    @Volatile
    private var persistentShell: AdbShellStream? = null
    private var shellDrainJob: Job? = null

    // Dedicated fast mutex for key/text events so navigation is never blocked by heavy commands
    private val keyDispatchMutex = Mutex()

    // Coroutine scope dedicated to session keep-alive heartbeats and background monitor
    private val keepAliveScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var keepAliveJob: Job? = null

    private val _sessionState = MutableStateFlow<AdbSessionState>(AdbSessionState.Disconnected)
    val sessionState: StateFlow<AdbSessionState> = _sessionState.asStateFlow()

    var connectedIp: String? = null
        private set
    var connectedPort: Int = 5555
        private set
    var lastTargetIp: String? = null
        private set
    var lastTargetPort: Int = 5555
        private set

    val isConnected: Boolean
        get() = dadb != null

    private fun getOrCreateKeyPair(): AdbKeyPair {
        val dir = context.filesDir
        val privFile = File(dir, "adbkey")
        val pubFile = File(dir, "adbkey.pub")
        if (!privFile.exists() || !pubFile.exists()) {
            AdbKeyPair.generate(privFile, pubFile)
        }
        return AdbKeyPair.read(privFile, pubFile)
    }

    private fun createDadbInstance(ip: String, port: Int): Dadb {
        val keyPair = getOrCreateKeyPair()
        // connectTimeout=10000ms, socketTimeout=0 (infinite / no read timeout on idle stream), keepAlive=true (TCP SO_KEEPALIVE)
        return Dadb.create(ip, port, keyPair, 10000, 0, true)
    }

    private fun getOrCreatePersistentShell(instance: Dadb): AdbShellStream {
        val current = persistentShell
        if (current != null) {
            return current
        }
        val newStream = instance.openShell("")
        persistentShell = newStream
        shellDrainJob?.cancel()
        shellDrainJob = keepAliveScope.launch {
            try {
                while (isActive) {
                    newStream.read() // Drains packet buffer continuously so writes never block
                }
            } catch (_: Exception) {
                // Shell closed or completed
            }
        }
        return newStream
    }

    private fun closePersistentShellInternal() {
        shellDrainJob?.cancel()
        shellDrainJob = null
        try {
            persistentShell?.close()
        } catch (_: Exception) {}
        persistentShell = null
    }

    // ==========================================
    // Connection Management & Keep-Alive
    // ==========================================

    suspend fun connect(
        ip: String,
        port: Int = 5555,
        onStatusUpdate: ((String) -> Unit)? = null
    ): AdbConnectionResult = withContext(Dispatchers.IO) {
        adbMutex.withLock {
            val startTime = System.currentTimeMillis()
            lastTargetIp = ip
            lastTargetPort = port
            _sessionState.value = AdbSessionState.Connecting(ip, port)

            try {
                cleanupSessionInternal()

                val instance = createDadbInstance(ip, port)

                // Dadb connects lazily on first socket operation. Verify connection actively.
                var verified = false
                var lastErrorMessage: String? = null
                val deadline = System.currentTimeMillis() + 25000L
                var authPromptShown = false

                while (System.currentTimeMillis() < deadline && !verified) {
                    try {
                        val probe = withTimeout(3000L) {
                            instance.shell("echo ping")
                        }
                        if (probe.exitCode == 0 || probe.output.contains("ping")) {
                            verified = true
                            break
                        } else {
                            val allOut = probe.allOutput.trim()
                            if (allOut.contains("unauthorized", ignoreCase = true)) {
                                onStatusUpdate?.invoke("Accept 'Allow USB Debugging' on TV screen (tick 'Always allow')...")
                            }
                            lastErrorMessage = allOut
                        }
                    } catch (e: Exception) {
                        val msg = e.localizedMessage?.lowercase() ?: ""
                        if (msg.contains("unauthorized") || msg.contains("auth") || msg.contains("failed to read response") || msg.contains("broken pipe") || msg.contains("connection reset")) {
                            if (!authPromptShown) {
                                onStatusUpdate?.invoke("Accept 'Allow USB Debugging' prompt on your TV screen...")
                                authPromptShown = true
                            }
                            lastErrorMessage = "Device unauthorized or key rejected. Please check TV screen and select 'Always allow'."
                        } else if (msg.contains("refused") || msg.contains("unreachable") || msg.contains("timed out")) {
                            lastErrorMessage = "Cannot connect to $ip:$port (${e.localizedMessage}). Make sure TV is ON and Wireless/USB Debugging is enabled."
                            break
                        } else {
                            lastErrorMessage = e.localizedMessage
                        }
                    }
                    delay(1200L)
                }

                if (!verified) {
                    cleanupSessionInternal()
                    val errorMsg = lastErrorMessage ?: "Failed to verify connection to $ip:$port. Make sure TV is awake."
                    _sessionState.value = AdbSessionState.Error(errorMsg)
                    return@withLock AdbConnectionResult.Failure(errorMsg)
                }

                dadb = instance
                connectedIp = ip
                connectedPort = port

                // Warm up the persistent interactive shell stream immediately upon connection
                try {
                    val stream = getOrCreatePersistentShell(instance)
                    stream.write("echo ready\n")
                } catch (e: Exception) {
                    android.util.Log.w("AdbManager", "Pre-warming persistent shell note: ${e.message}")
                }

                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                _sessionState.value = AdbSessionState.Connected(ip, port, latency)

                // Start active background keep-alive loop to prevent device from closing idle socket
                startKeepAliveJob()

                AdbConnectionResult.Success("Connected to $ip:$port", latency)
            } catch (e: TimeoutCancellationException) {
                cleanupSessionInternal()
                val timeoutMsg = "Connection timed out. Check TV screen for 'Allow USB Debugging' prompt and accept."
                _sessionState.value = AdbSessionState.Error(timeoutMsg)
                AdbConnectionResult.Failure(timeoutMsg)
            } catch (e: Exception) {
                cleanupSessionInternal()
                val failMsg = e.localizedMessage ?: "Failed to connect to $ip:$port"
                _sessionState.value = AdbSessionState.Error(failMsg)
                AdbConnectionResult.Failure(failMsg)
            }
        }
    }

    /**
     * Periodically sends low-overhead heartbeats to keep the ADB session alive
     * and prevent Android TV / Fire TV power management from terminating the socket.
     */
    private fun startKeepAliveJob() {
        keepAliveJob?.cancel()
        keepAliveJob = keepAliveScope.launch {
            while (isActive) {
                delay(18000L) // Heartbeat every 18 seconds (under 30s idle timeout)
                if (dadb == null) break

                try {
                    val stream = persistentShell
                    if (stream != null) {
                        // Lightweight comment write into shell stream acts as active TCP activity
                        stream.write("# ping\n")
                    } else {
                        val activeDadb = dadb
                        if (activeDadb != null) {
                            activeDadb.shell("echo 1")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AdbManager", "Keep-alive ping failed, attempting reconnect: ${e.message}")
                    tryReconnectInternal()
                }
            }
        }
    }

    private fun stopKeepAliveJob() {
        keepAliveJob?.cancel()
        keepAliveJob = null
    }

    private suspend fun tryReconnectInternal(): Boolean {
        val ip = lastTargetIp ?: connectedIp ?: return false
        val port = lastTargetPort
        return try {
            closePersistentShellInternal()
            try { dadb?.close() } catch (_: Exception) {}
            val instance = createDadbInstance(ip, port)
            val probe = withTimeout(5000L) { instance.shell("echo 1") }
            if (probe.exitCode == 0 || probe.output.contains("1")) {
                dadb = instance
                connectedIp = ip
                connectedPort = port
                try {
                    val stream = getOrCreatePersistentShell(instance)
                    stream.write("echo ready\n")
                } catch (_: Exception) {}
                _sessionState.value = AdbSessionState.Connected(ip, port, 5)
                true
            } else {
                _sessionState.value = AdbSessionState.Disconnected
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("AdbManager", "tryReconnectInternal failed to $ip:$port: ${e.message}")
            _sessionState.value = AdbSessionState.Disconnected
            false
        }
    }

    private fun cleanupSessionInternal() {
        stopKeepAliveJob()
        closePersistentShellInternal()
        try {
            dadb?.close()
        } catch (_: Exception) {}
        dadb = null
        connectedIp = null
        _sessionState.value = AdbSessionState.Disconnected
    }

    suspend fun disconnect(): AdbConnectionResult = withContext(Dispatchers.IO) {
        try {
            withTimeout(2000L) {
                adbMutex.withLock {
                    cleanupSessionInternal()
                    AdbConnectionResult.Success("Disconnected", 0)
                }
            }
        } catch (_: Exception) {
            cleanupSessionInternal()
            AdbConnectionResult.Success("Disconnected", 0)
        }
    }

    // ==========================================
    // Key Event Dispatchers & Helpers
    // ==========================================

    /**
     * Sends an Android key event by integer keycode (e.g. 19 for UP, 23 for CENTER, 4 for BACK).
     */
    suspend fun sendKey(keycode: Int): AdbCommandResult {
        return sendKeyFast(keycode.toString())
    }

    /**
     * Sends an Android key event by string name or number (e.g. "KEYCODE_DPAD_UP" or "DPAD_UP" or "19").
     */
    suspend fun sendKey(keycode: String): AdbCommandResult {
        return sendKeyFast(keycode)
    }

    /**
     * Ultra-fast, real-time key event dispatcher.
     * Uses persistent interactive shell stream (write command directly to stream)
     * achieving near-zero latency (<5-15ms) without per-key socket teardown.
     * Smoothly recovers stream/socket if the TV drops or suspends connection.
     */
    suspend fun sendKeyFast(keycode: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val targetKey = if (keycode.all { it.isDigit() }) {
            keycode
        } else if (keycode.startsWith("KEYCODE_")) {
            keycode
        } else {
            "KEYCODE_$keycode"
        }
        val command = "input keyevent $targetKey\n"
        val startTime = System.currentTimeMillis()

        try {
            withTimeout(2000L) {
                keyDispatchMutex.withLock {
                    val currentDadb = dadb ?: return@withLock AdbCommandResult.Failure("Not connected to TV")

                    // Attempt 1: Fast write directly to persistent shell stream (<5ms)
                    val stream = persistentShell
                    if (stream != null) {
                        try {
                            stream.write(command)
                            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                            return@withLock AdbCommandResult.Success("OK", latency)
                        } catch (e: Exception) {
                            closePersistentShellInternal()
                        }
                    }

                    // Attempt 2: Direct non-interactive shell command (10-25ms)
                    try {
                        currentDadb.shell("input keyevent $targetKey")
                        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                        // Pre-warm persistent shell in background
                        keepAliveScope.launch {
                            try { getOrCreatePersistentShell(currentDadb) } catch (_: Exception) {}
                        }
                        return@withLock AdbCommandResult.Success("OK", latency)
                    } catch (e: Exception) {
                        closePersistentShellInternal()
                        // Fast reconnect attempt if socket closed
                        if (tryReconnectInternal()) {
                            dadb?.shell("input keyevent $targetKey")
                            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                            return@withLock AdbCommandResult.Success("OK", latency)
                        } else {
                            return@withLock AdbCommandResult.Failure("Key dispatch failed: ${e.localizedMessage}")
                        }
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            AdbCommandResult.Failure("TV response timed out")
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "Key error")
        }
    }

    // High-Level Directional & Navigation Helpers
    suspend fun sendDpadUp(): AdbCommandResult = sendKeyFast(RemoteKeycodes.DPAD_UP)
    suspend fun sendDpadDown(): AdbCommandResult = sendKeyFast(RemoteKeycodes.DPAD_DOWN)
    suspend fun sendDpadLeft(): AdbCommandResult = sendKeyFast(RemoteKeycodes.DPAD_LEFT)
    suspend fun sendDpadRight(): AdbCommandResult = sendKeyFast(RemoteKeycodes.DPAD_RIGHT)
    suspend fun sendDpadCenter(): AdbCommandResult = sendKeyFast(RemoteKeycodes.DPAD_CENTER)
    suspend fun sendEnter(): AdbCommandResult = sendKeyFast(RemoteKeycodes.DPAD_CENTER)
    suspend fun sendBack(): AdbCommandResult = sendKeyFast(RemoteKeycodes.BACK)
    suspend fun sendHome(): AdbCommandResult = sendKeyFast(RemoteKeycodes.HOME)
    suspend fun sendMenu(): AdbCommandResult = sendKeyFast(RemoteKeycodes.MENU)
    suspend fun sendPower(): AdbCommandResult = sendKeyFast(RemoteKeycodes.POWER)
    suspend fun sendWakeup(): AdbCommandResult = sendKeyFast(RemoteKeycodes.WAKEUP)
    suspend fun sendSleep(): AdbCommandResult = sendKeyFast(RemoteKeycodes.SLEEP)

    // Volume & Media Helpers
    suspend fun volumeUp(): AdbCommandResult = sendKeyFast(RemoteKeycodes.VOLUME_UP)
    suspend fun volumeDown(): AdbCommandResult = sendKeyFast(RemoteKeycodes.VOLUME_DOWN)
    suspend fun volumeMute(): AdbCommandResult = sendKeyFast(RemoteKeycodes.VOLUME_MUTE)
    suspend fun mediaPlayPause(): AdbCommandResult = sendKeyFast(RemoteKeycodes.MEDIA_PLAY_PAUSE)
    suspend fun mediaPlay(): AdbCommandResult = sendKeyFast(RemoteKeycodes.MEDIA_PLAY)
    suspend fun mediaPause(): AdbCommandResult = sendKeyFast(RemoteKeycodes.MEDIA_PAUSE)
    suspend fun mediaStop(): AdbCommandResult = sendKeyFast(RemoteKeycodes.MEDIA_STOP)
    suspend fun mediaNext(): AdbCommandResult = sendKeyFast(RemoteKeycodes.MEDIA_NEXT)
    suspend fun mediaPrevious(): AdbCommandResult = sendKeyFast(RemoteKeycodes.MEDIA_PREVIOUS)
    suspend fun mediaFastForward(): AdbCommandResult = sendKeyFast(RemoteKeycodes.MEDIA_FAST_FORWARD)
    suspend fun mediaRewind(): AdbCommandResult = sendKeyFast(RemoteKeycodes.MEDIA_REWIND)

    // Channel & TV Input Helpers
    suspend fun channelUp(): AdbCommandResult = sendKeyFast(RemoteKeycodes.CHANNEL_UP)
    suspend fun channelDown(): AdbCommandResult = sendKeyFast(RemoteKeycodes.CHANNEL_DOWN)
    suspend fun tvInput(): AdbCommandResult = sendKeyFast(RemoteKeycodes.TV_INPUT)

    // Numpad Helper
    suspend fun sendNumpadDigit(digit: Int): AdbCommandResult {
        val safeDigit = digit.coerceIn(0, 9)
        return sendKeyFast("KEYCODE_$safeDigit")
    }

    // ==========================================
    // Command Execution & Shell Helpers
    // ==========================================

    /**
     * Executes an arbitrary ADB shell command on the connected device.
     */
    suspend fun executeCommand(command: String): AdbCommandResult = runShell(command)

    /**
     * Executes a shell command with automatic session recovery if the connection was interrupted.
     */
    suspend fun runShell(command: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            withTimeout(15000L) {
                adbMutex.withLock {
                    var activeDadb = dadb
                    if (activeDadb == null) {
                        if (tryReconnectInternal()) {
                            activeDadb = dadb
                        } else {
                            return@withLock AdbCommandResult.Failure("Not connected to TV")
                        }
                    }

                    try {
                        val response = activeDadb!!.shell(command)
                        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                        val output = response.output.trim()
                        val resultText = if (output.isNotEmpty()) output else "OK"
                        AdbCommandResult.Success(resultText, latency)
                    } catch (e: Exception) {
                        if (tryReconnectInternal()) {
                            try {
                                val retryResponse = dadb!!.shell(command)
                                val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                                val output = retryResponse.output.trim()
                                val resultText = if (output.isNotEmpty()) output else "OK"
                                AdbCommandResult.Success(resultText, latency)
                            } catch (retryEx: Exception) {
                                AdbCommandResult.Failure(retryEx.localizedMessage ?: "Command execution failed")
                            }
                        } else {
                            AdbCommandResult.Failure(e.localizedMessage ?: "Command execution failed")
                        }
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            AdbCommandResult.Failure("Shell command timed out")
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "Command error")
        }
    }

    /**
     * Sends formatted text into the currently active input field on the device.
     */
    suspend fun sendText(text: String): AdbCommandResult = sendTextFast(text)

    /**
     * Fast text sender with shell metacharacter and whitespace escaping.
     */
    suspend fun sendTextFast(text: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val escaped = buildString {
            for (ch in text) {
                when (ch) {
                    ' ' -> append("%s")
                    '\\', '"', '\'', '$', '`', '&', '|', ';', '<', '>', '(', ')', '*', '?', '~', '#', '!', '{', '}' -> {
                        append('\\').append(ch)
                    }
                    else -> append(ch)
                }
            }
        }
        val command = "input text \"$escaped\"\n"
        val startTime = System.currentTimeMillis()

        try {
            withTimeout(2500L) {
                keyDispatchMutex.withLock {
                    val currentDadb = dadb ?: return@withLock AdbCommandResult.Failure("Not connected to TV")

                    // Attempt 1: Fast write directly to persistent shell stream
                    val stream = persistentShell
                    if (stream != null) {
                        try {
                            stream.write(command)
                            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                            return@withLock AdbCommandResult.Success("OK", latency)
                        } catch (e: Exception) {
                            closePersistentShellInternal()
                        }
                    }

                    // Attempt 2: Direct dadb shell command
                    try {
                        currentDadb.shell("input text \"$escaped\"")
                        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                        AdbCommandResult.Success("OK", latency)
                    } catch (e: Exception) {
                        closePersistentShellInternal()
                        AdbCommandResult.Failure(e.localizedMessage ?: "Text dispatch failed")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            AdbCommandResult.Failure("TV response timed out")
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "Text error")
        }
    }

    // Touch & Swipe Input Helpers
    suspend fun sendTap(x: Int, y: Int): AdbCommandResult {
        return runShell("input tap $x $y")
    }

    suspend fun sendSwipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long = 300): AdbCommandResult {
        return runShell("input swipe $x1 $y1 $x2 $y2 $durationMs")
    }

    // Application Lifecycle Helpers
    suspend fun launchApp(packageName: String): AdbCommandResult {
        return runShell("cmd package launch-activity $packageName || monkey -p $packageName -c android.intent.category.LAUNCHER 1 || am start -n $packageName")
    }

    suspend fun stopApp(packageName: String): AdbCommandResult {
        return runShell("am force-stop $packageName")
    }

    suspend fun clearAppData(packageName: String): AdbCommandResult {
        return runShell("pm clear $packageName")
    }

    suspend fun uninstallApp(packageName: String): AdbCommandResult {
        return runShell("pm uninstall $packageName || pm uninstall -k --user 0 $packageName")
    }

    // System Control Helpers
    suspend fun reboot(mode: String = ""): AdbCommandResult {
        val cmd = if (mode.isBlank()) "reboot" else "reboot $mode"
        return runShell(cmd)
    }

    suspend fun takeScreenshot(remotePath: String = "/sdcard/screenshot.png"): AdbCommandResult {
        return runShell("screencap -p \"$remotePath\"")
    }

    // File Transfer & Package Installation Helpers
    suspend fun pushFile(localFile: File, remotePath: String): AdbCommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            withTimeout(60000L) {
                adbMutex.withLock {
                    var activeDadb = dadb
                    if (activeDadb == null) {
                        if (tryReconnectInternal()) {
                            activeDadb = dadb
                        } else {
                            return@withLock AdbCommandResult.Failure("Not connected to TV")
                        }
                    }

                    try {
                        activeDadb!!.push(localFile, remotePath)
                        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                        AdbCommandResult.Success("Uploaded to $remotePath", latency)
                    } catch (e: Exception) {
                        AdbCommandResult.Failure(e.localizedMessage ?: "File upload failed")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            AdbCommandResult.Failure("Upload timed out")
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "Upload error")
        }
    }

    suspend fun installApk(localApkFile: File): AdbCommandResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            withTimeout(120000L) {
                adbMutex.withLock {
                    var activeDadb = dadb
                    if (activeDadb == null) {
                        if (tryReconnectInternal()) {
                            activeDadb = dadb
                        } else {
                            return@withLock AdbCommandResult.Failure("Not connected to TV")
                        }
                    }

                    try {
                        try {
                            activeDadb!!.install(localApkFile)
                            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                            AdbCommandResult.Success("Success: App installed on TV", latency)
                        } catch (_: Exception) {
                            val safeFileName = "install_${System.currentTimeMillis()}.apk"
                            val tmpRemotePath = "/data/local/tmp/$safeFileName"
                            activeDadb!!.push(localApkFile, tmpRemotePath)
                            val installRes = activeDadb!!.shell("pm install -r -d -g \"$tmpRemotePath\" || pm install -r --user 0 \"$tmpRemotePath\"")
                            activeDadb!!.shell("rm -f \"$tmpRemotePath\"")
                            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                            if (installRes.output.contains("Success", ignoreCase = true)) {
                                AdbCommandResult.Success("Success: App installed on TV", latency)
                            } else {
                                val out = installRes.output.trim()
                                if (out.isBlank()) {
                                    AdbCommandResult.Success("Success: App installed on TV", latency)
                                } else {
                                    AdbCommandResult.Failure("Install result: $out")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        AdbCommandResult.Failure(e.localizedMessage ?: "APK installation failed")
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            AdbCommandResult.Failure("APK install timed out")
        } catch (e: Exception) {
            AdbCommandResult.Failure(e.localizedMessage ?: "APK install error")
        }
    }
}

