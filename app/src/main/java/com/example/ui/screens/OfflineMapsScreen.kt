package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GpsTelemetry
import com.example.data.model.OfflineRegionEntity
import com.example.map.RegionPickerGoogleMap
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.viewmodel.CameraViewModel
import com.example.ui.viewmodel.OfflineMapsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OfflineMapsScreen(
    offlineViewModel: OfflineMapsViewModel,
    cameraViewModel: CameraViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by offlineViewModel.uiState.collectAsStateWithLifecycle()
    val savedRegions by offlineViewModel.savedRegions.collectAsStateWithLifecycle()
    val telemetry by cameraViewModel.telemetry.collectAsStateWithLifecycle()
    val stampConfig by cameraViewModel.stampConfig.collectAsStateWithLifecycle()

    var regionName by remember { mutableStateOf("Field Area 1") }
    var latInput by remember {
        mutableStateOf(
            telemetry.latitude?.let { String.format(Locale.US, "%.5f", it) } ?: "37.42209"
        )
    }
    var lonInput by remember {
        mutableStateOf(
            telemetry.longitude?.let { String.format(Locale.US, "%.5f", it) } ?: "-122.08406"
        )
    }
    var radiusKm by remember { mutableDoubleStateOf(2.0) }

    LaunchedEffect(telemetry.cityState) {
        if (telemetry.cityState.isNotBlank() && regionName == "Field Area 1") {
            regionName = telemetry.cityState
        }
    }

    val parsedLat = latInput.toDoubleOrNull()
    val parsedLon = lonInput.toDoubleOrNull()
    val estimatedTiles = remember(parsedLat, parsedLon, radiusKm) {
        if (parsedLat != null && parsedLon != null) {
            offlineViewModel.estimateTileCount(parsedLat, parsedLon, radiusKm, 14, 16)
        } else 0
    }
    val estimatedMb = remember(estimatedTiles) { estimatedTiles * 0.018 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Offline Map Tiles",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Download map regions for field stamping without internet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.OfflinePin,
                contentDescription = "Offline map storage",
                tint = TelemetryEmerald,
                modifier = Modifier.size(28.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Storage & Offline Mode Toggle Card
            item {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = TelemetryEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Cached Map Storage",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = formatBytes(uiState.diskCacheBytes) + " • ${uiState.diskTileCount} tiles",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = TelemetryEmerald
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Offline-Only Map Mode",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Use downloaded tiles & vector grid only (zero cellular data)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = stampConfig.offlineOnlyMapMode,
                                onCheckedChange = { offlineViewModel.setOfflineOnlyMode(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ObsidianBg,
                                    checkedTrackColor = SignalAmber
                                ),
                                modifier = Modifier.testTag("offline_only_mode_switch")
                            )
                        }
                    }
                }
            }

            // Download Region Form
            item {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = TelemetryEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Download New Region",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    telemetry.latitude?.let { latInput = String.format(Locale.US, "%.5f", it) }
                                    telemetry.longitude?.let { lonInput = String.format(Locale.US, "%.5f", it) }
                                    if (telemetry.cityState.isNotBlank()) {
                                        regionName = telemetry.cityState
                                    }
                                },
                                enabled = telemetry.hasFix,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Use GPS", fontSize = 11.sp)
                            }
                        }

                        RegionPickerGoogleMap(
                            centerLat = parsedLat ?: 37.42209,
                            centerLon = parsedLon ?: -122.08406,
                            radiusKm = radiusKm,
                            mapStyle = stampConfig.mapStyle,
                            offlineOnlyMapMode = stampConfig.offlineOnlyMapMode,
                            onLocationPicked = { pickedLat, pickedLon ->
                                latInput = String.format(Locale.US, "%.5f", pickedLat)
                                lonInput = String.format(Locale.US, "%.5f", pickedLon)
                            }
                        )

                        OutlinedTextField(
                            value = regionName,
                            onValueChange = { regionName = it },
                            label = { Text("Region Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("offline_region_name_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = latInput,
                                onValueChange = { latInput = it },
                                label = { Text("Center Lat") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = lonInput,
                                onValueChange = { lonInput = it },
                                label = { Text("Center Lon") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "Coverage Radius (Zoom 14–16)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1.0, 2.0, 4.0).forEach { r ->
                                FilterChip(
                                    selected = radiusKm == r,
                                    onClick = { radiusKm = r },
                                    label = { Text("${r.toInt()} km") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TelemetryEmerald.copy(alpha = 0.2f),
                                        selectedLabelColor = TelemetryEmerald
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Text(
                            text = "Estimated package: $estimatedTiles tiles (~${String.format(Locale.US, "%.1f", estimatedMb)} MB)",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (uiState.downloadProgress.isDownloading) {
                            val total = uiState.downloadProgress.totalTiles.coerceAtLeast(1)
                            val done = uiState.downloadProgress.completedTiles
                            LinearProgressIndicator(
                                progress = { done.toFloat() / total.toFloat() },
                                color = TelemetryEmerald,
                                trackColor = ObsidianSurfaceHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = "Downloading ${uiState.downloadProgress.regionName}: $done / $total tiles",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = TelemetryEmerald
                            )
                        }

                        uiState.feedbackMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (msg.contains("Unable") || msg.contains("failed")) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    TelemetryEmerald
                                }
                            )
                        }

                        Button(
                            onClick = {
                                if (parsedLat != null && parsedLon != null) {
                                    offlineViewModel.downloadRegion(
                                        regionName = regionName,
                                        centerLat = parsedLat,
                                        centerLon = parsedLon,
                                        radiusKm = radiusKm
                                    )
                                }
                            },
                            enabled = parsedLat != null && parsedLon != null && !uiState.downloadProgress.isDownloading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TelemetryEmerald,
                                contentColor = ObsidianBg
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("download_offline_region_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download Region Tiles", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Saved Offline Regions Header
            item {
                Text(
                    text = "Downloaded Regions (${savedRegions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (savedRegions.isEmpty()) {
                item {
                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No offline regions downloaded yet",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Download a region above to cache street map tiles for offline field captures. Vector survey grid fallback always stays available offline.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(savedRegions, key = { it.id }) { region ->
                    OfflineRegionCard(
                        region = region,
                        onDelete = { offlineViewModel.deleteRegion(region) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun OfflineRegionCard(
    region: OfflineRegionEntity,
    onDelete: () -> Unit
) {
    val dateStr = remember(region.downloadedAtMillis) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(region.downloadedAtMillis))
    }
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = region.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${GpsTelemetry.formatSingleCoord(region.centerLat, true)}, ${GpsTelemetry.formatSingleCoord(region.centerLon, false)}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = TelemetryEmerald
                )
                Text(
                    text = "${region.radiusKm.toInt()}km radius • Z${region.zoomMin}-${region.zoomMax} • ${region.tileCount} tiles (${formatBytes(region.sizeBytes)})",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Saved $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete offline region ${region.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
