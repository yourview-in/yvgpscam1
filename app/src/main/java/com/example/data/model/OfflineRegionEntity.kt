package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_regions")
data class OfflineRegionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Double,
    val zoomMin: Int,
    val zoomMax: Int,
    val tileCount: Int,
    val sizeBytes: Long,
    val downloadedAtMillis: Long
)
