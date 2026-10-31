package com.tysonmakes.tvremoteapp.model

data class TvDevice(
    val ip: String,
    val port: Int = 5555,
    val name: String = "Android TV",
    val isOnline: Boolean = true,
    val lastConnected: Long = System.currentTimeMillis()
) {
    val fullAddress: String get() = "$ip:$port"
}

enum class RemoteTab(val title: String) {
    CONTROLS("Remote"),
    FILES("Files"),
    APPS("Apps"),
    TOOLS("Tools"),
    GAMEPAD("Gamepad"),
    INFO("Info"),
    TRACKPAD("Touchpad"),
    NUMPAD("Numpad"),
    TERMINAL("Shell")
}

enum class HapticIntensity(val label: String) {
    OFF("Off"),
    SUBTLE("Light"),
    MEDIUM("Medium"),
    STRONG("Strong")
}

enum class ResponseMode(val label: String, val description: String) {
    TURBO_STREAM("⚡ Turbo ADB (<20ms)", "Instant Native C++ Binder input injection"),
    STANDARD("Standard ADB (50-100ms)", "Standard socket execution with fallback")
}

enum class ThemeAccent(val label: String, val primaryHex: Long) {
    CYAN("Electric Teal", 0xFF00E5FF),
    SAGE("Emerald Sage", 0xFF385C41),
    AMBER("Fire Amber", 0xFFFF9100),
    PURPLE("Neon Violet", 0xFFA855F7),
    CRIMSON("Ruby Red", 0xFFEF4444)
}

enum class AppCategoryFilter(val label: String) {
    USER("User Apps"),
    SYSTEM("System Apps"),
    ALL("All Apps")
}

data class RemoteSettings(
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val responseMode: ResponseMode = ResponseMode.TURBO_STREAM,
    val repeatSpeedMs: Long = 85L,
    val autoConnectLastDevice: Boolean = true,
    val themeAccent: ThemeAccent = ThemeAccent.CYAN
)

data class DeviceTelemetry(
    val deviceName: String = "Family Room TV",
    val androidVersion: String = "11",
    val modelName: String = "Android TV",
    val manufacturer: String = "Google",
    val storageUsedGb: Float = 2.57f,
    val storageTotalGb: Float = 4.29f,
    val cpuUsagePercent: Int = 34,
    val ramUsedMb: Float = 725f,
    val ramTotalMb: Float = 912f,
    val wifiMac: String = "EC:9C:32:C4:27:85",
    val ethernetMac: String = "B4:60:77:00:BF:85",
    val downloadSpeedKb: Int = 2,
    val uploadSpeedKb: Int = 5,
    val uptimeString: String = "2 days, 14 hours",
    val isFetching: Boolean = false
)

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val versionName: String = "1.0",
    val versionCode: String = "",
    val sizeString: String = "25 MB",
    val apkPath: String = "",
    val isSystemApp: Boolean = false,
    val isEnabled: Boolean = true
)

enum class TvFileType {
    DIRECTORY,
    APK,
    VIDEO,
    AUDIO,
    IMAGE,
    DOCUMENT,
    ARCHIVE,
    OTHER
}

data class TvFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: String = "",
    val lastModified: String = "",
    val permissions: String = "",
    val fileType: TvFileType = TvFileType.OTHER
)

data class TvToolAction(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val command: String = "",
    val colorHex: Long = 0xFF00E5FF
)

data class FileTransferState(
    val isOpen: Boolean = false,
    val title: String = "",
    val fileName: String = "",
    val detailMessage: String = "",
    val isFinished: Boolean = false,
    val isSuccess: Boolean = true,
    val progress: Float = 0f
)

