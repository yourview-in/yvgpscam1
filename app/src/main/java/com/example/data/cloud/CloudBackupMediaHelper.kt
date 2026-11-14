package com.example.data.cloud

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.data.model.CapturedPhotoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object CloudBackupMediaHelper {

    suspend fun readPhotoJpegBytesForDrive(
        context: Context,
        mediaUri: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(mediaUri)
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Could not read photo file for Google Drive backup")
            if (rawBytes.size >= 2 && rawBytes[0] == 0xFF.toByte() && rawBytes[1] == 0xD8.toByte()) {
                return@runCatching rawBytes
            }
            // Fallback transcode to JPEG if source image is PNG/WebP
            val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                ?: throw IllegalStateException("Unsupported photo format for backup")
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, baos)
            bmp.recycle()
            baos.toByteArray()
        }
    }

    suspend fun restoreDriveBytesToDevice(
        context: Context,
        backup: DrivePhotoBackup,
        jpegBytes: ByteArray
    ): Result<CapturedPhotoEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, bounds)
            val restoredW = if (bounds.outWidth > 0) bounds.outWidth else backup.width
            val restoredH = if (bounds.outHeight > 0) bounds.outHeight else backup.height

            val fileName = if (backup.displayName.endsWith(".jpg", ignoreCase = true) ||
                backup.displayName.endsWith(".jpeg", ignoreCase = true)
            ) {
                backup.displayName
            } else {
                "YVGPSCAM_RESTORED_${backup.capturedAtMillis}.jpg"
            }

            val savedUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/YVGPSCAM")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val inserted = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("MediaStore refused restore URI")
                resolver.openOutputStream(inserted)?.use { out ->
                    out.write(jpegBytes)
                } ?: throw IllegalStateException("Failed to write restored image")

                applyRestoredExif(context, inserted, backup)

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(inserted, values, null, null)
                inserted
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "YVGPSCAM").apply {
                    mkdirs()
                }
                val outFile = File(dir, fileName)
                FileOutputStream(outFile).use { fos ->
                    fos.write(jpegBytes)
                }
                runCatching {
                    val exif = ExifInterface(outFile)
                    populateExif(exif, backup)
                    exif.saveAttributes()
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
            }

            CapturedPhotoEntity(
                mediaUri = savedUri.toString(),
                displayName = fileName,
                capturedAtMillis = backup.capturedAtMillis,
                latitude = backup.latitude,
                longitude = backup.longitude,
                altitudeMeters = backup.altitudeMeters,
                accuracyMeters = backup.accuracyMeters,
                compassAzimuth = backup.compassAzimuth,
                compassCardinal = backup.compassCardinal,
                addressLine = backup.addressLine,
                cityState = backup.cityState,
                country = backup.country,
                customNote = backup.customNote,
                templateName = backup.templateName,
                width = restoredW,
                height = restoredH
            )
        }
    }

    private fun applyRestoredExif(context: Context, uri: Uri, backup: DrivePhotoBackup) {
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                populateExif(exif, backup)
                exif.saveAttributes()
            }
        }
    }

    private fun populateExif(exif: ExifInterface, backup: DrivePhotoBackup) {
        val exifDate = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            .format(Date(backup.capturedAtMillis))
        exif.setAttribute(ExifInterface.TAG_DATETIME, exifDate)
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, exifDate)
        exif.setAttribute(ExifInterface.TAG_MAKE, "YVGPSCAM")
        exif.setAttribute(ExifInterface.TAG_SOFTWARE, "YVGPSCAM Drive Restore")
        val lat = backup.latitude
        val lon = backup.longitude
        if (lat != null && lon != null) {
            exif.setLatLong(lat, lon)
            backup.altitudeMeters?.let { alt ->
                exif.setAltitude(abs(alt))
                exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, if (alt >= 0) "0" else "1")
            }
        }
    }
}

class CloudMediaGateway(context: Context) {
    private val appContext = context.applicationContext

    suspend fun readJpegBytesForDrive(photo: CapturedPhotoEntity): Result<ByteArray> =
        CloudBackupMediaHelper.readPhotoJpegBytesForDrive(appContext, photo.mediaUri)

    suspend fun restoreDrivePhotoToDevice(
        backup: DrivePhotoBackup,
        jpegBytes: ByteArray
    ): Result<CapturedPhotoEntity> =
        CloudBackupMediaHelper.restoreDriveBytesToDevice(appContext, backup, jpegBytes)
}
