package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppThemeMode
import com.example.data.model.CoordinateFormat
import com.example.data.model.DateTimeFormat
import com.example.data.model.FileNameField
import com.example.data.model.MapStyle
import com.example.data.model.PhotoQualityLevel
import com.example.data.model.ScreenTapAction
import com.example.data.model.StampAccentColor
import com.example.data.model.StampConfig
import com.example.data.model.StampFont
import com.example.data.model.StampLayoutTemplate
import com.example.data.model.StampOrientationMode
import com.example.data.model.StampPosition
import com.example.data.model.StampSize
import com.example.data.model.StampTemplate
import com.example.data.model.VideoQualityLevel
import com.example.data.model.VolumeButtonAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.stampDataStore: DataStore<Preferences> by preferencesDataStore(name = "yvgpscam_stamp_prefs")

class StampPreferencesRepository(private val context: Context) {

    private object Keys {
        val TEMPLATE = stringPreferencesKey("template")
        val LAYOUT_TEMPLATE = stringPreferencesKey("layout_template")
        val REPORTING_LABEL = stringPreferencesKey("reporting_label")
        val STAMP_FONT = stringPreferencesKey("stamp_font")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val STAMP_SIZE = stringPreferencesKey("stamp_size")
        val STAMP_POSITION = stringPreferencesKey("stamp_position")
        val CUSTOM_BRAND_LABEL = stringPreferencesKey("custom_brand_label")
        val COORDINATE_FORMAT = stringPreferencesKey("coordinate_format")
        val DATETIME_FORMAT = stringPreferencesKey("datetime_format")
        val MAP_STYLE = stringPreferencesKey("map_style")
        val OFFLINE_ONLY_MAP = booleanPreferencesKey("offline_only_map")
        val SHOW_LOGO = booleanPreferencesKey("show_logo")
        val SHOW_MINI_MAP = booleanPreferencesKey("show_mini_map")
        val SHOW_SHORT_ADDRESS = booleanPreferencesKey("show_short_address")
        val SHOW_ADDRESS = booleanPreferencesKey("show_address")
        val SHOW_COUNTRY_FLAG = booleanPreferencesKey("show_country_flag")
        val SHOW_COORDINATES = booleanPreferencesKey("show_coordinates")
        val SHOW_PLUS_CODE = booleanPreferencesKey("show_plus_code")
        val SHOW_DATETIME = booleanPreferencesKey("show_datetime")
        val SHOW_TIMEZONE = booleanPreferencesKey("show_timezone")
        val SHOW_NUMBERING = booleanPreferencesKey("show_numbering")
        val PHOTO_SEQUENCE_NUMBER = intPreferencesKey("photo_sequence_number")
        val SHOW_NOTE = booleanPreferencesKey("show_note")
        val SHOW_ALTITUDE = booleanPreferencesKey("show_altitude")
        val SHOW_COMPASS_SPEED = booleanPreferencesKey("show_compass_speed")
        val SHOW_ACCURACY = booleanPreferencesKey("show_accuracy")
        val SHOW_MAGNETIC = booleanPreferencesKey("show_magnetic")
        val SHOW_PRESSURE = booleanPreferencesKey("show_pressure")
        val SHOW_WEATHER = booleanPreferencesKey("show_weather")
        val SHOW_PERSON_NAME = booleanPreferencesKey("show_person_name")
        val PERSON_NAME = stringPreferencesKey("person_name")
        val CUSTOM_NOTE = stringPreferencesKey("custom_note")
        val STAMP_OPACITY = floatPreferencesKey("stamp_opacity")
        val LOGO_OPACITY = floatPreferencesKey("logo_opacity")

        // File Name Config
        val FN_ENABLED = booleanPreferencesKey("fn_enabled")
        val FN_DATETIME = booleanPreferencesKey("fn_datetime")
        val FN_SEQUENCE = booleanPreferencesKey("fn_sequence")
        val FN_CUSTOM_1 = booleanPreferencesKey("fn_custom_1")
        val FN_CUSTOM_1_TEXT = stringPreferencesKey("fn_custom_1_text")
        val FN_CUSTOM_2 = booleanPreferencesKey("fn_custom_2")
        val FN_CUSTOM_2_TEXT = stringPreferencesKey("fn_custom_2_text")
        val FN_CUSTOM_3 = booleanPreferencesKey("fn_custom_3")
        val FN_CUSTOM_3_TEXT = stringPreferencesKey("fn_custom_3_text")
        val FN_FULL_ADDRESS = booleanPreferencesKey("fn_full_address")
        val FN_LAT_LON = booleanPreferencesKey("fn_lat_lon")
        val FN_PLUS_CODE = booleanPreferencesKey("fn_plus_code")
        val FN_TIMEZONE = booleanPreferencesKey("fn_timezone")
        val FN_NOTE = booleanPreferencesKey("fn_note")
        val FN_FIELD_ORDER = stringPreferencesKey("fn_field_order")

