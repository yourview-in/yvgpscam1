package com.example.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.data.model.CapturedPhotoEntity
import com.example.data.model.FileNameField
import com.example.data.model.GpsTelemetry
import com.example.data.model.StampConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MediaStoreSaver(
    private val context: Context,
    private val stampRenderer: PhotoStampRenderer
) {
    suspend fun saveStampedJpegBytes(
        jpegBytes: ByteArray,
        telemetry: GpsTelemetry,
        config: StampConfig,
        miniMapBitmap: Bitmap?,
        mirrorHorizontal: Boolean = false,
        isFrontCamera: Boolean = false,
        deviceOrientationDegrees: Int = 0
    ): Result<CapturedPhotoEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val decoded = decodeOrientedBitmap(jpegBytes, mirrorHorizontal)
                ?: throw IllegalStateException("Failed to decode captured photo")
            val now = System.currentTimeMillis()
            val fileName = buildCustomFileName(config, telemetry, now)

            if (config.autoSaveOriginalPhoto) {
                runCatching {
                    saveBitmapUnstampedOriginal(
                        bitmap = decoded,
                        fileName = "ORIG_$fileName",
                        jpegQuality = config.photoQuality.jpegQuality
                    )
                }
            }

            val stamped = stampRenderer.stampBitmap(
                source = decoded,
                telemetry = telemetry,
                config = config,
                miniMapBitmap = miniMapBitmap,
                capturedAtMillis = now,
                isFrontCamera = isFrontCamera,
                deviceOrientationDegrees = deviceOrientationDegrees
            )
            if (stamped !== decoded) decoded.recycle()
            persistStampedBitmap(stamped, telemetry, config, fileName, now)
        }
    }

    suspend fun stampAndSaveFromUri(
        sourceUri: Uri,
        telemetry: GpsTelemetry,
        config: StampConfig,
        miniMapBitmap: Bitmap?
    ): Result<CapturedPhotoEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Unable to read selected image")
            val decoded = decodeOrientedBitmap(bytes, mirrorHorizontal = false)
                ?: throw IllegalStateException("Unable to decode selected image")
            val now = System.currentTimeMillis()
            val fileName = buildCustomFileName(config, telemetry, now)
            val stamped = stampRenderer.stampBitmap(
                source = decoded,
                telemetry = telemetry,
                config = config,
                miniMapBitmap = miniMapBitmap,
                capturedAtMillis = now
            )
            if (stamped !== decoded) decoded.recycle()
            persistStampedBitmap(stamped, telemetry, config, fileName, now)
        }
    }

    fun buildRecordedVideoEntity(
        videoUri: Uri,
        telemetry: GpsTelemetry,
        config: StampConfig,
        width: Int,
        height: Int
    ): CapturedPhotoEntity {
        val now = System.currentTimeMillis()
        val videoName = buildCustomFileName(config, telemetry, now)
            .removeSuffix(".jpg") + ".mp4"
        return CapturedPhotoEntity(
            mediaUri = videoUri.toString(),
            displayName = videoName,
            capturedAtMillis = now,
            latitude = telemetry.latitude,
            longitude = telemetry.longitude,
            altitudeMeters = telemetry.altitudeMeters,
            accuracyMeters = telemetry.accuracyMeters,
            compassAzimuth = telemetry.compassAzimuth,
            compassCardinal = telemetry.compassCardinal,
            addressLine = telemetry.addressLine,
            cityState = telemetry.cityState,
            country = telemetry.country,
            customNote = config.customNote,
            templateName = config.layoutTemplate.displayName,
            width = width,
            height = height,
            mediaType = "VIDEO"
        )
    }

    private fun decodeOrientedBitmap(bytes: ByteArray, mirrorHorizontal: Boolean): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inMutable = true
        }
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val exif = runCatching { ExifInterface(ByteArrayInputStream(bytes)) }.getOrNull()
        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        ) ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }
        if (mirrorHorizontal) {
            matrix.postScale(-1f, 1f)
        }

        val maxDim = 3200f
        val largest = maxOf(raw.width, raw.height).toFloat()
        if (largest > maxDim) {
            val scale = maxDim / largest
            matrix.postScale(scale, scale)
        }

        return if (!matrix.isIdentity) {
            val transformed = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            if (transformed !== raw) raw.recycle()
            transformed
        } else {
            raw
        }
    }

    private fun saveBitmapUnstampedOriginal(
        bitmap: Bitmap,
        fileName: String,
        jpegQuality: Int
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/YVGPSCAM/Originals")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(insertedUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(insertedUri, values, null, null)
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "YVGPSCAM/Originals").apply {
                mkdirs()
            }
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, fos)
            }
        }
    }

    private fun persistStampedBitmap(
        stamped: Bitmap,
        telemetry: GpsTelemetry,
        config: StampConfig,
        fileName: String,
        now: Long
    ): CapturedPhotoEntity {
        val width = stamped.width
        val height = stamped.height
        val quality = config.photoQuality.jpegQuality.coerceIn(70, 98)

        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/YVGPSCAM")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("MediaStore failed to allocate photo URI")

            resolver.openOutputStream(insertedUri)?.use { out ->
                stamped.compress(Bitmap.CompressFormat.JPEG, quality, out)
            } ?: throw IllegalStateException("Failed to open output stream for photo")

            writeExifToUri(insertedUri, telemetry, config, now)

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(insertedUri, values, null, null)
            insertedUri
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "YVGPSCAM").apply {
                mkdirs()
            }
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { fos ->
                stamped.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            }
            writeExifToFile(outFile, telemetry, config, now)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
        }

        stamped.recycle()

        return CapturedPhotoEntity(
            mediaUri = uri.toString(),
            displayName = fileName,
            capturedAtMillis = now,
            latitude = telemetry.latitude,
            longitude = telemetry.longitude,
            altitudeMeters = telemetry.altitudeMeters,
            accuracyMeters = telemetry.accuracyMeters,
            compassAzimuth = telemetry.compassAzimuth,
            compassCardinal = telemetry.compassCardinal,
            addressLine = telemetry.addressLine,
            cityState = telemetry.cityState,
            country = telemetry.country,
            customNote = config.customNote,
            templateName = config.layoutTemplate.displayName,
            width = width,
            height = height,
            mediaType = "PHOTO"
        )
    }

    private fun writeExifToUri(uri: Uri, telemetry: GpsTelemetry, config: StampConfig, timestamp: Long) {
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                applyExifTags(exif, telemetry, config, timestamp)
                exif.saveAttributes()
            }
        }
    }

    private fun writeExifToFile(file: File, telemetry: GpsTelemetry, config: StampConfig, timestamp: Long) {
        runCatching {
            val exif = ExifInterface(file)
            applyExifTags(exif, telemetry, config, timestamp)
            exif.saveAttributes()
        }
    }

    private fun applyExifTags(
        exif: ExifInterface,
        telemetry: GpsTelemetry,
        config: StampConfig,
        timestamp: Long
    ) {
        val exifDate = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date(timestamp))
        exif.setAttribute(ExifInterface.TAG_DATETIME, exifDate)
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDate)
        exif.setAttribute(ExifInterface.TAG_MAKE, "YVGPSCAM")
        exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL ?: "Android GPS Camera")
        exif.setAttribute(ExifInterface.TAG_SOFTWARE, "YVGPSCAM 1.0")

        val descParts = listOfNotNull(
            telemetry.displayHeadlineAddress.takeIf { it.isNotBlank() },
            config.customNote.takeIf { it.isNotBlank() }
        )
        if (descParts.isNotEmpty()) {
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, descParts.joinToString(" | "))
        }

        val lat = telemetry.latitude
        val lon = telemetry.longitude
        if (lat != null && lon != null) {
            exif.setLatLong(lat, lon)
            exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, "GPS")
            telemetry.altitudeMeters?.let { alt ->
                exif.setAltitude(abs(alt))
                exif.setAttribute(
                    ExifInterface.TAG_GPS_ALTITUDE_REF,
                    if (alt >= 0) "0" else "1"
                )
            }
            exif.setAttribute(
                ExifInterface.TAG_GPS_IMG_DIRECTION,
                "${telemetry.compassAzimuth.toInt()}/1"
            )
            exif.setAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M")
        }
    }

    companion object {
        fun buildCustomFileName(
            config: StampConfig,
            telemetry: GpsTelemetry,
            timestampMillis: Long = System.currentTimeMillis()
        ): String {
            val fallbackStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestampMillis))
            if (!config.customFileNameEnabled) {
                return "YVGPSCAM_$fallbackStamp.jpg"
            }

            val parts = mutableListOf<String>()
            config.parsedFnFieldOrder().forEach { field ->
                when (field) {
                    FileNameField.DATE_TIME -> if (config.fnIncludeDateTime) {
                        parts.add(SimpleDateFormat("yyyyMMdd_hhmmssa", Locale.US).format(Date(timestampMillis)))
                    }
                    FileNameField.SEQUENCE -> if (config.fnIncludeSequence) {
                        parts.add(config.photoSequenceNumber.coerceAtLeast(1).toString())
                    }
                    FileNameField.CUSTOM_1 -> if (config.fnIncludeCustom1 && config.fnCustomText1.isNotBlank()) {
                        parts.add(config.fnCustomText1.trim())
                    }
                    FileNameField.CUSTOM_2 -> if (config.fnIncludeCustom2 && config.fnCustomText2.isNotBlank()) {
                        parts.add(config.fnCustomText2.trim())
                    }
                    FileNameField.CUSTOM_3 -> if (config.fnIncludeCustom3 && config.fnCustomText3.isNotBlank()) {
                        parts.add(config.fnCustomText3.trim())
                    }
                    FileNameField.FULL_ADDRESS -> if (config.fnIncludeFullAddress) {
                        val addr = listOf(telemetry.cityState, telemetry.country, telemetry.addressLine)
                            .filter { it.isNotBlank() }
                            .joinToString("_")
                        if (addr.isNotBlank()) parts.add(addr)
                    }
                    FileNameField.LAT_LON -> if (config.fnIncludeLatLon && telemetry.latitude != null && telemetry.longitude != null) {
                        parts.add(String.format(Locale.US, "%.6f_%.6f", telemetry.latitude, telemetry.longitude))
                    }
                    FileNameField.PLUS_CODE -> if (config.fnIncludePlusCode) {
                        telemetry.plusCode?.let { parts.add(it) }
                    }
                    FileNameField.TIME_ZONE -> if (config.fnIncludeTimeZone) {
                        parts.add(telemetry.formatTimeZoneOffset(timestampMillis).replace(" ", ""))
                    }
                    FileNameField.NOTE -> if (config.fnIncludeNote && config.customNote.isNotBlank()) {
                        parts.add(config.customNote.trim().take(28))
                    }
                }
            }

            val raw = parts.joinToString("_")
                .replace(Regex("[^A-Za-z0-9._-]+"), "_")
                .replace(Regex("_+"), "_")
                .trim('_', '.')
                .take(120)
                .trimEnd('_', '.')

            return if (raw.isBlank()) "YVGPSCAM_$fallbackStamp.jpg" else "$raw.jpg"
        }
    }
}
