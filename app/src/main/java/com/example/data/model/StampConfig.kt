package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.ShutterGold
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.TelemetryEmerald
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class StampTemplate(val displayName: String, val subtitle: String) {
    TACTICAL_DARK("Advance Dark", "Translucent dark telemetry card with mini-map"),
    CLASSIC_WHITE("Classic Field", "High-contrast light survey report card"),
    MINIMAL_BAR("Minimal Bar", "Sleek low-profile coordinate strip"),
    CORNER_BADGE("Corner Badge", "Compact logo emblem + mini-map badge")
}

enum class StampLayoutTemplate(val displayName: String, val subtitle: String) {
    ADVANCE("Advance Template", "Full customizable map, telemetry, weather & logo card"),
    SCAN_LOCATION("Scan Location Template", "Scannable Google Maps QR code & location card"),
    DATETIME("DateTime Template", "Hero digital clock, date split & coordinates"),
    CLASSIC("Classic Template", "Clean bright survey card with mini-map & telemetry"),
    REPORTING("Reporting Template", "Check In status badge, inspector & site coordinates"),
    NAVIGATIONAL_COMPASS("Navigational Compass", "Azimuth compass dial with facing direction & telemetry")
}

enum class FileNameField(val displayName: String) {
    DATE_TIME("Date & Time"),
    SEQUENCE("Sequence Number"),
    CUSTOM_1("Custom Name 1"),
    CUSTOM_2("Custom Name 2"),
    CUSTOM_3("Custom Name 3"),
    FULL_ADDRESS("Full Address"),
    LAT_LON("Latitude/Longitude"),
    PLUS_CODE("Plus Code"),
    TIME_ZONE("Time Zone"),
    NOTE("Note")
}

enum class StampFont(val displayName: String) {
    SPACE_GROTESK("Geometric"),
    JETBRAINS_MONO("Telemetry Mono"),
    PLUS_JAKARTA_SANS("Clean Sans")
}

enum class StampAccentColor(
    val displayName: String,
    val color: Color,
    val argbInt: Int
) {
    EMERALD("Emerald", TelemetryEmerald, 0xFF10E09A.toInt()),
    YV_GOLD("YV Gold", ShutterGold, 0xFFF5C518.toInt()),
    CYAN("Sky Cyan", SignalCyan, 0xFF38BDF8.toInt()),
    AMBER("Signal Amber", SignalAmber, 0xFFFFB020.toInt()),
    WHITE("Pure White", InkPrimary, 0xFFF0F6FA.toInt()),
    CORAL("Survey Coral", AlertCoral, 0xFFFF5A5F.toInt())
}

enum class StampSize(val displayName: String, val scaleFactor: Float) {
    COMPACT("Compact", 0.82f),
    MEDIUM("Standard", 1.0f),
    LARGE("Large", 1.22f)
}

enum class StampPosition(val displayName: String, val shortLabel: String) {
    TOP_LEFT("Top Left", "TL"),
    TOP_CENTER("Top Center", "TC"),
    TOP_RIGHT("Top Right", "TR"),
    BOTTOM_LEFT("Bottom Left", "BL"),
    BOTTOM_CENTER("Bottom Center", "BC"),
    BOTTOM_RIGHT("Bottom Right", "BR")
}

enum class CoordinateFormat(val displayName: String) {
    DECIMAL_DEGREES("Decimal (DD)"),
    DMS("Deg Min Sec (DMS)")
}

enum class DateTimeFormat(val displayName: String) {
    READABLE_12H("12h Readable"),
    ISO_LOCAL("24h ISO"),
    COMPACT_24H("Compact 24h")
}

enum class MapStyle(val displayName: String, val shortLabel: String) {
    STANDARD("Normal", "STD"),
    SATELLITE("Satellite", "SAT"),
    TERRAIN("Terrain", "TER"),
    HYBRID("Hybrid", "HYB")
}

enum class AppThemeMode(val displayName: String) {
    DARK("Dark Mode"),
    LIGHT("Light Mode"),
    SYSTEM("System Default")
}

enum class PhotoQualityLevel(val displayName: String, val jpegQuality: Int) {
    HIGH("High (95%)", 95),
    MEDIUM("Medium (85%)", 85),
    STANDARD("Standard (75%)", 75)
}

enum class VideoQualityLevel(val displayName: String) {
    HD_720P("720p HD"),
    FHD_1080P("1080p Full HD"),
    UHD_4K("4K UHD")
}

enum class VolumeButtonAction(val displayName: String) {
    PHOTO_CAPTURE("Photo Capture"),
    ZOOM("Zoom +/-"),
    OFF("Off")
}

enum class ScreenTapAction(val displayName: String) {
    FOCUS("Tap to Focus"),
    CAPTURE("Tap to Capture"),
    OFF("Off")
}

