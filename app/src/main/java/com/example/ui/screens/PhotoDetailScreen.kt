package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.camera.ExifMetadataDetails
import com.example.data.model.GpsTelemetry
import com.example.data.model.MapStyle
import com.example.map.PhotoDetailGoogleMap
import com.example.map.openDirectionsInGoogleMaps
import com.example.ui.components.SystemBarsImmersiveEffect
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.viewmodel.GalleryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PhotoDetailScreen(
    photoId: Long,
    galleryViewModel: GalleryViewModel,
    offlineOnlyMapMode: Boolean,
    showBackButton: Boolean = true,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photo by galleryViewModel.getPhotoFlow(photoId).collectAsStateWithLifecycle(initialValue = null)
    val mapThumbnails by galleryViewModel.mapThumbnailCache.collectAsStateWithLifecycle()
    val stampConfig by galleryViewModel.stampConfig.collectAsStateWithLifecycle()

    var exifDetails by remember(photoId) { mutableStateOf<ExifMetadataDetails?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isFullScreenPhoto by remember(photoId) { mutableStateOf(false) }
    var showFullScreenChrome by remember { mutableStateOf(true) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    SystemBarsImmersiveEffect(immersive = isFullScreenPhoto)
    BackHandler(enabled = isFullScreenPhoto) {
        isFullScreenPhoto = false
    }

    LaunchedEffect(photo?.id, photo?.mediaUri, stampConfig.mapStyle) {
        val current = photo ?: return@LaunchedEffect
        galleryViewModel.ensureMapThumbnailLoaded(current, offlineOnlyMapMode, stampConfig.mapStyle)
        if (exifDetails == null) {
            exifDetails = galleryViewModel.loadExifDetails(current.mediaUri)
        }
    }

    val currentPhoto = photo
    if (currentPhoto == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianBg),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading photo details...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val mapBitmap = mapThumbnails[currentPhoto.id]
    val dateFormatted = remember(currentPhoto.capturedAtMillis) {
        SimpleDateFormat("dd MMM yyyy, hh:mm:ss a z", Locale.getDefault())
            .format(Date(currentPhoto.capturedAtMillis))
    }

    val shareCurrentPhoto: () -> Unit = {
        val coords = if (currentPhoto.latitude != null && currentPhoto.longitude != null) {
            String.format(Locale.US, "%.6f, %.6f", currentPhoto.latitude, currentPhoto.longitude)
        } else {
            "GPS unavailable"
        }
        val locationLine = listOf(currentPhoto.addressLine, currentPhoto.cityState, currentPhoto.country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        val caption = buildString {
            append("Captured with YVGPSCAM\nCoordinates: $coords")
            if (locationLine.isNotBlank()) append("\nLocation: $locationLine")
            if (currentPhoto.customNote.isNotBlank()) append("\nNote: ${currentPhoto.customNote}")
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(currentPhoto.mediaUri))
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Geotagged Photo"))
    }

    if (isFullScreenPhoto) {
        val fullScreenViewer: @Composable () -> Unit = {
            var fsZoom by remember(currentPhoto.id) { mutableFloatStateOf(1f) }
            var fsPan by remember(currentPhoto.id) { mutableStateOf(Offset.Zero) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (fsZoom * zoom).coerceIn(1f, 5f)
                            fsZoom = nextScale
                            fsPan = if (nextScale > 1f) {
                                Offset(
                                    x = (fsPan.x + pan.x).coerceIn(-800f, 800f),
                                    y = (fsPan.y + pan.y).coerceIn(-800f, 800f)
                                )
                            } else {
                                Offset.Zero
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showFullScreenChrome = !showFullScreenChrome },
                            onDoubleTap = {
                                if (fsZoom > 1.05f) {
                                    fsZoom = 1f
                                    fsPan = Offset.Zero
                                } else {
                                    fsZoom = 2.5f
                                }
                            }
                        )
                    }
                    .testTag("photo_fullscreen_viewer")
            ) {
                AsyncImage(
                    model = currentPhoto.mediaUri,
                    contentDescription = currentPhoto.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = fsZoom
                            scaleY = fsZoom
                            translationX = fsPan.x
                            translationY = fsPan.y
                        }
                )

                AnimatedVisibility(
                    visible = showFullScreenChrome,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.62f))
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(
                                onClick = { isFullScreenPhoto = false },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("exit_photo_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "Exit full screen viewer",
                                    tint = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = currentPhoto.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dateFormatted,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = TelemetryEmerald
                                )
                            }
                        }

                        IconButton(
                            onClick = shareCurrentPhoto,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share photo",
                                tint = TelemetryEmerald
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showFullScreenChrome,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Double-tap to zoom • Tap to hide controls",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        if (showBackButton) {
            fullScreenViewer()
            return
        } else {
            Dialog(
                onDismissRequest = { isFullScreenPhoto = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                fullScreenViewer()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .then(if (showBackButton) Modifier.statusBarsPadding() else Modifier)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to gallery"
                        )
                    }
                }
                Column {
                    Text(
                        text = currentPhoto.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentPhoto.width}×${currentPhoto.height} px • ${currentPhoto.templateName}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(
                    onClick = { isFullScreenPhoto = true },
                    modifier = Modifier.testTag("detail_top_fullscreen_button")
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "View full screen", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(
                    onClick = shareCurrentPhoto,
                    modifier = Modifier.testTag("detail_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share photo", tint = TelemetryEmerald)
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.testTag("detail_delete_button")
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete photo",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Pinch-to-Zoom Photo Viewport (Tap for Full Screen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ObsidianSurface)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (zoomScale * zoom).coerceIn(1f, 4f)
                        zoomScale = nextScale
                        panOffset = if (nextScale > 1f) {
                            Offset(
                                x = (panOffset.x + pan.x).coerceIn(-400f, 400f),
                                y = (panOffset.y + pan.y).coerceIn(-400f, 400f)
                            )
                        } else {
                            Offset.Zero
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { isFullScreenPhoto = true },
                        onDoubleTap = {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = currentPhoto.mediaUri,
                contentDescription = currentPhoto.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
            )

            Surface(
                color = Color.Black.copy(alpha = 0.62f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = "Tap for full screen",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            IconButton(
                onClick = { isFullScreenPhoto = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .testTag("detail_fullscreen_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Open full screen viewer",
                    tint = TelemetryEmerald
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Location Map + Coordinates Action Card
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val lat = currentPhoto.latitude
                val lon = currentPhoto.longitude

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "GPS Location Telemetry",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (lat != null && lon != null) {
                        Text(
                            text = "${GpsTelemetry.formatSingleCoord(lat, true)}  •  ${GpsTelemetry.formatSingleCoord(lon, false)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = TelemetryEmerald
                        )
                        Text(
                            text = "${GpsTelemetry.toDms(lat, true)}  ${GpsTelemetry.toDms(lon, false)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Coordinates unavailable at capture",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (lat != null && lon != null) {
                    PhotoDetailGoogleMap(
                        latitude = lat,
                        longitude = lon,
                        title = currentPhoto.displayName,
                        snippet = currentPhoto.cityState.ifBlank { currentPhoto.addressLine },
                        mapStyle = stampConfig.mapStyle,
                        offlineOnlyMapMode = offlineOnlyMapMode,
                        fallbackBitmap = mapBitmap
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MapStyle.entries.forEach { style ->
                        FilterChip(
                            selected = stampConfig.mapStyle == style,
                            onClick = { galleryViewModel.setMapStyle(style) },
                            label = { Text(style.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TelemetryEmerald.copy(alpha = 0.2f),
                                selectedLabelColor = TelemetryEmerald
                            )
                        )
                    }
                }

                if (lat != null && lon != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val coordText = String.format(Locale.US, "%.6f, %.6f", lat, lon)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("GPS Coordinates", coordText))
                                Toast.makeText(context, "Copied $coordText", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy Coords")
                        }

                        Button(
                            onClick = {
                                openDirectionsInGoogleMaps(
                                    context = context,
                                    latitude = lat,
                                    longitude = lon,
                                    label = currentPhoto.displayName
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TelemetryEmerald,
                                contentColor = ObsidianBg
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("detail_open_google_maps_button")
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Google Maps", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Detailed EXIF & Metadata Card
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Embedded EXIF & Sensor Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(color = ObsidianBorder)

                ExifRow("Captured Timestamp", dateFormatted)
                if (currentPhoto.addressLine.isNotBlank()) {
                    ExifRow("Address", currentPhoto.addressLine)
                }
                if (currentPhoto.cityState.isNotBlank() || currentPhoto.country.isNotBlank()) {
                    ExifRow(
                        "Locality",
                        listOf(currentPhoto.cityState, currentPhoto.country).filter { it.isNotBlank() }.joinToString(", ")
                    )
                }
                currentPhoto.altitudeMeters?.let {
                    ExifRow("Elevation (Altitude)", String.format(Locale.US, "%.1f meters", it))
                }
                currentPhoto.accuracyMeters?.let {
                    ExifRow("GPS Accuracy", String.format(Locale.US, "±%.1f meters", it))
                }
                ExifRow("Compass Heading", "${currentPhoto.compassAzimuth.roundToInt()}° (${currentPhoto.compassCardinal})")
                if (currentPhoto.customNote.isNotBlank()) {
                    ExifRow("Field Note", currentPhoto.customNote)
                }

                HorizontalDivider(color = ObsidianBorder)

                val exif = exifDetails
                ExifRow("EXIF Make / Software", "${exif?.make ?: "YVGPSCAM"} • ${exif?.software ?: "YVGPSCAM 1.0"}")
                ExifRow("Device Model", exif?.model ?: android.os.Build.MODEL)
                ExifRow("EXIF DateTimeOriginal", exif?.dateTimeOriginal ?: dateFormatted)
                if (exif?.gpsLatitude != null && exif.gpsLongitude != null) {
                    ExifRow(
                        "EXIF GPS Tag",
                        String.format(Locale.US, "%.6f, %.6f", exif.gpsLatitude, exif.gpsLongitude)
                    )
                }
                exif?.gpsAltitude?.let {
                    ExifRow("EXIF GPS Altitude", String.format(Locale.US, "%.1f m", it))
                }
                exif?.gpsDirection?.let {
                    ExifRow("EXIF Img Direction", "$it (Magnetic)")
                }
                ExifRow("Dimensions", "${currentPhoto.width} × ${currentPhoto.height} px")
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Geotagged Photo?") },
            text = { Text("This removes ${currentPhoto.displayName} from your YVGPSCAM gallery and device storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        galleryViewModel.deletePhoto(currentPhoto, onDeleted = onBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ExifRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f)
        )
    }
}
