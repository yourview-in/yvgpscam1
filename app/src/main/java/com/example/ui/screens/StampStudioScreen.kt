package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CoordinateFormat
import com.example.data.model.DateTimeFormat
import com.example.data.model.GpsSignalState
import com.example.data.model.GpsTelemetry
import com.example.data.model.MapStyle
import com.example.data.model.StampAccentColor
import com.example.data.model.StampFont
import com.example.data.model.StampPosition
import com.example.data.model.StampSize
import com.example.data.model.StampTemplate
import com.example.ui.components.AdBannerSlot
import com.example.ui.components.StampPreviewCard
import com.example.ui.components.YvGpsCamLogoBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.ShutterGold
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.viewmodel.CameraViewModel
import kotlin.math.roundToInt

@Composable
fun StampStudioScreen(
    viewModel: CameraViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val liveTelemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val config by viewModel.stampConfig.collectAsStateWithLifecycle()
    val miniMapBitmap by viewModel.miniMapBitmap.collectAsStateWithLifecycle()

    val previewTelemetry = remember(liveTelemetry) {
        if (liveTelemetry.hasFix) {
            liveTelemetry
        } else {
            GpsTelemetry(
                latitude = 37.422094,
                longitude = -122.084057,
                altitudeMeters = 24.0,
                accuracyMeters = 4.0f,
                speedKmh = 12.0f,
                compassAzimuth = liveTelemetry.compassAzimuth,
                compassCardinal = liveTelemetry.compassCardinal,
                magneticFieldMicroTesla = liveTelemetry.magneticFieldMicroTesla ?: 418f,
                pressureHpa = liveTelemetry.pressureHpa ?: 1014f,
                weatherTempCelsius = liveTelemetry.weatherTempCelsius ?: 25f,
                weatherHumidityPercent = liveTelemetry.weatherHumidityPercent ?: 53,
                weatherWindKmh = liveTelemetry.weatherWindKmh ?: 15f,
                addressLine = "1600 Amphitheatre Pkwy, Mountain View, CA 94043",
                cityState = "Mountain View, CA",
                country = "USA",
                countryCode = "US",
                signalState = GpsSignalState.LOCKED
            )
        }
    }

    var expandCoords by remember { mutableStateOf(false) }
    var expandDateTime by remember { mutableStateOf(false) }
    var expandLogo by remember { mutableStateOf(false) }
    var expandNote by remember { mutableStateOf(false) }
    var expandPerson by remember { mutableStateOf(false) }
    var expandStyleAppearance by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header: < Advance Template (IMG_4848)
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
                    .testTag("advance_template_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to camera",
                    tint = Color.White
                )
            }
            Text(
                text = "Advance Template",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 48.dp)
            )
        }

        HorizontalDivider(color = ObsidianBorder)

        // Live Interactive Stamp Preview Stage (top)
        val stampAlignment = when (config.stampPosition) {
            StampPosition.TOP_LEFT -> Alignment.TopStart
            StampPosition.TOP_CENTER -> Alignment.TopCenter
            StampPosition.TOP_RIGHT -> Alignment.TopEnd
            StampPosition.BOTTOM_LEFT -> Alignment.BottomStart
            StampPosition.BOTTOM_CENTER -> Alignment.BottomCenter
            StampPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF142836),
                            Color(0xFF0C1A24),
                            Color(0xFF102B26)
                        )
                    )
                )
                .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                .padding(10.dp),
            contentAlignment = stampAlignment
        ) {
            StampPreviewCard(
                telemetry = previewTelemetry,
                config = config,
                miniMapBitmap = miniMapBitmap
            )
        }

        // Scrollable Advance Template Checklist + 4 Map Types + Appearance
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. 4-Card Map Type Selector matching IMG_4849 (Normal | Satellite | Terrain | Hybrid)
            item {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (config.showMiniMap) ShutterGold else Color.Transparent)
                                    .border(1.5.dp, if (config.showMiniMap) ShutterGold else ObsidianBorder, CircleShape)
                                    .clickable {
                                        viewModel.updateStampConfig { it.copy(showMiniMap = !it.showMiniMap) }
                                    }
                                    .testTag("toggle_mini_map_check"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (config.showMiniMap) {
                                    Icon(Icons.Default.Check, contentDescription = "Mini-map enabled", tint = Color(0xFF141619), modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(
                                text = "Map Type",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = config.mapStyle.displayName,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ShutterGold
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val mapTypeCards = listOf(
                                Triple(MapStyle.STANDARD, Icons.Default.Map, Color(0xFF28465E)),
                                Triple(MapStyle.SATELLITE, Icons.Default.SatelliteAlt, Color(0xFF1E3B2E)),
                                Triple(MapStyle.TERRAIN, Icons.Default.Terrain, Color(0xFF2F4534)),
                                Triple(MapStyle.HYBRID, Icons.Default.Layers, Color(0xFF2A3B48))
                            )
                            mapTypeCards.forEach { (style, icon, tileTint) ->
                                val selected = config.mapStyle == style
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) ShutterGold.copy(alpha = 0.16f) else ObsidianSurfaceHigh)
                                        .border(
                                            width = if (selected) 1.8.dp else 1.dp,
                                            color = if (selected) ShutterGold else ObsidianBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            viewModel.updateStampConfig { it.copy(mapStyle = style, showMiniMap = true) }
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp)
                                        .testTag("studio_map_style_${style.name}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(tileTint),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = style.displayName,
                                            tint = if (selected) ShutterGold else Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = style.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) ShutterGold else Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Primary Address, Codes, Date & Brand Checklist Rows (IMG_4848)
            item {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        AdvanceFieldRow(
                            label = "Short Address",
                            valuePreview = previewTelemetry.displayHeadlineAddress,
                            checked = config.showShortAddress,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showShortAddress = !it.showShortAddress) }
                            },
                            testTag = "toggle_short_address"
                        )

                        AdvanceFieldRow(
                            label = "Full Address",
                            valuePreview = previewTelemetry.addressLine,
                            checked = config.showAddress,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showAddress = !it.showAddress) }
                            },
                            testTag = "toggle_full_address"
                        )

                        AdvanceFieldRow(
                            label = "Country Flag",
                            valuePreview = previewTelemetry.countryFlagEmoji.ifBlank { "🏳️" },
                            checked = config.showCountryFlag,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showCountryFlag = !it.showCountryFlag) }
                            },
                            testTag = "toggle_country_flag"
                        )

                        AdvanceFieldRow(
                            label = "Lat/Long",
                            valuePreview = previewTelemetry.formatCoordinates(config.coordinateFormat),
                            checked = config.showCoordinates,
                            hasSettingsArrow = true,
                            expanded = expandCoords,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showCoordinates = !it.showCoordinates) }
                            },
                            onExpandToggle = { expandCoords = !expandCoords },
                            testTag = "toggle_coordinates"
                        )
                        AnimatedVisibility(visible = expandCoords) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 38.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CoordinateFormat.entries.forEach { fmt ->
                                    FilterChip(
                                        selected = config.coordinateFormat == fmt,
                                        onClick = { viewModel.updateStampConfig { it.copy(coordinateFormat = fmt) } },
                                        label = { Text(fmt.displayName, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ShutterGold.copy(alpha = 0.2f),
                                            selectedLabelColor = ShutterGold
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        AdvanceFieldRow(
                            label = "Plus Code",
                            valuePreview = previewTelemetry.plusCode ?: "--",
                            checked = config.showPlusCode,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showPlusCode = !it.showPlusCode) }
                            },
                            testTag = "toggle_plus_code"
                        )

                        AdvanceFieldRow(
                            label = "Date & Time",
                            valuePreview = previewTelemetry.formatTimestamp(config.dateTimeFormat),
                            checked = config.showDateTime,
                            hasSettingsArrow = true,
                            expanded = expandDateTime,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showDateTime = !it.showDateTime) }
                            },
                            onExpandToggle = { expandDateTime = !expandDateTime },
                            testTag = "toggle_datetime"
                        )
                        AnimatedVisibility(visible = expandDateTime) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(start = 38.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DateTimeFormat.entries.forEach { fmt ->
                                    FilterChip(
                                        selected = config.dateTimeFormat == fmt,
                                        onClick = { viewModel.updateStampConfig { it.copy(dateTimeFormat = fmt) } },
                                        label = { Text(fmt.displayName, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ShutterGold.copy(alpha = 0.2f),
                                            selectedLabelColor = ShutterGold
                                        )
                                    )
                                }
                            }
                        }

                        AdvanceFieldRow(
                            label = "Time Zone",
                            valuePreview = previewTelemetry.formatTimeZoneOffset(),
                            checked = config.showTimeZone,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showTimeZone = !it.showTimeZone) }
                            },
                            testTag = "toggle_timezone"
                        )

                        AdvanceFieldRow(
                            label = "Numbering",
                            valuePreview = "#${config.photoSequenceNumber}",
                            checked = config.showNumbering,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showNumbering = !it.showNumbering) }
                            },
                            testTag = "toggle_numbering"
                        )

                        // Permanent Brand Logo Row (Locked, Non-Removable)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { expandLogo = !expandLogo }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("brand_logo_locked_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(ShutterGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Brand logo permanently enabled",
                                    tint = Color(0xFF141619),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Logo",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Permanent brand watermark",
                                        tint = ShutterGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = "Required brand logo • ${(config.logoOpacity * 100).roundToInt()}% opacity",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Image(
                                painter = painterResource(id = R.drawable.img_brand_yv_logo),
                                contentDescription = "YVGPSCAM brand logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, ShutterGold.copy(alpha = 0.6f), CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = if (expandLogo) Icons.Default.ExpandLess else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Adjust brand title and opacity",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        AnimatedVisibility(visible = expandLogo) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 34.dp, end = 6.dp, bottom = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = config.customBrandLabel,
                                    onValueChange = { label ->
                                        viewModel.updateStampConfig { it.copy(customBrandLabel = label.take(24)) }
                                    },
                                    label = { Text("Stamp Brand Title") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("brand_title_input")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Brand Logo Opacity", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${(config.logoOpacity * 100).roundToInt()}%", fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = ShutterGold)
                                }
                                Slider(
                                    value = config.logoOpacity,
                                    onValueChange = { op -> viewModel.updateStampConfig { it.copy(logoOpacity = op) } },
                                    valueRange = 0.25f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = ShutterGold, activeTrackColor = ShutterGold),
                                    modifier = Modifier.testTag("brand_logo_opacity_slider")
                                )
                            }
                        }

                        AdvanceFieldRow(
                            label = "Note/Hashtag",
                            valuePreview = config.customNote.ifBlank { "Tap arrow to add site note" },
                            checked = config.showNote,
                            hasSettingsArrow = true,
                            expanded = expandNote,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showNote = !it.showNote) }
                            },
                            onExpandToggle = { expandNote = !expandNote },
                            testTag = "toggle_note"
                        )
                        AnimatedVisibility(visible = expandNote) {
                            OutlinedTextField(
                                value = config.customNote,
                                onValueChange = { note ->
                                    viewModel.updateStampConfig { it.copy(customNote = note.take(60)) }
                                },
                                label = { Text("Note / Hashtag") },
                                placeholder = { Text("e.g., #SiteTracking Field Survey") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 34.dp, end = 6.dp, bottom = 10.dp)
                                    .testTag("stamp_note_input")
                            )
                        }

                        AdvanceFieldRow(
                            label = "Person Name",
                            valuePreview = config.personName.ifBlank { "Captured by" },
                            checked = config.showPersonName,
                            hasSettingsArrow = true,
                            expanded = expandPerson,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showPersonName = !it.showPersonName) }
                            },
                            onExpandToggle = { expandPerson = !expandPerson },
                            testTag = "toggle_person_name"
                        )
                        AnimatedVisibility(visible = expandPerson) {
                            OutlinedTextField(
                                value = config.personName,
                                onValueChange = { name ->
                                    viewModel.updateStampConfig { it.copy(personName = name.take(32)) }
                                },
                                label = { Text("Person / Surveyor Name") },
                                placeholder = { Text("e.g., Daniel Morris") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 34.dp, end = 6.dp, bottom = 10.dp)
                                    .testTag("person_name_input")
                            )
                        }
                    }
                }
            }

            // 3. Telemetry Sensors & Environmental Metrics (IMG_4849)
            item {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(
                            text = "Live Sensors & Environment",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ShutterGold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )

                        AdvanceFieldRow(
                            label = "Weather & Humidity",
                            valuePreview = previewTelemetry.weatherTempCelsius?.let {
                                "${it.roundToInt()}°C · ${previewTelemetry.weatherHumidityPercent ?: 0}%"
                            } ?: "Auto when online",
                            checked = config.showWeather,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showWeather = !it.showWeather) }
                            },
                            testTag = "toggle_weather"
                        )

                        AdvanceFieldRow(
                            label = "Wind Speed & Compass",
                            valuePreview = "${(previewTelemetry.weatherWindKmh ?: previewTelemetry.speedKmh ?: 0f).roundToInt()} km/h · ${previewTelemetry.compassCardinal}",
                            checked = config.showCompassAndSpeed,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showCompassAndSpeed = !it.showCompassAndSpeed) }
                            },
                            testTag = "toggle_wind_compass"
                        )

                        AdvanceFieldRow(
                            label = "Altitude",
                            valuePreview = previewTelemetry.altitudeMeters?.let { "${it.roundToInt()} m" } ?: "-- m",
                            checked = config.showAltitude,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showAltitude = !it.showAltitude) }
                            },
                            testTag = "toggle_altitude"
                        )

                        AdvanceFieldRow(
                            label = "Magnetic Field",
                            valuePreview = previewTelemetry.magneticFieldMicroTesla?.let { "${it.roundToInt()} µT" } ?: "-- µT",
                            checked = config.showMagneticField,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showMagneticField = !it.showMagneticField) }
                            },
                            testTag = "toggle_magnetic"
                        )

                        AdvanceFieldRow(
                            label = "Pressure",
                            valuePreview = previewTelemetry.pressureHpa?.let { "${it.roundToInt()} hPa" } ?: "Device barometer",
                            checked = config.showPressure,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showPressure = !it.showPressure) }
                            },
                            testTag = "toggle_pressure"
                        )

                        AdvanceFieldRow(
                            label = "Accuracy",
                            valuePreview = previewTelemetry.accuracyMeters?.let { "±${it.roundToInt()} m" } ?: "-- m",
                            checked = config.showAccuracy,
                            onToggle = {
                                viewModel.updateStampConfig { it.copy(showAccuracy = !it.showAccuracy) }
                            },
                            testTag = "toggle_accuracy"
                        )
                    }
                }
            }

            // 4. Collapsible Style, Typography, Color, Size & Position Section
            item {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandStyleAppearance = !expandStyleAppearance },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = ShutterGold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Card Style, Font, Color & Position",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Icon(
                                imageVector = if (expandStyleAppearance) Icons.Default.ExpandLess else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Toggle appearance customization",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = expandStyleAppearance) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Text("Template Style", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StampTemplate.entries.forEach { tmpl ->
                                        FilterChip(
                                            selected = config.template == tmpl,
                                            onClick = { viewModel.updateStampConfig { it.copy(template = tmpl) } },
                                            label = { Text(tmpl.displayName) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ShutterGold.copy(alpha = 0.2f),
                                                selectedLabelColor = ShutterGold
                                            ),
                                            modifier = Modifier.testTag("template_${tmpl.name}")
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                Text("Font", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StampFont.entries.forEach { font ->
                                        FilterChip(
                                            selected = config.stampFont == font,
                                            onClick = { viewModel.updateStampConfig { it.copy(stampFont = font) } },
                                            label = { Text(font.displayName, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ShutterGold.copy(alpha = 0.2f),
                                                selectedLabelColor = ShutterGold
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                Text("Accent Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StampAccentColor.entries.forEach { swatch ->
                                        val isSelected = config.accentColor == swatch
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(swatch.color)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) Color.White else ObsidianBorder,
                                                    shape = CircleShape
                                                )
                                                .clickable { viewModel.updateStampConfig { it.copy(accentColor = swatch) } },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = swatch.displayName, tint = ObsidianBg, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                Text("Stamp Size", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StampSize.entries.forEach { sz ->
                                        FilterChip(
                                            selected = config.stampSize == sz,
                                            onClick = { viewModel.updateStampConfig { it.copy(stampSize = sz) } },
                                            label = { Text(sz.displayName) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ShutterGold.copy(alpha = 0.2f),
                                                selectedLabelColor = ShutterGold
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = ShutterGold, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Stamp Position", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(6.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(StampPosition.TOP_LEFT, StampPosition.TOP_CENTER, StampPosition.TOP_RIGHT).forEach { pos ->
                                            PositionGridChip(pos, config.stampPosition == pos, {
                                                viewModel.updateStampConfig { it.copy(stampPosition = pos) }
                                            }, Modifier.weight(1f))
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(StampPosition.BOTTOM_LEFT, StampPosition.BOTTOM_CENTER, StampPosition.BOTTOM_RIGHT).forEach { pos ->
                                            PositionGridChip(pos, config.stampPosition == pos, {
                                                viewModel.updateStampConfig { it.copy(stampPosition = pos) }
                                            }, Modifier.weight(1f))
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Overlay Card Opacity", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${(config.stampOpacity * 100).roundToInt()}%", fontFamily = JetBrainsMonoFontFamily, color = ShutterGold, fontSize = 12.sp)
                                }
                                Slider(
                                    value = config.stampOpacity,
                                    onValueChange = { op -> viewModel.updateStampConfig { it.copy(stampOpacity = op) } },
                                    valueRange = 0.35f..1.0f,
                                    colors = SliderDefaults.colors(thumbColor = ShutterGold, activeTrackColor = ShutterGold),
                                    modifier = Modifier.testTag("stamp_overlay_opacity_slider")
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(18.dp)) }
        }

        AdBannerSlot(
            placementId = "advance_template",
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun AdvanceFieldRow(
    label: String,
    valuePreview: String,
    checked: Boolean,
    hasSettingsArrow: Boolean = false,
    expanded: Boolean = false,
    onToggle: () -> Unit,
    onExpandToggle: (() -> Unit)? = null,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) ShutterGold else Color.Transparent)
                .border(
                    width = 1.6.dp,
                    color = if (checked) ShutterGold else Color.White.copy(alpha = 0.38f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "$label enabled",
                    tint = Color(0xFF141619),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        if (valuePreview.isNotBlank()) {
            Text(
                text = valuePreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .width(118.dp),
                textAlign = TextAlign.End
            )
        }

        if (hasSettingsArrow && onExpandToggle != null) {
            IconButton(
                onClick = onExpandToggle,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Configure $label",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PositionGridChip(
    position: StampPosition,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) ShutterGold.copy(alpha = 0.22f) else ObsidianSurfaceHigh,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = if (selected) ShutterGold else ObsidianBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .testTag("position_${position.name}")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = position.shortLabel,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (selected) ShutterGold else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = position.displayName,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