enum class StampOrientationMode(val displayName: String) {
    AUTOMATIC("Automatic"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
}

@Serializable
data class ManualLocationItem(
    val id: String,
    val shortName: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double
)

private val configJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

data class StampConfig(
    val template: StampTemplate = StampTemplate.TACTICAL_DARK,
    val layoutTemplate: StampLayoutTemplate = StampLayoutTemplate.ADVANCE,
    val reportingLabel: String = "Check In",
    val stampFont: StampFont = StampFont.PLUS_JAKARTA_SANS,
    val accentColor: StampAccentColor = StampAccentColor.YV_GOLD,
    val stampSize: StampSize = StampSize.MEDIUM,
    val stampPosition: StampPosition = StampPosition.BOTTOM_CENTER,
    val customBrandLabel: String = "YVGPSCAM",
    val coordinateFormat: CoordinateFormat = CoordinateFormat.DECIMAL_DEGREES,
    val dateTimeFormat: DateTimeFormat = DateTimeFormat.READABLE_12H,
    val mapStyle: MapStyle = MapStyle.STANDARD,
    val offlineOnlyMapMode: Boolean = false,
    val showLogo: Boolean = true,
    val showMiniMap: Boolean = true,
    val showShortAddress: Boolean = true,
    val showAddress: Boolean = true,
    val showCountryFlag: Boolean = true,
    val showCoordinates: Boolean = true,
    val showPlusCode: Boolean = true,
    val showDateTime: Boolean = true,
    val showTimeZone: Boolean = false,
    val showNumbering: Boolean = false,
    val photoSequenceNumber: Int = 1,
    val showNote: Boolean = true,
    val showAltitude: Boolean = true,
    val showCompassAndSpeed: Boolean = true,
    val showAccuracy: Boolean = true,
    val showMagneticField: Boolean = true,
    val showPressure: Boolean = false,
    val showWeather: Boolean = true,
    val showPersonName: Boolean = false,
    val personName: String = "",
    val customNote: String = "",
    val stampOpacity: Float = 0.88f,
    val logoOpacity: Float = 1.0f,
    // Custom File Name Builder settings
    val customFileNameEnabled: Boolean = true,
    val fnIncludeDateTime: Boolean = true,
    val fnIncludeSequence: Boolean = true,
    val fnIncludeCustom1: Boolean = true,
    val fnCustomText1: String = "YVGPSCAM",
    val fnIncludeCustom2: Boolean = false,
    val fnCustomText2: String = "",
    val fnIncludeCustom3: Boolean = false,
    val fnCustomText3: String = "",
    val fnIncludeFullAddress: Boolean = false,
    val fnIncludeLatLon: Boolean = true,
    val fnIncludePlusCode: Boolean = false,
    val fnIncludeTimeZone: Boolean = false,
    val fnIncludeNote: Boolean = false,
    val fnFieldOrderSerialized: String = FileNameField.entries.joinToString(",") { it.name },
    // App Settings & Camera Tools
    val appLanguage: String = "English",
    val appThemeMode: AppThemeMode = AppThemeMode.DARK,
    val photoQuality: PhotoQualityLevel = PhotoQualityLevel.HIGH,
    val videoQuality: VideoQualityLevel = VideoQualityLevel.FHD_1080P,
    val volumeButtonAction: VolumeButtonAction = VolumeButtonAction.PHOTO_CAPTURE,
    val screenTapAction: ScreenTapAction = ScreenTapAction.FOCUS,
    val qrDetectionEnabled: Boolean = true,
    val stampOrientation: StampOrientationMode = StampOrientationMode.AUTOMATIC,
    val showCameraIndicator: Boolean = false,
    val autoSaveOriginalPhoto: Boolean = false,
    val emailAfterCapture: Boolean = false,
    val defaultReportEmail: String = "",
    val autoShareAfterCapture: Boolean = false,
    val showReportsModeOnCamera: Boolean = true,
    val mirrorFrontCamera: Boolean = true,
    val recordVideoWithVoice: Boolean = true,
    val showDigitalLevel: Boolean = false,
    val timerDelaySeconds: Int = 0,
    // Manual Location Overrides
    val useManualLocation: Boolean = false,
    val selectedManualLocationId: String? = null,
    val manualLocationsJson: String = "[]"
) {
    fun parsedFnFieldOrder(): List<FileNameField> {
        val parsed = fnFieldOrderSerialized
            .split(",")
            .mapNotNull { token -> runCatching { FileNameField.valueOf(token.trim()) }.getOrNull() }
            .distinct()
        val missing = FileNameField.entries.filterNot { parsed.contains(it) }
        return parsed + missing
    }

    fun parsedManualLocations(): List<ManualLocationItem> =
        runCatching { configJson.decodeFromString<List<ManualLocationItem>>(manualLocationsJson) }
            .getOrDefault(emptyList())

    fun selectedManualLocation(): ManualLocationItem? {
        if (!useManualLocation || selectedManualLocationId.isNullOrBlank()) return null
        return parsedManualLocations().firstOrNull { it.id == selectedManualLocationId }
    }

    companion object {
        fun serializeFnFieldOrder(order: List<FileNameField>): String {
            val clean = order.distinct()
            val missing = FileNameField.entries.filterNot { clean.contains(it) }
            return (clean + missing).joinToString(",") { it.name }
        }

        fun serializeManualLocations(items: List<ManualLocationItem>): String =
            runCatching { configJson.encodeToString(items) }.getOrDefault("[]")
    }
}
