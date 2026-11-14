package com.example.data.cloud

data class DrivePhotoBackup(
    val driveFileId: String,
    val backupId: String,
    val displayName: String,
    val capturedAtMillis: Long,
    val sizeBytes: Long = 0L,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitudeMeters: Double? = null,
    val accuracyMeters: Float? = null,
    val compassAzimuth: Float = 0f,
    val compassCardinal: String = "N",
    val addressLine: String = "",
    val cityState: String = "",
    val country: String = "",
    val customNote: String = "",
    val templateName: String = "TACTICAL_DARK",
    val width: Int = 1080,
    val height: Int = 1440,
    val thumbnailLink: String? = null
)
