package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CoordinateFormat
import com.example.data.model.GpsTelemetry
import com.example.data.model.StampConfig
import com.example.data.model.StampFont
import com.example.data.model.StampLayoutTemplate
import com.example.data.model.StampTemplate
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.ShutterGold
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.StampLightSubtext
import com.example.ui.theme.StampLightText
import com.example.ui.theme.TelemetryEmerald
import com.example.util.QrCodeHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun YvGpsCamLogoBadge(
    config: StampConfig,
    size: Dp = 28.dp,
    alpha: Float = 1f,
    modifier: Modifier = Modifier
) {
    val safeAlpha = alpha.coerceIn(0.25f, 1f)
    Image(
        painter = painterResource(id = R.drawable.img_brand_yv_logo),
        contentDescription = "${config.customBrandLabel} logo",
        contentScale = ContentScale.Crop,
        alpha = safeAlpha,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.25f))
            .border(
                1.dp,
                config.accentColor.color.copy(alpha = 0.6f * safeAlpha),
                RoundedCornerShape(size * 0.25f)
            )
    )
}

@Composable
fun StampPreviewCard(
    telemetry: GpsTelemetry,
    config: StampConfig,
    miniMapBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    when (config.layoutTemplate) {
        StampLayoutTemplate.SCAN_LOCATION -> ScanLocationStampCard(
            telemetry = telemetry,
            config = config,
            isFrontCamera = isFrontCamera,
            modifier = modifier,
            onClick = onClick
        )
        StampLayoutTemplate.DATETIME -> DateTimeStampCard(
            telemetry = telemetry,
            config = config,
            miniMapBitmap = miniMapBitmap,
            isFrontCamera = isFrontCamera,
            modifier = modifier,
            onClick = onClick
        )
        StampLayoutTemplate.REPORTING -> ReportingStampCard(
            telemetry = telemetry,
            config = config,
            miniMapBitmap = miniMapBitmap,
            isFrontCamera = isFrontCamera,
            modifier = modifier,
            onClick = onClick
        )
        StampLayoutTemplate.NAVIGATIONAL_COMPASS -> NavigationalCompassStampCard(
            telemetry = telemetry,
            config = config,
            miniMapBitmap = miniMapBitmap,
            isFrontCamera = isFrontCamera,
            modifier = modifier,
            onClick = onClick
        )
        StampLayoutTemplate.CLASSIC, StampLayoutTemplate.ADVANCE -> AdvanceOrClassicStampCard(
            telemetry = telemetry,
            config = if (config.layoutTemplate == StampLayoutTemplate.CLASSIC) {
                config.copy(template = StampTemplate.CLASSIC_WHITE)
            } else {
                config
            },
            miniMapBitmap = miniMapBitmap,
            isFrontCamera = isFrontCamera,
            modifier = modifier,
            onClick = onClick
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvanceOrClassicStampCard(
    telemetry: GpsTelemetry,
    config: StampConfig,
    miniMapBitmap: Bitmap?,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val scale = config.stampSize.scaleFactor
    val isLight = config.template == StampTemplate.CLASSIC_WHITE
    val alpha = config.stampOpacity.coerceIn(0.35f, 1f)
    val logoAlpha = config.logoOpacity.coerceIn(0.25f, 1f)

    val bgColor = when (config.template) {
        StampTemplate.TACTICAL_DARK -> Color(0xFF0B0E12).copy(alpha = (alpha * 0.84f).coerceAtLeast(0.45f))
        StampTemplate.CLASSIC_WHITE -> Color(0xFFF6F9FB).copy(alpha = alpha)
        StampTemplate.MINIMAL_BAR -> Color(0xFF090D11).copy(alpha = (alpha * 0.85f).coerceAtLeast(0.42f))
        StampTemplate.CORNER_BADGE -> Color(0xFF0D141A).copy(alpha = alpha)
    }

    val textOverlayAlpha = alpha.coerceAtLeast(0.65f)
    val titleColor = (if (isLight) StampLightText else Color(0xFFF7FAFC)).copy(alpha = textOverlayAlpha)
    val subColor = (if (isLight) StampLightSubtext else Color(0xFFD0DCE5)).copy(alpha = textOverlayAlpha)
    val coordColor = (if (isLight) Color(0xFF0C5F44) else Color(0xFFE7EEF4)).copy(alpha = textOverlayAlpha)

    val primaryFont = when (config.stampFont) {
        StampFont.SPACE_GROTESK -> SpaceGroteskFontFamily
        StampFont.JETBRAINS_MONO -> JetBrainsMonoFontFamily
        StampFont.PLUS_JAKARTA_SANS -> PlusJakartaSansFontFamily
    }

    val maxCardWidth = when (config.template) {
        StampTemplate.CORNER_BADGE -> (320f * scale.coerceAtMost(1.08f)).dp
        StampTemplate.MINIMAL_BAR -> 540.dp
        else -> (430f * scale.coerceAtMost(1.05f)).dp
    }

    val activeMiniMap = if (config.showMiniMap && config.template != StampTemplate.MINIMAL_BAR) miniMapBitmap else null
    val mapWidth = if (config.template == StampTemplate.CORNER_BADGE) (66f * scale).dp else (78f * scale).dp

    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    val flagSuffix = if (config.showCountryFlag && telemetry.countryFlagEmoji.isNotBlank()) {
        " ${telemetry.countryFlagEmoji}"
    } else ""

    Box(
        modifier = modifier
            .widthIn(max = maxCardWidth)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(0.8.dp, Color.White.copy(alpha = if (isLight) 0.25f else 0.14f), RoundedCornerShape(12.dp))
            .then(clickableModifier)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (activeMiniMap != null) {
                Box(
                    modifier = Modifier
                        .width(mapWidth)
                        .fillMaxHeight()
                        .heightIn(min = (92f * scale).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                ) {
                    Image(
                        bitmap = activeMiniMap.asImageBitmap(),
                        contentDescription = "GPS mini-map preview",
                        contentScale = ContentScale.Crop,
                        alpha = textOverlayAlpha,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.5.dp)
                    ) {
                        if (config.showShortAddress) {
                            Text(
                                text = "${telemetry.displayHeadlineAddress}$flagSuffix",
                                color = titleColor,
                                fontFamily = primaryFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = (13.2f * scale).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (flagSuffix.isNotBlank()) {
                            Text(
                                text = flagSuffix.trim(),
                                fontSize = (12f * scale).sp
                            )
                        }

                        if (config.showAddress && telemetry.addressLine.isNotBlank() && config.template != StampTemplate.CORNER_BADGE) {
                            Text(
                                text = telemetry.addressLine,
                                color = subColor,
                                fontFamily = primaryFont,
                                fontSize = (9.3f * scale).sp,
                                lineHeight = (11.8f * scale).sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val plusStr = if (config.showPlusCode && telemetry.plusCode != null) {
                            "Plus Code : ${telemetry.plusCode}"
                        } else ""
                        val coordStr = if (config.showCoordinates) {
                            telemetry.formatCoordinates(config.coordinateFormat)
                        } else ""
                        val combinedCoord = listOf(plusStr, coordStr).filter { it.isNotBlank() }.joinToString(" • ")
                        if (combinedCoord.isNotBlank()) {
                            Text(
                                text = combinedCoord,
                                color = coordColor,
                                fontFamily = primaryFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = (9.0f * scale).sp,
                                maxLines = if (plusStr.isNotBlank() && coordStr.isNotBlank() && config.coordinateFormat == CoordinateFormat.DMS) 2 else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val timeParts = buildList {
                            if (config.showDateTime) add(telemetry.formatTimestamp(config.dateTimeFormat))
                            if (config.showTimeZone) add(telemetry.formatTimeZoneOffset())
                            if (config.showNumbering) add("#${config.photoSequenceNumber}")
                        }
                        if (timeParts.isNotEmpty()) {
                            Text(
                                text = timeParts.joinToString(" • "),
                                color = subColor,
                                fontFamily = primaryFont,
                                fontSize = (9.0f * scale).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (config.showPersonName && config.personName.isNotBlank()) {
                            Text(
                                text = "Person : ${config.personName.trim()}",
                                color = coordColor,
                                fontFamily = primaryFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = (9.0f * scale).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (config.showNote && config.customNote.isNotBlank()) {
                            Text(
                                text = "Note : ${config.customNote.trim()}",
                                color = titleColor,
                                fontFamily = primaryFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = (9.0f * scale).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Mandatory Brand Watermark Column
                    BrandWatermarkColumn(
                        config = config,
                        telemetry = telemetry,
                        titleColor = titleColor,
                        primaryFont = primaryFont,
                        scale = scale,
                        logoAlpha = logoAlpha,
                        isFrontCamera = isFrontCamera
                    )
                }

                // Bottom Sensor Telemetry Metrics Row
                val metricItems = buildTelemetryMetricItems(telemetry, config)
                if (metricItems.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        metricItems.forEach { (icon, tint, label) ->
                            MetricChipItem(
                                icon = icon,
                                tint = tint,
                                label = label,
                                textColor = titleColor,
                                scale = scale
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanLocationStampCard(
    telemetry: GpsTelemetry,
    config: StampConfig,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val scale = config.stampSize.scaleFactor
    val qrUrl = remember(telemetry.latitude, telemetry.longitude) {
        QrCodeHelper.buildLocationQrUrl(telemetry.latitude, telemetry.longitude)
    }
    val qrBitmap = remember(qrUrl) { QrCodeHelper.generateQrBitmap(qrUrl, 180) }
    val alpha = config.stampOpacity.coerceIn(0.45f, 1f)
    val flagSuffix = if (config.showCountryFlag && telemetry.countryFlagEmoji.isNotBlank()) " ${telemetry.countryFlagEmoji}" else ""

    Box(
        modifier = modifier
            .widthIn(max = 430.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B0E12).copy(alpha = alpha * 0.88f))
            .border(1.dp, ShutterGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Scan QR to open location in Google Maps",
                    modifier = Modifier
                        .size((72f * scale).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .padding(3.dp)
                )
                Text(
                    text = "SCAN LOCATION",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ShutterGold
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${telemetry.displayHeadlineAddress}$flagSuffix",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (12.5f * scale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (telemetry.addressLine.isNotBlank()) {
                    Text(
                        text = telemetry.addressLine,
                        color = Color(0xFFD0DCE5),
                        fontSize = (9.2f * scale).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = telemetry.formatCoordinates(config.coordinateFormat),
                    color = TelemetryEmerald,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = (9f * scale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = telemetry.formatTimestamp(config.dateTimeFormat),
                    color = Color(0xFFB8C7D4),
                    fontSize = (8.8f * scale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            BrandWatermarkColumn(
                config = config,
                telemetry = telemetry,
                titleColor = Color.White,
                primaryFont = PlusJakartaSansFontFamily,
                scale = scale,
                logoAlpha = config.logoOpacity,
                isFrontCamera = isFrontCamera
            )
        }
    }
}

@Composable
private fun DateTimeStampCard(
    telemetry: GpsTelemetry,
    config: StampConfig,
    miniMapBitmap: Bitmap?,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val scale = config.stampSize.scaleFactor
    val now = telemetry.lastUpdatedMillis
    val timeOnly = remember(now) { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(now)) }
    val dateOnly = remember(now) { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(now)) }
    val weekdayOnly = remember(now) { SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(now)) }

    Box(
        modifier = modifier
            .widthIn(max = 430.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B0E12).copy(alpha = config.stampOpacity.coerceIn(0.45f, 1f) * 0.88f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = timeOnly,
                        color = Color.White,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = (20f * scale).sp,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ShutterGold)
                    )
                    Column {
                        Text(
                            text = dateOnly,
                            color = ShutterGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = (10.5f * scale).sp
                        )
                        Text(
                            text = weekdayOnly,
                            color = Color(0xFFD0DCE5),
                            fontSize = (9.5f * scale).sp
                        )
                    }
                }

                Text(
                    text = telemetry.displayHeadlineAddress,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (11f * scale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = telemetry.formatCoordinates(config.coordinateFormat),
                    color = TelemetryEmerald,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = (9f * scale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (config.showMiniMap && miniMapBitmap != null) {
                Image(
                    bitmap = miniMapBitmap.asImageBitmap(),
                    contentDescription = "Mini-map preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size((54f * scale).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                )
            }

            BrandWatermarkColumn(
                config = config,
                telemetry = telemetry,
                titleColor = Color.White,
                primaryFont = PlusJakartaSansFontFamily,
                scale = scale * 0.9f,
                logoAlpha = config.logoOpacity,
                isFrontCamera = isFrontCamera
            )
        }
    }
}

@Composable
private fun ReportingStampCard(
    telemetry: GpsTelemetry,
    config: StampConfig,
    miniMapBitmap: Bitmap?,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val scale = config.stampSize.scaleFactor
    val flagSuffix = if (config.showCountryFlag && telemetry.countryFlagEmoji.isNotBlank()) " ${telemetry.countryFlagEmoji}" else ""

    Box(
        modifier = modifier
            .widthIn(max = 430.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E141B).copy(alpha = config.stampOpacity.coerceIn(0.45f, 1f) * 0.9f))
            .border(1.dp, TelemetryEmerald.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (config.showMiniMap && miniMapBitmap != null) {
                Image(
                    bitmap = miniMapBitmap.asImageBitmap(),
                    contentDescription = "Report mini-map",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width((70f * scale).dp)
                        .fillMaxHeight()
                        .heightIn(min = (86f * scale).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TelemetryEmerald)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF0A1410),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = config.reportingLabel.ifBlank { "Check In" },
                                color = Color(0xFF0A1410),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    if (config.showNumbering) {
                        Text(
                            text = "#${config.photoSequenceNumber}",
                            color = ShutterGold,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "${telemetry.displayHeadlineAddress}$flagSuffix",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (12f * scale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (telemetry.addressLine.isNotBlank()) {
                    Text(
                        text = telemetry.addressLine,
                        color = Color(0xFFD0DCE5),
                        fontSize = (9f * scale).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = telemetry.formatCoordinates(config.coordinateFormat),
                    color = TelemetryEmerald,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = (8.8f * scale).sp,
                    maxLines = 1
                )
                Text(
                    text = telemetry.formatTimestamp(config.dateTimeFormat),
                    color = Color(0xFFB8C7D4),
                    fontSize = (8.8f * scale).sp,
                    maxLines = 1
                )
                if (config.personName.isNotBlank()) {
                    Text(
                        text = "Inspector: ${config.personName.trim()}",
                        color = ShutterGold,
                        fontSize = (8.8f * scale).sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                if (config.customNote.isNotBlank()) {
                    Text(
                        text = "Note: ${config.customNote.trim()}",
                        color = Color.White,
                        fontSize = (8.8f * scale).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            BrandWatermarkColumn(
                config = config,
                telemetry = telemetry,
                titleColor = Color.White,
                primaryFont = PlusJakartaSansFontFamily,
                scale = scale,
                logoAlpha = config.logoOpacity,
                isFrontCamera = isFrontCamera
            )
        }
    }
}

@Composable
private fun NavigationalCompassStampCard(
    telemetry: GpsTelemetry,
    config: StampConfig,
    miniMapBitmap: Bitmap?,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val scale = config.stampSize.scaleFactor
    val azimuth = telemetry.compassAzimuth
    val cardinal = telemetry.compassCardinal

    Box(
        modifier = modifier
            .widthIn(max = 430.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A0F14).copy(alpha = config.stampOpacity.coerceIn(0.45f, 1f) * 0.9f))
            .border(1.dp, ShutterGold.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Circular Azimuth Compass Dial + Facing Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier.size((68f * scale).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val r = size.minDimension / 2f - 4.dp.toPx()
                        drawCircle(
                            color = Color(0xFF141E26),
                            radius = r,
                            center = center
                        )
                        drawCircle(
                            color = ShutterGold.copy(alpha = 0.7f),
                            radius = r,
                            center = center,
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                        // Tick marks
                        for (deg in 0 until 360 step 45) {
                            val rad = Math.toRadians((deg - 90).toDouble())
                            val inner = r * if (deg % 90 == 0) 0.72f else 0.84f
                            drawLine(
                                color = if (deg == 0) AlertCoral else Color.White.copy(alpha = 0.55f),
                                start = Offset(
                                    center.x + (inner * cos(rad)).toFloat(),
                                    center.y + (inner * sin(rad)).toFloat()
                                ),
                                end = Offset(
                                    center.x + (r * cos(rad)).toFloat(),
                                    center.y + (r * sin(rad)).toFloat()
                                ),
                                strokeWidth = if (deg % 90 == 0) 1.8.dp.toPx() else 1.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        // Compass needle pointing to azimuth
                        val needleRad = Math.toRadians((azimuth - 90f).toDouble())
                        val tip = Offset(
                            center.x + (r * 0.66f * cos(needleRad)).toFloat(),
                            center.y + (r * 0.66f * sin(needleRad)).toFloat()
                        )
                        val tail = Offset(
                            center.x - (r * 0.35f * cos(needleRad)).toFloat(),
                            center.y - (r * 0.35f * sin(needleRad)).toFloat()
                        )
                        val leftWingRad = needleRad + Math.PI / 2
                        val wingDist = r * 0.14f
                        val path = Path().apply {
                            moveTo(tip.x, tip.y)
                            lineTo(
                                center.x + (wingDist * cos(leftWingRad)).toFloat(),
                                center.y + (wingDist * sin(leftWingRad)).toFloat()
                            )
                            lineTo(tail.x, tail.y)
                            lineTo(
                                center.x - (wingDist * cos(leftWingRad)).toFloat(),
                                center.y - (wingDist * sin(leftWingRad)).toFloat()
                            )
                            close()
                        }
                        drawPath(path, color = ShutterGold)
                        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = center)
                    }
                    Text(
                        text = "${azimuth.roundToInt()}°",
                        color = Color.White,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ShutterGold)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Facing $cardinal",
                        color = Color(0xFF141619),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Right Location + Map + Telemetry Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = telemetry.displayHeadlineAddress,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (12f * scale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = telemetry.formatCoordinates(config.coordinateFormat),
                    color = TelemetryEmerald,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = (8.8f * scale).sp,
                    maxLines = 1
                )
                Text(
                    text = telemetry.formatTimestamp(config.dateTimeFormat),
                    color = Color(0xFFD0DCE5),
                    fontSize = (8.5f * scale).sp,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val altText = telemetry.altitudeMeters?.let { "${it.roundToInt()} m" } ?: "-- m"
                    val magText = telemetry.magneticFieldMicroTesla?.let { "${it.roundToInt()} µT" } ?: "-- µT"
                    Text(
                        text = "Alt $altText",
                        color = ShutterGold,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = magText,
                        color = SignalCyan,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (config.showMiniMap && miniMapBitmap != null) {
                Image(
                    bitmap = miniMapBitmap.asImageBitmap(),
                    contentDescription = "Compass mini-map",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size((48f * scale).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                )
            }

            BrandWatermarkColumn(
                config = config,
                telemetry = telemetry,
                titleColor = Color.White,
                primaryFont = PlusJakartaSansFontFamily,
                scale = scale * 0.88f,
                logoAlpha = config.logoOpacity,
                isFrontCamera = isFrontCamera
            )
        }
    }
}

@Composable
private fun BrandWatermarkColumn(
    config: StampConfig,
    telemetry: GpsTelemetry,
    titleColor: Color,
    primaryFont: androidx.compose.ui.text.font.FontFamily,
    scale: Float,
    logoAlpha: Float,
    isFrontCamera: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.width((68f * scale).dp)
    ) {
        YvGpsCamLogoBadge(
            config = config,
            size = (36f * scale).dp,
            alpha = logoAlpha
        )
        Text(
            text = config.customBrandLabel.ifBlank { "YVGPSCAM" },
            color = titleColor,
            fontFamily = primaryFont,
            fontWeight = FontWeight.Bold,
            fontSize = (8.0f * scale).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (config.showCameraIndicator) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = if (isFrontCamera) Icons.Default.CameraFront else Icons.Default.CameraRear,
                    contentDescription = null,
                    tint = ShutterGold,
                    modifier = Modifier.size((10f * scale).dp)
                )
                Text(
                    text = if (isFrontCamera) "Front" else "Rear",
                    color = titleColor,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = (7.5f * scale).sp
                )
            }
        } else if (config.showWeather && telemetry.weatherTempCelsius != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Weather temperature",
                    tint = ShutterGold,
                    modifier = Modifier.size((11f * scale).dp)
                )
                Text(
                    text = "${telemetry.weatherTempCelsius.roundToInt()}°C",
                    color = titleColor,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (8.5f * scale).sp,
                    maxLines = 1
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = null,
                    tint = if (telemetry.hasFix) TelemetryEmerald else ShutterGold,
                    modifier = Modifier.size((11f * scale).dp)
                )
                Text(
                    text = if (telemetry.accuracyMeters != null) {
                        "±${telemetry.accuracyMeters.roundToInt()}m"
                    } else if (telemetry.hasFix) {
                        "LOCK"
                    } else {
                        "WAIT"
                    },
                    color = titleColor,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (8.3f * scale).sp,
                    maxLines = 1
                )
            }
        }
    }
}

private fun buildTelemetryMetricItems(
    telemetry: GpsTelemetry,
    config: StampConfig
): List<Triple<ImageVector, Color, String>> = buildList {
    if (config.showCompassAndSpeed) {
        val windOrSpeed = telemetry.weatherWindKmh ?: telemetry.speedKmh
        val speedStr = windOrSpeed?.let { String.format(Locale.US, "%.0f km/h", it) }
            ?: "${telemetry.compassAzimuth.roundToInt()}° ${telemetry.compassCardinal}"
        add(Triple(Icons.Default.Air, SignalCyan, speedStr))
        if (windOrSpeed != null) {
            add(
                Triple(
                    Icons.Default.Explore,
                    Color(0xFF4FC3F7),
                    "${telemetry.compassAzimuth.roundToInt()}° ${telemetry.compassCardinal}"
                )
            )
        }
    }
    if (config.showWeather && telemetry.weatherHumidityPercent != null) {
        add(Triple(Icons.Default.WaterDrop, Color(0xFF60A5FA), "${telemetry.weatherHumidityPercent}%"))
    }
    if (config.showAltitude) {
        val altStr = telemetry.altitudeMeters?.let { String.format(Locale.US, "%.0f m", it) } ?: "-- m"
        add(Triple(Icons.Default.Landscape, ShutterGold, altStr))
    }
    if (config.showMagneticField) {
        val magStr = telemetry.magneticFieldMicroTesla?.let { String.format(Locale.US, "%.0f µT", it) } ?: "-- µT"
        add(Triple(Icons.Default.Sensors, AlertCoral, magStr))
    }
    if (config.showPressure) {
        val pressStr = telemetry.pressureHpa?.let { String.format(Locale.US, "%.0f hPa", it) } ?: "-- hPa"
        add(Triple(Icons.Default.Compress, Color(0xFFA78BFA), pressStr))
    }
    if (config.showAccuracy) {
        val accStr = telemetry.accuracyMeters?.let { String.format(Locale.US, "±%.0fm", it) } ?: "-- m"
        add(Triple(Icons.Default.GpsFixed, TelemetryEmerald, accStr))
    }
}

@Composable
private fun MetricChipItem(
    icon: ImageVector,
    tint: Color,
    label: String,
    textColor: Color,
    scale: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((11.5f * scale).dp)
        )
        Text(
            text = label,
            color = textColor,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (8.6f * scale).sp,
            maxLines = 1
        )
    }
}
