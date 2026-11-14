package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GpsTelemetry
import com.example.ui.components.AdBannerSlot
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ShutterGold
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.viewmodel.CameraViewModel
import java.util.Locale

@Composable
fun LocationsScreen(
    viewModel: CameraViewModel,
    onBack: () -> Unit,
    onOpenOfflineMaps: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val config by viewModel.stampConfig.collectAsStateWithLifecycle()
    val liveGps by viewModel.liveGpsTelemetry.collectAsStateWithLifecycle()
    val miniMapBitmap by viewModel.miniMapBitmap.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val manualLocations = remember(config.manualLocationsJson, searchQuery) {
        val all = config.parsedManualLocations()
        if (searchQuery.isBlank()) all else all.filter {
            it.shortName.contains(searchQuery, ignoreCase = true) ||
                it.fullAddress.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("locations_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Locations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onOpenOfflineMaps,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("locations_offline_maps_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflinePin,
                        contentDescription = "Offline Map Tiles",
                        tint = TelemetryEmerald
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search manual locations...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("locations_search_input")
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current Location Section (IMG_4856)
                item {
                    Text(
                        text = "Current Location",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    val isCurrentActive = !config.useManualLocation
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isCurrentActive) 2.dp else 1.dp,
                                color = if (isCurrentActive) ShutterGold else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.selectCurrentGpsLocation() }
                            .testTag("select_current_location_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (miniMapBitmap != null && !miniMapBitmap!!.isRecycled) {
                                    Image(
                                        bitmap = miniMapBitmap!!.asImageBitmap(),
                                        contentDescription = "Current GPS location map",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.GpsFixed,
                                        contentDescription = null,
                                        tint = TelemetryEmerald,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = liveGps.displayHeadlineAddress,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = liveGps.displayStreetAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (liveGps.latitude != null && liveGps.longitude != null) {
                                    Text(
                                        text = "${GpsTelemetry.formatSingleCoord(liveGps.latitude!!, true)}, ${GpsTelemetry.formatSingleCoord(liveGps.longitude!!, false)}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = TelemetryEmerald
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrentActive) ShutterGold else Color.Transparent)
                                    .border(
                                        1.5.dp,
                                        if (isCurrentActive) ShutterGold else MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCurrentActive) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Active current location",
                                        tint = Color(0xFF141619),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Manual Location Section
                item {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Manual Location",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${manualLocations.size} saved",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (manualLocations.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = ShutterGold,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "No Manual Locations Saved",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap the + button below to pin a custom site address or coordinates for indoor/pre-survey stamping.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(manualLocations, key = { it.id }) { loc ->
                        val isSelected = config.useManualLocation && config.selectedManualLocationId == loc.id
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
                                .clickable { viewModel.selectManualLocation(loc.id) }
                                .testTag("manual_location_card_${loc.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSelected) ShutterGold else TelemetryEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = loc.shortName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (loc.fullAddress.isNotBlank()) {
                                        Text(
                                            text = loc.fullAddress,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = String.format(Locale.US, "%.6f, %.6f", loc.latitude, loc.longitude),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = TelemetryEmerald
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteManualLocation(loc.id) },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete manual location ${loc.shortName}",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) ShutterGold else Color.Transparent)
                                        .border(
                                            1.5.dp,
                                            if (isSelected) ShutterGold else MaterialTheme.colorScheme.outline,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected manual location",
                                            tint = Color(0xFF141619),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdBannerSlot(placementId = "locations")
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = ShutterGold,
            contentColor = Color(0xFF141619),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_manual_location_fab")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add manual location"
            )
        }
    }

    if (showAddDialog) {
        var shortName by remember { mutableStateOf("") }
        var fullAddress by remember { mutableStateOf("") }
        var latText by remember {
            mutableStateOf(liveGps.latitude?.let { String.format(Locale.US, "%.6f", it) } ?: "37.422094")
        }
        var lonText by remember {
            mutableStateOf(liveGps.longitude?.let { String.format(Locale.US, "%.6f", it) } ?: "-122.084057")
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Manual Location") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = shortName,
                        onValueChange = { shortName = it.take(40) },
                        label = { Text("Short Name / Site Title") },
                        placeholder = { Text("e.g., North Substation") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_loc_name_input")
                    )
                    OutlinedTextField(
                        value = fullAddress,
                        onValueChange = { fullAddress = it.take(120) },
                        label = { Text("Full Address") },
                        placeholder = { Text("Street, City, Postal Code") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_loc_address_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latText,
                            onValueChange = { latText = it },
                            label = { Text("Latitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lonText,
                            onValueChange = { lonText = it },
                            label = { Text("Longitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (liveGps.hasFix) {
                        OutlinedButton(
                            onClick = {
                                if (shortName.isBlank()) shortName = liveGps.displayHeadlineAddress
                                if (fullAddress.isBlank()) fullAddress = liveGps.addressLine
                                latText = String.format(Locale.US, "%.6f", liveGps.latitude!!)
                                lonText = String.format(Locale.US, "%.6f", liveGps.longitude!!)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Fill From Live GPS")
                        }
                    }
                }
            },
            confirmButton = {
                val parsedLat = latText.toDoubleOrNull()
                val parsedLon = lonText.toDoubleOrNull()
                val valid = shortName.isNotBlank() &&
                    parsedLat != null && parsedLat in -90.0..90.0 &&
                    parsedLon != null && parsedLon in -180.0..180.0
                Button(
                    enabled = valid,
                    onClick = {
                        viewModel.addManualLocation(
                            shortName = shortName,
                            fullAddress = fullAddress,
                            latitude = parsedLat!!,
                            longitude = parsedLon!!,
                            activateImmediately = true
                        )
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShutterGold,
                        contentColor = Color(0xFF141619)
                    ),
                    modifier = Modifier.testTag("save_manual_location_button")
                ) {
                    Text("Save & Use", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
