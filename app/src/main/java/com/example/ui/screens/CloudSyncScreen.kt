package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddToDrive
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.cloud.DriveAuthOutcome
import com.example.data.cloud.DrivePhotoBackup
import com.example.data.cloud.GoogleAccountProfile
import com.example.data.cloud.GoogleDriveAccountStore
import com.example.data.cloud.GoogleDriveAuthHelper
import com.example.data.cloud.GoogleDriveAutoSyncCoordinator
import com.example.data.cloud.GoogleDriveBackupRepository
import com.example.data.local.PhotoRepository
import com.example.data.model.CapturedPhotoEntity
import com.example.ui.theme.AlertCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.viewmodel.CloudBackupsUiState
import com.example.ui.viewmodel.CloudSyncViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudSyncRouteScreen(
    photoRepository: PhotoRepository,
    autoSyncCoordinator: GoogleDriveAutoSyncCoordinator,
    accountStore: GoogleDriveAccountStore,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val account by accountStore.accountFlow.collectAsStateWithLifecycle(initialValue = GoogleAccountProfile())
    val viewModel: CloudSyncViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CloudSyncViewModel(
                    autoSyncCoordinator = autoSyncCoordinator,
                    photoRepository = photoRepository
                )
            }
        }
    )

    if (!account.isSignedIn) {
        GoogleDriveSignInGate(
            accountStore = accountStore,
            onSignedIn = { autoSyncCoordinator.triggerSyncNow() },
            modifier = modifier
        )
    } else {
        GoogleDriveBackupDashboard(
            account = account,
            accountStore = accountStore,
            viewModel = viewModel,
            onOpenCamera = onOpenCamera,
            modifier = modifier
        )
    }
}

