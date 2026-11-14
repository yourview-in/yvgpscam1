package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GpsSignalState
import com.example.data.model.GpsTelemetry
import com.example.data.model.StampLayoutTemplate
import com.example.data.model.StampTemplate
import com.example.ui.components.AdBannerSlot
import com.example.ui.components.StampPreviewCard
import com.example.ui.theme.ShutterGold
import com.example.ui.viewmodel.CameraViewModel

@Composable
fun TemplatePickerScreen(
    viewModel: CameraViewModel,
    onBack: () -> Unit,
    onCustomizeAdvance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.stampConfig.collectAsStateWithLifecycle()
    val liveTelemetry by viewModel.telemetry.collectAsStateWithLifecycle()
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

    var showReportingBadgeDialog by remember { mutableStateOf(false) }
    var reportingDraft by remember(config.reportingLabel) { mutableStateOf(config.reportingLabel) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header: < Templates
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
                    .testTag("template_picker_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Templates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 48.dp)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(StampLayoutTemplate.entries, key = { it.name }) { layout ->
                val isSelected = config.layoutTemplate == layout
                val previewConfig = remember(config, layout) {
                    config.copy(
                        layoutTemplate = layout,
                        template = if (layout == StampLayoutTemplate.CLASSIC) StampTemplate.CLASSIC_WHITE else config.template
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = layout.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ShutterGold else MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = layout.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(ShutterGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected template",
                                    tint = Color(0xFF141619),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF1A2C3B),
                                        Color(0xFF101C26),
                                        Color(0xFF132F28)
                                    )
                                )
                            )
                            .border(
                                width = if (isSelected) 2.2.dp else 1.dp,
                                color = if (isSelected) ShutterGold else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.selectLayoutTemplate(layout) }
                            .padding(10.dp)
                            .testTag("select_layout_template_${layout.name}")
                    ) {
                        StampPreviewCard(
                            telemetry = previewTelemetry,
                            config = previewConfig,
                            miniMapBitmap = miniMapBitmap
                        )

                        // Golden Edit Pencil FAB on Advance Template (IMG_4857) & Badge edit on Reporting
                        if (layout == StampLayoutTemplate.ADVANCE) {
                            Surface(
                                color = ShutterGold,
                                shape = CircleShape,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.selectLayoutTemplate(layout)
                                        onCustomizeAdvance()
                                    }
                                    .testTag("edit_advance_template_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Customize Advance Template",
                                        tint = Color(0xFF141619),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else if (layout == StampLayoutTemplate.REPORTING) {
                            Surface(
                                color = ShutterGold,
                                shape = CircleShape,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        viewModel.selectLayoutTemplate(layout)
                                        showReportingBadgeDialog = true
                                    }
                                    .testTag("edit_reporting_badge_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Reporting Status Badge",
                                        tint = Color(0xFF141619),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AdBannerSlot(placementId = "template_picker")
    }

    if (showReportingBadgeDialog) {
        AlertDialog(
            onDismissRequest = { showReportingBadgeDialog = false },
            title = { Text("Reporting Status Badge") },
            text = {
                OutlinedTextField(
                    value = reportingDraft,
                    onValueChange = { reportingDraft = it.take(20) },
                    label = { Text("Badge Label") },
                    placeholder = { Text("Check In, Inspected, Delivered...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateStampConfig {
                            it.copy(reportingLabel = reportingDraft.trim().ifBlank { "Check In" })
                        }
                        showReportingBadgeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ShutterGold,
                        contentColor = Color(0xFF141619)
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportingBadgeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
