package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.camera.MediaStoreSaver
import com.example.data.model.FileNameField
import com.example.data.model.GpsSignalState
import com.example.data.model.GpsTelemetry
import com.example.data.model.StampConfig
import com.example.ui.components.AdBannerSlot
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ShutterGold
import com.example.ui.viewmodel.CameraViewModel

@Composable
fun FileNameScreen(
    viewModel: CameraViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.stampConfig.collectAsStateWithLifecycle()
    val liveTelemetry by viewModel.telemetry.collectAsStateWithLifecycle()

    val previewTelemetry = remember(liveTelemetry) {
        if (liveTelemetry.hasFix) {
            liveTelemetry
        } else {
            GpsTelemetry(
                latitude = 37.422094,
                longitude = -122.084057,
                addressLine = "1600 Amphitheatre Pkwy, Mountain View",
                cityState = "Mountain View",
                country = "USA",
                countryCode = "US",
                signalState = GpsSignalState.LOCKED
            )
        }
    }

    val livePreviewFilename = remember(config, previewTelemetry) {
        MediaStoreSaver.buildCustomFileName(config, previewTelemetry)
    }

    val orderedFields = remember(config.fnFieldOrderSerialized) {
        config.parsedFnFieldOrder()
    }

    var expandCustom1 by remember { mutableStateOf(true) }
    var expandCustom2 by remember { mutableStateOf(false) }
    var expandCustom3 by remember { mutableStateOf(false) }

    fun moveField(index: Int, delta: Int) {
        val target = index + delta
        if (target !in orderedFields.indices) return
        val mutable = orderedFields.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(target, item)
        viewModel.updateStampConfig {
            it.copy(fnFieldOrderSerialized = StampConfig.serializeFnFieldOrder(mutable))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("filename_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "File Name",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    viewModel.updateStampConfig {
                        it.copy(
                            fnFieldOrderSerialized = StampConfig.serializeFnFieldOrder(FileNameField.entries)
                        )
                    }
                },
                modifier = Modifier.testTag("fn_reset_order_button")
            ) {
                Text("Reset Order", color = ShutterGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Master Custom File Name Switch
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom File Name",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Include location & telemetry details in saved JPG names",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = config.customFileNameEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateStampConfig { it.copy(customFileNameEnabled = enabled) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF141619),
                                checkedTrackColor = ShutterGold
                            ),
                            modifier = Modifier.testTag("filename_master_switch")
                        )
                    }
                }
            }

            // Live Filename Preview Box
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ShutterGold.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .testTag("filename_live_preview")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NEXT CAPTURE FILE NAME",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ShutterGold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = livePreviewFilename,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Use the up/down arrows on any row below to change segment order",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Reorderable Checklist Items (Date/Time, Sequence, Custom1/2/3, Address, LatLon, PlusCode, TimeZone, Note)
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        orderedFields.forEachIndexed { index, field ->
                            when (field) {
                                FileNameField.DATE_TIME -> {
                                    FileNameCheckRow(
                                        title = "Date & Time",
                                        subtitle = "Timestamp segment (YYYYMMDD_hhmmss)",
                                        checked = config.fnIncludeDateTime,
                                        enabled = config.customFileNameEnabled,
                                        onToggle = { v -> viewModel.updateStampConfig { it.copy(fnIncludeDateTime = v) } },
                                        canMoveUp = index > 0,
                                        canMoveDown = index < orderedFields.lastIndex,
                                        onMoveUp = { moveField(index, -1) },
                                        onMoveDown = { moveField(index, +1) },
                                        testTag = "fn_toggle_datetime"
                                    )
                                }
                                FileNameField.SEQUENCE -> {
                                    FileNameCheckRow(
                                        title = "Sequence Number",
                                        subtitle = "Current photo #${config.photoSequenceNumber}",
                                        checked = config.fnIncludeSequence,
                                        enabled = config.customFileNameEnabled,
                                        onToggle = { v -> viewModel.updateStampConfig { it.copy(fnIncludeSequence = v) } },
                                        canMoveUp = index > 0,
                                        canMoveDown = index < orderedFields.lastIndex,
                                        onMoveUp = { moveField(index, -1) },
                                        onMoveDown = { moveField(index, +1) },
                                        trailingContent = {
                                            TextButton(
                                                onClick = { viewModel.updateStampConfig { it.copy(photoSequenceNumber = 1) } },
                                                modifier = Modifier.testTag("fn_reset_sequence_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Reset sequence to 1",
                                                    tint = ShutterGold,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(Modifier.width(2.dp))
                                                Text("#1", color = ShutterGold, fontSize = 11.sp)
                                            }
                                        },
                                        testTag = "fn_toggle_sequence"
                                    )
                                }
                                FileNameField.CUSTOM_1 -> {
                                    FileNameCheckRow(
                                        title = "Custom Name 1",
                                        subtitle = config.fnCustomText1.ifBlank { "Tap arrow to edit tag" },
                                        checked = config.fnIncludeCustom1,
                                        enabled = config.customFileNameEnabled,
                                        onToggle = { v -> viewModel.updateStampConfig { it.copy(fnIncludeCustom1 = v) } },
                                        canMoveUp = index > 0,
                                        canMoveDown = index < orderedFields.lastIndex,
                                        onMoveUp = { moveField(index, -1) },
                                        onMoveDown = { moveField(index, +1) },
                                        trailingContent = {
                                            IconButton(onClick = { expandCustom1 = !expandCustom1 }) {
                                                Icon(
                                                    imageVector = if (expandCustom1) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = "Edit Custom Name 1"
                                                )
                                            }
                                        },
                                        testTag = "fn_toggle_custom1"
                                    )
                                    AnimatedVisibility(visible = expandCustom1) {
                                        OutlinedTextField(
                                            value = config.fnCustomText1,
                                            onValueChange = { txt -> viewModel.updateStampConfig { it.copy(fnCustomText1 = txt.take(32)) } },
                                            label = { Text("Custom Name 1 Text") },
                                            placeholder = { Text("e.g., YVGPSCAM") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                                .testTag("fn_custom1_input")
                                        )
                                    }
                                }
                                FileNameField.CUSTOM_2 -> {
                                    FileNameCheckRow(
                                        title = "Custom Name 2",
                                        subtitle = config.fnCustomText2.ifBlank { "Optional secondary tag" },
                                        checked = config.fnIncludeCustom2,
                                        enabled = config.customFileNameEnabled,
                                        onToggle = { v -> viewModel.updateStampConfig { it.copy(fnIncludeCustom2 = v) } },
                                        canMoveUp = index > 0,
                                        canMoveDown = index < orderedFields.lastIndex,
                                        onMoveUp = { moveField(index, -1) },
                                        onMoveDown = { moveField(index, +1) },
                                        trailingContent = {
                                            IconButton(onClick = { expandCustom2 = !expandCustom2 }) {
                                                Icon(
                                                    imageVector = if (expandCustom2) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = "Edit Custom Name 2"
                                                )
                                            }
                                        },
                                        testTag = "fn_toggle_custom2"
                                    )
                                    AnimatedVisibility(visible = expandCustom2) {
                                        OutlinedTextField(
                                            value = config.fnCustomText2,
                                            onValueChange = { txt -> viewModel.updateStampConfig { it.copy(fnCustomText2 = txt.take(32)) } },
                                            label = { Text("Custom Name 2 Text") },
                                            placeholder = { Text("e.g., SiteInspection") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                                .testTag("fn_custom2_input")
                                        )
                                    }
                                }
                                FileNameField.CUSTOM_3 -> {
                                    FileNameCheckRow(
                                        title = "Custom Name 3",
                                        subtitle = config.fnCustomText3.ifBlank { "Optional project/department code" },
                                        checked = config.fnIncludeCustom3,
                                        enabled = config.customFileNameEnabled,
                                        onToggle = { v -> viewModel.updateStampConfig { it.copy(fnIncludeCustom3 = v) } },
                                        canMoveUp = index > 0,
                                        canMoveDown = index < orderedFields.lastIndex,
                                        onMoveUp = { moveField(index, -1) },
                                        onMoveDown = { moveField(index, +1) },
                                        trailingContent = {
                                            IconButton(onClick = { expandCustom3 = !expandCustom3 }) {
                                                Icon(
                                                    imageVector = if (expandCustom3) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = "Edit Custom Name 3"
                                                )
                                            }
                                        },
                                        testTag = "fn_toggle_custom3"
                                    )
                                    AnimatedVisibility(visible = expandCustom3) {
                                        OutlinedTextField(
                                            value = config.fnCustomText3,
                                            onValueChange = { txt -> viewModel.updateStampConfig { it.copy(fnCustomText3 = txt.take(32)) } },
                                            label = { Text("Custom Name 3 Text") },
                                            placeholder = { Text("e.g., Phase2") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                                .testTag("fn_custom3_input")
                                        )
                                    }
                                }
                                FileNameField.FULL_ADDRESS -> {
                                    FileNameCheckRow(
                                        title = "Full Address",
                                        subtitle = "City, country & street name",
                                        checked = config.fnIncludeFullAddress,
                                        enabled = config.customFileNameEnabled,
                                        onToggle = { v -> viewModel.updateStampConfig { it.copy(fnIncludeFullAddress = v) } },
                                        canMoveUp = index > 0,
                                        canMoveDown = index < orderedFields.lastIndex,
                                        onMoveUp = { moveField(index, -1) },
                                        onMoveDown = { moveField(index, +1) },
                                        testTag = "fn_toggle_full_address"
                                    )
                                }
                                FileNameField.LAT_LON -> {
                                    FileNameCheckRow(
                                        title = "Lat/Long",
                                        subtitle = "Decimal coordinates in filename",
                                        checked = config.fnIncludeLatLon,
                                        enabled = config.customFileNameEnabled,
                                        onToggle = { v -> viewModel.updateStampConfig { it.copy(fnIncludeLatLon = v) } },
                                        canMoveUp = index > 0,
                                        canMoveDown = index < orderedFields.lastIndex,
                                        onMoveUp = { moveField(index, -1) },
                                        onMoveDown = { moveField(index, +1) },
                                        testTag = "fn_toggle_lat_lon"
                                    )
                                }
                                FileNameField.PLUS_CODE -> {
                                    FileNameCheckRow(
                                        title = "Plus Code",
                                        subtitle = "Open Location Code (e.g., 849VCWC8+R9)",
                                        checked = config.fnIncludePlusCode,
                                        enabled = config.customFileNameEnabled,
                                        onToggle = { v -> viewModel.updateStampConfig { it.copy(fnIncludePlusCode = v) } },
                                        canMoveUp = index > 0,
                                        canMoveDown = index < orderedFields.lastIndex,
                                        onMoveUp = { moveField(index, -1) },
                                        onMoveDown = { moveField(index, +1) },
                                        testTag = "fn_toggle_plus_code"
                                    )
                                }
                                FileNameField.TIME_ZONE -> {
                                    FileNameCheckRow(
                                        title = "Time Zone",
                                        subtitle = previewTelemetry.formatTimeZoneOffset(),
                                        checked = config.fnIncludeTimeZone,
                                        enabled = config.customFileNameEnabled,
                                        onToggle = { v -> viewModel.updateStampConfig { it.copy(fnIncludeTimeZone = v) } },
                                        canMoveUp = index > 0,
                                        canMoveDown = index < orderedFields.lastIndex,
                                        onMoveUp = { moveField(index, -1) },
                                        onMoveDown = { moveField(index, +1) },
                                        testTag = "fn_toggle_time_zone"
                                    )
                                }
                                FileNameField.NOTE -> {
                                    FileNameCheckRow(
                                        title = "Note",
                                        subtitle = config.customNote.ifBlank { "Uses active camera stamp note" },
                                        checked = config.fnIncludeNote,
                                        enabled = config.customFileNameEnabled,
                                        onToggle = { v -> viewModel.updateStampConfig { it.copy(fnIncludeNote = v) } },
                                        canMoveUp = index > 0,
                                        canMoveDown = index < orderedFields.lastIndex,
                                        onMoveUp = { moveField(index, -1) },
                                        onMoveDown = { moveField(index, +1) },
                                        testTag = "fn_toggle_note"
                                    )
                                }
                            }
                            if (index < orderedFields.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("filename_done_button")
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }

        AdBannerSlot(placementId = "file_name")
    }
}

@Composable
private fun FileNameCheckRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    testTag: String,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle(!checked) }
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) ShutterGold else Color.Transparent)
                .border(
                    width = 1.6.dp,
                    color = if (checked) ShutterGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "$title enabled",
                    tint = Color(0xFF141619),
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f)
            )
        }

        trailingContent?.invoke()

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onMoveUp,
                enabled = enabled && canMoveUp,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Move $title up",
                    modifier = Modifier.size(16.dp),
                    tint = if (enabled && canMoveUp) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = enabled && canMoveDown,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "Move $title down",
                    modifier = Modifier.size(16.dp),
                    tint = if (enabled && canMoveDown) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                )
            }
        }
    }
}