@Composable
private fun GoogleDriveSignInGate(
    accountStore: GoogleDriveAccountStore,
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val profile = GoogleDriveAuthHelper.attemptSilentGoogleSignIn(context, credentialManager)
        if (profile != null) {
            accountStore.saveAccount(profile.email, profile.displayName)
            onSignedIn()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = ObsidianSurface,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_brand_yv_logo),
                    contentDescription = "YVGPSCAM Brand Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
                )

                Text(
                    text = "Google Drive Auto Backup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Sign in once with your Google Account to enable automatic background backup and automatic restore for your geotagged photos whenever you are online.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurfaceHigh)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TelemetryEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Auto Backup & Auto Restore via Drive",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = TelemetryEmerald
                    )
                }

                if (authError != null) {
                    Text(
                        text = authError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = AlertCoral,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    enabled = !isLoading && activity != null,
                    onClick = {
                        val host = activity ?: return@Button
                        isLoading = true
                        authError = null
                        scope.launch {
                            GoogleDriveAuthHelper.signInInteractive(host, credentialManager).fold(
                                onSuccess = { profile ->
                                    isLoading = false
                                    if (profile != null) {
                                        accountStore.saveAccount(profile.email, profile.displayName)
                                        onSignedIn()
                                    }
                                },
                                onFailure = { err ->
                                    isLoading = false
                                    authError = err.localizedMessage ?: "Sign in failed"
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TelemetryEmerald,
                        contentColor = ObsidianBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = ObsidianBg,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AddToDrive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Sign in with Google", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleDriveBackupDashboard(
    account: GoogleAccountProfile,
    accountStore: GoogleDriveAccountStore,
    viewModel: CloudSyncViewModel,
    onOpenCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val needsDriveConsent by viewModel.needsDriveConsent.collectAsStateWithLifecycle()
    val accessToken by viewModel.accessToken.collectAsStateWithLifecycle()
    val backupsState by viewModel.cloudBackupsState.collectAsStateWithLifecycle()
    val localPhotos by viewModel.localPhotos.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    var hasAutoPromptedConsent by remember(account.email) { mutableStateOf(false) }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val host = activity ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            GoogleDriveAuthHelper.extractTokenFromResolutionIntent(host, result.data).fold(
                onSuccess = { token -> viewModel.onAccessTokenGranted(token) },
                onFailure = { err ->
                    viewModel.onDriveAuthorizationError(err.localizedMessage ?: "Google Drive consent failed.")
                }
            )
        } else {
            viewModel.onDriveAuthorizationError("Google Drive permission is needed to enable Auto Backup & Restore.")
        }
    }

    fun requestDriveAuthorization() {
        val host = activity ?: return
        scope.launch {
            when (val outcome = GoogleDriveAuthHelper.requestDriveAccessToken(host)) {
                is DriveAuthOutcome.TokenReady -> viewModel.onAccessTokenGranted(outcome.accessToken)
                is DriveAuthOutcome.NeedsResolution -> consentLauncher.launch(outcome.intentSenderRequest)
                is DriveAuthOutcome.Failed -> viewModel.onDriveAuthorizationError(outcome.message)
            }
        }
    }

    // Automatically trigger consent resolution if signed in and online without Drive permission yet
    LaunchedEffect(account.email, needsDriveConsent, isOnline) {
        if (isOnline && needsDriveConsent && !hasAutoPromptedConsent && activity != null) {
            hasAutoPromptedConsent = true
            requestDriveAuthorization()
        }
    }

    LaunchedEffect(actionState.statusMessage, actionState.errorMessage) {
        if (actionState.statusMessage != null || actionState.errorMessage != null) {
            delay(4000)
            viewModel.clearFeedback()
        }
    }

    val driveBackups = (backupsState as? CloudBackupsUiState.Success)?.backups.orEmpty()
    val driveIds = remember(driveBackups) { driveBackups.map { it.backupId }.toSet() }
    val localIds = remember(localPhotos) {
        localPhotos.map {
            GoogleDriveBackupRepository.buildDeterministicBackupId(it.capturedAtMillis, it.displayName)
        }.toSet()
    }
    val pendingUploadCount = remember(localPhotos, driveIds) {
        localPhotos.count {
            !driveIds.contains(GoogleDriveBackupRepository.buildDeterministicBackupId(it.capturedAtMillis, it.displayName))
        }
    }
    val driveOnlyCount = remember(driveBackups, localIds) {
        driveBackups.count { !localIds.contains(it.backupId) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
    ) {
        // Header with Account Info, Sync Trigger & Sign Out
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_brand_yv_logo),
                    contentDescription = "YVGPSCAM Brand Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                )
                Column {
                    Text(
                        text = account.displayName.ifBlank { "Google Drive Account" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = account.email.ifBlank { "Connected to Google Drive" },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = TelemetryEmerald,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (accessToken != null && isOnline) {
                    IconButton(
                        enabled = !actionState.isSyncing,
                        onClick = { viewModel.refreshBackups() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianSurfaceHigh)
                            .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                            .testTag("drive_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Google Drive now",
                            tint = TelemetryEmerald
                        )
                    }
                }
                IconButton(
                    onClick = {
                        scope.launch {
                            viewModel.clearSession()
                            GoogleDriveAuthHelper.signOut(credentialManager, accountStore)
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurfaceHigh)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                        .testTag("cloud_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign out",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (backupsState is CloudBackupsUiState.NeedsDrivePermission) {
                item {
                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, TelemetryEmerald.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddToDrive,
                                contentDescription = null,
                                tint = TelemetryEmerald,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "One-Time Google Drive Permission",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Allow access once to enable fully automatic backup and automatic restore in your personal '${GoogleDriveBackupRepository.BACKUP_FOLDER_NAME}' folder.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            actionState.errorMessage?.let { err ->
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AlertCoral,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Button(
                                onClick = { requestDriveAuthorization() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TelemetryEmerald,
                                    contentColor = ObsidianBg
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("authorize_drive_button")
                            ) {
                                Icon(Icons.Default.AddToDrive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Enable Auto Backup & Restore", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Auto-Sync Status & Summary Card
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Google Drive Auto-Sync",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Folder: ${GoogleDriveBackupRepository.BACKUP_FOLDER_NAME} • ${localPhotos.size} local • ${driveBackups.size} in Drive",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (!isOnline) {
                                        Icons.Default.WifiOff
                                    } else if (pendingUploadCount == 0 && driveOnlyCount == 0 && driveBackups.isNotEmpty()) {
                                        Icons.Default.CloudDone
                                    } else {
                                        Icons.Default.Autorenew
                                    },
                                    contentDescription = null,
                                    tint = if (isOnline) TelemetryEmerald else SignalAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Automatic Backup & Restore active badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ObsidianSurfaceHigh)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("auto_sync_status_badge")
                            ) {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.Autorenew else Icons.Default.WifiOff,
                                    contentDescription = null,
                                    tint = if (isOnline) TelemetryEmerald else SignalAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (!isOnline) {
                                        "OFFLINE • AUTO-SYNC QUEUED UNTIL CONNECTED"
                                    } else if (actionState.isSyncing) {
                                        "AUTO-SYNC RUNNING..."
                                    } else {
                                        "AUTO BACKUP & AUTO RESTORE ACTIVE"
                                    },
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOnline) TelemetryEmerald else SignalAmber
                                )
                            }

                            if (actionState.isSyncing) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = actionState.progressLabel,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = TelemetryEmerald
                                    )
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = TelemetryEmerald,
                                        trackColor = ObsidianSurfaceHigh
                                    )
                                }
                            }

                            actionState.statusMessage?.let { msg ->
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TelemetryEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            actionState.errorMessage?.let { err ->
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AlertCoral
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    enabled = !actionState.isSyncing && isOnline && localPhotos.isNotEmpty(),
                                    onClick = { viewModel.backupAllMissing() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TelemetryEmerald,
                                        contentColor = ObsidianBg
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cloud_backup_all_button")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (pendingUploadCount > 0) "Sync Now ($pendingUploadCount)" else "All Synced",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                OutlinedButton(
                                    enabled = !actionState.isSyncing && isOnline && driveOnlyCount > 0,
                                    onClick = { viewModel.restoreAllMissing() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cloud_restore_all_button")
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (driveOnlyCount > 0) "Restore ($driveOnlyCount)" else "Up to Date",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Queued local photos waiting for upload (if offline or skipped previously)
                val pendingLocals = localPhotos.filter {
                    !driveIds.contains(
                        GoogleDriveBackupRepository.buildDeterministicBackupId(it.capturedAtMillis, it.displayName)
                    )
                }
                if (pendingLocals.isNotEmpty()) {
                    item {
                        Text(
                            text = if (isOnline) "Queued for Auto Backup (${pendingLocals.size})" else "Waiting for Internet (${pendingLocals.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = SignalAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(pendingLocals, key = { "local_${it.id}" }) { photo ->
                        PendingLocalPhotoCard(
                            photo = photo,
                            isSyncing = actionState.isSyncing,
                            isOnline = isOnline,
                            onUpload = { viewModel.backupSinglePhoto(photo) }
                        )
                    }
                }

                // Google Drive Archive Section
                item {
                    Text(
                        text = "Google Drive Archive (${driveBackups.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                when (val state = backupsState) {
                    CloudBackupsUiState.NeedsDrivePermission -> Unit
                    CloudBackupsUiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = TelemetryEmerald)
                            }
                        }
                    }

                    is CloudBackupsUiState.Error -> {
                        item {
                            Surface(
                                color = ObsidianSurface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, AlertCoral.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = AlertCoral)
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AlertCoral,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedButton(onClick = { viewModel.refreshBackups() }) {
                                        Text("Retry", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    is CloudBackupsUiState.Success -> {
                        if (state.backups.isEmpty()) {
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
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Ready for Auto Backup",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (localPhotos.isNotEmpty()) {
                                                "Your geotagged photos will upload automatically to your personal Google Drive while connected."
                                            } else {
                                                "Capture geotagged photos in the Camera tab—they will back up automatically to Google Drive."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        if (localPhotos.isEmpty()) {
                                            OutlinedButton(onClick = onOpenCamera) {
                                                Text("Open Camera")
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            items(state.backups, key = { it.driveFileId }) { backup ->
                                DriveBackupItemCard(
                                    backup = backup,
                                    accessToken = accessToken.orEmpty(),
                                    isOnDevice = localIds.contains(backup.backupId),
                                    isSyncing = actionState.isSyncing,
                                    onRestore = { viewModel.restoreBackup(backup) },
                                    onDelete = { viewModel.deleteBackup(backup) }
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PendingLocalPhotoCard(
    photo: CapturedPhotoEntity,
    isSyncing: Boolean,
    isOnline: Boolean,
    onUpload: () -> Unit
) {
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SignalAmber.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = photo.mediaUri,
                contentDescription = photo.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianSurfaceHigh)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = photo.cityState.ifBlank { photo.displayName },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
                        .format(Date(photo.capturedAtMillis)),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                enabled = !isSyncing && isOnline,
                onClick = onUpload,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = "Back up photo", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Back Up", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun DriveBackupItemCard(
    backup: DrivePhotoBackup,
    accessToken: String,
    isOnDevice: Boolean,
    isSyncing: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val dateLabel = remember(backup.capturedAtMillis) {
        SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date(backup.capturedAtMillis))
    }
    val thumbRequest = remember(backup.driveFileId, accessToken) {
        if (accessToken.isNotBlank()) {
            ImageRequest.Builder(context)
                .data(GoogleDriveBackupRepository.buildMediaUrl(backup.driveFileId))
                .addHeader("Authorization", "Bearer $accessToken")
                .size(160)
                .crossfade(true)
                .build()
        } else null
    }

    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .testTag("cloud_backup_card_${backup.driveFileId}")
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
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianSurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                if (thumbRequest != null) {
                    AsyncImage(
                        model = thumbRequest,
                        contentDescription = backup.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.AddToDrive, contentDescription = null, tint = TelemetryEmerald)
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = listOf(backup.cityState, backup.country)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                        .ifBlank { backup.displayName },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val lat = backup.latitude
                val lon = backup.longitude
                if (lat != null && lon != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = TelemetryEmerald,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.5f, %.5f", lat, lon),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.5.sp,
                            color = TelemetryEmerald
                        )
                    }
                }

                Text(
                    text = dateLabel,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = if (isOnDevice) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = if (isOnDevice) TelemetryEmerald else SignalAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (isOnDevice) "Synced on device" else "Drive only",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = if (isOnDevice) TelemetryEmerald else SignalAmber
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (!isOnDevice) {
                    IconButton(
                        enabled = !isSyncing,
                        onClick = onRestore,
                        modifier = Modifier.testTag("restore_backup_${backup.driveFileId}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Restore to device",
                            tint = TelemetryEmerald
                        )
                    }
                }
                IconButton(
                    enabled = !isSyncing,
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_backup_${backup.driveFileId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete from Google Drive",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