        // Settings & Tools
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val PHOTO_QUALITY = stringPreferencesKey("photo_quality")
        val VIDEO_QUALITY = stringPreferencesKey("video_quality")
        val VOLUME_BUTTON_ACTION = stringPreferencesKey("volume_button_action")
        val SCREEN_TAP_ACTION = stringPreferencesKey("screen_tap_action")
        val QR_DETECTION_ENABLED = booleanPreferencesKey("qr_detection_enabled")
        val STAMP_ORIENTATION = stringPreferencesKey("stamp_orientation")
        val SHOW_CAMERA_INDICATOR = booleanPreferencesKey("show_camera_indicator")
        val AUTO_SAVE_ORIGINAL = booleanPreferencesKey("auto_save_original")
        val EMAIL_AFTER_CAPTURE = booleanPreferencesKey("email_after_capture")
        val DEFAULT_REPORT_EMAIL = stringPreferencesKey("default_report_email")
        val AUTO_SHARE_AFTER_CAPTURE = booleanPreferencesKey("auto_share_after_capture")
        val SHOW_REPORTS_MODE = booleanPreferencesKey("show_reports_mode")
        val MIRROR_FRONT_CAM = booleanPreferencesKey("mirror_front_cam")
        val RECORD_VIDEO_VOICE = booleanPreferencesKey("record_video_voice")
        val SHOW_DIGITAL_LEVEL = booleanPreferencesKey("show_digital_level")
        val TIMER_DELAY_SECONDS = intPreferencesKey("timer_delay_seconds")

        // Manual Location Override
        val USE_MANUAL_LOCATION = booleanPreferencesKey("use_manual_location")
        val SELECTED_MANUAL_LOC_ID = stringPreferencesKey("selected_manual_loc_id")
        val MANUAL_LOCATIONS_JSON = stringPreferencesKey("manual_locations_json")
    }

