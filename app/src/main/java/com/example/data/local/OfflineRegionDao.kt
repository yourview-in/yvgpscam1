package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.OfflineRegionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineRegionDao {
    @Query("SELECT * FROM offline_regions ORDER BY downloadedAtMillis DESC")
    fun getAllRegions(): Flow<List<OfflineRegionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegion(region: OfflineRegionEntity): Long

    @Query("DELETE FROM offline_regions WHERE id = :id")
    suspend fun deleteRegionById(id: Long)
}