object RemoteKeycodes {
    const val DPAD_UP = "KEYCODE_DPAD_UP"
    const val DPAD_DOWN = "KEYCODE_DPAD_DOWN"
    const val DPAD_LEFT = "KEYCODE_DPAD_LEFT"
    const val DPAD_RIGHT = "KEYCODE_DPAD_RIGHT"
    const val DPAD_CENTER = "KEYCODE_DPAD_CENTER"
    const val BACK = "KEYCODE_BACK"
    const val HOME = "KEYCODE_HOME"
    const val MENU = "KEYCODE_MENU"
    const val POWER = "KEYCODE_POWER"
    const val SLEEP = "KEYCODE_SLEEP"
    const val WAKEUP = "KEYCODE_WAKEUP"
    const val VOLUME_UP = "KEYCODE_VOLUME_UP"
    const val VOLUME_DOWN = "KEYCODE_VOLUME_DOWN"
    const val VOLUME_MUTE = "KEYCODE_VOLUME_MUTE"
    const val MEDIA_PLAY_PAUSE = "KEYCODE_MEDIA_PLAY_PAUSE"
    const val MEDIA_PLAY = "KEYCODE_MEDIA_PLAY"
    const val MEDIA_PAUSE = "KEYCODE_MEDIA_PAUSE"
    const val MEDIA_STOP = "KEYCODE_MEDIA_STOP"
    const val MEDIA_NEXT = "KEYCODE_MEDIA_NEXT"
    const val MEDIA_PREVIOUS = "KEYCODE_MEDIA_PREVIOUS"
    const val MEDIA_FAST_FORWARD = "KEYCODE_MEDIA_FAST_FORWARD"
    const val MEDIA_REWIND = "KEYCODE_MEDIA_REWIND"
    const val CHANNEL_UP = "KEYCODE_CHANNEL_UP"
    const val CHANNEL_DOWN = "KEYCODE_CHANNEL_DOWN"
    const val TV_INPUT = "KEYCODE_TV_INPUT"
    const val SETTINGS = "KEYCODE_SETTINGS"
    const val SEARCH = "KEYCODE_SEARCH"
    const val VOICE_ASSIST = "KEYCODE_VOICE_ASSIST"
    const val NOTIFICATION = "KEYCODE_NOTIFICATION"
    const val APP_SWITCH = "KEYCODE_APP_SWITCH"

    // Gamepad controller buttons
    const val BUTTON_A = "KEYCODE_BUTTON_A"
    const val BUTTON_B = "KEYCODE_BUTTON_B"
    const val BUTTON_X = "KEYCODE_BUTTON_X"
    const val BUTTON_Y = "KEYCODE_BUTTON_Y"
    const val BUTTON_L1 = "KEYCODE_BUTTON_L1"
    const val BUTTON_R1 = "KEYCODE_BUTTON_R1"
    const val BUTTON_START = "KEYCODE_BUTTON_START"
    const val BUTTON_SELECT = "KEYCODE_BUTTON_SELECT"

    // Numpad & Colors
    const val NUM_0 = "KEYCODE_0"
    const val NUM_1 = "KEYCODE_1"
    const val NUM_2 = "KEYCODE_2"
    const val NUM_3 = "KEYCODE_3"
    const val NUM_4 = "KEYCODE_4"
    const val NUM_5 = "KEYCODE_5"
    const val NUM_6 = "KEYCODE_6"
    const val NUM_7 = "KEYCODE_7"
    const val NUM_8 = "KEYCODE_8"
    const val NUM_9 = "KEYCODE_9"

    const val PROG_RED = "KEYCODE_PROG_RED"
    const val PROG_GREEN = "KEYCODE_PROG_GREEN"
    const val PROG_YELLOW = "KEYCODE_PROG_YELLOW"
    const val PROG_BLUE = "KEYCODE_PROG_BLUE"
}

