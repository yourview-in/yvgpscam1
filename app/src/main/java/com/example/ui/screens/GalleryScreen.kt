package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.CapturedPhotoEntity
import com.example.data.model.GpsTelemetry
import com.example.ui.components.AdBannerSlot
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.ShutterGold
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.viewmodel.DateFilterPreset
import com.example.ui.viewmodel.GalleryViewMode
import com.example.ui.viewmodel.GalleryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    galleryViewModel: GalleryViewModel,
    offlineOnlyMapMode: Boolean,
    onPhotoClick: (Long) -> Unit,
    onGoToCamera: () -> Unit,
    onOpenImageOnMap: () -> Unit = {},
    onOpenPdfReport: (List<Long>) -> Unit = {},
    onOpenCloudSync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photos by galleryViewModel.photos.collectAsStateWithLifecycle()
    val searchQuery by galleryViewModel.searchQuery.collectAsStateWithLifecycle()
    val viewMode by galleryViewModel.viewMode.collectAsStateWithLifecycle()
    val dateFilter by galleryViewModel.dateFilter.collectAsStateWithLifecycle()
    val renameStatusMessage by galleryViewModel.renameStatusMessage.collectAsStateWithLifecycle()
    val isSelectionMode by galleryViewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedPhotoIds by galleryViewModel.selectedPhotoIds.collectAsStateWithLifecycle()

    var selectedPhotoIdForSplit by remember { mutableStateOf<Long?>(null) }
    var showSingleDatePicker by remember { mutableStateOf(false) }
    var showRangeDatePicker by remember { mutableStateOf(false) }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        galleryViewModel.clearSelection()
    }

    fun handleMediaItemTap(photo: CapturedPhotoEntity) {
        if (isSelectionMode) {
            galleryViewModel.togglePhotoSelection(photo.id)
        } else if (photo.isVideo) {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(photo.mediaUri), "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching {
                context.startActivity(viewIntent)
            }.onFailure {
                Toast.makeText(context, "Unable to open video player", Toast.LENGTH_SHORT).show()
            }
        } else if (selectedPhotoIdForSplit != null) {
            selectedPhotoIdForSplit = photo.id
        } else {
            onPhotoClick(photo.id)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        val isExpanded = maxWidth >= 840.dp

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(if (isExpanded) 0.48f else 1f)
                    .fillMaxHeight()
            ) {
                // Header: Multi-Select Action Bar (IMG_4861) or Clean Gallery Top Bar (IMG_4860)
                if (isSelectionMode) {
                    val allVisibleSelected = photos.isNotEmpty() && selectedPhotoIds.size == photos.size
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // "< Back" Pill Button matching IMG_4861
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { galleryViewModel.clearSelection() }
                                    .testTag("gallery_cancel_selection_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Exit multi-select",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        text = "Back",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Text(
                                text = if (selectedPhotoIds.size == 1) {
                                    "1 item sele..."
                                } else {
                                    "${selectedPhotoIds.size} selected"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )

                            // Right Pill Container with Delete | Rename | Share | PDF Report icons (IMG_4861)
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = CircleShape,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    IconButton(
                                        enabled = selectedPhotoIds.isNotEmpty(),
                                        onClick = { showBatchDeleteConfirm = true },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("gallery_delete_selected_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete selected",
                                            tint = if (selectedPhotoIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        enabled = selectedPhotoIds.isNotEmpty(),
                                        onClick = { showBatchRenameDialog = true },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("gallery_rename_selected_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DriveFileRenameOutline,
                                            contentDescription = "Rename selected",
                                            tint = if (selectedPhotoIds.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        enabled = selectedPhotoIds.isNotEmpty(),
                                        onClick = {
                                            val selectedPhotos = photos.filter { selectedPhotoIds.contains(it.id) }
                                            shareMultiplePhotosViaAndroidSheet(context, selectedPhotos)
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("gallery_share_selected_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share selected",
                                            tint = if (selectedPhotoIds.isNotEmpty()) TelemetryEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        enabled = selectedPhotoIds.isNotEmpty(),
                                        onClick = { onOpenPdfReport(selectedPhotoIds.toList()) },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .testTag("gallery_pdf_selected_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = "Create PDF report for selected",
                                            tint = if (selectedPhotoIds.isNotEmpty()) ShutterGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Compact Select All / Clear All bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { galleryViewModel.selectAllVisiblePhotos() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("gallery_select_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = null,
                                    tint = ShutterGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (allVisibleSelected) "Clear All" else "Select All (${photos.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ShutterGold
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = onGoToCamera,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .testTag("gallery_back_to_camera_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Back to camera"
                                )
                            }
                            Image(
                                painter = painterResource(id = R.drawable.img_brand_yv_logo),
                                contentDescription = "YVGPSCAM Brand Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            )
                            Column {
                                Text(
                                    text = "Collection",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${photos.size} geotagged",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.5.sp,
                                    color = TelemetryEmerald
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Map Icon -> Loads Interactive Google Map Screen (IMG_4862)
                            IconButton(
                                onClick = onOpenImageOnMap,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .testTag("gallery_open_google_map_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Map,
                                    contentDescription = "Open Google Map",
                                    tint = TelemetryEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Cloud Sync Shortcut
                            IconButton(
                                onClick = onOpenCloudSync,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .testTag("gallery_cloud_sync_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Cloud Backup",
                                    tint = ShutterGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // List / Grid toggle
                            IconButton(
                                onClick = { galleryViewModel.toggleViewMode() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .testTag("gallery_toggle_layout_button")
                            ) {
                                Icon(
                                    imageVector = if (viewMode == GalleryViewMode.GRID) Icons.Default.ViewAgenda else Icons.Default.GridView,
                                    contentDescription = "Toggle grid or list layout",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Multi-Select Mode Toggle
                            if (photos.isNotEmpty()) {
                                IconButton(
                                    onClick = { galleryViewModel.enterSelectionMode() },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                        .testTag("gallery_enter_select_mode_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Checklist,
                                        contentDescription = "Select multiple photos",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Search Bar + Date Filter Chips (collapsed during selection mode for maximum photo space)
                if (!isSelectionMode) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { galleryViewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search address, note, or filename...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { galleryViewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .testTag("search_photos_input")
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = dateFilter.preset == DateFilterPreset.ALL,
                            onClick = { galleryViewModel.clearDateFilter() },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ShutterGold.copy(alpha = 0.22f),
                                selectedLabelColor = ShutterGold
                            ),
                            modifier = Modifier.testTag("date_filter_all")
                        )
                        FilterChip(
                            selected = dateFilter.preset == DateFilterPreset.TODAY,
                            onClick = { galleryViewModel.applyTodayFilter() },
                            label = { Text("Today") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ShutterGold.copy(alpha = 0.22f),
                                selectedLabelColor = ShutterGold
                            ),
                            modifier = Modifier.testTag("date_filter_today")
                        )
                        FilterChip(
                            selected = dateFilter.preset == DateFilterPreset.LAST_7_DAYS,
                            onClick = { galleryViewModel.applyLast7DaysFilter() },
                            label = { Text("7 Days") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ShutterGold.copy(alpha = 0.22f),
                                selectedLabelColor = ShutterGold
                            ),
                            modifier = Modifier.testTag("date_filter_7days")
                        )
                        FilterChip(
                            selected = dateFilter.preset == DateFilterPreset.SPECIFIC_DATE,
                            onClick = { showSingleDatePicker = true },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(if (dateFilter.preset == DateFilterPreset.SPECIFIC_DATE) dateFilter.displayLabel else "Pick Date") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ShutterGold.copy(alpha = 0.22f),
                                selectedLabelColor = ShutterGold
                            ),
                            modifier = Modifier.testTag("gallery_date_filter_button")
                        )
                        FilterChip(
                            selected = dateFilter.preset == DateFilterPreset.CUSTOM_RANGE,
                            onClick = { showRangeDatePicker = true },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text(if (dateFilter.preset == DateFilterPreset.CUSTOM_RANGE) dateFilter.displayLabel else "Range") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ShutterGold.copy(alpha = 0.22f),
                                selectedLabelColor = ShutterGold
                            ),
                            modifier = Modifier.testTag("gallery_date_range_button")
                        )
                    }
                }

                if (renameStatusMessage != null) {
                    Surface(
                        color = TelemetryEmerald.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .border(1.dp, TelemetryEmerald.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = renameStatusMessage.orEmpty(),
                                color = TelemetryEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(onClick = { galleryViewModel.clearRenameStatus() }) {
                                Text("Dismiss", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                if (photos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_brand_yv_logo),
                                    contentDescription = "YVGPSCAM Brand Logo",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                                Text(
                                    text = if (dateFilter.preset != DateFilterPreset.ALL || searchQuery.isNotBlank()) {
                                        "No Matching Captures Found"
                                    } else {
                                        stringResource(R.string.empty_gallery_title)
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (dateFilter.preset != DateFilterPreset.ALL || searchQuery.isNotBlank()) {
                                        "Try clearing your search query or date filter to see all captures."
                                    } else {
                                        stringResource(R.string.empty_gallery_subtitle)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                if (dateFilter.preset != DateFilterPreset.ALL || searchQuery.isNotBlank()) {
                                    Button(
                                        onClick = {
                                            galleryViewModel.clearDateFilter()
                                            galleryViewModel.updateSearchQuery("")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ShutterGold, contentColor = Color(0xFF141619))
                                    ) {
                                        Text("Reset Filters", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = onGoToCamera,
                                        colors = ButtonDefaults.buttonColors(containerColor = ShutterGold, contentColor = Color(0xFF141619)),
                                        modifier = Modifier.testTag("empty_gallery_camera_button")
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Open Camera", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else if (viewMode == GalleryViewMode.LIST) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(photos, key = { it.id }) { photo ->
                            GalleryPhotoListCard(
                                photo = photo,
                                isSelectionMode = isSelectionMode,
                                isChecked = selectedPhotoIds.contains(photo.id),
                                isSelected = (isSelectionMode && selectedPhotoIds.contains(photo.id)) ||
                                    (!isSelectionMode && isExpanded && selectedPhotoIdForSplit == photo.id),
                                onShare = { sharePhotoViaAndroidSheet(context, photo) },
                                onLongClick = { galleryViewModel.enterSelectionMode(photo.id) },
                                onClick = {
                                    if (!isSelectionMode && isExpanded) {
                                        selectedPhotoIdForSplit = photo.id
                                    } else {
                                        handleMediaItemTap(photo)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 104.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(photos, key = { it.id }) { photo ->
                            GalleryPhotoGridCard(
                                photo = photo,
                                isSelectionMode = isSelectionMode,
                                isChecked = selectedPhotoIds.contains(photo.id),
                                onLongClick = { galleryViewModel.enterSelectionMode(photo.id) },
                                onClick = {
                                    if (!isSelectionMode && isExpanded) {
                                        selectedPhotoIdForSplit = photo.id
                                    } else {
                                        handleMediaItemTap(photo)
                                    }
                                }
                            )
                        }
                    }
                }

                AdBannerSlot(placementId = "gallery")
            }

            if (isExpanded) {
                val activeId = selectedPhotoIdForSplit ?: photos.firstOrNull()?.id
                Box(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                        .border(1.dp, ObsidianBorder)
                ) {
                    if (activeId != null) {
                        PhotoDetailScreen(
                            photoId = activeId,
                            galleryViewModel = galleryViewModel,
                            offlineOnlyMapMode = offlineOnlyMapMode,
                            showBackButton = false,
                            onBack = { selectedPhotoIdForSplit = null }
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Select a photo to inspect EXIF & GPS telemetry",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSingleDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showSingleDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            galleryViewModel.applySpecificUtcDate(millis)
                        }
                        showSingleDatePicker = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { showSingleDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showRangeDatePicker) {
        val rangeState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showRangeDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = rangeState.selectedStartDateMillis
                        val end = rangeState.selectedEndDateMillis ?: start
                        if (start != null && end != null) {
                            galleryViewModel.applyCustomUtcDateRange(start, end)
                        }
                        showRangeDatePicker = false
                    },
                    enabled = rangeState.selectedStartDateMillis != null
                ) { Text("Apply Range") }
            },
            dismissButton = { TextButton(onClick = { showRangeDatePicker = false }) { Text("Cancel") } }
        ) {
            DateRangePicker(state = rangeState)
        }
    }

    if (showBatchRenameDialog) {
        val selectedList = remember(photos, selectedPhotoIds) {
            photos.filter { selectedPhotoIds.contains(it.id) }
        }
        if (selectedList.isNotEmpty()) {
            BatchRenameTemplateDialog(
                selectedPhotos = selectedList,
                previewFormatter = { photo, template ->
                    galleryViewModel.previewRenamedFilename(photo, template, 1)
                },
                onDismiss = { showBatchRenameDialog = false },
                onConfirmRename = { template ->
                    showBatchRenameDialog = false
                    galleryViewModel.renameSelectedPhotos(template)
                }
            )
        } else {
            showBatchRenameDialog = false
        }
    }

    if (showBatchDeleteConfirm) {
        val selectedCount = selectedPhotoIds.size
        if (selectedCount > 0) {
            AlertDialog(
                onDismissRequest = { showBatchDeleteConfirm = false },
                title = {
                    Text(if (selectedCount == 1) "Delete Selected Item?" else "Delete $selectedCount Selected Items?")
                },
                text = {
                    Text(
                        "Permanently remove the selected captures from this device and gallery. Copies backed up to Google Drive stay safe."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBatchDeleteConfirm = false
                            galleryViewModel.deleteSelectedPhotos { deletedIds ->
                                if (selectedPhotoIdForSplit != null && deletedIds.contains(selectedPhotoIdForSplit)) {
                                    selectedPhotoIdForSplit = null
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.testTag("confirm_batch_delete_button")
                    ) {
                        Text(
                            text = if (selectedCount == 1) "Delete" else "Delete $selectedCount",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showBatchDeleteConfirm = false },
                        modifier = Modifier.testTag("cancel_batch_delete_button")
                    ) { Text("Cancel") }
                }
            )
        } else {
            showBatchDeleteConfirm = false
        }
    }
}

@Composable
private fun BatchRenameTemplateDialog(
    selectedPhotos: List<CapturedPhotoEntity>,
    previewFormatter: (CapturedPhotoEntity, String) -> String,
    onDismiss: () -> Unit,
    onConfirmRename: (String) -> Unit
) {
    var templateText by remember { mutableStateOf("YV_{date}_{coords}_{index}") }
    val samplePreview = remember(templateText, selectedPhotos) {
        previewFormatter(selectedPhotos.first(), templateText)
    }
    val placeholders = listOf(
        "{date}" to "YYYYMMDD",
        "{time}" to "HHMMSS",
        "{lat}" to "Latitude",
        "{lon}" to "Longitude",
        "{coords}" to "Lat+Lon",
        "{city}" to "Locality",
        "{index}" to "01, 02..",
        "{orig}" to "Original"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename ${selectedPhotos.size} Selected Items") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = templateText,
                    onValueChange = { templateText = it },
                    label = { Text("Naming Template") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_rename_template_input")
                )

                Text(
                    text = "Tap placeholder to append:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    placeholders.forEach { (token, desc) ->
                        FilterChip(
                            selected = templateText.contains(token, ignoreCase = true),
                            onClick = {
                                val separator = if (templateText.isEmpty() || templateText.endsWith("_") || templateText.endsWith("-")) "" else "_"
                                templateText = "$templateText$separator$token"
                            },
                            label = { Text("$token ($desc)", fontSize = 11.sp) },
                            modifier = Modifier.testTag("rename_token_${token.trim('{', '}')}")
                        )
                    }
                }

                Surface(
                    color = ObsidianSurfaceHigh,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Live Preview (Item 1 of ${selectedPhotos.size}):",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = samplePreview,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TelemetryEmerald
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmRename(templateText) },
                enabled = templateText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ShutterGold, contentColor = Color(0xFF141619)),
                modifier = Modifier.testTag("confirm_batch_rename_button")
            ) {
                Text("Rename ${selectedPhotos.size}", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryPhotoListCard(
    photo: CapturedPhotoEntity,
    isSelectionMode: Boolean,
    isChecked: Boolean,
    isSelected: Boolean,
    onShare: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = remember(photo.capturedAtMillis) {
        SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(photo.capturedAtMillis))
    }
    val coordsStr = remember(photo.latitude, photo.longitude) {
        if (photo.latitude != null && photo.longitude != null) {
            "${GpsTelemetry.formatSingleCoord(photo.latitude, true)}, ${GpsTelemetry.formatSingleCoord(photo.longitude, false)}"
        } else {
            "No GPS coordinates recorded"
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ShutterGold else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("gallery_photo_card_${photo.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val req = remember(photo.mediaUri) {
                    ImageRequest.Builder(context).data(photo.mediaUri).crossfade(false).build()
                }
                AsyncImage(
                    model = req,
                    contentDescription = "Captured media ${photo.displayName}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (photo.isVideo) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = "Video capture",
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(44.dp)
                    )
                }
                if (isSelectionMode) {
                    SelectionCheckBadge(
                        checked = isChecked,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val headline = listOf(photo.cityState, photo.country)
                .filter { it.isNotBlank() }
                .joinToString(", ")
                .ifBlank { photo.addressLine.ifBlank { photo.displayName } }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (photo.customNote.isNotBlank()) {
                        Text(
                            text = photo.customNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("gallery_share_button_${photo.id}")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share ${photo.displayName}", tint = TelemetryEmerald)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (photo.latitude != null) Icons.Default.GpsFixed else Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = if (photo.latitude != null) TelemetryEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = coordsStr,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = if (photo.latitude != null) TelemetryEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(dateStr, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("${photo.compassAzimuth.roundToInt()}° ${photo.compassCardinal}", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryPhotoGridCard(
    photo: CapturedPhotoEntity,
    isSelectionMode: Boolean,
    isChecked: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val req = remember(photo.mediaUri) {
        ImageRequest.Builder(context)
            .data(photo.mediaUri)
            .size(340)
            .crossfade(false)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isChecked) {
                    Modifier.border(2.5.dp, ShutterGold, RoundedCornerShape(6.dp))
                } else {
                    Modifier
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("gallery_grid_card_${photo.id}")
    ) {
        AsyncImage(
            model = req,
            contentDescription = "Captured media ${photo.displayName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (photo.isVideo) {
            Icon(
                imageVector = Icons.Default.PlayCircleFilled,
                contentDescription = "Video capture",
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(34.dp)
            )
        }

        // Golden-Yellow checkmark badge matching IMG_4861
        if (isSelectionMode) {
            SelectionCheckBadge(
                checked = isChecked,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            )
        }
    }
}

@Composable
private fun SelectionCheckBadge(
    checked: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (checked) ShutterGold else Color.Black.copy(alpha = 0.25f))
            .border(
                width = 1.6.dp,
                color = if (checked) ShutterGold else Color.White.copy(alpha = 0.85f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

private fun sharePhotoViaAndroidSheet(context: Context, photo: CapturedPhotoEntity) {
    val coords = if (photo.latitude != null && photo.longitude != null) {
        String.format(Locale.US, "%.6f, %.6f", photo.latitude, photo.longitude)
    } else {
        "GPS unavailable"
    }
    val locationLine = listOf(photo.addressLine, photo.cityState, photo.country)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    val caption = buildString {
        append("Captured with YVGPSCAM\n")
        append("Coordinates: $coords")
        if (locationLine.isNotBlank()) append("\nLocation: $locationLine")
        if (photo.customNote.isNotBlank()) append("\nNote: ${photo.customNote}")
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = if (photo.isVideo) "video/mp4" else "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, Uri.parse(photo.mediaUri))
        putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Geotagged Capture"))
}

private fun shareMultiplePhotosViaAndroidSheet(
    context: Context,
    photos: List<CapturedPhotoEntity>
) {
    if (photos.isEmpty()) return
    if (photos.size == 1) {
        sharePhotoViaAndroidSheet(context, photos.first())
        return
    }
    val uris = ArrayList<Uri>(photos.map { Uri.parse(it.mediaUri) })
    val summary = buildString {
        append("Shared ${photos.size} geotagged captures via YVGPSCAM\n")
        photos.take(5).forEachIndexed { idx, p ->
            val coords = if (p.latitude != null && p.longitude != null) {
                String.format(Locale.US, "%.5f, %.5f", p.latitude, p.longitude)
            } else {
                "No GPS"
            }
            append("${idx + 1}. ${p.displayName} ($coords)\n")
        }
        if (photos.size > 5) append("...and ${photos.size - 5} more")
    }

    val allImages = photos.none { it.isVideo }
    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = if (allImages) "image/jpeg" else "*/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        putExtra(Intent.EXTRA_TEXT, summary.trim())
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share ${photos.size} Geotagged Captures"))
}
