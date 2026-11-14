package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.TextPaint
import android.text.TextUtils
import com.example.camera.LogoBitmapProvider
import com.example.data.model.CapturedPhotoEntity
import com.example.data.model.GpsTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GeneratedPdfReport(
    val file: File,
    val pageCount: Int,
    val photoCount: Int,
    val fileSizeBytes: Long
)

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f

    suspend fun generateReport(
        context: Context,
        title: String,
        inspectorName: String,
        summaryNote: String,
        photos: List<CapturedPhotoEntity>
    ): Result<GeneratedPdfReport> = withContext(Dispatchers.IO) {
        runCatching {
            require(photos.isNotEmpty()) { "Select at least one photo for the PDF report." }

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outputFile = File(reportsDir, "YVGPSCAM_Report_$stamp.pdf")

            val document = PdfDocument()
            val logoBmp = runCatching { LogoBitmapProvider.loadStampLogoBitmap(context, 72) }.getOrNull()

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(17, 24, 34) }
            val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(245, 197, 24) }
            val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(248, 250, 252) }
            val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(214, 222, 232)
                style = Paint.Style.STROKE
                strokeWidth = 1.2f
            }

            val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val subHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(210, 220, 230)
                textSize = 10.5f
                typeface = Typeface.SANS_SERIF
            }
            val cardTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 12f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 9.5f
                typeface = Typeface.SANS_SERIF
            }
            val monoPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(12, 110, 82)
                textSize = 9.2f
                typeface = Typeface.MONOSPACE
            }

            val photosPerPage = 3
            val pages = photos.chunked(photosPerPage)
            val dateFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val generatedAtStr = dateFmt.format(Date())

            pages.forEachIndexed { pageIndex, chunk ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // White page canvas
                canvas.drawColor(Color.WHITE)

                // Top YVGPSCAM Branded Header
                val headerRect = RectF(0f, 0f, PAGE_WIDTH.toFloat(), 92f)
                canvas.drawRect(headerRect, headerPaint)
                canvas.drawRect(0f, 89f, PAGE_WIDTH.toFloat(), 92f, goldPaint)

                if (logoBmp != null) {
                    val dest = RectF(MARGIN, 18f, MARGIN + 48f, 66f)
                    canvas.drawBitmap(logoBmp, null, dest, null)
                }

                val titleLeft = MARGIN + if (logoBmp != null) 60f else 0f
                val safeTitle = title.ifBlank { "YVGPSCAM Field Report" }
                canvas.drawText(
                    TextUtils.ellipsize(safeTitle, titlePaint, PAGE_WIDTH - titleLeft - MARGIN, TextUtils.TruncateAt.END).toString(),
                    titleLeft,
                    38f,
                    titlePaint
                )

                val metaParts = buildList {
                    add("Generated: $generatedAtStr")
                    if (inspectorName.isNotBlank()) add("By: ${inspectorName.trim()}")
                    add("Items: ${photos.size}")
                }.joinToString("  •  ")
                canvas.drawText(
                    TextUtils.ellipsize(metaParts, subHeaderPaint, PAGE_WIDTH - titleLeft - MARGIN, TextUtils.TruncateAt.END).toString(),
                    titleLeft,
                    56f,
                    subHeaderPaint
                )

                if (summaryNote.isNotBlank() && pageIndex == 0) {
                    canvas.drawText(
                        TextUtils.ellipsize("Summary: ${summaryNote.trim()}", subHeaderPaint, PAGE_WIDTH - titleLeft - MARGIN, TextUtils.TruncateAt.END).toString(),
                        titleLeft,
                        74f,
                        subHeaderPaint
                    )
                } else {
                    canvas.drawText("YVGPSCAM Official Telemetry Log", titleLeft, 74f, subHeaderPaint)
                }

                // Render up to 3 photo cards on the page
                var yCursor = 112f
                val cardWidth = PAGE_WIDTH - MARGIN * 2f
                val cardHeight = 216f

                chunk.forEachIndexed { idx, photo ->
                    val globalIndex = pageIndex * photosPerPage + idx + 1
                    val cardRect = RectF(MARGIN, yCursor, MARGIN + cardWidth, yCursor + cardHeight)
                    canvas.drawRoundRect(cardRect, 10f, 10f, cardBgPaint)
                    canvas.drawRoundRect(cardRect, 10f, 10f, cardBorderPaint)

                    // Photo Thumbnail on left
                    val thumbRect = RectF(cardRect.left + 12f, cardRect.top + 12f, cardRect.left + 178f, cardRect.bottom - 12f)
                    val thumb = loadSampledBitmap(context, photo.mediaUri, 340)
                    if (thumb != null) {
                        drawCenterCroppedBitmap(canvas, thumb, thumbRect)
                        thumb.recycle()
                    } else {
                        val phPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(226, 232, 240) }
                        canvas.drawRoundRect(thumbRect, 6f, 6f, phPaint)
                        canvas.drawText(
                            if (photo.isVideo) "VIDEO" else "MEDIA",
                            thumbRect.centerX() - 18f,
                            thumbRect.centerY(),
                            bodyPaint
                        )
                    }

                    // Telemetry Details on right
                    val infoLeft = thumbRect.right + 16f
                    val infoMaxW = cardRect.right - infoLeft - 14f
                    var ty = cardRect.top + 28f

                    val headline = "#$globalIndex  ${photo.displayName}"
                    canvas.drawText(
                        TextUtils.ellipsize(headline, cardTitlePaint, infoMaxW, TextUtils.TruncateAt.END).toString(),
                        infoLeft,
                        ty,
                        cardTitlePaint
                    )
                    ty += 18f

                    val locTitle = listOf(photo.cityState, photo.country).filter { it.isNotBlank() }.joinToString(", ")
                        .ifBlank { "Captured Location" }
                    canvas.drawText(
                        TextUtils.ellipsize(locTitle, bodyPaint, infoMaxW, TextUtils.TruncateAt.END).toString(),
                        infoLeft,
                        ty,
                        bodyPaint
                    )
                    ty += 16f

                    if (photo.addressLine.isNotBlank()) {
                        canvas.drawText(
                            TextUtils.ellipsize(photo.addressLine, bodyPaint, infoMaxW, TextUtils.TruncateAt.END).toString(),
                            infoLeft,
                            ty,
                            bodyPaint
                        )
                        ty += 16f
                    }

                    val coords = if (photo.latitude != null && photo.longitude != null) {
                        "${GpsTelemetry.formatSingleCoord(photo.latitude, true)},  ${GpsTelemetry.formatSingleCoord(photo.longitude, false)}"
                    } else {
                        "Coordinates unavailable"
                    }
                    canvas.drawText(
                        TextUtils.ellipsize("GPS: $coords", monoPaint, infoMaxW, TextUtils.TruncateAt.END).toString(),
                        infoLeft,
                        ty,
                        monoPaint
                    )
                    ty += 16f

                    if (photo.latitude != null && photo.longitude != null) {
                        val plusCode = GpsTelemetry.encodePlusCode(photo.latitude, photo.longitude)
                        canvas.drawText("Plus Code: $plusCode", infoLeft, ty, monoPaint)
                        ty += 16f
                    }

                    val altStr = photo.altitudeMeters?.let { String.format(Locale.US, "%.1f m", it) } ?: "--"
                    val accStr = photo.accuracyMeters?.let { String.format(Locale.US, "±%.1f m", it) } ?: "--"
                    canvas.drawText(
                        "Alt: $altStr  •  Compass: ${photo.compassAzimuth.toInt()}° ${photo.compassCardinal}  •  Acc: $accStr",
                        infoLeft,
                        ty,
                        bodyPaint
                    )
                    ty += 16f

                    canvas.drawText("Captured: ${dateFmt.format(Date(photo.capturedAtMillis))}", infoLeft, ty, bodyPaint)
                    ty += 16f

                    if (photo.customNote.isNotBlank()) {
                        canvas.drawText(
                            TextUtils.ellipsize("Note: ${photo.customNote}", bodyPaint, infoMaxW, TextUtils.TruncateAt.END).toString(),
                            infoLeft,
                            ty,
                            bodyPaint
                        )
                    }

                    yCursor += cardHeight + 14f
                }

                // Footer
                canvas.drawText(
                    "YVGPSCAM Verified Geotag Report  •  Page ${pageIndex + 1} of ${pages.size}",
                    MARGIN,
                    PAGE_HEIGHT - 16f,
                    bodyPaint
                )

                document.finishPage(page)
            }

            FileOutputStream(outputFile).use { fos ->
                document.writeTo(fos)
            }
            document.close()
            logoBmp?.recycle()

            GeneratedPdfReport(
                file = outputFile,
                pageCount = pages.size,
                photoCount = photos.size,
                fileSizeBytes = outputFile.length()
            )
        }
    }

    private fun loadSampledBitmap(context: Context, uriString: String, maxSize: Int): Bitmap? {
        return runCatching {
            val uri = Uri.parse(uriString)
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOpts)
            }
            var inSample = 1
            while (boundsOpts.outWidth / inSample > maxSize * 2 || boundsOpts.outHeight / inSample > maxSize * 2) {
                inSample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            }
        }.getOrNull()
    }

    private fun drawCenterCroppedBitmap(canvas: Canvas, bitmap: Bitmap, dest: RectF) {
        val scale = maxOf(dest.width() / bitmap.width.toFloat(), dest.height() / bitmap.height.toFloat())
        val scaledW = bitmap.width * scale
        val scaledH = bitmap.height * scale
        val left = dest.centerX() - scaledW / 2f
        val top = dest.centerY() - scaledH / 2f
        canvas.save()
        canvas.clipRect(dest)
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + scaledW, top + scaledH), null)
        canvas.restore()
    }
}