object KeycodeMapper {
    fun toNumeric(keycode: String): Int {
        return when (keycode) {
            RemoteKeycodes.DPAD_UP -> 19
            RemoteKeycodes.DPAD_DOWN -> 20
            RemoteKeycodes.DPAD_LEFT -> 21
            RemoteKeycodes.DPAD_RIGHT -> 22
            RemoteKeycodes.DPAD_CENTER -> 23
            RemoteKeycodes.BACK -> 4
            RemoteKeycodes.HOME -> 3
            RemoteKeycodes.MENU -> 82
            RemoteKeycodes.POWER -> 26
            RemoteKeycodes.SLEEP -> 223
            RemoteKeycodes.WAKEUP -> 224
            RemoteKeycodes.VOLUME_UP -> 24
            RemoteKeycodes.VOLUME_DOWN -> 25
            RemoteKeycodes.VOLUME_MUTE -> 164
            RemoteKeycodes.MEDIA_PLAY_PAUSE -> 85
            RemoteKeycodes.MEDIA_PLAY -> 126
            RemoteKeycodes.MEDIA_PAUSE -> 127
            RemoteKeycodes.MEDIA_STOP -> 86
            RemoteKeycodes.MEDIA_NEXT -> 87
            RemoteKeycodes.MEDIA_PREVIOUS -> 88
            RemoteKeycodes.MEDIA_FAST_FORWARD -> 90
            RemoteKeycodes.MEDIA_REWIND -> 89
            RemoteKeycodes.CHANNEL_UP -> 166
            RemoteKeycodes.CHANNEL_DOWN -> 167
            RemoteKeycodes.TV_INPUT -> 178
            RemoteKeycodes.SETTINGS -> 176
            RemoteKeycodes.SEARCH -> 84
            RemoteKeycodes.VOICE_ASSIST -> 231
            RemoteKeycodes.NOTIFICATION -> 83
            RemoteKeycodes.APP_SWITCH -> 187
            RemoteKeycodes.BUTTON_A -> 96
            RemoteKeycodes.BUTTON_B -> 97
            RemoteKeycodes.BUTTON_X -> 99
            RemoteKeycodes.BUTTON_Y -> 100
            RemoteKeycodes.BUTTON_L1 -> 102
            RemoteKeycodes.BUTTON_R1 -> 103
            RemoteKeycodes.BUTTON_START -> 108
            RemoteKeycodes.BUTTON_SELECT -> 109
            RemoteKeycodes.NUM_0 -> 7
            RemoteKeycodes.NUM_1 -> 8
            RemoteKeycodes.NUM_2 -> 9
            RemoteKeycodes.NUM_3 -> 10
            RemoteKeycodes.NUM_4 -> 11
            RemoteKeycodes.NUM_5 -> 12
            RemoteKeycodes.NUM_6 -> 13
            RemoteKeycodes.NUM_7 -> 14
            RemoteKeycodes.NUM_8 -> 15
            RemoteKeycodes.NUM_9 -> 16
            RemoteKeycodes.PROG_RED -> 183
            RemoteKeycodes.PROG_GREEN -> 184
            RemoteKeycodes.PROG_YELLOW -> 185
            RemoteKeycodes.PROG_BLUE -> 186
            else -> {
                if (keycode.all { it.isDigit() }) keycode.toIntOrNull() ?: 23
                else 23
            }
        }
    }
}

val ATV_GRID_TOOLS = listOf(
    TvToolAction("install_apk", "Install APK", "Pick APK from phone & sideload to TV", "install", colorHex = 0xFF00E5FF),
    TvToolAction("upload_file", "Upload to Downloads", "Send phone files to /sdcard/Download", "upload", colorHex = 0xFF38BDF8),
    TvToolAction("file_manager", "File Manager", "Explore TV internal storage", "folder", colorHex = 0xFFFBBF24),
    TvToolAction("channels", "Channels", "Live TV channel selector", "live_tv", colorHex = 0xFFA78BFA),
    TvToolAction("screen_mirror", "Screen Cast", "Wireless casting settings", "cast", colorHex = 0xFF34D399),
    TvToolAction("gamepad", "Gamepad", "Virtual controller for TV games", "gamepad", colorHex = 0xFFEC4899),
    TvToolAction("screenshot", "Screenshot", "Instant TV display grab & view", "screenshot", colorHex = 0xFF06B6D4),
    TvToolAction("screen_record", "Screen Record", "Capture screen recording (10s)", "videocam", colorHex = 0xFFF97316),
    TvToolAction("clear_cache", "Boost Cache", "Trim RAM & optimize speed", "delete", colorHex = 0xFFEF4444),
    TvToolAction("screensaver", "Screensaver", "Trigger Ambient Mode / Daydream", "auto_awesome", colorHex = 0xFFA855F7),
    TvToolAction("power_menu", "Power Menu", "Sleep, Reboot, Soft Reboot, Power", "power_settings_new", colorHex = 0xFFF43F5E)
)

data class QuickTvApp(
    val name: String,
    val packageName: String,
    val iconName: String,
    val colorHex: Long
)

val QUICK_TV_APPS = listOf(
    QuickTvApp("YouTube", "com.google.android.youtube.tv", "youtube", 0xFFFF0000),
    QuickTvApp("Netflix", "com.netflix.ninja", "netflix", 0xFFE50914),
    QuickTvApp("Prime", "com.amazon.amazonvideo.livingroom", "prime", 0xFF00A8E1),
    QuickTvApp("Disney+", "com.disney.disneyplus", "disney", 0xFF113CCF),
    QuickTvApp("Spotify", "com.spotify.tv.android", "spotify", 0xFF1DB954),
    QuickTvApp("Twitch", "tv.twitch.android.app", "twitch", 0xFF9146FF),
    QuickTvApp("Plex", "com.plexapp.android", "plex", 0xFFE5A00D),
    QuickTvApp("Store", "com.android.vending", "store", 0xFF00875F)
)
