package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.data.model.CapturedPhotoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class PhotoRepository(
    private val context: Context,
    private val photoDao: CapturedPhotoDao
) {
    val allPhotos: Flow<List<CapturedPhotoEntity>> = photoDao.getAllPhotos()

    fun getPhotoById(id: Long): Flow<CapturedPhotoEntity?> = photoDao.getPhotoById(id)

    fun searchPhotos(query: String): Flow<List<CapturedPhotoEntity>> =
        if (query.isBlank()) photoDao.getAllPhotos() else photoDao.searchPhotos(query.trim())

    suspend fun insertPhoto(photo: CapturedPhotoEntity): Long = photoDao.insertPhoto(photo)

    suspend fun deletePhoto(photo: CapturedPhotoEntity) = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(photo.mediaUri)
            if (uri.scheme == "content") {
                context.contentResolver.delete(uri, null, null)
            } else if (uri.scheme == "file" && uri.path != null) {
                File(uri.path!!).delete()
            }
        }
        photoDao.deletePhotoById(photo.id)
    }

    suspend fun deletePhotos(photos: List<CapturedPhotoEntity>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (photos.isEmpty()) return@runCatching 0
            photos.forEach { photo ->
                runCatching {
                    val uri = Uri.parse(photo.mediaUri)
                    if (uri.scheme == "content") {
                        context.contentResolver.delete(uri, null, null)
                    } else if (uri.scheme == "file" && uri.path != null) {
                        File(uri.path!!).delete()
                    }
                }
            }
            photos.map { it.id }.chunked(500).forEach { chunk ->
                photoDao.deletePhotosByIds(chunk)
            }
            photos.size
        }
    }

    suspend fun renamePhotosWithTemplate(
        photos: List<CapturedPhotoEntity>,
        template: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val seenNames = mutableSetOf<String>()
            var renamedCount = 0

            photos.forEachIndexed { idx, photo ->
                val baseFilename = formatTemplateFilename(photo, template, idx + 1)
                val stem = baseFilename.removeSuffix(".jpg")
                var candidate = baseFilename
                var suffix = 2
                while (!seenNames.add(candidate.lowercase(Locale.US))) {
                    candidate = "${stem}_$suffix.jpg"
                    suffix++
                }

                var updatedUri = photo.mediaUri
                val parsedUri = Uri.parse(photo.mediaUri)
                if (parsedUri.scheme == "content") {
                    runCatching {
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, candidate)
                        }
                        context.contentResolver.update(parsedUri, values, null, null)
                    }
                } else if (parsedUri.scheme == "file" && !parsedUri.path.isNullOrBlank()) {
                    val srcFile = File(parsedUri.path!!)
                    if (srcFile.exists()) {
                        val targetFile = File(srcFile.parentFile, candidate)
                        if (srcFile.absolutePath == targetFile.absolutePath || srcFile.renameTo(targetFile)) {
                            updatedUri = Uri.fromFile(targetFile).toString()
                        }
                    }
                }

                photoDao.updatePhotoDisplayNameAndUri(photo.id, candidate, updatedUri)
                renamedCount++
            }
            renamedCount
        }
    }

    companion object {
        fun formatTemplateFilename(
            photo: CapturedPhotoEntity,
            template: String,
            indexOneBased: Int = 1
        ): String {
            val rawTemplate = template.trim().ifBlank { "YV_{date}_{coords}_{index}" }
            val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(photo.capturedAtMillis))
            val timeStr = SimpleDateFormat("HHmmss", Locale.US).format(Date(photo.capturedAtMillis))
            val dateTimeStr = "${dateStr}_$timeStr"
            val latStr = photo.latitude?.let { lat ->
                val hemi = if (lat >= 0) "N" else "S"
                String.format(Locale.US, "%.4f%s", abs(lat), hemi)
            } ?: "NoLat"
            val lonStr = photo.longitude?.let { lon ->
                val hemi = if (lon >= 0) "E" else "W"
                String.format(Locale.US, "%.4f%s", abs(lon), hemi)
            } ?: "NoLon"
            val coordsStr = if (photo.latitude != null && photo.longitude != null) {
                "${latStr}_$lonStr"
            } else {
                "NoGPS"
            }
            val cityStr = photo.cityState
                .ifBlank { photo.country }
                .ifBlank { "Field" }
                .replace(Regex("[^A-Za-z0-9]+"), "_")
                .trim('_')
                .ifBlank { "Field" }
            val indexStr = String.format(Locale.US, "%02d", indexOneBased)
            val origStr = photo.displayName
                .substringBeforeLast('.')
                .replace(Regex("[^A-Za-z0-9._-]+"), "_")
                .ifBlank { "photo" }

            val expanded = rawTemplate
                .replace("{date}", dateStr, ignoreCase = true)
                .replace("{time}", timeStr, ignoreCase = true)
                .replace("{datetime}", dateTimeStr, ignoreCase = true)
                .replace("{lat}", latStr, ignoreCase = true)
                .replace("{lon}", lonStr, ignoreCase = true)
                .replace("{coords}", coordsStr, ignoreCase = true)
                .replace("{city}", cityStr, ignoreCase = true)
                .replace("{index}", indexStr, ignoreCase = true)
                .replace("{orig}", origStr, ignoreCase = true)

            val sanitized = expanded
                .removeSuffix(".jpg")
                .removeSuffix(".jpeg")
                .removeSuffix(".JPG")
                .replace(Regex("[^A-Za-z0-9._-]+"), "_")
                .replace(Regex("_+"), "_")
                .trim('_', '.')
                .ifBlank { "YV_${dateStr}_$indexStr" }

            return "$sanitized.jpg"
        }
    }
}
