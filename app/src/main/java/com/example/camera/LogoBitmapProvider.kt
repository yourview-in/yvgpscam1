package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.example.R

object LogoBitmapProvider {

    @Volatile
    private var cachedMasterLogo: Bitmap? = null

    fun loadStampLogoBitmap(context: Context, targetSizePx: Int = 160): Bitmap {
        val master = cachedMasterLogo?.takeIf { !it.isRecycled } ?: synchronized(this) {
            cachedMasterLogo?.takeIf { !it.isRecycled }
                ?: BitmapFactory.decodeResource(context.resources, R.drawable.img_brand_yv_logo)
                    ?.also { cachedMasterLogo = it }
        }
        return if (master != null) {
            // Return an independent scaled/copied bitmap because callers recycle the returned instance
            if (master.width == targetSizePx && master.height == targetSizePx) {
                master.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                Bitmap.createScaledBitmap(master, targetSizePx, targetSizePx, true)
            }
        } else {
            renderYvGoldMarkBitmap(targetSizePx)
        }
    }

    fun renderYvGoldMarkBitmap(size: Int = 160): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val s = size.toFloat()

        // Dark obsidian rounded square badge
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(14, 23, 28)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(0f, 0f, s, s), s * 0.22f, s * 0.22f, bgPaint)

        // Subtle gold border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(229, 169, 59)
            style = Paint.Style.STROKE
            strokeWidth = s * 0.04f
        }
        canvas.drawRoundRect(
            RectF(s * 0.04f, s * 0.04f, s * 0.96f, s * 0.96f),
            s * 0.20f,
            s * 0.20f,
            borderPaint
        )

        val goldShader = LinearGradient(
            s * 0.15f, s * 0.2f, s * 0.85f, s * 0.8f,
            intArrayOf(
                Color.rgb(179, 128, 38),
                Color.rgb(243, 202, 102),
                Color.rgb(214, 146, 52)
            ),
            null,
            Shader.TileMode.CLAMP
        )

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = goldShader
            style = Paint.Style.FILL
        }

        // Viewfinder corner brackets over Y
        val bracketStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = goldShader
            style = Paint.Style.STROKE
            strokeWidth = s * 0.055f
            strokeCap = Paint.Cap.SQUARE
        }
        // Left top bracket
        val leftBracket = Path().apply {
            moveTo(s * 0.16f, s * 0.34f)
            lineTo(s * 0.16f, s * 0.21f)
            lineTo(s * 0.29f, s * 0.21f)
        }
        canvas.drawPath(leftBracket, bracketStroke)

        // Right top bracket
        val rightBracket = Path().apply {
            moveTo(s * 0.49f, s * 0.21f)
            lineTo(s * 0.62f, s * 0.21f)
            lineTo(s * 0.62f, s * 0.34f)
        }
        canvas.drawPath(rightBracket, bracketStroke)

        // Stylized bold geometric 'Y'
        val yPath = Path().apply {
            moveTo(s * 0.18f, s * 0.27f)
            lineTo(s * 0.31f, s * 0.27f)
            lineTo(s * 0.39f, s * 0.44f)
            lineTo(s * 0.47f, s * 0.27f)
            lineTo(s * 0.60f, s * 0.27f)
            lineTo(s * 0.45f, s * 0.56f)
            lineTo(s * 0.45f, s * 0.78f)
            lineTo(s * 0.33f, s * 0.78f)
            lineTo(s * 0.33f, s * 0.56f)
            close()
        }
        canvas.drawPath(yPath, fillPaint)

        // Stylized bold geometric 'V'
        val vPath = Path().apply {
            moveTo(s * 0.49f, s * 0.48f)
            lineTo(s * 0.57f, s * 0.38f)
            lineTo(s * 0.63f, s * 0.61f)
            lineTo(s * 0.75f, s * 0.27f)
            lineTo(s * 0.87f, s * 0.27f)
            lineTo(s * 0.69f, s * 0.78f)
            lineTo(s * 0.57f, s * 0.78f)
            close()
        }
        canvas.drawPath(vPath, fillPaint)

        return bmp
    }
}