    private fun readFromPreferences(prefs: Preferences): StampConfig = StampConfig(
        template = prefs[Keys.TEMPLATE]?.let { runCatching { StampTemplate.valueOf(it) }.getOrNull() }
            ?: StampTemplate.TACTICAL_DARK,
        layoutTemplate = prefs[Keys.LAYOUT_TEMPLATE]?.let { runCatching { StampLayoutTemplate.valueOf(it) }.getOrNull() }
            ?: StampLayoutTemplate.ADVANCE,
        reportingLabel = prefs[Keys.REPORTING_LABEL] ?: "Check In",
        stampFont = prefs[Keys.STAMP_FONT]?.let { runCatching { StampFont.valueOf(it) }.getOrNull() }
            ?: StampFont.PLUS_JAKARTA_SANS,
        accentColor = prefs[Keys.ACCENT_COLOR]?.let { runCatching { StampAccentColor.valueOf(it) }.getOrNull() }
            ?: StampAccentColor.YV_GOLD,
        stampSize = prefs[Keys.STAMP_SIZE]?.let { runCatching { StampSize.valueOf(it) }.getOrNull() }
            ?: StampSize.MEDIUM,
        stampPosition = prefs[Keys.STAMP_POSITION]?.let { runCatching { StampPosition.valueOf(it) }.getOrNull() }
            ?: StampPosition.BOTTOM_CENTER,
        customBrandLabel = prefs[Keys.CUSTOM_BRAND_LABEL] ?: "YVGPSCAM",
        coordinateFormat = prefs[Keys.COORDINATE_FORMAT]?.let { runCatching { CoordinateFormat.valueOf(it) }.getOrNull() }
            ?: CoordinateFormat.DECIMAL_DEGREES,
        dateTimeFormat = prefs[Keys.DATETIME_FORMAT]?.let { runCatching { DateTimeFormat.valueOf(it) }.getOrNull() }
            ?: DateTimeFormat.READABLE_12H,
        mapStyle = parseMapStyle(prefs[Keys.MAP_STYLE]),
        offlineOnlyMapMode = prefs[Keys.OFFLINE_ONLY_MAP] ?: false,
        showLogo = true,
        showMiniMap = prefs[Keys.SHOW_MINI_MAP] ?: true,
        showShortAddress = prefs[Keys.SHOW_SHORT_ADDRESS] ?: true,
        showAddress = prefs[Keys.SHOW_ADDRESS] ?: true,
        showCountryFlag = prefs[Keys.SHOW_COUNTRY_FLAG] ?: true,
        showCoordinates = prefs[Keys.SHOW_COORDINATES] ?: true,
        showPlusCode = prefs[Keys.SHOW_PLUS_CODE] ?: true,
        showDateTime = prefs[Keys.SHOW_DATETIME] ?: true,
        showTimeZone = prefs[Keys.SHOW_TIMEZONE] ?: false,
        showNumbering = prefs[Keys.SHOW_NUMBERING] ?: false,
        photoSequenceNumber = (prefs[Keys.PHOTO_SEQUENCE_NUMBER] ?: 1).coerceAtLeast(1),
        showNote = prefs[Keys.SHOW_NOTE] ?: true,
        showAltitude = prefs[Keys.SHOW_ALTITUDE] ?: true,
        showCompassAndSpeed = prefs[Keys.SHOW_COMPASS_SPEED] ?: true,
        showAccuracy = prefs[Keys.SHOW_ACCURACY] ?: true,
        showMagneticField = prefs[Keys.SHOW_MAGNETIC] ?: true,
        showPressure = prefs[Keys.SHOW_PRESSURE] ?: false,
        showWeather = prefs[Keys.SHOW_WEATHER] ?: true,
        showPersonName = prefs[Keys.SHOW_PERSON_NAME] ?: false,
        personName = prefs[Keys.PERSON_NAME] ?: "",
        customNote = prefs[Keys.CUSTOM_NOTE] ?: "",
        stampOpacity = (prefs[Keys.STAMP_OPACITY] ?: 0.88f).coerceIn(0.35f, 1.0f),
        logoOpacity = (prefs[Keys.LOGO_OPACITY] ?: 1.0f).coerceIn(0.25f, 1.0f),
        customFileNameEnabled = prefs[Keys.FN_ENABLED] ?: true,
        fnIncludeDateTime = prefs[Keys.FN_DATETIME] ?: true,
        fnIncludeSequence = prefs[Keys.FN_SEQUENCE] ?: true,
        fnIncludeCustom1 = prefs[Keys.FN_CUSTOM_1] ?: true,
        fnCustomText1 = prefs[Keys.FN_CUSTOM_1_TEXT] ?: "YVGPSCAM",
        fnIncludeCustom2 = prefs[Keys.FN_CUSTOM_2] ?: false,
        fnCustomText2 = prefs[Keys.FN_CUSTOM_2_TEXT] ?: "",
        fnIncludeCustom3 = prefs[Keys.FN_CUSTOM_3] ?: false,
        fnCustomText3 = prefs[Keys.FN_CUSTOM_3_TEXT] ?: "",
        fnIncludeFullAddress = prefs[Keys.FN_FULL_ADDRESS] ?: false,
        fnIncludeLatLon = prefs[Keys.FN_LAT_LON] ?: true,
        fnIncludePlusCode = prefs[Keys.FN_PLUS_CODE] ?: false,
        fnIncludeTimeZone = prefs[Keys.FN_TIMEZONE] ?: false,
        fnIncludeNote = prefs[Keys.FN_NOTE] ?: false,
        fnFieldOrderSerialized = prefs[Keys.FN_FIELD_ORDER]
            ?: FileNameField.entries.joinToString(",") { it.name },
        appLanguage = prefs[Keys.APP_LANGUAGE] ?: "English",
        appThemeMode = prefs[Keys.APP_THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
            ?: AppThemeMode.DARK,
        photoQuality = prefs[Keys.PHOTO_QUALITY]?.let { runCatching { PhotoQualityLevel.valueOf(it) }.getOrNull() }
            ?: PhotoQualityLevel.HIGH,
        videoQuality = prefs[Keys.VIDEO_QUALITY]?.let { runCatching { VideoQualityLevel.valueOf(it) }.getOrNull() }
            ?: VideoQualityLevel.FHD_1080P,
        volumeButtonAction = prefs[Keys.VOLUME_BUTTON_ACTION]?.let { runCatching { VolumeButtonAction.valueOf(it) }.getOrNull() }
            ?: VolumeButtonAction.PHOTO_CAPTURE,
        screenTapAction = prefs[Keys.SCREEN_TAP_ACTION]?.let { runCatching { ScreenTapAction.valueOf(it) }.getOrNull() }
            ?: ScreenTapAction.FOCUS,
        qrDetectionEnabled = prefs[Keys.QR_DETECTION_ENABLED] ?: true,
        stampOrientation = prefs[Keys.STAMP_ORIENTATION]?.let { runCatching { StampOrientationMode.valueOf(it) }.getOrNull() }
            ?: StampOrientationMode.AUTOMATIC,
        showCameraIndicator = prefs[Keys.SHOW_CAMERA_INDICATOR] ?: false,
        autoSaveOriginalPhoto = prefs[Keys.AUTO_SAVE_ORIGINAL] ?: false,
        emailAfterCapture = prefs[Keys.EMAIL_AFTER_CAPTURE] ?: false,
        defaultReportEmail = prefs[Keys.DEFAULT_REPORT_EMAIL] ?: "",
        autoShareAfterCapture = prefs[Keys.AUTO_SHARE_AFTER_CAPTURE] ?: false,
        showReportsModeOnCamera = prefs[Keys.SHOW_REPORTS_MODE] ?: true,
        mirrorFrontCamera = prefs[Keys.MIRROR_FRONT_CAM] ?: true,
        recordVideoWithVoice = prefs[Keys.RECORD_VIDEO_VOICE] ?: true,
        showDigitalLevel = prefs[Keys.SHOW_DIGITAL_LEVEL] ?: false,
        timerDelaySeconds = prefs[Keys.TIMER_DELAY_SECONDS] ?: 0,
        useManualLocation = prefs[Keys.USE_MANUAL_LOCATION] ?: false,
        selectedManualLocationId = prefs[Keys.SELECTED_MANUAL_LOC_ID]?.takeIf { it.isNotBlank() },
        manualLocationsJson = prefs[Keys.MANUAL_LOCATIONS_JSON] ?: "[]"
    )

    val stampConfig: Flow<StampConfig> = context.stampDataStore.data.map { prefs ->
        readFromPreferences(prefs)
    }

    suspend fun updateConfig(transform: (StampConfig) -> StampConfig) {
        context.stampDataStore.edit { prefs ->
            val current = readFromPreferences(prefs)
            val updated = transform(current)
            prefs[Keys.TEMPLATE] = updated.template.name
            prefs[Keys.LAYOUT_TEMPLATE] = updated.layoutTemplate.name
            prefs[Keys.REPORTING_LABEL] = updated.reportingLabel
            prefs[Keys.STAMP_FONT] = updated.stampFont.name
            prefs[Keys.ACCENT_COLOR] = updated.accentColor.name
            prefs[Keys.STAMP_SIZE] = updated.stampSize.name
            prefs[Keys.STAMP_POSITION] = updated.stampPosition.name
            prefs[Keys.CUSTOM_BRAND_LABEL] = updated.customBrandLabel
            prefs[Keys.COORDINATE_FORMAT] = updated.coordinateFormat.name
            prefs[Keys.DATETIME_FORMAT] = updated.dateTimeFormat.name
            prefs[Keys.MAP_STYLE] = updated.mapStyle.name
            prefs[Keys.OFFLINE_ONLY_MAP] = updated.offlineOnlyMapMode
            prefs[Keys.SHOW_LOGO] = true
            prefs[Keys.SHOW_MINI_MAP] = updated.showMiniMap
            prefs[Keys.SHOW_SHORT_ADDRESS] = updated.showShortAddress
            prefs[Keys.SHOW_ADDRESS] = updated.showAddress
            prefs[Keys.SHOW_COUNTRY_FLAG] = updated.showCountryFlag
            prefs[Keys.SHOW_COORDINATES] = updated.showCoordinates
            prefs[Keys.SHOW_PLUS_CODE] = updated.showPlusCode
            prefs[Keys.SHOW_DATETIME] = updated.showDateTime
            prefs[Keys.SHOW_TIMEZONE] = updated.showTimeZone
            prefs[Keys.SHOW_NUMBERING] = updated.showNumbering
            prefs[Keys.PHOTO_SEQUENCE_NUMBER] = updated.photoSequenceNumber.coerceAtLeast(1)
            prefs[Keys.SHOW_NOTE] = updated.showNote
            prefs[Keys.SHOW_ALTITUDE] = updated.showAltitude
            prefs[Keys.SHOW_COMPASS_SPEED] = updated.showCompassAndSpeed
            prefs[Keys.SHOW_ACCURACY] = updated.showAccuracy
            prefs[Keys.SHOW_MAGNETIC] = updated.showMagneticField
            prefs[Keys.SHOW_PRESSURE] = updated.showPressure
            prefs[Keys.SHOW_WEATHER] = updated.showWeather
            prefs[Keys.SHOW_PERSON_NAME] = updated.showPersonName
            prefs[Keys.PERSON_NAME] = updated.personName
            prefs[Keys.CUSTOM_NOTE] = updated.customNote
            prefs[Keys.STAMP_OPACITY] = updated.stampOpacity
            prefs[Keys.LOGO_OPACITY] = updated.logoOpacity
            prefs[Keys.FN_ENABLED] = updated.customFileNameEnabled
            prefs[Keys.FN_DATETIME] = updated.fnIncludeDateTime
            prefs[Keys.FN_SEQUENCE] = updated.fnIncludeSequence
            prefs[Keys.FN_CUSTOM_1] = updated.fnIncludeCustom1
            prefs[Keys.FN_CUSTOM_1_TEXT] = updated.fnCustomText1
            prefs[Keys.FN_CUSTOM_2] = updated.fnIncludeCustom2
            prefs[Keys.FN_CUSTOM_2_TEXT] = updated.fnCustomText2
            prefs[Keys.FN_CUSTOM_3] = updated.fnIncludeCustom3
            prefs[Keys.FN_CUSTOM_3_TEXT] = updated.fnCustomText3
            prefs[Keys.FN_FULL_ADDRESS] = updated.fnIncludeFullAddress
            prefs[Keys.FN_LAT_LON] = updated.fnIncludeLatLon
            prefs[Keys.FN_PLUS_CODE] = updated.fnIncludePlusCode
            prefs[Keys.FN_TIMEZONE] = updated.fnIncludeTimeZone
            prefs[Keys.FN_NOTE] = updated.fnIncludeNote
            prefs[Keys.FN_FIELD_ORDER] = updated.fnFieldOrderSerialized
            prefs[Keys.APP_LANGUAGE] = updated.appLanguage
            prefs[Keys.APP_THEME_MODE] = updated.appThemeMode.name
            prefs[Keys.PHOTO_QUALITY] = updated.photoQuality.name
            prefs[Keys.VIDEO_QUALITY] = updated.videoQuality.name
            prefs[Keys.VOLUME_BUTTON_ACTION] = updated.volumeButtonAction.name
            prefs[Keys.SCREEN_TAP_ACTION] = updated.screenTapAction.name
            prefs[Keys.QR_DETECTION_ENABLED] = updated.qrDetectionEnabled
            prefs[Keys.STAMP_ORIENTATION] = updated.stampOrientation.name
            prefs[Keys.SHOW_CAMERA_INDICATOR] = updated.showCameraIndicator
            prefs[Keys.AUTO_SAVE_ORIGINAL] = updated.autoSaveOriginalPhoto
            prefs[Keys.EMAIL_AFTER_CAPTURE] = updated.emailAfterCapture
            prefs[Keys.DEFAULT_REPORT_EMAIL] = updated.defaultReportEmail
            prefs[Keys.AUTO_SHARE_AFTER_CAPTURE] = updated.autoShareAfterCapture
            prefs[Keys.SHOW_REPORTS_MODE] = updated.showReportsModeOnCamera
            prefs[Keys.MIRROR_FRONT_CAM] = updated.mirrorFrontCamera
            prefs[Keys.RECORD_VIDEO_VOICE] = updated.recordVideoWithVoice
            prefs[Keys.SHOW_DIGITAL_LEVEL] = updated.showDigitalLevel
            prefs[Keys.TIMER_DELAY_SECONDS] = updated.timerDelaySeconds
            prefs[Keys.USE_MANUAL_LOCATION] = updated.useManualLocation
            prefs[Keys.SELECTED_MANUAL_LOC_ID] = updated.selectedManualLocationId.orEmpty()
            prefs[Keys.MANUAL_LOCATIONS_JSON] = updated.manualLocationsJson
        }
    }

    suspend fun incrementSequenceNumber() {
        context.stampDataStore.edit { prefs ->
            val current = (prefs[Keys.PHOTO_SEQUENCE_NUMBER] ?: 1).coerceAtLeast(1)
            prefs[Keys.PHOTO_SEQUENCE_NUMBER] = current + 1
        }
    }

    private fun parseMapStyle(raw: String?): MapStyle = when (raw) {
        "SATELLITE" -> MapStyle.SATELLITE
        "TERRAIN", "TOPO_VECTOR" -> MapStyle.TERRAIN
        "HYBRID" -> MapStyle.HYBRID
        "STANDARD", "STREET_TILE", null -> MapStyle.STANDARD
        else -> runCatching { MapStyle.valueOf(raw) }.getOrDefault(MapStyle.STANDARD)
    }
}
