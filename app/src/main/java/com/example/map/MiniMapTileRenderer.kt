package com.example.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.LruCache
import com.example.data.model.MapStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class MiniMapTileRenderer(
    private val offlineMapRepository: OfflineMapRepository
) {
    private val tileMemoryCache = object : LruCache<String, Bitmap>(120) {
        override fun sizeOf(key: String, value: Bitmap): Int = 1
    }

    suspend fun renderMiniMapBitmap(
        latitude: Double?,
        longitude: Double?,
        zoom: Int = 15,
        outputSizePx: Int = 260,
        mapStyle: MapStyle = MapStyle.STANDARD,
        offlineOnly: Boolean = false,
        accentColorInt: Int = 0xFF10E09A.toInt(),
        isLightCard: Boolean = false
    ): Bitmap = withContext(Dispatchers.IO) {
        val result = Bitmap.createBitmap(outputSizePx, outputSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        if (latitude == null || longitude == null) {
            drawRealisticVectorCartography(
                canvas = canvas,
                size = outputSizePx,
                lat = null,
                lon = null,
                mapStyle = mapStyle,
                hasFix = false
            )
            drawRealisticPinAndMapChrome(
                canvas = canvas,
                size = outputSizePx,
                mapStyle = mapStyle,
                accentColorInt = accentColorInt,
                hasFix = false
            )
            return@withContext result
        }

        val googleStaticBackdrop = if (!offlineOnly) {
            loadGoogleStaticMapBitmap(latitude, longitude, zoom, outputSizePx, mapStyle)
        } else {
            null
        }

        val tileBackdrop = googleStaticBackdrop ?: composeTilesForLocation(
            lat = latitude,
            lon = longitude,
            zoom = zoom,
            outputSizePx = outputSizePx,
            mapStyle = mapStyle,
            offlineOnly = offlineOnly
        )

        if (tileBackdrop != null) {
            canvas.drawBitmap(tileBackdrop, null, RectF(0f, 0f, outputSizePx.toFloat(), outputSizePx.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
            if (googleStaticBackdrop == null) {
                tileBackdrop.recycle()
            }
        } else {
            drawRealisticVectorCartography(
                canvas = canvas,
                size = outputSizePx,
                lat = latitude,
                lon = longitude,
                mapStyle = mapStyle,
                hasFix = true
            )
        }

        drawRealisticPinAndMapChrome(
            canvas = canvas,
            size = outputSizePx,
            mapStyle = mapStyle,
            accentColorInt = accentColorInt,
            hasFix = true
        )

        result
    }

    private fun loadGoogleStaticMapBitmap(
        lat: Double,
        lon: Double,
        zoom: Int,
        outputSizePx: Int,
        mapStyle: MapStyle
    ): Bitmap? {
        val qLat = String.format(Locale.US, "%.4f", lat)
        val qLon = String.format(Locale.US, "%.4f", lon)
        val memKey = "gstatic/${mapStyle.name}/$zoom/$qLat/$qLon"
        tileMemoryCache.get(memKey)?.let { cached ->
            if (!cached.isRecycled) return cached
        }
        val bytes = offlineMapRepository.fetchGoogleStaticMapAndCache(lat, lon, zoom, outputSizePx, mapStyle)
            ?: return null
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        tileMemoryCache.put(memKey, bmp)
        return bmp
    }

    private fun composeTilesForLocation(
        lat: Double,
        lon: Double,
        zoom: Int,
        outputSizePx: Int,
        mapStyle: MapStyle,
        offlineOnly: Boolean
    ): Bitmap? {
        val fracX = OfflineMapRepository.lonToFractionalTileX(lon, zoom)
        val fracY = OfflineMapRepository.latToFractionalTileY(lat, zoom)
        val centerTileX = floor(fracX).toInt()
        val centerTileY = floor(fracY).toInt()
        val offsetInCenterTileX = ((fracX - centerTileX) * 256.0).toFloat()
        val offsetInCenterTileY = ((fracY - centerTileY) * 256.0).toFloat()

        val maxTileIndex = (1 shl zoom) - 1
        val centerPixelX = 256f + offsetInCenterTileX
        val centerPixelY = 256f + offsetInCenterTileY
        val cropHalf = outputSizePx.coerceIn(180, 340) / 2f
        val left = (centerPixelX - cropHalf).toInt().coerceIn(0, 768 - cropHalf.toInt() * 2)
        val top = (centerPixelY - cropHalf).toInt().coerceIn(0, 768 - cropHalf.toInt() * 2)
        val cropSize = (cropHalf.toInt() * 2).coerceAtLeast(1)

        val minDx = if (left < 256) -1 else 0
        val maxDx = if (left + cropSize > 512) 1 else 0
        val minDy = if (top < 256) -1 else 0
        val maxDy = if (top + cropSize > 512) 1 else 0

        val gridBitmap = Bitmap.createBitmap(768, 768, Bitmap.Config.ARGB_8888)
        val gridCanvas = Canvas(gridBitmap)
        var loadedCount = 0

        for (dx in minDx..maxDx) {
            for (dy in minDy..maxDy) {
                val tx = (centerTileX + dx).coerceIn(0, maxTileIndex)
                val ty = (centerTileY + dy).coerceIn(0, maxTileIndex)
                val tileBitmap = loadSingleTile(zoom, tx, ty, mapStyle, offlineOnly)
                if (tileBitmap != null) {
                    loadedCount++
                    val drawX = ((dx + 1) * 256).toFloat()
                    val drawY = ((dy + 1) * 256).toFloat()
                    gridCanvas.drawBitmap(tileBitmap, drawX, drawY, null)
                    if (mapStyle == MapStyle.HYBRID && !offlineOnly) {
                        loadHybridOverlayBitmap(zoom, tx, ty)?.let { overlayBmp ->
                            gridCanvas.drawBitmap(overlayBmp, drawX, drawY, null)
                        }
                    }
                }
            }
        }

        if (loadedCount == 0) {
            gridBitmap.recycle()
            return null
        }

        val cropped = Bitmap.createBitmap(gridBitmap, left, top, cropSize, cropSize)
        gridBitmap.recycle()
        return if (cropSize != outputSizePx) {
            val scaled = Bitmap.createScaledBitmap(cropped, outputSizePx, outputSizePx, true)
            cropped.recycle()
            scaled
        } else {
            cropped
        }
    }

    private fun loadSingleTile(z: Int, x: Int, y: Int, mapStyle: MapStyle, offlineOnly: Boolean): Bitmap? {
        val key = "${mapStyle.name}/$z/$x/$y"
        tileMemoryCache.get(key)?.let { if (!it.isRecycled) return it }

        val cachedFile = offlineMapRepository.getTileFile(z, x, y, mapStyle)
        if (cachedFile.exists() && cachedFile.length() > 0L) {
            val bmp = BitmapFactory.decodeFile(cachedFile.absolutePath)
            if (bmp != null) {
                tileMemoryCache.put(key, bmp)
                return bmp
            }
        }

        if (offlineOnly) return null

        val bytes = offlineMapRepository.fetchTileFromNetworkAndCache(z, x, y, mapStyle) ?: return null
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bmp != null) {
            tileMemoryCache.put(key, bmp)
        }
        return bmp
    }

    private fun loadHybridOverlayBitmap(z: Int, x: Int, y: Int): Bitmap? {
        val key = "HYBRID_OVERLAY/$z/$x/$y"
        tileMemoryCache.get(key)?.let { if (!it.isRecycled) return it }
        val bytes = offlineMapRepository.fetchHybridOverlayTile(z, x, y) ?: return null
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bmp != null) {
            tileMemoryCache.put(key, bmp)
        }
        return bmp
    }

    private fun drawRealisticVectorCartography(
        canvas: Canvas,
        size: Int,
        lat: Double?,
        lon: Double?,
        mapStyle: MapStyle,
        hasFix: Boolean
    ) {
        val s = size.toFloat()
        val shiftX = if (lat != null && lon != null) {
            (((sin(lat * 31.7) * 0.5 + 0.5) - 0.5) * 0.14 * s).toFloat()
        } else 0f
        val shiftY = if (lat != null && lon != null) {
            (((cos(lon * 31.7) * 0.5 + 0.5) - 0.5) * 0.14 * s).toFloat()
        } else 0f

        when (mapStyle) {
            MapStyle.STANDARD -> drawRealisticRoadMap(canvas, s, shiftX, shiftY, hasFix)
            MapStyle.SATELLITE -> drawRealisticSatelliteMap(canvas, s, shiftX, shiftY, withRoadLabels = false)
            MapStyle.TERRAIN -> drawRealisticTerrainMap(canvas, s, shiftX, shiftY)
            MapStyle.HYBRID -> drawRealisticSatelliteMap(canvas, s, shiftX, shiftY, withRoadLabels = true)
        }
    }

    private fun drawRealisticRoadMap(
        canvas: Canvas,
        s: Float,
        dx: Float,
        dy: Float,
        hasFix: Boolean
    ) {
        // Warm daylight map base (Google / Apple Maps style)
        canvas.drawColor(Color.rgb(242, 239, 230))

        // Parks & wooded green zones
        val parkFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(198, 232, 180)
            style = Paint.Style.FILL
        }
        val parkDeep = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(178, 219, 158)
            style = Paint.Style.FILL
        }
        val leftPark = Path().apply {
            moveTo(0f, 0f)
            lineTo(s * 0.40f + dx * 0.5f, 0f)
            quadTo(s * 0.34f + dx, s * 0.24f, s * 0.18f + dx * 0.5f, s * 0.36f + dy * 0.4f)
            lineTo(0f, s * 0.33f)
            close()
        }
        canvas.drawPath(leftPark, parkFill)
        canvas.drawRoundRect(
            RectF(s * 0.58f + dx * 0.4f, s * 0.62f + dy * 0.4f, s * 0.96f, s * 0.92f),
            s * 0.05f,
            s * 0.05f,
            parkFill
        )
        canvas.drawCircle(s * 0.14f, s * 0.15f, s * 0.065f, parkDeep)
        canvas.drawCircle(s * 0.22f, s * 0.20f, s * 0.045f, parkDeep)
        canvas.drawCircle(s * 0.78f + dx * 0.3f, s * 0.76f + dy * 0.3f, s * 0.06f, parkDeep)

        // Winding river / creek on right-bottom diagonal
        val riverPath = Path().apply {
            moveTo(s * 0.80f + dx * 0.3f, 0f)
            cubicTo(
                s * 0.74f + dx * 0.4f, s * 0.30f,
                s * 0.92f + dx * 0.3f, s * 0.58f,
                s * 0.85f + dx * 0.2f, s
            )
        }
        val riverStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(154, 208, 248)
            style = Paint.Style.STROKE
            strokeWidth = s * 0.072f
            strokeCap = Paint.Cap.ROUND
        }
        val riverBank = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(129, 191, 236)
            style = Paint.Style.STROKE
            strokeWidth = s * 0.082f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawPath(riverPath, riverBank)
        canvas.drawPath(riverPath, riverStroke)

        // Building blocks along city parcels
        val bldgFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(228, 224, 214)
            style = Paint.Style.FILL
        }
        val bldgBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(209, 204, 192)
            style = Paint.Style.STROKE
            strokeWidth = (s / 240f).coerceAtLeast(0.8f)
        }
        val blocks = listOf(
            RectF(s * 0.44f, s * 0.08f, s * 0.56f, s * 0.17f),
            RectF(s * 0.59f, s * 0.11f, s * 0.68f, s * 0.19f),
            RectF(s * 0.31f, s * 0.31f, s * 0.42f, s * 0.41f),
            RectF(s * 0.50f, s * 0.27f, s * 0.63f, s * 0.36f),
            RectF(s * 0.12f, s * 0.47f, s * 0.24f, s * 0.56f),
            RectF(s * 0.33f, s * 0.54f, s * 0.45f, s * 0.64f),
            RectF(s * 0.54f, s * 0.49f, s * 0.66f, s * 0.57f),
            RectF(s * 0.14f, s * 0.72f, s * 0.27f, s * 0.83f),
            RectF(s * 0.37f, s * 0.77f, s * 0.48f, s * 0.87f)
        )
        blocks.forEach { r ->
            val shifted = RectF(r.left + dx * 0.35f, r.top + dy * 0.35f, r.right + dx * 0.35f, r.bottom + dy * 0.35f)
            canvas.drawRoundRect(shifted, s * 0.012f, s * 0.012f, bldgFill)
            canvas.drawRoundRect(shifted, s * 0.012f, s * 0.012f, bldgBorder)
        }

        // Secondary local street network (two-pass cased lines)
        val localRoads = listOf(
            Path().apply { moveTo(0f, s * 0.24f + dy * 0.5f); lineTo(s, s * 0.21f + dy * 0.4f) },
            Path().apply { moveTo(0f, s * 0.67f + dy * 0.5f); lineTo(s * 0.78f, s * 0.65f + dy * 0.3f) },
            Path().apply { moveTo(s * 0.28f + dx * 0.5f, 0f); lineTo(s * 0.31f + dx * 0.4f, s) },
            Path().apply { moveTo(s * 0.70f + dx * 0.4f, 0f); lineTo(s * 0.66f + dx * 0.3f, s) }
        )
        drawCasedRoads(canvas, localRoads, widthPx = s * 0.026f, casingColor = Color.rgb(212, 207, 196), fillColor = Color.WHITE)

        // Collector avenues (soft cream-yellow)
        val collectorRoads = listOf(
            Path().apply {
                moveTo(0f, s * 0.44f + dy * 0.5f)
                quadTo(s * 0.48f, s * 0.46f + dy * 0.3f, s, s * 0.42f + dy * 0.4f)
            }
        )
        drawCasedRoads(canvas, collectorRoads, widthPx = s * 0.036f, casingColor = Color.rgb(220, 198, 144), fillColor = Color.rgb(255, 245, 202))

        // Main arterial highway + roundabout intersection (Google Maps amber-gold)
        val highway = listOf(
            Path().apply {
                moveTo(s * 0.47f + dx * 0.4f, 0f)
                cubicTo(
                    s * 0.46f + dx * 0.3f, s * 0.35f,
                    s * 0.54f + dx * 0.3f, s * 0.68f,
                    s * 0.49f + dx * 0.2f, s
                )
            }
        )
        drawCasedRoads(canvas, highway, widthPx = s * 0.048f, casingColor = Color.rgb(224, 166, 46), fillColor = Color.rgb(251, 211, 90))

        // Subtle roundabout circle at center highway crossing
        val rx = s * 0.485f + dx * 0.3f
        val ry = s * 0.45f + dy * 0.35f
        canvas.drawCircle(rx, ry, s * 0.045f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(224, 166, 46) })
        canvas.drawCircle(rx, ry, s * 0.034f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(251, 211, 90) })
        canvas.drawCircle(rx, ry, s * 0.015f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(198, 232, 180) })

        // Map street & park typography with white halo
        drawHaloedMapLabel(canvas, if (hasFix) "Park Ave" else "Market St", s * 0.18f, s * 0.42f + dy * 0.3f, s * 0.048f, darkBackground = false)
        drawHaloedMapLabel(canvas, "Central Park", s * 0.17f, s * 0.14f, s * 0.045f, darkBackground = false)
        drawHaloedMapLabel(canvas, "River Rd", s * 0.72f, s * 0.62f, s * 0.044f, darkBackground = false)
    }

    private fun drawRealisticSatelliteMap(
        canvas: Canvas,
        s: Float,
        dx: Float,
        dy: Float,
        withRoadLabels: Boolean
    ) {
        // Rich dark earth mosaic base
        canvas.drawColor(Color.rgb(38, 56, 39))

        val fieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        // Natural land / soil / pasture patchwork
        fieldPaint.color = Color.rgb(52, 74, 46)
        canvas.drawRect(0f, 0f, s * 0.46f + dx * 0.5f, s * 0.52f + dy * 0.5f, fieldPaint)
        fieldPaint.color = Color.rgb(68, 78, 52)
        canvas.drawRect(s * 0.46f + dx * 0.5f, 0f, s, s * 0.42f + dy * 0.4f, fieldPaint)
        fieldPaint.color = Color.rgb(46, 63, 44)
        canvas.drawRect(0f, s * 0.52f + dy * 0.5f, s * 0.58f + dx * 0.3f, s, fieldPaint)
        fieldPaint.color = Color.rgb(62, 68, 52)
        canvas.drawRect(s * 0.58f + dx * 0.3f, s * 0.42f + dy * 0.4f, s, s, fieldPaint)

        // River / canal channel
        val waterPath = Path().apply {
            moveTo(0f, s * 0.74f + dy * 0.3f)
            cubicTo(s * 0.35f, s * 0.66f + dy * 0.4f, s * 0.65f, s * 0.84f, s, s * 0.76f)
        }
        canvas.drawPath(
            waterPath,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(25, 58, 76)
                style = Paint.Style.STROKE
                strokeWidth = s * 0.09f
                strokeCap = Paint.Cap.ROUND
            }
        )
        canvas.drawPath(
            waterPath,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(34, 77, 99)
                style = Paint.Style.STROKE
                strokeWidth = s * 0.055f
                strokeCap = Paint.Cap.ROUND
            }
        )

        // Asphalt roads
        val asphalt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(60, 64, 70)
            style = Paint.Style.STROKE
            strokeWidth = s * 0.044f
        }
        canvas.drawLine(0f, s * 0.43f + dy * 0.4f, s, s * 0.46f + dy * 0.3f, asphalt)
        canvas.drawLine(s * 0.50f + dx * 0.4f, 0f, s * 0.48f + dx * 0.3f, s, asphalt)

        // Subtle white dashed center stripe
        val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 225, 228, 232)
            style = Paint.Style.STROKE
            strokeWidth = (s / 190f).coerceAtLeast(1f)
            pathEffect = DashPathEffect(floatArrayOf(s * 0.03f, s * 0.025f), 0f)
        }
        canvas.drawLine(0f, s * 0.43f + dy * 0.4f, s, s * 0.46f + dy * 0.3f, dash)

        // Building roofs with directional ground shadows
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(110, 14, 18, 15)
            style = Paint.Style.FILL
        }
        val roofLight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(162, 156, 144)
            style = Paint.Style.FILL
        }
        val roofSlate = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(112, 118, 124)
            style = Paint.Style.FILL
        }
        val roofs = listOf(
            RectF(s * 0.16f, s * 0.25f, s * 0.28f, s * 0.35f) to true,
            RectF(s * 0.60f, s * 0.22f, s * 0.72f, s * 0.33f) to false,
            RectF(s * 0.22f, s * 0.52f, s * 0.34f, s * 0.61f) to false,
            RectF(s * 0.63f, s * 0.51f, s * 0.77f, s * 0.60f) to true
        )
        val shadowOffset = s * 0.016f
        roofs.forEach { (r, light) ->
            val shifted = RectF(r.left + dx * 0.3f, r.top + dy * 0.3f, r.right + dx * 0.3f, r.bottom + dy * 0.3f)
            canvas.drawRect(
                shifted.left + shadowOffset,
                shifted.top + shadowOffset,
                shifted.right + shadowOffset,
                shifted.bottom + shadowOffset,
                shadowPaint
            )
            canvas.drawRect(shifted, if (light) roofLight else roofSlate)
        }

        // Dense tree canopy clusters with shadows
        val treeShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 10, 18, 12) }
        val treeDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 58, 32) }
        val treeMid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(44, 82, 44) }
        val trees = listOf(
            Triple(0.14f, 0.14f, 0.055f),
            Triple(0.37f, 0.16f, 0.048f),
            Triple(0.84f, 0.20f, 0.06f),
            Triple(0.15f, 0.86f, 0.05f),
            Triple(0.82f, 0.63f, 0.052f)
        )
        trees.forEach { (tx, ty, tr) ->
            val cx = s * tx + dx * 0.25f
            val cy = s * ty + dy * 0.25f
            val rad = s * tr
            canvas.drawCircle(cx + shadowOffset * 0.8f, cy + shadowOffset * 0.8f, rad, treeShadow)
            canvas.drawCircle(cx, cy, rad, treeDark)
            canvas.drawCircle(cx - rad * 0.2f, cy - rad * 0.2f, rad * 0.65f, treeMid)
        }

        if (withRoadLabels) {
            val hybridRoads = listOf(
                Path().apply { moveTo(0f, s * 0.43f + dy * 0.4f); lineTo(s, s * 0.46f + dy * 0.3f) },
                Path().apply { moveTo(s * 0.50f + dx * 0.4f, 0f); lineTo(s * 0.48f + dx * 0.3f, s) }
            )
            drawCasedRoads(
                canvas,
                hybridRoads,
                widthPx = s * 0.032f,
                casingColor = Color.argb(190, 214, 152, 32),
                fillColor = Color.argb(220, 252, 224, 120)
            )
            drawHaloedMapLabel(canvas, "Hwy 101", s * 0.22f, s * 0.39f + dy * 0.4f, s * 0.048f, darkBackground = true)
            drawHaloedMapLabel(canvas, "Main Blvd", s * 0.69f, s * 0.43f + dy * 0.3f, s * 0.044f, darkBackground = true)
        }
    }

    private fun drawRealisticTerrainMap(
        canvas: Canvas,
        s: Float,
        dx: Float,
        dy: Float
    ) {
        // Warm topographic relief base
        canvas.drawColor(Color.rgb(236, 234, 221))

        val forestPatch = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(208, 227, 186)
            style = Paint.Style.FILL
        }
        val ridgeShade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(218, 210, 184)
            style = Paint.Style.FILL
        }
        val highPeak = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(245, 240, 224)
            style = Paint.Style.FILL
        }

        // Organic elevation relief shapes
        canvas.drawOval(RectF(-s * 0.1f + dx, s * 0.05f + dy, s * 0.72f + dx, s * 0.76f + dy), forestPatch)
        canvas.drawOval(RectF(s * 0.15f + dx, s * 0.16f + dy, s * 0.68f + dx, s * 0.62f + dy), ridgeShade)
        canvas.drawOval(RectF(s * 0.28f + dx, s * 0.25f + dy, s * 0.56f + dx, s * 0.49f + dy), highPeak)

        // Smoothed topographic contour loops
        val contourLight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(145, 146, 158, 122)
            strokeWidth = (s / 210f).coerceAtLeast(0.9f)
            style = Paint.Style.STROKE
        }
        val contourMajor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(185, 122, 134, 98)
            strokeWidth = (s / 140f).coerceAtLeast(1.3f)
            style = Paint.Style.STROKE
        }

        val cx = s * 0.42f + dx * 0.7f
        val cy = s * 0.37f + dy * 0.7f
        listOf(0.08f, 0.14f, 0.21f, 0.29f, 0.38f, 0.48f).forEachIndexed { idx, factor ->
            val rx = s * factor * 1.15f
            val ry = s * factor * 0.85f
            val contour = Path().apply {
                moveTo(cx - rx, cy)
                cubicTo(cx - rx, cy - ry * 1.1f, cx + rx * 0.9f, cy - ry * 0.9f, cx + rx, cy + ry * 0.1f)
                cubicTo(cx + rx * 1.05f, cy + ry * 0.95f, cx - rx * 0.85f, cy + ry * 1.1f, cx - rx, cy)
                close()
            }
            canvas.drawPath(contour, if (idx % 2 == 0) contourMajor else contourLight)
        }

        // Mountain stream
        val stream = Path().apply {
            moveTo(s * 0.82f, 0f)
            cubicTo(s * 0.70f, s * 0.35f, s * 0.88f, s * 0.68f, s * 0.66f, s)
        }
        canvas.drawPath(
            stream,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(122, 189, 236)
                style = Paint.Style.STROKE
                strokeWidth = s * 0.032f
                strokeCap = Paint.Cap.ROUND
            }
        )

        // Scenic white-cased road + dashed trail
        drawCasedRoads(
            canvas,
            listOf(Path().apply { moveTo(0f, s * 0.62f + dy * 0.3f); quadTo(s * 0.45f, s * 0.52f, s, s * 0.58f) }),
            widthPx = s * 0.030f,
            casingColor = Color.rgb(198, 189, 166),
            fillColor = Color.WHITE
        )
        canvas.drawPath(
            Path().apply { moveTo(s * 0.22f, 0f); quadTo(s * 0.36f, s * 0.46f, s * 0.28f, s) },
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(138, 102, 68)
                style = Paint.Style.STROKE
                strokeWidth = (s / 130f).coerceAtLeast(1.4f)
                pathEffect = DashPathEffect(floatArrayOf(s * 0.03f, s * 0.02f), 0f)
            }
        )

        drawHaloedMapLabel(canvas, "540m", cx, cy - s * 0.02f, s * 0.044f, darkBackground = false)
        drawHaloedMapLabel(canvas, "Ridge Trl", s * 0.29f, s * 0.82f, s * 0.042f, darkBackground = false)
    }

    private fun drawCasedRoads(
        canvas: Canvas,
        paths: List<Path>,
        widthPx: Float,
        casingColor: Int,
        fillColor: Int
    ) {
        val casingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = casingColor
            style = Paint.Style.STROKE
            strokeWidth = (widthPx + 2.4f).coerceAtLeast(2.8f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fillColor
            style = Paint.Style.STROKE
            strokeWidth = widthPx.coerceAtLeast(1.6f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        paths.forEach { canvas.drawPath(it, casingPaint) }
        paths.forEach { canvas.drawPath(it, innerPaint) }
    }

    private fun drawHaloedMapLabel(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        textSizePx: Float,
        darkBackground: Boolean
    ) {
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (darkBackground) Color.argb(215, 16, 22, 28) else Color.argb(235, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = (textSizePx * 0.28f).coerceAtLeast(2f)
            textSize = textSizePx
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (darkBackground) Color.rgb(245, 248, 252) else Color.rgb(71, 85, 105)
            style = Paint.Style.FILL
            textSize = textSizePx
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(text, x, y, haloPaint)
        canvas.drawText(text, x, y, textPaint)
    }

    private fun drawRealisticPinAndMapChrome(
        canvas: Canvas,
        size: Int,
        mapStyle: MapStyle,
        accentColorInt: Int,
        hasFix: Boolean
    ) {
        val s = size.toFloat()
        val tipX = s / 2f
        val tipY = s * 0.56f

        // Subtle GPS accuracy halo around pin tip
        if (hasFix) {
            canvas.drawCircle(
                tipX,
                tipY,
                s * 0.13f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(38, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))
                    style = Paint.Style.FILL
                }
            )
            canvas.drawCircle(
                tipX,
                tipY,
                s * 0.13f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(115, 59, 130, 246)
                    style = Paint.Style.STROKE
                    strokeWidth = (s / 210f).coerceAtLeast(1f)
                }
            )
        }

        // Pin tip ground shadow ellipse
        canvas.drawOval(
            RectF(tipX - s * 0.055f, tipY - s * 0.016f, tipX + s * 0.055f, tipY + s * 0.022f),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(95, 15, 23, 42)
                style = Paint.Style.FILL
            }
        )

        // Realistic teardrop red map pin (head centered above tipY)
        val headR = s * 0.088f
        val headCy = tipY - headR * 1.95f
        val pinPath = Path().apply {
            moveTo(tipX, tipY)
            cubicTo(
                tipX - headR * 0.92f, tipY - headR * 0.85f,
                tipX - headR, headCy + headR * 0.25f,
                tipX - headR, headCy
            )
            arcTo(
                RectF(tipX - headR, headCy - headR, tipX + headR, headCy + headR),
                180f,
                180f,
                false
            )
            cubicTo(
                tipX + headR, headCy + headR * 0.25f,
                tipX + headR * 0.92f, tipY - headR * 0.85f,
                tipX, tipY
            )
            close()
        }

        canvas.drawPath(
            pinPath,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(234, 67, 53) // Google Maps crimson red
                style = Paint.Style.FILL
            }
        )
        canvas.drawPath(
            pinPath,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(179, 20, 18)
                style = Paint.Style.STROKE
                strokeWidth = (s / 170f).coerceAtLeast(1.2f)
            }
        )
        // White pin bullseye hole
        canvas.drawCircle(
            tipX,
            headCy,
            headR * 0.38f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
        )

        // Subtle "Maps" badge at bottom-left (IMG_4847 look, minimal obstruction)
        val isDarkMap = mapStyle == MapStyle.SATELLITE || mapStyle == MapStyle.HYBRID
        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkMap) Color.argb(175, 15, 23, 30) else Color.argb(215, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val mapsBadge = RectF(s * 0.045f, s * 0.86f, s * 0.29f, s * 0.955f)
        canvas.drawRoundRect(mapsBadge, s * 0.028f, s * 0.028f, pillBg)
        canvas.drawText(
            "Maps",
            mapsBadge.centerX(),
            mapsBadge.centerY() + s * 0.018f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDarkMap) Color.WHITE else Color.rgb(30, 41, 59)
                textSize = s * 0.052f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
        )

        // Compact top-right minimal North mark
        val northBadge = RectF(s * 0.83f, s * 0.045f, s * 0.955f, s * 0.165f)
        canvas.drawRoundRect(northBadge, s * 0.03f, s * 0.03f, pillBg)
        canvas.drawText(
            "N",
            northBadge.centerX(),
            northBadge.centerY() + s * 0.021f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDarkMap) Color.rgb(245, 197, 24) else Color.rgb(220, 38, 38)
                textSize = s * 0.058f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
        )

        if (!hasFix) {
            val statusBadge = RectF(s * 0.16f, s * 0.045f, s * 0.78f, s * 0.155f)
            canvas.drawRoundRect(statusBadge, s * 0.035f, s * 0.035f, pillBg)
            canvas.drawText(
                "GPS Sync...",
                statusBadge.centerX(),
                statusBadge.centerY() + s * 0.02f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isDarkMap) Color.WHITE else Color.rgb(30, 41, 59)
                    textSize = s * 0.050f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
            )
        }
    }
}
