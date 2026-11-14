package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.util.LruCache
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeHelper {

    private val qrCache = LruCache<String, Bitmap>(24)

    fun buildLocationQrUrl(latitude: Double?, longitude: Double?): String {
        return if (latitude != null && longitude != null) {
            String.format(java.util.Locale.US, "https://maps.google.com/?q=%.6f,%.6f", latitude, longitude)
        } else {
            "https://maps.google.com/"
        }
    }

    fun generateQrBitmap(
        content: String,
        sizePx: Int = 220,
        fgColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE
    ): Bitmap {
        val cacheKey = "${content}_${sizePx}_${fgColor}_$bgColor"
        qrCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return cached
        }

        val safeContent = content.ifBlank { "https://maps.google.com/" }
        val safeSize = sizePx.coerceIn(64, 512)
        val bitmap = runCatching {
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val bitMatrix = QRCodeWriter().encode(
                safeContent,
                BarcodeFormat.QR_CODE,
                safeSize,
                safeSize,
                hints
            )
            val pixels = IntArray(safeSize * safeSize)
            for (y in 0 until safeSize) {
                val offset = y * safeSize
                for (x in 0 until safeSize) {
                    pixels[offset + x] = if (bitMatrix[x, y]) fgColor else bgColor
                }
            }
            Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, safeSize, 0, 0, safeSize, safeSize)
            }
        }.getOrElse {
            Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888).apply {
                eraseColor(bgColor)
            }
        }
        qrCache.put(cacheKey, bitmap)
        return bitmap
    }

    fun decodeQrFromYPlane(
        yBytes: ByteArray,
        width: Int,
        height: Int
    ): String? {
        if (width <= 0 || height <= 0 || yBytes.size < width * height) return null
        return runCatching {
            val source = PlanarYUVLuminanceSource(
                yBytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val reader = QRCodeReader()
            val hints = mapOf(DecodeHintType.TRY_HARDER to false)
            reader.decode(binaryBitmap, hints)?.text?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}
