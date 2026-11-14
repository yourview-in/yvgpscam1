package com.example.camera

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExifMetadataDetails(
    val dateTimeOriginal: String? = null,
    val make: String? = null,
    val model: String? = null,
    val software: String? = null,
    val imageWidth: String? = null,
    val imageHeight: String? = null,
    val gpsLatitude: Double? = null,
    val gpsLongitude: Double? = null,
    val gpsAltitude: Double? = null,
    val gpsDirection: String? = null,
    val description: String? = null
)

object ExifMetadataReader {

    suspend fun readFromUri(context: Context, uriString: String): ExifMetadataDetails =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val latLong = exif.latLong
                    val altitude = exif.getAltitude(Double.NaN).takeIf { !it.isNaN() }
                    ExifMetadataDetails(
                        dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                            ?: exif.getAttribute(ExifInterface.TAG_DATETIME),
                        make = exif.getAttribute(ExifInterface.TAG_MAKE),
                        model = exif.getAttribute(ExifInterface.TAG_MODEL),
                        software = exif.getAttribute(ExifInterface.TAG_SOFTWARE),
                        imageWidth = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH),
                        imageHeight = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH),
                        gpsLatitude = latLong?.getOrNull(0),
                        gpsLongitude = latLong?.getOrNull(1),
                        gpsAltitude = altitude,
                        gpsDirection = exif.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION),
                        description = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
                    )
                } ?: ExifMetadataDetails()
            }.getOrDefault(ExifMetadataDetails())
        }
}
