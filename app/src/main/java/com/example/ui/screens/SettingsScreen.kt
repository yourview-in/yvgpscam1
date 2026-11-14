package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AppThemeMode
import com.example.data.model.PhotoQualityLevel
import com.example.data.model.ScreenTapAction
import com.example.data.model.StampOrientationMode
import com.example.data.model.VideoQualityLevel
import com.example.data.model.VolumeButtonAction
import com.example.ui.components.AdBannerSlot
import com.example.ui.theme.ShutterGold
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.viewmodel.CameraViewModel

private enum class SettingsChoiceDialog {
    NONE,
    LANGUAGE,
    THEME,
    PHOTO_QUALITY,
    VIDEO_QUALITY,
    VOLUME_BUTTON,
    SCREEN_TAP,
    STAMP_ORIENTATION,
    EMAIL_ADDRESS,
    ABOUT_APP
}

@Composable
fun SettingsScreen(
    viewModel: CameraViewModel,
    onBack: () -> Unit,
    onOpenPdfReport: () -> Unit,
    onOpenImageOnMap: () -> Unit,
    onOpenOfflineMaps: () -> Unit,
    onOpenCloudSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.stampConfig.collectAsStateWithLifecycle()
    var activeDialog by remember { mutableStateOf(SettingsChoiceDialog.NONE) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header: < Settings
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
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Settings",
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
            // GENERAL
            item {
                SettingsSectionHeader("General")
                SettingsCardContainer {
                    SettingsNavigationRow(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = config.appLanguage,
                        onClick = { activeDialog = SettingsChoiceDialog.LANGUAGE },
                        testTag = "settings_language_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavigationRow(
                        icon = Icons.Default.Palette,
                        title = "App Theme",
                        subtitle = config.appThemeMode.displayName,
                        onClick = { activeDialog = SettingsChoiceDialog.THEME },
                        testTag = "settings_theme_row"
                    )
                }
            }

            // CAMERA
            item {
                SettingsSectionHeader("Camera")
                SettingsCardContainer {
                    SettingsNavigationRow(
                        icon = Icons.Default.HighQuality,
                        title = "Photo Quality",
                        subtitle = config.photoQuality.displayName,
                        onClick = { activeDialog = SettingsChoiceDialog.PHOTO_QUALITY },
                        testTag = "settings_photo_quality_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavigationRow(
                        icon = Icons.Default.Videocam,
                        title = "Video Quality",
                        subtitle = config.videoQuality.displayName,
                        onClick = { activeDialog = SettingsChoiceDialog.VIDEO_QUALITY },
                        testTag = "settings_video_quality_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavigationRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Volume Button",
                        subtitle = config.volumeButtonAction.displayName,
                        onClick = { activeDialog = SettingsChoiceDialog.VOLUME_BUTTON },
                        testTag = "settings_volume_button_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavigationRow(
                        icon = Icons.Default.TouchApp,
                        title = "Screen Tap",
                        subtitle = config.screenTapAction.displayName,
                        onClick = { activeDialog = SettingsChoiceDialog.SCREEN_TAP },
                        testTag = "settings_screen_tap_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        icon = Icons.Default.QrCodeScanner,
                        title = "QR Code Detection",
                        subtitle = "Detect QR codes live in camera preview",
                        checked = config.qrDetectionEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateStampConfig { it.copy(qrDetectionEnabled = enabled) }
                        },
                        testTag = "settings_qr_detection_switch"
                    )
                }
            }

            // STAMP
            item {
                SettingsSectionHeader("Stamp")
                SettingsCardContainer {
                    SettingsNavigationRow(
                        icon = Icons.Default.ScreenRotation,
                        title = "Stamp Orientation",
                        subtitle = config.stampOrientation.displayName,
                        onClick = { activeDialog = SettingsChoiceDialog.STAMP_ORIENTATION },
                        testTag = "settings_stamp_orientation_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    // Locked YVGPSCAM Brand Logo row (non-removable)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(
                                    context,
                                    "YVGPSCAM brand logo is permanently enabled on stamped media.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("settings_brand_logo_locked_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_brand_yv_logo),
                            contentDescription = "YVGPSCAM brand logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .border(1.dp, ShutterGold, RoundedCornerShape(7.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Show App Logo",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Required brand logo",
                                    tint = ShutterGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "YVGPSCAM watermark permanently active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = ShutterGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ON",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ShutterGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        icon = Icons.Default.CameraAlt,
                        title = "Show Camera Indicator",
                        subtitle = "Display Front or Rear lens badge on stamp",
                        checked = config.showCameraIndicator,
                        onCheckedChange = { enabled ->
                            viewModel.updateStampConfig { it.copy(showCameraIndicator = enabled) }
                        },
                        testTag = "settings_camera_indicator_switch"
                    )
                }
            }

            // STORAGE
            item {
                SettingsSectionHeader("Storage")
                SettingsCardContainer {
                    SettingsSwitchRow(
                        icon = Icons.Default.SaveAlt,
                        title = "Auto Save Original Photo",
                        subtitle = "Also save an unstamped copy to Pictures/YVGPSCAM/Originals",
                        checked = config.autoSaveOriginalPhoto,
                        onCheckedChange = { enabled ->
                            viewModel.updateStampConfig { it.copy(autoSaveOriginalPhoto = enabled) }
                        },
                        testTag = "settings_auto_save_original_switch"
                    )
                }
            }

            // REPORT AFTER CAPTURE
            item {
                SettingsSectionHeader("Report After Capture")
                SettingsCardContainer {
                    SettingsSwitchRow(
                        icon = Icons.Default.Email,
                        title = "Email Photo",
                        subtitle = if (config.defaultReportEmail.isNotBlank()) {
                            "Send to ${config.defaultReportEmail}"
                        } else {
                            "Open email composer after capturing"
                        },
                        checked = config.emailAfterCapture,
                        onCheckedChange = { enabled ->
                            viewModel.updateStampConfig { it.copy(emailAfterCapture = enabled) }
                        },
                        testTag = "settings_email_photo_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavigationRow(
                        icon = Icons.Default.Description,
                        title = "Default Report Email",
                        subtitle = config.defaultReportEmail.ifBlank { "Tap to set recipient email" },
                        onClick = { activeDialog = SettingsChoiceDialog.EMAIL_ADDRESS },
                        testTag = "settings_report_email_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavigationRow(
                        icon = Icons.Default.PictureAsPdf,
                        title = "PDF Report Generator",
                        subtitle = "Compile multi-photo field telemetry report",
                        onClick = onOpenPdfReport,
                        testTag = "settings_pdf_report_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        icon = Icons.Default.Share,
                        title = "Share Photo",
                        subtitle = "Automatically open Share Sheet after capture",
                        checked = config.autoShareAfterCapture,
                        onCheckedChange = { enabled ->
                            viewModel.updateStampConfig { it.copy(autoShareAfterCapture = enabled) }
                        },
                        testTag = "settings_auto_share_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchRow(
                        icon = Icons.Default.PictureAsPdf,
                        title = "Reports Mode on Camera",
                        subtitle = "Show REPORTS shortcut in camera mode strip",
                        checked = config.showReportsModeOnCamera,
                        onCheckedChange = { enabled ->
                            viewModel.updateStampConfig { it.copy(showReportsModeOnCamera = enabled) }
                        },
                        testTag = "settings_reports_mode_switch"
                    )
                }
            }

            // HELP & SUPPORT
            item {
                SettingsSectionHeader("Help & Support")
                SettingsCardContainer {
                    SettingsNavigationRow(
                        icon = Icons.Default.Map,
                        title = "Image on Map",
                        subtitle = "View captured photos pinned on interactive Google Maps",
                        onClick = onOpenImageOnMap,
                        testTag = "settings_image_on_map_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavigationRow(
                        icon = Icons.Default.OfflinePin,
                        title = "Offline Map Tiles",
                        subtitle = "Manage cached regional map tiles",
                        onClick = onOpenOfflineMaps,
                        testTag = "settings_offline_maps_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavigationRow(
                        icon = Icons.Default.CloudUpload,
                        title = "Google Drive Backup & Sync",
                        subtitle = "Manage cloud backup account & sync queue",
                        onClick = onOpenCloudSync,
                        testTag = "settings_cloud_sync_row"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsNavigationRow(
                        icon = Icons.Default.Info,
                        title = "About YVGPSCAM",
                        subtitle = "GPS telemetry camera & field reporting suite",
                        onClick = { activeDialog = SettingsChoiceDialog.ABOUT_APP },
                        testTag = "settings_about_row"
                    )
                }
            }
        }

        AdBannerSlot(placementId = "settings")
    }

    // Selection Dialogs
    when (activeDialog) {
        SettingsChoiceDialog.LANGUAGE -> {
            SingleChoiceSettingsDialog(
                title = "App Language",
                options = listOf("English", "Hindi", "Telugu", "Spanish", "French"),
                selected = config.appLanguage,
                labelMapper = { it },
                onSelect = { lang ->
                    viewModel.updateStampConfig { it.copy(appLanguage = lang) }
                    activeDialog = SettingsChoiceDialog.NONE
                },
                onDismiss = { activeDialog = SettingsChoiceDialog.NONE }
            )
        }

        SettingsChoiceDialog.THEME -> {
            SingleChoiceSettingsDialog(
                title = "App Theme",
                options = AppThemeMode.entries,
                selected = config.appThemeMode,
                labelMapper = { it.displayName },
                onSelect = { mode ->
                    viewModel.updateStampConfig { it.copy(appThemeMode = mode) }
                    activeDialog = SettingsChoiceDialog.NONE
                },
                onDismiss = { activeDialog = SettingsChoiceDialog.NONE }
            )
        }

        SettingsChoiceDialog.PHOTO_QUALITY -> {
            SingleChoiceSettingsDialog(
                title = "Photo Quality",
                options = PhotoQualityLevel.entries,
                selected = config.photoQuality,
                labelMapper = { it.displayName },
                onSelect = { quality ->
                    viewModel.updateStampConfig { it.copy(photoQuality = quality) }
                    activeDialog = SettingsChoiceDialog.NONE
                },
                onDismiss = { activeDialog = SettingsChoiceDialog.NONE }
            )
        }

        SettingsChoiceDialog.VIDEO_QUALITY -> {
            SingleChoiceSettingsDialog(
                title = "Video Quality",
                options = VideoQualityLevel.entries,
                selected = config.videoQuality,
                labelMapper = { it.displayName },
                onSelect = { quality ->
                    viewModel.updateStampConfig { it.copy(videoQuality = quality) }
                    activeDialog = SettingsChoiceDialog.NONE
                },
                onDismiss = { activeDialog = SettingsChoiceDialog.NONE }
            )
        }

        SettingsChoiceDialog.VOLUME_BUTTON -> {
            SingleChoiceSettingsDialog(
                title = "Volume Button Action",
                options = VolumeButtonAction.entries,
                selected = config.volumeButtonAction,
                labelMapper = { it.displayName },
                onSelect = { action ->
                    viewModel.updateStampConfig { it.copy(volumeButtonAction = action) }
                    activeDialog = SettingsChoiceDialog.NONE
                },
                onDismiss = { activeDialog = SettingsChoiceDialog.NONE }
            )
        }

        SettingsChoiceDialog.SCREEN_TAP -> {
            SingleChoiceSettingsDialog(
                title = "Screen Tap Action",
                options = ScreenTapAction.entries,
                selected = config.screenTapAction,
                labelMapper = { it.displayName },
                onSelect = { action ->
                    viewModel.updateStampConfig { it.copy(screenTapAction = action) }
                    activeDialog = SettingsChoiceDialog.NONE
                },
                onDismiss = { activeDialog = SettingsChoiceDialog.NONE }
            )
        }

        SettingsChoiceDialog.STAMP_ORIENTATION -> {
            SingleChoiceSettingsDialog(
                title = "Stamp Orientation",
                options = StampOrientationMode.entries,
                selected = config.stampOrientation,
                labelMapper = { it.displayName },
                onSelect = { orientation ->
                    viewModel.updateStampConfig { it.copy(stampOrientation = orientation) }
                    activeDialog = SettingsChoiceDialog.NONE
                },
                onDismiss = { activeDialog = SettingsChoiceDialog.NONE }
            )
        }

        SettingsChoiceDialog.EMAIL_ADDRESS -> {
            var emailDraft by remember(config.defaultReportEmail) {
                mutableStateOf(config.defaultReportEmail)
            }
            AlertDialog(
                onDismissRequest = { activeDialog = SettingsChoiceDialog.NONE },
                title = { Text("Default Report Email") },
                text = {
                    OutlinedTextField(
                        value = emailDraft,
                        onValueChange = { emailDraft = it.trim() },
                        label = { Text("Recipient Email") },
                        placeholder = { Text("reports@company.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_email_input")
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateStampConfig { it.copy(defaultReportEmail = emailDraft) }
                            activeDialog = SettingsChoiceDialog.NONE
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
                    TextButton(onClick = { activeDialog = SettingsChoiceDialog.NONE }) {
                        Text("Cancel")
                    }
                }
            )
        }

        SettingsChoiceDialog.ABOUT_APP -> {
            AlertDialog(
                onDismissRequest = { activeDialog = SettingsChoiceDialog.NONE },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_brand_yv_logo),
                            contentDescription = "YVGPSCAM Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(9.dp))
                        )
                        Text("YVGPSCAM")
                    }
                },
                text = {
                    Text(
                        text = "YVGPSCAM captures verified geotagged photos and videos with live GPS coordinates, Plus Codes, Google Maps tiles, compass azimuth, horizon leveling, QR telemetry, and multi-photo PDF field reports."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { activeDialog = SettingsChoiceDialog.NONE }) {
                        Text("Close", color = TelemetryEmerald, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        SettingsChoiceDialog.NONE -> Unit
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = ShutterGold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCardContainer(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ShutterGold,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Open $title",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) TelemetryEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF141619),
                checkedTrackColor = ShutterGold
            )
        )
    }
}

@Composable
private fun <T> SingleChoiceSettingsDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelMapper: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    val isSelected = option == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(option) }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = ShutterGold)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = labelMapper(option),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
