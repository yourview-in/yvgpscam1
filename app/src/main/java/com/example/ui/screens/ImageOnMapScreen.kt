package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.CapturedPhotoEntity
import com.example.data.model.GpsTelemetry
import com.example.data.model.MapStyle
import com.example.map.GoogleMapsConfig
import com.example.map.openDirectionsInGoogleMaps
import com.example.map.toGoogleMapType
import com.example.ui.components.AdBannerSlot
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ShutterGold
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.viewmodel.CameraViewModel
import com.example.ui.viewmodel.GalleryViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImageOnMapScreen(
    galleryViewModel: GalleryViewModel,
    cameraViewModel: CameraViewModel,
    onBack: () -> Unit,
    onInspectPhoto: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photos by galleryViewModel.photos.collectAsStateWithLifecycle()
    val config by cameraViewModel.stampConfig.collectAsStateWithLifecycle()
    val liveGps by cameraViewModel.liveGpsTelemetry.collectAsStateWithLifecycle()
    val activeTelemetry by cameraViewModel.telemetry.collectAsStateWithLifecycle()

    val hasLocationPermission = remember(context) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    val geotaggedPhotos = remember(photos) {
        photos.filter { it.latitude != null && it.longitude != null }
    }
    var selectedPhotoId by remember(geotaggedPhotos) {
        mutableStateOf<Long?>(null)
    }
    val selectedPhoto = remember(selectedPhotoId, geotaggedPhotos) {
        geotaggedPhotos.firstOrNull { it.id == selectedPhotoId }
    }

    val hasApiKey = remember { GoogleMapsConfig.isApiKeyConfigured() }
    val showInteractiveMap = hasApiKey && !config.offlineOnlyMapMode

    val userCurrentLatLng = remember(
        liveGps.latitude,
        liveGps.longitude,
        activeTelemetry.latitude,
        activeTelemetry.longitude,
        geotaggedPhotos
    ) {
        when {
            liveGps.latitude != null && liveGps.longitude != null ->
                LatLng(liveGps.latitude!!, liveGps.longitude!!)
            activeTelemetry.latitude != null && activeTelemetry.longitude != null ->
                LatLng(activeTelemetry.latitude!!, activeTelemetry.longitude!!)
            geotaggedPhotos.isNotEmpty() ->
                LatLng(geotaggedPhotos.first().latitude!!, geotaggedPhotos.first().longitude!!)
            else -> LatLng(17.385044, 78.486671)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userCurrentLatLng, 15.5f)
    }
    var isMapLoaded by remember { mutableStateOf(false) }
    var centeredOnceOnGps by remember { mutableStateOf(false) }
    var recenterTrigger by remember { mutableIntStateOf(0) }

    // Center once when GPS or map becomes ready so user dragging is NEVER overridden
    LaunchedEffect(isMapLoaded, liveGps.latitude, liveGps.longitude) {
        if (!isMapLoaded || centeredOnceOnGps) return@LaunchedEffect
        centeredOnceOnGps = true
        runCatching {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(userCurrentLatLng, 15.8f),
                durationMs = 450
            )
        }
    }

    // Only re-center when the user explicitly taps the My Location crosshair button
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger == 0 || !isMapLoaded) return@LaunchedEffect
        runCatching {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(userCurrentLatLng, 16.0f),
                durationMs = 420
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header: < Image on Map (matching IMG_4862)
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
                    .testTag("image_on_map_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Image on Map",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = if (geotaggedPhotos.isEmpty()) "Live GPS" else "${geotaggedPhotos.size} pins",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TelemetryEmerald,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("image_on_map_canvas")
        ) {
            if (showInteractiveMap) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapType = config.mapStyle.toGoogleMapType(),
                        isMyLocationEnabled = hasLocationPermission
                    ),
                    uiSettings = MapUiSettings(
                        scrollGesturesEnabled = true,
                        scrollGesturesEnabledDuringRotateOrZoom = true,
                        zoomGesturesEnabled = true,
                        tiltGesturesEnabled = true,
                        rotationGesturesEnabled = true,
                        zoomControlsEnabled = true,
                        compassEnabled = true,
                        myLocationButtonEnabled = false,
                        mapToolbarEnabled = true
                    ),
                    onMapClick = { selectedPhotoId = null },
                    onMapLoaded = { isMapLoaded = true }
                ) {
                    // User's current GPS pin (always visible)
                    Marker(
                        state = remember(userCurrentLatLng.latitude, userCurrentLatLng.longitude) {
                            MarkerState(position = userCurrentLatLng)
                        },
                        title = "Your Current Location",
                        snippet = activeTelemetry.displayHeadlineAddress.ifBlank {
                            String.format(
                                Locale.US,
                                "%.5f, %.5f",
                                userCurrentLatLng.latitude,
                                userCurrentLatLng.longitude
                            )
                        },
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )

                    // Geotagged Capture Markers
                    geotaggedPhotos.forEach { photo ->
                        val lat = photo.latitude ?: return@forEach
                        val lon = photo.longitude ?: return@forEach
                        val isSelected = photo.id == selectedPhotoId
                        Marker(
                            state = remember(photo.id, lat, lon) { MarkerState(position = LatLng(lat, lon)) },
                            title = photo.displayName,
                            snippet = photo.cityState.ifBlank {
                                String.format(Locale.US, "%.5f, %.5f", lat, lon)
                            },
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

                // Bottom-right Circular Layer Switcher & My-Location Crosshair (IMG_4862)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = 14.dp,
                            bottom = if (selectedPhoto != null) 168.dp else 18.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = config.mapStyle.displayName,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ShutterGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                val nextStyle = MapStyle.entries[
                                    (MapStyle.entries.indexOf(config.mapStyle) + 1) % MapStyle.entries.size
                                ]
                                cameraViewModel.updateStampConfig { it.copy(mapStyle = nextStyle) }
                                Toast
                                    .makeText(context, "Map type: ${nextStyle.displayName}", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            .testTag("image_on_map_layers_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Cycle Google Map style",
                                tint = Color(0xFF1F2937),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { recenterTrigger++ }
                            .testTag("image_on_map_recenter_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Center map on your current location",
                                tint = Color(0xFF1F2937),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            } else {
                // Offline-only mode or missing Maps API key fallback
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = if (config.offlineOnlyMapMode) TelemetryEmerald else SignalAmber,
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = if (config.offlineOnlyMapMode) {
                                    "Offline-Only Map Mode Active"
                                } else {
                                    "Google Maps API Key Required"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (config.offlineOnlyMapMode) {
                                    "Turn off Offline-Only Map Mode in Offline Maps to load live Google Maps tiles."
                                } else {
                                    "Add MAPS_API_KEY in AI Studio Secrets to render interactive Google Maps directly inside Image on Map."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            OutlinedButton(
                                onClick = {
                                    openDirectionsInGoogleMaps(
                                        context = context,
                                        latitude = userCurrentLatLng.latitude,
                                        longitude = userCurrentLatLng.longitude,
                                        label = activeTelemetry.displayHeadlineAddress
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open Current Location in Google Maps")
                            }
                        }
                    }
                }
            }

            // Bottom Selected Photo Preview Card on pin tap
            selectedPhoto?.let { photo ->
                SelectedMapPhotoCard(
                    photo = photo,
                    onDismiss = { selectedPhotoId = null },
                    onInspect = { onInspectPhoto(photo.id) },
                    onDirections = {
                        if (photo.latitude != null && photo.longitude != null) {
                            openDirectionsInGoogleMaps(
                                context = context,
                                latitude = photo.latitude,
                                longitude = photo.longitude,
                                label = photo.displayName
                            )
                        }
                    },
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = if (photo.isVideo) "video/mp4" else "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(photo.mediaUri))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share ${photo.displayName}"))
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            }
        }

        AdBannerSlot(placementId = "image_on_map")
    }
}

@Composable
private fun SelectedMapPhotoCard(
    photo: CapturedPhotoEntity,
    onDismiss: () -> Unit,
    onInspect: () -> Unit,
    onDirections: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateText = remember(photo.capturedAtMillis) {
        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(photo.capturedAtMillis))
    }
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ShutterGold.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
            .testTag("image_on_map_selected_card")
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
                        .size(58.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                        .clickable(onClick = onInspect)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = photo.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = photo.cityState.ifBlank { photo.addressLine }.ifBlank { "Captured Site" },
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
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close selected pin card")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onShare,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share capture", modifier = Modifier.size(16.dp))
                }
                OutlinedButton(
                    onClick = onDirections,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Directions", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onInspect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShutterGold,
                        contentColor = ObsidianBg
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Text("Inspect", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
