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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.camera.ExifMetadataDetails
import com.example.data.model.CapturedPhotoEntity
import com.example.data.model.GpsTelemetry
import com.example.data.model.MapStyle
import com.example.map.PhotoDetailGoogleMap
import com.example.map.openDirectionsInGoogleMaps
import com.example.ui.components.AdBannerSlot
import com.example.ui.components.SystemBarsImmersiveEffect
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ShutterGold
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val allPhotos by galleryViewModel.photos.collectAsStateWithLifecycle()
    val fallbackPhoto by galleryViewModel.getPhotoFlow(photoId).collectAsStateWithLifecycle(initialValue = null)
    val mapThumbnails by galleryViewModel.mapThumbnailCache.collectAsStateWithLifecycle()
    val stampConfig by galleryViewModel.stampConfig.collectAsStateWithLifecycle()

    val pagerPhotos: List<CapturedPhotoEntity> = remember(allPhotos, fallbackPhoto, photoId) {
        when {
            allPhotos.any { it.id == photoId } -> allPhotos
            fallbackPhoto != null -> listOf(fallbackPhoto!!) + allPhotos.filter { it.id != photoId }
            else -> allPhotos
        }
    }

    val initialIndex = remember(pagerPhotos, photoId) {
        pagerPhotos.indexOfFirst { it.id == photoId }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { pagerPhotos.size }
    )

    var hasInitializedPage by remember(photoId) { mutableStateOf(false) }
    LaunchedEffect(pagerPhotos.size, photoId) {
        if (!hasInitializedPage && pagerPhotos.isNotEmpty()) {
            val targetIdx = pagerPhotos.indexOfFirst { it.id == photoId }
            if (targetIdx >= 0) {
                pagerState.scrollToPage(targetIdx)
                hasInitializedPage = true
            }
        }
    }

    val currentPhoto = pagerPhotos.getOrNull(pagerState.currentPage) ?: fallbackPhoto
    val activePhotoId = currentPhoto?.id ?: photoId

    var showDetails by remember { mutableStateOf(false) }
    var exifDetails by remember(activePhotoId) { mutableStateOf<ExifMetadataDetails?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isFullScreenPhoto by remember { mutableStateOf(false) }
    var showFullScreenChrome by remember { mutableStateOf(true) }

    SystemBarsImmersiveEffect(immersive = isFullScreenPhoto)
    BackHandler(enabled = isFullScreenPhoto || showDetails) {
        if (isFullScreenPhoto) {
            isFullScreenPhoto = false
        } else if (showDetails) {
            showDetails = false
        }
    }

    // Load EXIF & map preview for whichever photo the user swiped onto when Details is opened
    LaunchedEffect(currentPhoto?.id, currentPhoto?.mediaUri, stampConfig.mapStyle, showDetails) {
        val target = currentPhoto ?: return@LaunchedEffect
        if (showDetails) {
            galleryViewModel.ensureMapThumbnailLoaded(target, offlineOnlyMapMode, stampConfig.mapStyle)
            if (exifDetails == null) {
                exifDetails = galleryViewModel.loadExifDetails(target.mediaUri)
            }
        }
    }

    if (currentPhoto == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading photo...", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            type = if (currentPhoto.isVideo) "video/mp4" else "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(currentPhoto.mediaUri))
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Geotagged Capture"))
    }

    fun openVideoExternally(photo: CapturedPhotoEntity) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(photo.mediaUri), "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(viewIntent)
        }.onFailure {
            Toast.makeText(context, "Unable to open video player", Toast.LENGTH_SHORT).show()
        }
    }

    if (isFullScreenPhoto) {
        val fullScreenViewer: @Composable () -> Unit = {
            var currentItemZoomed by remember(currentPhoto.id) { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .testTag("photo_fullscreen_viewer")
            ) {
                HorizontalPager(
                    state = pagerState,
                    key = { index -> pagerPhotos.getOrNull(index)?.id ?: index },
                    userScrollEnabled = !currentItemZoomed,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val pagePhoto = pagerPhotos[pageIndex]
                    var fsZoom by remember(pagePhoto.id) { mutableFloatStateOf(1f) }
                    var fsPan by remember(pagePhoto.id) { mutableStateOf(Offset.Zero) }

                    LaunchedEffect(fsZoom, pagerState.currentPage) {
                        if (pagerState.currentPage == pageIndex) {
                            currentItemZoomed = fsZoom > 1.05f
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pagePhoto.id, currentItemZoomed) {
                                detectTapGestures(
                                    onTap = { showFullScreenChrome = !showFullScreenChrome },
                                    onDoubleTap = {
                                        if (fsZoom > 1.05f) {
                                            fsZoom = 1f
                                            fsPan = Offset.Zero
                                            currentItemZoomed = false
                                        } else {
                                            fsZoom = 2.5f
                                            currentItemZoomed = true
                                        }
                                    }
                                )
                            }
                            .then(
                                if (fsZoom > 1.05f) {
                                    Modifier.pointerInput(pagePhoto.id) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            val nextScale = (fsZoom * zoom).coerceIn(1f, 5f)
                                            fsZoom = nextScale
                                            currentItemZoomed = nextScale > 1.05f
                                            fsPan = if (nextScale > 1.05f) {
                                                Offset(
                                                    x = (fsPan.x + pan.x).coerceIn(-900f, 900f),
                                                    y = (fsPan.y + pan.y).coerceIn(-900f, 900f)
                                                )
                                            } else {
                                                Offset.Zero
                                            }
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = pagePhoto.mediaUri,
                            contentDescription = pagePhoto.displayName,
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
                        if (pagePhoto.isVideo) {
                            IconButton(
                                onClick = { openVideoExternally(pagePhoto) },
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleFilled,
                                    contentDescription = "Play video",
                                    tint = ShutterGold,
                                    modifier = Modifier.size(56.dp)
                                )
                            }
                        }
                    }
                }

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
                            .background(Color.Black.copy(alpha = 0.65f))
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
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${pagerState.currentPage + 1}/${pagerPhotos.size.coerceAtLeast(1)} • $dateFormatted",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = TelemetryEmerald,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                            text = if (pagerPhotos.size > 1) {
                                "Swipe to switch • Double-tap to zoom • Tap to hide"
                            } else {
                                "Double-tap to zoom • Tap to hide controls"
                            },
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
            .background(MaterialTheme.colorScheme.background)
            .then(if (showBackButton) Modifier.statusBarsPadding() else Modifier)
            .navigationBarsPadding()
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to gallery"
                        )
                    }
                }
                Column(modifier = Modifier.padding(start = if (showBackButton) 0.dp else 8.dp)) {
                    Text(
                        text = currentPhoto.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val pageCounter = if (pagerPhotos.size > 1) {
                        "${pagerState.currentPage + 1}/${pagerPhotos.size} • "
                    } else {
                        ""
                    }
                    Text(
                        text = "$pageCounter${currentPhoto.width}×${currentPhoto.height} px • ${currentPhoto.templateName}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row {
                IconButton(
                    onClick = { isFullScreenPhoto = true },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("detail_top_fullscreen_button")
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = "View full screen")
                }
                IconButton(
                    onClick = shareCurrentPhoto,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("detail_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share photo", tint = TelemetryEmerald)
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("detail_delete_button")
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete photo",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Side-Scrollable HorizontalPager Viewport (full height when Details hidden; responsive split when Details open)
        Box(
            modifier = Modifier
                .weight(if (showDetails) 0.42f else 1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
                .testTag("photo_detail_pager_container")
        ) {
            HorizontalPager(
                state = pagerState,
                key = { index -> pagerPhotos.getOrNull(index)?.id ?: index },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("photo_detail_horizontal_pager")
            ) { pageIndex ->
                val pagePhoto = pagerPhotos[pageIndex]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pagePhoto.id) {
                            detectTapGestures(
                                onTap = {
                                    if (pagePhoto.isVideo) {
                                        openVideoExternally(pagePhoto)
                                    } else {
                                        isFullScreenPhoto = true
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = pagePhoto.mediaUri,
                        contentDescription = pagePhoto.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (pagePhoto.isVideo) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.58f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = "Play video",
                                tint = ShutterGold,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }

            if (pagerPhotos.size > 1) {
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.42f))
                            .testTag("detail_prev_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous photo",
                            tint = Color.White
                        )
                    }
                }

                if (pagerState.currentPage < pagerPhotos.lastIndex) {
                    IconButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.42f))
                            .testTag("detail_next_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next photo",
                            tint = Color.White
                        )
                    }
                }
            }

            Surface(
                color = Color.Black.copy(alpha = 0.62f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = if (pagerPhotos.size > 1) {
                        "Swipe left/right • Tap for full screen"
                    } else {
                        "Tap for full screen"
                    },
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

        // Bottom Bar below photo: Information stays hidden unless user clicks "Details"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showDetails = !showDetails },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (showDetails) ShutterGold else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (showDetails) ObsidianBg else MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("detail_toggle_details_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (showDetails) "Hide Details" else "Details",
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (showDetails) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (currentPhoto.latitude != null && currentPhoto.longitude != null) {
                OutlinedButton(
                    onClick = {
                        openDirectionsInGoogleMaps(
                            context = context,
                            latitude = currentPhoto.latitude,
                            longitude = currentPhoto.longitude,
                            label = currentPhoto.displayName
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("detail_quick_maps_button")
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = TelemetryEmerald, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Map", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Expandable Information Section — ONLY visible when user explicitly clicks Details
        if (showDetails) {
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 14.dp)
                    .testTag("photo_detail_expanded_info")
            ) {
                // Location Map + Coordinates Action Card
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
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
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
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
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
            }
        }

        AdBannerSlot(placementId = "photo_detail")
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Geotagged Capture?") },
            text = { Text("This removes ${currentPhoto.displayName} from your YVGPSCAM gallery and device storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        val isLast = pagerPhotos.size <= 1
                        galleryViewModel.deletePhoto(
                            photo = currentPhoto,
                            onDeleted = {
                                if (isLast) {
                                    onBack()
                                } else {
                                    Toast.makeText(context, "Capture deleted", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
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
