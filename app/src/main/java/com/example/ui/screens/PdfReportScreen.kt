package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.GpsTelemetry
import com.example.ui.components.AdBannerSlot
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ShutterGold
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.viewmodel.CameraViewModel
import com.example.ui.viewmodel.GalleryViewModel
import com.example.util.GeneratedPdfReport
import com.example.util.PdfReportGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PdfReportScreen(
    galleryViewModel: GalleryViewModel,
    cameraViewModel: CameraViewModel,
    initialSelectedPhotoIds: Set<Long> = emptySet(),
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photos by galleryViewModel.photos.collectAsStateWithLifecycle()
    val config by cameraViewModel.stampConfig.collectAsStateWithLifecycle()

    var reportTitle by remember { mutableStateOf("YVGPSCAM Field Inspection Report") }
    var inspectorName by remember(config.personName) { mutableStateOf(config.personName) }
    var summaryNote by remember { mutableStateOf("") }
    var selectedIds by remember(initialSelectedPhotoIds) {
        mutableStateOf(initialSelectedPhotoIds)
    }
    var isGenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var generatedReport by remember { mutableStateOf<GeneratedPdfReport?>(null) }

    LaunchedEffect(photos, initialSelectedPhotoIds) {
        if (selectedIds.isEmpty() && photos.isNotEmpty() && initialSelectedPhotoIds.isEmpty()) {
            selectedIds = photos.take(6).map { it.id }.toSet()
        }
    }

    val selectedPhotos = remember(photos, selectedIds) {
        photos.filter { selectedIds.contains(it.id) }
    }

    fun sharePdf(report: GeneratedPdfReport, emailMode: Boolean) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            report.file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, reportTitle.ifBlank { "YVGPSCAM PDF Report" })
            putExtra(
                Intent.EXTRA_TEXT,
                "Attached YVGPSCAM PDF field report (${report.photoCount} captures, ${report.pageCount} pages)."
            )
            if (emailMode && config.defaultReportEmail.isNotBlank()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(config.defaultReportEmail))
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, if (emailMode) "Email PDF Report" else "Share PDF Report"))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
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
                    .testTag("pdf_report_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "PDF Field Report",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Report Metadata",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ShutterGold
                        )
                        OutlinedTextField(
                            value = reportTitle,
                            onValueChange = { reportTitle = it.take(64) },
                            label = { Text("Report Title") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_report_title_input")
                        )
                        OutlinedTextField(
                            value = inspectorName,
                            onValueChange = { inspectorName = it.take(40) },
                            label = { Text("Inspector / Prepared By") },
                            placeholder = { Text("Optional surveyor name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_report_inspector_input")
                        )
                        OutlinedTextField(
                            value = summaryNote,
                            onValueChange = { summaryNote = it.take(120) },
                            label = { Text("Summary Note") },
                            placeholder = { Text("Site overview or inspection remarks") },
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pdf_report_summary_input")
                        )
                    }
                }
            }

            // Generated PDF Output Card
            generatedReport?.let { report ->
                item {
                    Surface(
                        color = TelemetryEmerald.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TelemetryEmerald.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                            .testTag("generated_pdf_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = TelemetryEmerald,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = report.file.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${report.pageCount} pages • ${report.photoCount} items • ${(report.fileSizeBytes / 1024).coerceAtLeast(1)} KB",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = TelemetryEmerald
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { sharePdf(report, emailMode = true) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("email_generated_pdf_button")
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Email PDF")
                                }
                                Button(
                                    onClick = { sharePdf(report, emailMode = false) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TelemetryEmerald,
                                        contentColor = ObsidianBg
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("share_generated_pdf_button")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Share PDF", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            errorMessage?.let { msg ->
                item {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Selection Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Included Captures (${selectedPhotos.size} / ${photos.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { selectedIds = photos.map { it.id }.toSet() }) {
                            Text("Select All", color = TelemetryEmerald)
                        }
                        TextButton(onClick = { selectedIds = emptySet() }) {
                            Text("Clear")
                        }
                    }
                }
            }

            if (photos.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    ) {
                        Text(
                            text = "No photos in your gallery yet. Capture geotagged photos first to generate a PDF report.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(photos, key = { it.id }) { photo ->
                    val checked = selectedIds.contains(photo.id)
                    val dateStr = remember(photo.capturedAtMillis) {
                        SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(photo.capturedAtMillis))
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (checked) 1.8.dp else 1.dp,
                                color = if (checked) ShutterGold else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                selectedIds = if (checked) selectedIds - photo.id else selectedIds + photo.id
                            }
                            .testTag("pdf_select_photo_${photo.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = photo.mediaUri,
                                contentDescription = photo.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = photo.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
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
                                val coords = if (photo.latitude != null && photo.longitude != null) {
                                    "${GpsTelemetry.formatSingleCoord(photo.latitude, true)}, ${GpsTelemetry.formatSingleCoord(photo.longitude, false)}"
                                } else {
                                    "No GPS fix"
                                }
                                Text(
                                    text = "$coords • $dateStr",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = TelemetryEmerald,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (checked) ShutterGold else Color.Transparent)
                                    .border(
                                        1.5.dp,
                                        if (checked) ShutterGold else MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (checked) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected for PDF",
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

        // Sticky Bottom Generate Button
        Button(
            enabled = selectedPhotos.isNotEmpty() && !isGenerating,
            onClick = {
                isGenerating = true
                errorMessage = null
                scope.launch {
                    val result = PdfReportGenerator.generateReport(
                        context = context,
                        title = reportTitle,
                        inspectorName = inspectorName,
                        summaryNote = summaryNote,
                        photos = selectedPhotos
                    )
                    isGenerating = false
                    result.onSuccess { rep ->
                        generatedReport = rep
                    }.onFailure { err ->
                        errorMessage = err.message ?: "Failed to generate PDF report."
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = ShutterGold,
                contentColor = Color(0xFF141619)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(50.dp)
                .testTag("generate_pdf_report_button")
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    color = Color(0xFF141619),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Generating PDF...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generate PDF Report (${selectedPhotos.size})", fontWeight = FontWeight.Bold)
            }
        }

        AdBannerSlot(placementId = "pdf_report")
    }
}
