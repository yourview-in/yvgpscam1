package com.example.map

import android.content.Context
import com.example.data.local.OfflineRegionDao
import com.example.data.model.MapStyle
import com.example.data.model.OfflineRegionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

data class TileCoord(val z: Int, val x: Int, val y: Int)

data class DownloadProgress(
    val isDownloading: Boolean = false,
    val regionName: String = "",
    val completedTiles: Int = 0,
    val totalTiles: Int = 0,
    val failedTiles: Int = 0,
    val errorMessage: String? = null
)

class OfflineMapRepository(
    private val context: Context,
    private val regionDao: OfflineRegionDao,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) {
    private val tilesRootDir: File by lazy {
        File(context.filesDir, "offline_tiles").apply { mkdirs() }
    }

    val savedRegions: Flow<List<OfflineRegionEntity>> = regionDao.getAllRegions()

    fun getTileFile(z: Int, x: Int, y: Int, mapStyle: MapStyle = MapStyle.STANDARD): File {
        return when (mapStyle) {
            MapStyle.STANDARD -> File(tilesRootDir, "$z/$x/$y.png")
            MapStyle.SATELLITE -> File(tilesRootDir, "satellite/$z/$x/$y.jpg")
            MapStyle.TERRAIN -> File(tilesRootDir, "terrain/$z/$x/$y.png")
            MapStyle.HYBRID -> File(tilesRootDir, "hybrid/$z/$x/$y.jpg")
        }
    }

    fun hasCachedTile(z: Int, x: Int, y: Int, mapStyle: MapStyle = MapStyle.STANDARD): Boolean {
        val file = getTileFile(z, x, y, mapStyle)
        return file.exists() && file.length() > 0L
    }

    fun saveTileBytes(z: Int, x: Int, y: Int, bytes: ByteArray, mapStyle: MapStyle = MapStyle.STANDARD) {
        val file = getTileFile(z, x, y, mapStyle)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { it.write(bytes) }
    }

    fun fetchTileFromNetworkAndCache(z: Int, x: Int, y: Int, mapStyle: MapStyle = MapStyle.STANDARD): ByteArray? {
        val url = when (mapStyle) {
            MapStyle.STANDARD -> "https://tile.openstreetmap.org/$z/$x/$y.png"
            MapStyle.SATELLITE -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x"
            MapStyle.TERRAIN -> "https://tile.opentopomap.org/$z/$x/$y.png"
            MapStyle.HYBRID -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x"
        }
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "YVGPSCAM-Android/1.0 (GPS Map Camera)")
            .build()
        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        saveTileBytes(z, x, y, bytes, mapStyle)
                        bytes
                    } else null
                } else null
            }
        }.getOrNull()
    }

    fun estimateTilesForRegion(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        zoomMin: Int,
        zoomMax: Int
    ): List<TileCoord> {
        val latDelta = radiusKm / 111.0
        val cosLat = cos(Math.toRadians(centerLat)).coerceAtLeast(0.1)
        val lonDelta = radiusKm / (111.32 * cosLat)

        val minLat = (centerLat - latDelta).coerceIn(-85.0, 85.0)
        val maxLat = (centerLat + latDelta).coerceIn(-85.0, 85.0)
        val minLon = (centerLon - lonDelta).coerceIn(-180.0, 180.0)
        val maxLon = (centerLon + lonDelta).coerceIn(-180.0, 180.0)

        val result = mutableListOf<TileCoord>()
        for (z in zoomMin..zoomMax) {
            val n = 1 shl z
            val x1 = lonToTileX(minLon, z).coerceIn(0, n - 1)
            val x2 = lonToTileX(maxLon, z).coerceIn(0, n - 1)
            val y1 = latToTileY(maxLat, z).coerceIn(0, n - 1)
            val y2 = latToTileY(minLat, z).coerceIn(0, n - 1)
            for (x in minOf(x1, x2)..maxOf(x1, x2)) {
                for (y in minOf(y1, y2)..maxOf(y1, y2)) {
                    result.add(TileCoord(z, x, y))
                    if (result.size >= 360) return result
                }
            }
        }
        return result
    }

    suspend fun downloadRegion(
        regionName: String,
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        zoomMin: Int = 14,
        zoomMax: Int = 16,
        onProgress: (DownloadProgress) -> Unit
    ): Result<OfflineRegionEntity> = withContext(Dispatchers.IO) {
        val tiles = estimateTilesForRegion(centerLat, centerLon, radiusKm, zoomMin, zoomMax)
        if (tiles.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No tiles found for region"))
        }

        var completed = 0
        var failed = 0
        var totalBytes = 0L

        onProgress(
            DownloadProgress(
                isDownloading = true,
                regionName = regionName,
                completedTiles = 0,
                totalTiles = tiles.size
            )
        )

        for (tile in tiles) {
            val existing = getTileFile(tile.z, tile.x, tile.y)
            if (existing.exists() && existing.length() > 0L) {
                totalBytes += existing.length()
                completed++
            } else {
                val bytes = fetchTileFromNetworkAndCache(tile.z, tile.x, tile.y)
                if (bytes != null) {
                    totalBytes += bytes.size
                    completed++
                } else {
                    failed++
                }
            }
            onProgress(
                DownloadProgress(
                    isDownloading = true,
                    regionName = regionName,
                    completedTiles = completed + failed,
                    totalTiles = tiles.size,
                    failedTiles = failed
                )
            )
        }

        if (completed == 0) {
            return@withContext Result.failure(
                IllegalStateException("Unable to download map tiles. Check internet connectivity.")
            )
        }

        val entity = OfflineRegionEntity(
            name = regionName.ifBlank { "Offline Area" },
            centerLat = centerLat,
            centerLon = centerLon,
            radiusKm = radiusKm,
            zoomMin = zoomMin,
            zoomMax = zoomMax,
            tileCount = completed,
            sizeBytes = totalBytes,
            downloadedAtMillis = System.currentTimeMillis()
        )
        val id = regionDao.insertRegion(entity)
        Result.success(entity.copy(id = id))
    }

    suspend fun deleteRegion(region: OfflineRegionEntity) = withContext(Dispatchers.IO) {
        val tiles = estimateTilesForRegion(
            region.centerLat,
            region.centerLon,
            region.radiusKm,
            region.zoomMin,
            region.zoomMax
        )
        tiles.forEach { tile ->
            val f = getTileFile(tile.z, tile.x, tile.y)
            if (f.exists()) f.delete()
        }
        regionDao.deleteRegionById(region.id)
    }

    suspend fun getTotalDiskCacheBytes(): Long = withContext(Dispatchers.IO) {
        tilesRootDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    suspend fun getTotalCachedTileCount(): Int = withContext(Dispatchers.IO) {
        tilesRootDir.walkTopDown().count { it.isFile && (it.extension == "png" || it.extension == "jpg") }
    }

    companion object {
        fun lonToTileX(lon: Double, zoom: Int): Int {
            return floor((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
        }

        fun latToTileY(lat: Double, zoom: Int): Int {
            val latRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
            return floor(
                (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)
            ).toInt()
        }

        fun lonToFractionalTileX(lon: Double, zoom: Int): Double {
            return (lon + 180.0) / 360.0 * (1 shl zoom)
        }

        fun latToFractionalTileY(lat: Double, zoom: Int): Double {
            val latRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
            return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)
        }

        fun tileYToLat(y: Double, zoom: Int): Double {
            val n = PI - 2.0 * PI * y / 2.0.pow(zoom)
            return Math.toDegrees(atan(sinh(n)))
        }
    }
}
