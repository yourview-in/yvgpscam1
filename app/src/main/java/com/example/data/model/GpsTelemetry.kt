package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

enum class GpsSignalState {
    ACQUIRING,
    COARSE,
    LOCKED,
    PERMISSION_DENIED
}

data class GpsTelemetry(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitudeMeters: Double? = null,
    val accuracyMeters: Float? = null,
    val speedKmh: Float? = null,
    val bearingDegrees: Float? = null,
    val compassAzimuth: Float = 0f,
    val compassCardinal: String = "N",
    val magneticFieldMicroTesla: Float? = null,
    val pressureHpa: Float? = null,
    val weatherTempCelsius: Float? = null,
    val weatherHumidityPercent: Int? = null,
    val weatherWindKmh: Float? = null,
    val addressLine: String = "",
    val cityState: String = "",
    val country: String = "",
    val countryCode: String = "",
    val postalCode: String = "",
    val signalState: GpsSignalState = GpsSignalState.ACQUIRING,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
) {
    val hasFix: Boolean
        get() = latitude != null && longitude != null

    val fullLocality: String
        get() = listOf(cityState, country).filter { it.isNotBlank() }.joinToString(", ")

    val plusCode: String?
        get() = if (latitude != null && longitude != null) encodePlusCode(latitude, longitude) else null

    val countryFlagEmoji: String
        get() = countryCodeToFlagEmoji(countryCode)

    fun formatTimeZoneOffset(timeMillis: Long = System.currentTimeMillis()): String {
        val tz = java.util.TimeZone.getDefault()
        val offsetMillis = tz.getOffset(timeMillis)
        val sign = if (offsetMillis >= 0) "+" else "-"
        val totalMinutes = abs(offsetMillis) / 60000
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return String.format(Locale.US, "GMT %s%02d:%02d", sign, hours, mins)
    }

    val displayHeadlineAddress: String
        get() = when {
            cityState.isNotBlank() && country.isNotBlank() -> "$cityState, $country"
            addressLine.isNotBlank() -> addressLine
            hasFix -> "Coordinates Locked"
            signalState == GpsSignalState.PERMISSION_DENIED -> "Location Permission Required"
            else -> "Acquiring Satellite Fix..."
        }

    val displayStreetAddress: String
        get() = when {
            addressLine.isNotBlank() -> addressLine
            hasFix -> "Lat ${formatSingleCoord(latitude!!, true)}  Lon ${formatSingleCoord(longitude!!, false)}"
            else -> "Waiting for GPS telemetry signal"
        }

    fun formatCoordinates(format: CoordinateFormat): String {
        val lat = latitude ?: return "Lat --.------°  Lon --.------°"
        val lon = longitude ?: return "Lat --.------°  Lon --.------°"
        return when (format) {
            CoordinateFormat.DECIMAL_DEGREES -> {
                val latDir = if (lat >= 0) "N" else "S"
                val lonDir = if (lon >= 0) "E" else "W"
                String.format(
                    Locale.US,
                    "%.6f° %s,  %.6f° %s",
                    abs(lat), latDir, abs(lon), lonDir
                )
            }
            CoordinateFormat.DMS -> {
                "${toDms(lat, true)}   ${toDms(lon, false)}"
            }
        }
    }

    fun formatTimestamp(format: DateTimeFormat, timeMillis: Long = System.currentTimeMillis()): String {
        val pattern = when (format) {
            DateTimeFormat.READABLE_12H -> "EEEE, dd MMMM yyyy hh:mm:ss a"
            DateTimeFormat.ISO_LOCAL -> "yyyy-MM-dd  HH:mm:ss z"
            DateTimeFormat.COMPACT_24H -> "dd/MM/yy  HH:mm:ss"
        }
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timeMillis))
    }

    companion object {
        private const val OLC_ALPHABET = "23456789CFGHJMPQRVWX"

        fun encodePlusCode(latitude: Double, longitude: Double): String {
            var lat = latitude.coerceIn(-90.0, 90.0)
            if (lat == 90.0) lat = 89.999999
            var lon = ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
            lat += 90.0
            lon += 180.0

            val sb = StringBuilder()
            var resolution = 20.0
            for (pair in 0 until 5) {
                val latDigit = floor(lat / resolution).toInt().coerceIn(0, 19)
                val lonDigit = floor(lon / resolution).toInt().coerceIn(0, 19)
                lat -= latDigit * resolution
                lon -= lonDigit * resolution
                sb.append(OLC_ALPHABET[latDigit])
                sb.append(OLC_ALPHABET[lonDigit])
                if (pair == 3) sb.append('+')
                resolution /= 20.0
            }
            return sb.toString()
        }

        fun countryCodeToFlagEmoji(code: String): String {
            val clean = code.trim().uppercase(Locale.US)
            if (clean.length != 2 || !clean[0].isLetter() || !clean[1].isLetter()) return ""
            val first = Character.codePointAt(clean, 0) - 0x41 + 0x1F1E6
            val second = Character.codePointAt(clean, 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(first)) + String(Character.toChars(second))
        }

        fun formatSingleCoord(value: Double, isLat: Boolean): String {
            val dir = if (isLat) {
                if (value >= 0) "N" else "S"
            } else {
                if (value >= 0) "E" else "W"
            }
            return String.format(Locale.US, "%.6f° %s", abs(value), dir)
        }

        fun toDms(decimal: Double, isLat: Boolean): String {
            val dir = if (isLat) {
                if (decimal >= 0) "N" else "S"
            } else {
                if (decimal >= 0) "E" else "W"
            }
            val absVal = abs(decimal)
            val degrees = absVal.toInt()
            val minutesFull = (absVal - degrees) * 60.0
            val minutes = minutesFull.toInt()
            val seconds = (minutesFull - minutes) * 60.0
            return String.format(Locale.US, "%d°%02d'%05.2f\"%s", degrees, minutes, seconds, dir)
        }

        fun azimuthToCardinal(azimuth: Float): String {
            val normalized = ((azimuth % 360f) + 360f) % 360f
            val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
            val index = (((normalized + 22.5f) % 360f) / 45f).toInt().coerceIn(0, 7)
            return directions[index]
        }
    }
}
