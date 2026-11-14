package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CapturedPhotoEntity
import com.example.data.model.StampConfig
import com.example.ui.theme.FocusReticleGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ShutterGold
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.viewmodel.CameraHudState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Expandable Quick Tools grid matching IMG_4851 & IMG_4859:
 * Ratio | Grid | Timer | Full View / Mirror | Voice | Level | Add Text / Stamp Pos | Import | Drive.
 */
@Composable
fun CameraQuickToolsDrawer(
    hudState: CameraHudState,
    config: StampConfig,
    onCycleRatio: () -> Unit,
    onToggleGrid: () -> Unit,
    onCycleTimer: () -> Unit,
    onToggleFullView: () -> Unit,
    onToggleMirror: () -> Unit,
    onToggleVoice: () -> Unit,
    onToggleLevel: () -> Unit,
    onCycleStampPosition: () -> Unit,
    onOpenNoteDialog: () -> Unit,
    onImportPhoto: () -> Unit,
    onOpenDriveSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xE8161B22),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .testTag("camera_quick_tools_drawer")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Row 1: Ratio | Grid | Timer | Full View
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickToolTile(
                    icon = Icons.Default.AspectRatio,
                    label = "Ratio (${hudState.aspectRatio.label})",
                    active = hudState.aspectRatio.label != "3:4",
                    onClick = onCycleRatio,
                    testTag = "quick_tool_ratio",
                    modifier = Modifier.weight(1f)
                )
                QuickToolTile(
                    icon = Icons.Default.GridOn,
                    label = "Grid",
                    active = hudState.showGrid,
                    onClick = onToggleGrid,
                    testTag = "quick_tool_grid",
                    modifier = Modifier.weight(1f)
                )
                QuickToolTile(
                    icon = Icons.Default.Timer,
                    label = if (config.timerDelaySeconds > 0) "Timer ${config.timerDelaySeconds}s" else "Timer",
                    active = config.timerDelaySeconds > 0,
                    onClick = onCycleTimer,
                    testTag = "quick_tool_timer",
                    modifier = Modifier.weight(1f)
                )
                QuickToolTile(
                    icon = if (hudState.isFullScreenCamera) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    label = "Full View",
                    active = hudState.isFullScreenCamera,
                    onClick = onToggleFullView,
                    testTag = "quick_tool_full_view",
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Mirror | Voice | Level | Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickToolTile(
                    icon = Icons.Default.Flip,
                    label = "Mirror",
                    active = config.mirrorFrontCamera,
                    onClick = onToggleMirror,
                    testTag = "quick_tool_mirror",
                    modifier = Modifier.weight(1f)
                )
                QuickToolTile(
                    icon = if (config.recordVideoWithVoice) Icons.Default.Mic else Icons.Default.MicOff,
                    label = "Voice",
                    active = config.recordVideoWithVoice,
                    onClick = onToggleVoice,
                    testTag = "quick_tool_voice",
                    modifier = Modifier.weight(1f)
                )
                QuickToolTile(
                    icon = Icons.Default.ScreenRotation,
                    label = "Level",
                    active = config.showDigitalLevel,
                    onClick = onToggleLevel,
                    testTag = "quick_tool_level",
                    modifier = Modifier.weight(1f)
                )
                QuickToolTile(
                    icon = Icons.Default.EditNote,
                    label = "Add Text",
                    active = config.customNote.isNotBlank(),
                    onClick = onOpenNoteDialog,
                    testTag = "quick_tool_note",
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 3: Stamp Pos | Import Stamp | Drive Backup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onCycleStampPosition)
                        .testTag("camera_stamp_position_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AddLocationAlt,
                            contentDescription = "Cycle stamp position",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Pos: ${config.stampPosition.shortLabel}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onImportPhoto)
                        .testTag("quick_tool_import")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Stamp Gallery Photo",
                            tint = ShutterGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Stamp Photo",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
                Surface(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenDriveSync)
                        .testTag("quick_tool_drive")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Drive Sync",
                            tint = TelemetryEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Drive Sync",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickToolTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (active) ShutterGold else Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color(0xFF141619) else Color.White,
                modifier = Modifier.size(19.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (active) ShutterGold else Color.White.copy(alpha = 0.9f),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Digital Horizon Level indicator overlay drawn at the center of the camera preview.
 */
@Composable
fun DigitalHorizonLevelOverlay(
    pitchDeg: Float,
    rollDeg: Float,
    modifier: Modifier = Modifier
) {
    val isLevel = abs(rollDeg) <= 1.5f
    val lineColor = if (isLevel) FocusReticleGreen else ShutterGold

    Column(
        modifier = modifier.testTag("camera_digital_horizon_level"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.size(width = 160.dp, height = 38.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val rad = Math.toRadians(rollDeg.coerceIn(-45f, 45f).toDouble())
            val halfSpan = 46.dp.toPx()
            val dx = (cos(rad) * halfSpan).toFloat()
            val dy = (sin(rad) * halfSpan).toFloat()

            // Fixed outer reference ticks
            drawLine(
                color = Color.White.copy(alpha = 0.55f),
                start = Offset(cx - halfSpan - 14.dp.toPx(), cy),
                end = Offset(cx - halfSpan - 4.dp.toPx(), cy),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = 0.55f),
                start = Offset(cx + halfSpan + 4.dp.toPx(), cy),
                end = Offset(cx + halfSpan + 14.dp.toPx(), cy),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Rotating horizon bar
            drawLine(
                color = lineColor,
                start = Offset(cx - dx, cy - dy),
                end = Offset(cx + dx, cy + dy),
                strokeWidth = 2.6.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = lineColor,
                radius = 3.5.dp.toPx(),
                center = Offset(cx, cy)
            )
        }
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "LVL ${rollDeg.roundToInt()}° · P ${pitchDeg.roundToInt()}°",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = lineColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Clickable QR detection pill shown at the top of the viewfinder when ZXing decodes a live QR code.
 */
@Composable
fun DetectedQrActionBanner(
    qrText: String,
    context: Context,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isWebUrl = qrText.startsWith("http://", ignoreCase = true) ||
        qrText.startsWith("https://", ignoreCase = true) ||
        qrText.startsWith("geo:", ignoreCase = true)

    Surface(
        color = Color(0xEB111822),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .padding(horizontal = 16.dp)
            .border(1.dp, ShutterGold.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
            .clickable {
                if (isWebUrl) {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(qrText)))
                    }.onFailure {
                        copyQrToClipboard(context, qrText)
                    }
                } else {
                    copyQrToClipboard(context, qrText)
                }
            }
            .testTag("camera_detected_qr_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Detected QR code",
                tint = ShutterGold,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = if (isWebUrl) "QR Link Detected • Tap to Open" else "QR Text Detected • Tap to Copy",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ShutterGold
                )
                Text(
                    text = qrText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (isWebUrl) Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.ContentCopy,
                contentDescription = null,
                tint = TelemetryEmerald,
                modifier = Modifier.size(16.dp)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss QR banner", tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
    }
}

private fun copyQrToClipboard(context: Context, text: String) {
    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cb?.setPrimaryClip(ClipData.newPlainText("Scanned QR", text))
    Toast.makeText(context, "QR copied: $text", Toast.LENGTH_SHORT).show()
}

fun launchPostCaptureAction(
    context: Context,
    entity: CapturedPhotoEntity,
    emailRecipient: String,
    preferEmail: Boolean
) {
    val uri = Uri.parse(entity.mediaUri)
    val coords = if (entity.latitude != null && entity.longitude != null) {
        String.format(Locale.US, "%.6f, %.6f", entity.latitude, entity.longitude)
    } else {
        "GPS unavailable"
    }
    val locationLine = listOf(entity.addressLine, entity.cityState, entity.country)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    val bodyText = buildString {
        append("Captured with YVGPSCAM\n")
        append("Coordinates: $coords")
        if (locationLine.isNotBlank()) append("\nLocation: $locationLine")
        if (entity.customNote.isNotBlank()) append("\nNote: ${entity.customNote}")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (entity.isVideo) "video/mp4" else "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, bodyText)
        if (preferEmail) {
            putExtra(Intent.EXTRA_SUBJECT, "YVGPSCAM Field Capture: ${entity.displayName}")
            if (emailRecipient.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailRecipient))
            }
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooserTitle = if (preferEmail) "Email Captured Photo" else "Share Geotagged Photo"
    runCatching {
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
