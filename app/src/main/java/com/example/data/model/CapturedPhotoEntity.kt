package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "captured_photos")
data class CapturedPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val mediaUri: String,
    val displayName: String,
    val capturedAtMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val compassAzimuth: Float,
    val compassCardinal: String,
    val addressLine: String,
    val cityState: String,
    val country: String,
    val customNote: String,
    val templateName: String,
    val width: Int,
    val height: Int,
    val mediaType: String = "PHOTO"
) {
    val isVideo: Boolean
        get() = mediaType.equals("VIDEO", ignoreCase = true)
}
