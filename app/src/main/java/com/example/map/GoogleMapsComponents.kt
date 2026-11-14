package com.example.map

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.data.model.CapturedPhotoEntity
import com.example.data.model.GpsTelemetry
import com.example.data.model.MapStyle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.TelemetryEmerald
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoogleMapsConfig {
    fun isApiKeyConfigured(rawKey: String = BuildConfig.MAPS_API_KEY): Boolean {
        val trimmed = rawKey.trim()
        return trimmed.isNotEmpty() &&
            !trimmed.startsWith("MY_", ignoreCase = true) &&
            !trimmed.equals("null", ignoreCase = true)
    }
}

fun MapStyle.toGoogleMapType(): MapType = when (this) {
    MapStyle.STANDARD -> MapType.NORMAL
    MapStyle.SATELLITE -> MapType.SATELLITE
    MapStyle.TERRAIN -> MapType.TERRAIN
    MapStyle.HYBRID -> MapType.HYBRID
}

fun openDirectionsInGoogleMaps(
    context: Context,
    latitude: Double,
    longitude: Double,
    label: String
) {
    val encodedLabel = Uri.encode(label.ifBlank { "Capture Location" })
    val googleMapsUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
    val dedicatedIntent = Intent(Intent.ACTION_VIEW, googleMapsUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    val startedDedicated = runCatching {
        context.startActivity(dedicatedIntent)
    }.isSuccess

    if (!startedDedicated) {
        val genericGeoIntent = Intent(Intent.ACTION_VIEW, googleMapsUri)
        val startedGeneric = runCatching {
            context.startActivity(genericGeoIntent)
        }.isSuccess
        if (!startedGeneric) {
            val webUri = Uri.parse(
                String.format(
                    Locale.US,
                    "https://www.google.com/maps/search/?api=1&query=%.6f,%.6f",
                    latitude,
                    longitude
                )
            )
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            runCatching { context.startActivity(webIntent) }.onFailure {
                Toast.makeText(context, "No maps app or browser found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun PhotoDetailGoogleMap(
    latitude: Double,
    longitude: Double,
    title: String,
    snippet: String,
    mapStyle: MapStyle,
    offlineOnlyMapMode: Boolean,
    fallbackBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val hasApiKey = remember { GoogleMapsConfig.isApiKeyConfigured() }
    val showInteractiveMap = hasApiKey && !offlineOnlyMapMode

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianSurfaceHigh)
            .border(1.dp, TelemetryEmerald.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
    ) {
        if (showInteractiveMap) {
            val target = remember(latitude, longitude) { LatLng(latitude, longitude) }
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(target, 16f)
            }
            var isMapLoaded by remember { mutableStateOf(false) }

            LaunchedEffect(latitude, longitude, isMapLoaded) {
                if (isMapLoaded) {
                    runCatching {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(target, 16f),
                            durationMs = 450
                        )
                    }
                } else {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(target, 16f)
                }
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = mapStyle.toGoogleMapType()),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    compassEnabled = true,
                    mapToolbarEnabled = true
                ),
                onMapLoaded = { isMapLoaded = true }
            ) {
                Marker(
                    state = remember(latitude, longitude) { MarkerState(position = target) },
                    title = title,
                    snippet = snippet,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                )
            }
        } else {
            if (fallbackBitmap != null) {
                Image(
                    bitmap = fallbackBitmap.asImageBitmap(),
                    contentDescription = "Offline capture location map",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map placeholder",
                    tint = TelemetryEmerald,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                )
            }

            Surface(
                color = ObsidianBg.copy(alpha = 0.86f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = if (offlineOnlyMapMode) {
                        "Offline-Only Mode • Cached/vector map shown"
                    } else {
                        "Set MAPS_API_KEY in AI Studio Secrets for interactive Google Maps"
                    },
                    fontSize = 11.sp,
                    color = if (offlineOnlyMapMode) TelemetryEmerald else SignalAmber,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun RegionPickerGoogleMap(
    centerLat: Double,
    centerLon: Double,
    radiusKm: Double,
    mapStyle: MapStyle,
    offlineOnlyMapMode: Boolean,
    onLocationPicked: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasApiKey = remember { GoogleMapsConfig.isApiKeyConfigured() }
    val showInteractiveMap = hasApiKey && !offlineOnlyMapMode

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianSurfaceHigh)
            .border(1.dp, TelemetryEmerald.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .testTag("offline_region_map_picker")
    ) {
        if (showInteractiveMap) {
            val target = remember(centerLat, centerLon) { LatLng(centerLat, centerLon) }
            val zoomForRadius = remember(radiusKm) {
                when {
                    radiusKm <= 1.2 -> 14.0f
                    radiusKm <= 2.5 -> 13.0f
                    else -> 12.0f
                }
            }
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(target, zoomForRadius)
            }
            var isMapLoaded by remember { mutableStateOf(false) }

            LaunchedEffect(centerLat, centerLon, zoomForRadius, isMapLoaded) {
                if (isMapLoaded) {
                    runCatching {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(target, zoomForRadius),
                            durationMs = 350
                        )
                    }
                } else {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(target, zoomForRadius)
                }
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = mapStyle.toGoogleMapType()),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    compassEnabled = true,
                    mapToolbarEnabled = false
                ),
                onMapClick = { latLng ->
                    onLocationPicked(latLng.latitude, latLng.longitude)
                },
                onMapLoaded = { isMapLoaded = true }
            ) {
                Marker(
                    state = remember(centerLat, centerLon) { MarkerState(position = target) },
                    title = "Download Region Center",
                    snippet = String.format(Locale.US, "%.5f, %.5f", centerLat, centerLon),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                )
                Circle(
                    center = target,
                    radius = radiusKm * 1000.0,
                    strokeColor = TelemetryEmerald,
                    strokeWidth = 4f,
                    fillColor = TelemetryEmerald.copy(alpha = 0.16f)
                )
            }

            Surface(
                color = ObsidianBg.copy(alpha = 0.85f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = TelemetryEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Tap map to set ${radiusKm.toInt()}km region",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = TelemetryEmerald,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = String.format(Locale.US, "%.5f, %.5f • %.0f km radius", centerLat, centerLon, radiusKm),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    color = TelemetryEmerald,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (offlineOnlyMapMode) {
                        "Interactive Google Map picker is paused while Offline-Only Mode is active. Edit coordinates below."
                    } else {
                        "Add MAPS_API_KEY in AI Studio Secrets to pick regions visually on Google Maps."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun GalleryMultiPhotoGoogleMap(
    photos: List<CapturedPhotoEntity>,
    mapStyle: MapStyle,
    offlineOnlyMapMode: Boolean,
    onInspectPhoto: (Long) -> Unit,
    onSharePhoto: (CapturedPhotoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val geotaggedPhotos = remember(photos) {
        photos.filter { it.latitude != null && it.longitude != null }
    }
    var selectedPhotoId by remember(geotaggedPhotos) {
        mutableStateOf(geotaggedPhotos.firstOrNull()?.id)
    }
    val selectedPhoto = remember(selectedPhotoId, geotaggedPhotos) {
        geotaggedPhotos.firstOrNull { it.id == selectedPhotoId }
    }

    if (geotaggedPhotos.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TelemetryEmerald,
                        modifier = Modifier.size(38.dp)
                    )
                    Text(
                        text = "No Geotagged Captures Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Capture photos with GPS lock enabled to explore your survey pins on Google Maps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val hasApiKey = remember { GoogleMapsConfig.isApiKeyConfigured() }
    val showInteractiveMap = hasApiKey && !offlineOnlyMapMode

    if (!showInteractiveMap) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = if (offlineOnlyMapMode) TelemetryEmerald else SignalAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (offlineOnlyMapMode) {
                                "Offline-Only Map Mode Active"
                            } else {
                                "Google Maps API Key Required"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (offlineOnlyMapMode) {
                            "Switch off Offline-Only Map Mode in the Offline tab to load interactive Google Maps pins for your ${geotaggedPhotos.size} geotagged photos."
                        } else {
                            "Add your MAPS_API_KEY in the AI Studio Secrets panel to unlock interactive Google Maps exploration for all ${geotaggedPhotos.size} geotagged photos."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    selectedPhoto?.let { photo ->
                        if (photo.latitude != null && photo.longitude != null) {
                            OutlinedButton(
                                onClick = {
                                    openDirectionsInGoogleMaps(
                                        context = context,
                                        latitude = photo.latitude,
                                        longitude = photo.longitude,
                                        label = photo.displayName
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open Latest Pin in Maps App")
                            }
                        }
                    }
                }
            }
        }
        return
    }

    val first = geotaggedPhotos.first()
    val initialTarget = remember(first.id) {
        LatLng(first.latitude ?: 0.0, first.longitude ?: 0.0)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialTarget, 14f)
    }
    var isMapLoaded by remember { mutableStateOf(false) }
    var fitTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(isMapLoaded, fitTrigger, geotaggedPhotos.size) {
        if (!isMapLoaded || geotaggedPhotos.isEmpty()) return@LaunchedEffect
        runCatching {
            if (geotaggedPhotos.size == 1) {
                val p = geotaggedPhotos.first()
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(p.latitude!!, p.longitude!!),
                        15f
                    ),
                    durationMs = 500
                )
            } else {
                val boundsBuilder = LatLngBounds.Builder()
                geotaggedPhotos.forEach { p ->
                    val lat = p.latitude ?: return@forEach
                    val lon = p.longitude ?: return@forEach
                    boundsBuilder.include(LatLng(lat, lon))
                }
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 110),
                    durationMs = 550
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
            .testTag("gallery_google_map_container")
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(mapType = mapStyle.toGoogleMapType()),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                compassEnabled = true,
                mapToolbarEnabled = true
            ),
            onMapLoaded = { isMapLoaded = true }
        ) {
            geotaggedPhotos.forEach { photo ->
                val lat = photo.latitude ?: return@forEach
                val lon = photo.longitude ?: return@forEach
                val isSelected = photo.id == selectedPhotoId
                val snippetText = photo.cityState.ifBlank {
                    String.format(Locale.US, "%.5f, %.5f", lat, lon)
                }
                Marker(
                    state = remember(photo.id, lat, lon) { MarkerState(position = LatLng(lat, lon)) },
                    title = photo.displayName,
                    snippet = snippetText,
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (isSelected) BitmapDescriptorFactory.HUE_ORANGE else BitmapDescriptorFactory.HUE_GREEN
                    ),
                    onClick = {
                        selectedPhotoId = photo.id
                        false
                    }
                )
            }
        }

        // Top Floating Pin Counter & Fit All Button
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = ObsidianBg.copy(alpha = 0.88f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = "${geotaggedPhotos.size} pins",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelemetryEmerald,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }

            Surface(
                color = ObsidianBg.copy(alpha = 0.88f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, TelemetryEmerald.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .clickable { fitTrigger++ }
                    .testTag("gallery_map_fit_all_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitScreen,
                        contentDescription = "Fit all photo pins on map",
                        tint = TelemetryEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Fit All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Selected Photo Bottom Preview Card
        selectedPhoto?.let { photo ->
            val dateText = remember(photo.capturedAtMillis) {
                SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(photo.capturedAtMillis))
            }
            Surface(
                color = ObsidianBg.copy(alpha = 0.94f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(10.dp)
                    .border(1.dp, TelemetryEmerald.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .testTag("gallery_map_selected_card")
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AsyncImage(
                            model = photo.mediaUri,
                            contentDescription = photo.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                                .clickable { onInspectPhoto(photo.id) }
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = photo.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = photo.cityState.ifBlank { photo.addressLine }.ifBlank { "Captured Location" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (photo.latitude != null && photo.longitude != null) {
                                Text(
                                    text = "${GpsTelemetry.formatSingleCoord(photo.latitude, true)}, ${GpsTelemetry.formatSingleCoord(photo.longitude, false)} • $dateText",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = TelemetryEmerald,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { selectedPhotoId = null },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss selected pin card",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSharePhoto(photo) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("map_share_photo_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        if (photo.latitude != null && photo.longitude != null) {
                            OutlinedButton(
                                onClick = {
                                    openDirectionsInGoogleMaps(
                                        context = context,
                                        latitude = photo.latitude,
                                        longitude = photo.longitude,
                                        label = photo.displayName
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("map_directions_button")
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Directions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Button(
                            onClick = { onInspectPhoto(photo.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TelemetryEmerald,
                                contentColor = ObsidianBg
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("map_inspect_photo_button")
                        ) {
                            Text("Inspect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
