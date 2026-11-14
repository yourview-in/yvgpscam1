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
import kotlin.math.floor

class MiniMapTileRenderer(
    private val offlineMapRepository: OfflineMapRepository
) {
    private val tileMemoryCache = object : LruCache<String, Bitmap>(96) {
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
            drawStyledVectorBase(canvas, outputSizePx, null, null, mapStyle, accentColorInt, isLightCard, hasFix = false)
            return@withContext result
        }

        val tileBackdrop = composeTilesForLocation(
            lat = latitude,
            lon = longitude,
            zoom = zoom,
            outputSizePx = outputSizePx,
            mapStyle = mapStyle,
            offlineOnly = offlineOnly
        )
        val usedTileBackdrop = if (tileBackdrop != null) {
            canvas.drawBitmap(tileBackdrop, 0f, 0f, null)
            tileBackdrop.recycle()
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(36, 11, 16, 20)
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, outputSizePx.toFloat(), outputSizePx.toFloat(), vignettePaint)
            true
        } else {
            drawStyledVectorBase(canvas, outputSizePx, latitude, longitude, mapStyle, accentColorInt, isLightCard, hasFix = true)
            false
        }

        drawLocationReticleAndBadges(
            canvas = canvas,
            size = outputSizePx,
            lat = latitude,
            lon = longitude,
            zoom = zoom,
            mapStyle = mapStyle,
            accentColorInt = accentColorInt,
            hasTiles = usedTileBackdrop
        )

        result
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

        // Fetch only the tiles that actually intersect the crop rectangle (1–4 tiles instead of 9)
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
                    gridCanvas.drawBitmap(
                        tileBitmap,
                        ((dx + 1) * 256).toFloat(),
                        ((dy + 1) * 256).toFloat(),
                        null
                    )
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
        tileMemoryCache.get(key)?.let { return it }

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

    private fun drawStyledVectorBase(
        canvas: Canvas,
        size: Int,
        lat: Double?,
        lon: Double?,
        mapStyle: MapStyle,
        accentColorInt: Int,
        isLightCard: Boolean,
        hasFix: Boolean
    ) {
        val s = size.toFloat()
        val cx = s / 2f
        val cy = s / 2f
        val seedOffset = if (lat != null && lon != null) {
            (((lat * 1000.0) % 1.0 + (lon * 1000.0) % 1.0) * 0.16).toFloat()
        } else 0.08f

        when (mapStyle) {
            MapStyle.SATELLITE, MapStyle.HYBRID -> {
                // Aerial earth/canopy & parcel patchwork
                canvas.drawColor(Color.rgb(24, 42, 34))
                val patchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
                patchPaint.color = Color.rgb(34, 62, 46)
                canvas.drawRect(0f, 0f, s * (0.48f + seedOffset * 0.4f), s * 0.55f, patchPaint)
                patchPaint.color = Color.rgb(48, 56, 44)
                canvas.drawRect(s * 0.44f, s * 0.36f, s, s, patchPaint)
                patchPaint.color = Color.rgb(22, 54, 72)
                val river = Path().apply {
                    moveTo(0f, s * (0.68f - seedOffset))
                    quadTo(s * 0.5f, s * (0.58f + seedOffset), s, s * (0.76f - seedOffset * 0.5f))
                    lineTo(s, s * (0.86f - seedOffset * 0.5f))
                    quadTo(s * 0.5f, s * (0.68f + seedOffset), 0f, s * (0.78f - seedOffset))
                    close()
                }
                canvas.drawPath(river, patchPaint)

                val canopyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(95, 58, 94, 64)
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(s * 0.25f, s * 0.3f, s * 0.16f, canopyPaint)
                canvas.drawCircle(s * 0.72f, s * 0.28f, s * 0.14f, canopyPaint)

                val roadAlpha = if (mapStyle == MapStyle.HYBRID) 215 else 145
                val roadColor = if (mapStyle == MapStyle.HYBRID) Color.argb(roadAlpha, 245, 224, 140) else Color.argb(roadAlpha, 210, 206, 188)
                val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = roadColor
                    strokeWidth = (s / if (mapStyle == MapStyle.HYBRID) 64f else 80f).coerceAtLeast(1.8f)
                    style = Paint.Style.STROKE
                }
                canvas.drawLine(0f, s * (0.42f + seedOffset * 0.3f), s, s * (0.46f - seedOffset * 0.2f), roadPaint)
                canvas.drawLine(s * 0.52f, 0f, s * 0.47f, s, roadPaint)
            }

            MapStyle.TERRAIN -> {
                // Hypsometric terrain relief & contour rings
                canvas.drawColor(Color.rgb(32, 46, 36))
                val hillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
                hillPaint.color = Color.rgb(44, 64, 48)
                canvas.drawCircle(cx - s * 0.14f, cy - s * 0.1f, s * 0.46f, hillPaint)
                hillPaint.color = Color.rgb(66, 84, 58)
                canvas.drawCircle(cx - s * 0.08f, cy - s * 0.06f, s * 0.30f, hillPaint)
                hillPaint.color = Color.rgb(98, 108, 76)
                canvas.drawCircle(cx, cy, s * 0.16f, hillPaint)

                val contourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(115, 224, 212, 174)
                    strokeWidth = (s / 175f).coerceAtLeast(1.1f)
                    style = Paint.Style.STROKE
                }
                listOf(0.14f, 0.24f, 0.35f, 0.48f).forEach { radiusFactor ->
                    canvas.drawCircle(cx - s * 0.04f, cy - s * 0.03f, s * radiusFactor, contourPaint)
                }

                val ridgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(110, 168, 212, 180)
                    strokeWidth = (s / 110f).coerceAtLeast(1.4f)
                    style = Paint.Style.STROKE
                    pathEffect = DashPathEffect(floatArrayOf(s * 0.04f, s * 0.025f), 0f)
                }
                val ridge = Path().apply {
                    moveTo(0f, s * (0.3f + seedOffset))
                    quadTo(s * 0.48f, s * (0.5f - seedOffset), s, s * (0.34f + seedOffset * 0.4f))
                }
                canvas.drawPath(ridge, ridgePaint)
            }

            MapStyle.STANDARD -> {
                val bgColor = if (isLightCard) Color.rgb(227, 236, 242) else Color.rgb(16, 28, 38)
                canvas.drawColor(bgColor)

                val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isLightCard) Color.argb(46, 20, 45, 65) else Color.argb(42, 148, 166, 181)
                    strokeWidth = (s / 220f).coerceAtLeast(1f)
                    style = Paint.Style.STROKE
                }
                val step = s / 6f
                for (i in 1..5) {
                    val pos = step * i
                    canvas.drawLine(pos, 0f, pos, s, gridPaint)
                    canvas.drawLine(0f, pos, s, pos, gridPaint)
                }

                val arterialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isLightCard) Color.argb(145, 56, 95, 130) else Color.argb(120, 56, 189, 248)
                    strokeWidth = (s / 55f).coerceAtLeast(2.2f)
                    style = Paint.Style.STROKE
                }
                val road = Path().apply {
                    moveTo(0f, s * (0.32f + seedOffset))
                    quadTo(s * 0.45f, s * (0.52f - seedOffset), s, s * (0.38f + seedOffset * 0.5f))
                }
                canvas.drawPath(road, arterialPaint)
                canvas.drawLine(s * (0.48f + seedOffset * 0.3f), 0f, s * (0.52f - seedOffset * 0.2f), s, arterialPaint)

                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(72, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))
                    strokeWidth = (s / 180f).coerceAtLeast(1.2f)
                    style = Paint.Style.STROKE
                    pathEffect = DashPathEffect(floatArrayOf(s * 0.03f, s * 0.02f), 0f)
                }
                canvas.drawCircle(cx, cy, s * 0.22f, ringPaint)
            }
        }

        if (!hasFix) {
            val noFixPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isLightCard && mapStyle == MapStyle.STANDARD) Color.rgb(70, 90, 105) else Color.rgb(210, 224, 232)
                textSize = s * 0.088f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.MONOSPACE
            }
            canvas.drawText("NO GPS FIX", cx, cy + s * 0.25f, noFixPaint)
        }
    }

    private fun drawLocationReticleAndBadges(
        canvas: Canvas,
        size: Int,
        lat: Double,
        lon: Double,
        zoom: Int,
        mapStyle: MapStyle,
        accentColorInt: Int,
        hasTiles: Boolean
    ) {
        val s = size.toFloat()
        val cx = s / 2f
        val cy = s / 2f

        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(64, Color.red(accentColorInt), Color.green(accentColorInt), Color.blue(accentColorInt))
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, s * 0.12f, haloPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, s * 0.055f, ringPaint)

        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, s * 0.036f, corePaint)

        val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(198, 11, 16, 20)
            style = Paint.Style.FILL
        }
        val nRect = RectF(s * 0.78f, s * 0.06f, s * 0.94f, s * 0.20f)
        canvas.drawRoundRect(nRect, s * 0.04f, s * 0.04f, badgeBg)

        val nText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorInt
            textSize = s * 0.085f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText("N", nRect.centerX(), nRect.centerY() + s * 0.03f, nText)

        val pillRect = RectF(s * 0.05f, s * 0.81f, s * 0.95f, s * 0.95f)
        canvas.drawRoundRect(pillRect, s * 0.04f, s * 0.04f, badgeBg)
        val pillText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = s * 0.066f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.MONOSPACE
        }
        val tag = if (hasTiles) "${mapStyle.shortLabel} Z$zoom" else mapStyle.shortLabel
        val coordSummary = String.format(Locale.US, "%.3f, %.3f · %s", lat, lon, tag)
        canvas.drawText(coordSummary, pillRect.centerX(), pillRect.centerY() + s * 0.024f, pillText)
    }
}
