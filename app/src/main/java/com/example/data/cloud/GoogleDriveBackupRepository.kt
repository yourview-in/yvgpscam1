package com.example.data.cloud

import com.example.data.model.CapturedPhotoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class DriveUnauthorizedException(message: String) : IOException(message)

class GoogleDriveBackupRepository(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    @Volatile
    private var cachedFolderId: String? = null

    fun clearSessionCache() {
        cachedFolderId = null
    }

    suspend fun findOrCreateBackupFolder(accessToken: String): String = withContext(Dispatchers.IO) {
        cachedFolderId?.let { return@withContext it }

        val query = "name='$BACKUP_FOLDER_NAME' and mimeType='$FOLDER_MIME' and trashed=false"
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "$DRIVE_FILES_URL?q=$encodedQuery&fields=files(id,name)&pageSize=1"

        val searchReq = Request.Builder()
            .url(searchUrl)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(searchReq).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            checkAuthAndSuccess(resp.code, bodyStr, "search Drive folder")
            val files = JSONObject(bodyStr).optJSONArray("files")
            if (files != null && files.length() > 0) {
                val id = files.getJSONObject(0).getString("id")
                cachedFolderId = id
                return@withContext id
            }
        }

        val createJson = JSONObject().apply {
            put("name", BACKUP_FOLDER_NAME)
            put("mimeType", FOLDER_MIME)
        }
        val createReq = Request.Builder()
            .url("$DRIVE_FILES_URL?fields=id")
            .header("Authorization", "Bearer $accessToken")
            .post(createJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(createReq).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            checkAuthAndSuccess(resp.code, bodyStr, "create Drive folder")
            val id = JSONObject(bodyStr).getString("id")
            cachedFolderId = id
            id
        }
    }

    suspend fun listBackups(accessToken: String): Result<List<DrivePhotoBackup>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val folderId = findOrCreateBackupFolder(accessToken)
                val query = "'$folderId' in parents and trashed=false and mimeType='image/jpeg'"
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val fields = URLEncoder.encode(
                    "files(id,name,size,createdTime,appProperties,thumbnailLink)",
                    "UTF-8"
                )
                val url = "$DRIVE_FILES_URL?q=$encodedQuery&fields=$fields&orderBy=createdTime%20desc&pageSize=100"

                val req = Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    val bodyStr = resp.body?.string().orEmpty()
                    checkAuthAndSuccess(resp.code, bodyStr, "list Google Drive backups")
                    parseBackupsResponse(bodyStr)
                }
            }
        }

    suspend fun uploadPhotoBackup(
        accessToken: String,
        photo: CapturedPhotoEntity,
        jpegBytes: ByteArray
    ): Result<DrivePhotoBackup> = withContext(Dispatchers.IO) {
        runCatching {
            val folderId = findOrCreateBackupFolder(accessToken)
            val backupId = buildDeterministicBackupId(photo.capturedAtMillis, photo.displayName)
            val fileName = if (photo.displayName.endsWith(".jpg", ignoreCase = true) ||
                photo.displayName.endsWith(".jpeg", ignoreCase = true)
            ) {
                photo.displayName
            } else {
                "${photo.displayName}.jpg"
            }

            val metadataJson = buildUploadMetadataJson(
                folderId = folderId,
                fileName = fileName,
                backupId = backupId,
                photo = photo
            )

            val multipartBody = MultipartBody.Builder()
                .setType(MULTIPART_RELATED_MEDIA_TYPE)
                .addPart(metadataJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .addPart(jpegBytes.toRequestBody(JPEG_MEDIA_TYPE))
                .build()

            val fields = URLEncoder.encode("id,name,size,createdTime,appProperties,thumbnailLink", "UTF-8")
            val req = Request.Builder()
                .url("$DRIVE_UPLOAD_URL?uploadType=multipart&fields=$fields")
                .header("Authorization", "Bearer $accessToken")
                .post(multipartBody)
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                checkAuthAndSuccess(resp.code, bodyStr, "upload photo to Google Drive")
                parseFileObject(JSONObject(bodyStr))
            }
        }
    }

    suspend fun downloadPhotoBytes(
        accessToken: String,
        driveFileId: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("$DRIVE_FILES_URL/$driveFileId?alt=media")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403) {
                    throw DriveUnauthorizedException("Google Drive authorization expired (${resp.code})")
                }
                if (!resp.isSuccessful) {
                    throw IOException("Failed to download from Google Drive (HTTP ${resp.code})")
                }
                resp.body?.bytes() ?: throw IOException("Empty photo payload from Google Drive")
            }
        }
    }

    suspend fun deleteBackup(
        accessToken: String,
        driveFileId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("$DRIVE_FILES_URL/$driveFileId")
                .header("Authorization", "Bearer $accessToken")
                .delete()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (resp.code == 404) return@use // Already deleted
                checkAuthAndSuccess(resp.code, bodyStr, "delete Google Drive backup")
            }
        }
    }

    private fun checkAuthAndSuccess(code: Int, body: String, action: String) {
        if (code == 401 || code == 403) {
            cachedFolderId = null
            throw DriveUnauthorizedException("Google Drive authorization needed ($code)")
        }
        if (code !in 200..299) {
            val apiMsg = runCatching {
                JSONObject(body).optJSONObject("error")?.optString("message")
            }.getOrNull().orEmpty()
            val detail = if (apiMsg.isNotBlank()) ": $apiMsg" else " (HTTP $code)"
            throw IOException("Could not $action$detail")
        }
    }

    companion object {
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        const val BACKUP_FOLDER_NAME = "YVGPSCam"
        private const val FOLDER_MIME = "application/vnd.google-apps.folder"
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val JPEG_MEDIA_TYPE = "image/jpeg".toMediaType()
        private val MULTIPART_RELATED_MEDIA_TYPE = "multipart/related".toMediaType()

        fun buildDeterministicBackupId(capturedAtMillis: Long, displayName: String): String {
            val cleanName = displayName
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .take(56)
                .ifBlank { "photo" }
            return "b_${capturedAtMillis}_$cleanName".take(90)
        }

        fun buildMediaUrl(driveFileId: String): String =
            "$DRIVE_FILES_URL/$driveFileId?alt=media"

        internal fun buildUploadMetadataJson(
            folderId: String,
            fileName: String,
            backupId: String,
            photo: CapturedPhotoEntity
        ): JSONObject {
            val appProps = JSONObject().apply {
                put("bid", truncateUtf8(backupId, 96))
                put("cap", photo.capturedAtMillis.toString())
                photo.latitude?.let { put("lat", String.format(Locale.US, "%.6f", it)) }
                photo.longitude?.let { put("lon", String.format(Locale.US, "%.6f", it)) }
                photo.altitudeMeters?.let { put("alt", String.format(Locale.US, "%.1f", it)) }
                photo.accuracyMeters?.let { put("acc", String.format(Locale.US, "%.1f", it)) }
                put("azi", String.format(Locale.US, "%.1f", photo.compassAzimuth))
                put("crd", truncateUtf8(photo.compassCardinal, 8))
                if (photo.addressLine.isNotBlank()) put("addr", truncateUtf8(photo.addressLine, 84))
                if (photo.cityState.isNotBlank()) put("city", truncateUtf8(photo.cityState, 72))
                if (photo.country.isNotBlank()) put("ctry", truncateUtf8(photo.country, 48))
                if (photo.customNote.isNotBlank()) put("note", truncateUtf8(photo.customNote, 84))
                put("tpl", truncateUtf8(photo.templateName, 36))
                put("dim", "${photo.width.coerceAtLeast(1)}x${photo.height.coerceAtLeast(1)}")
            }

            return JSONObject().apply {
                put("name", fileName)
                put("parents", JSONArray().put(folderId))
                put("mimeType", "image/jpeg")
                put("appProperties", appProps)
            }
        }

        internal fun parseBackupsResponse(jsonString: String): List<DrivePhotoBackup> {
            val root = JSONObject(jsonString)
            val files = root.optJSONArray("files") ?: return emptyList()
            val list = ArrayList<DrivePhotoBackup>(files.length())
            for (i in 0 until files.length()) {
                val obj = files.optJSONObject(i) ?: continue
                list.add(parseFileObject(obj))
            }
            return list.sortedByDescending { it.capturedAtMillis }
        }

        internal fun parseFileObject(obj: JSONObject): DrivePhotoBackup {
            val driveId = obj.getString("id")
            val name = obj.optString("name", "YVGPSCAM_Photo.jpg")
            val size = obj.optString("size", "0").toLongOrNull() ?: 0L
            val props = obj.optJSONObject("appProperties")

            val capturedAt = props?.optString("cap")?.toLongOrNull()
                ?: parseIsoTimeMillis(obj.optString("createdTime"))
                ?: System.currentTimeMillis()
            val backupId = props?.optString("bid")?.takeIf { it.isNotBlank() }
                ?: buildDeterministicBackupId(capturedAt, name)

            val dims = props?.optString("dim")?.split("x")
            val w = dims?.getOrNull(0)?.toIntOrNull()?.coerceAtLeast(1) ?: 1080
            val h = dims?.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1440

            return DrivePhotoBackup(
                driveFileId = driveId,
                backupId = backupId,
                displayName = name,
                capturedAtMillis = capturedAt,
                sizeBytes = size,
                latitude = props?.optString("lat")?.toDoubleOrNull(),
                longitude = props?.optString("lon")?.toDoubleOrNull(),
                altitudeMeters = props?.optString("alt")?.toDoubleOrNull(),
                accuracyMeters = props?.optString("acc")?.toFloatOrNull(),
                compassAzimuth = props?.optString("azi")?.toFloatOrNull() ?: 0f,
                compassCardinal = props?.optString("crd")?.takeIf { it.isNotBlank() } ?: "N",
                addressLine = props?.optString("addr").orEmpty(),
                cityState = props?.optString("city").orEmpty(),
                country = props?.optString("ctry").orEmpty(),
                customNote = props?.optString("note").orEmpty(),
                templateName = props?.optString("tpl")?.takeIf { it.isNotBlank() } ?: "TACTICAL_DARK",
                width = w,
                height = h,
                thumbnailLink = obj.optString("thumbnailLink").takeIf { it.isNotBlank() }
            )
        }

        private fun parseIsoTimeMillis(iso: String?): Long? {
            if (iso.isNullOrBlank()) return null
            val patterns = arrayOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'"
            )
            for (p in patterns) {
                val parsed = runCatching {
                    SimpleDateFormat(p, Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.parse(iso)?.time
                }.getOrNull()
                if (parsed != null) return parsed
            }
            return null
        }

        private fun truncateUtf8(input: String, maxBytes: Int): String {
            if (input.toByteArray(Charsets.UTF_8).size <= maxBytes) return input
            val sb = StringBuilder()
            var used = 0
            for (ch in input) {
                val chBytes = ch.toString().toByteArray(Charsets.UTF_8).size
                if (used + chBytes > maxBytes) break
                sb.append(ch)
                used += chBytes
            }
            return sb.toString()
        }
    }
}
