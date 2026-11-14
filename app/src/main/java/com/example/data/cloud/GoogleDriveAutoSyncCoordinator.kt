package com.example.data.cloud

import android.content.Context
import androidx.credentials.CredentialManager
import com.example.data.local.PhotoRepository
import com.example.data.model.CapturedPhotoEntity
import com.example.ui.viewmodel.CloudBackupsUiState
import com.example.ui.viewmodel.CloudSyncActionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GoogleDriveAutoSyncCoordinator(
    private val appContext: Context,
    private val accountStore: GoogleDriveAccountStore,
    private val driveRepository: GoogleDriveBackupRepository,
    private val photoRepository: PhotoRepository,
    private val mediaGateway: CloudMediaGateway,
    private val connectivityObserver: NetworkConnectivityObserver
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    @Volatile
    private var started = false

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    private val _needsDriveConsent = MutableStateFlow(false)
    val needsDriveConsent: StateFlow<Boolean> = _needsDriveConsent.asStateFlow()

    private val _backupsState = MutableStateFlow<CloudBackupsUiState>(CloudBackupsUiState.Loading)
    val backupsState: StateFlow<CloudBackupsUiState> = _backupsState.asStateFlow()

    private val _actionState = MutableStateFlow(CloudSyncActionState())
    val actionState: StateFlow<CloudSyncActionState> = _actionState.asStateFlow()

    @OptIn(FlowPreview::class)
    fun start() {
        if (started) return
        started = true
        connectivityObserver.startObserving()

        // Try silent Google Sign-In on startup so returning users auto-sync immediately
        scope.launch {
            val currentAccount = accountStore.accountFlow.first()
            if (!currentAccount.isSignedIn) {
                runCatching {
                    val cm = CredentialManager.create(appContext)
                    val profile = GoogleDriveAuthHelper.attemptSilentGoogleSignIn(appContext, cm)
                    if (profile != null) {
                        accountStore.saveAccount(profile.email, profile.displayName)
                    }
                }
            }
        }

        // Automatically run Auto Backup + Auto Restore whenever logged in, online, or local photos change
        scope.launch {
            val localSignatureFlow = photoRepository.allPhotos
                .map { list -> list.map { "${it.id}:${it.capturedAtMillis}:${it.displayName}" } }
                .distinctUntilChanged()

            combine(
                accountStore.accountFlow.map { it.isSignedIn }.distinctUntilChanged(),
                connectivityObserver.isOnline,
                localSignatureFlow
            ) { isSignedIn, online, _ ->
                isSignedIn to online
            }
                .debounce(900L)
                .collectLatest { (isSignedIn, online) ->
                    if (!isSignedIn) {
                        _accessToken.value = null
                        _needsDriveConsent.value = false
                        return@collectLatest
                    }
                    if (!online) {
                        _actionState.update {
                            if (!it.isSyncing) {
                                it.copy(
                                    progressLabel = "",
                                    statusMessage = "Offline — Auto Backup & Restore queued until internet returns."
                                )
                            } else it
                        }
                        return@collectLatest
                    }
                    syncNowInternal(forceRefreshList = true)
                }
        }
    }

    fun onAccessTokenGranted(token: String) {
        _accessToken.value = token
        _needsDriveConsent.value = false
        scope.launch {
            syncNowInternal(forceRefreshList = true)
        }
    }

    fun onDriveAuthorizationError(message: String) {
        _accessToken.value = null
        _needsDriveConsent.value = true
        _backupsState.value = CloudBackupsUiState.NeedsDrivePermission
        _actionState.update { it.copy(errorMessage = message, statusMessage = null) }
    }

    fun clearSession() {
        _accessToken.value = null
        _needsDriveConsent.value = false
        driveRepository.clearSessionCache()
        _backupsState.value = CloudBackupsUiState.NeedsDrivePermission
        _actionState.value = CloudSyncActionState()
    }

    fun triggerSyncNow() {
        scope.launch {
            syncNowInternal(forceRefreshList = true)
        }
    }

    fun backupSinglePhotoNow(photo: CapturedPhotoEntity) {
        scope.launch {
            val detId = GoogleDriveBackupRepository.buildDeterministicBackupId(photo.capturedAtMillis, photo.displayName)
            accountStore.unmarkSkippedForAutoBackup(detId)
            syncMutex.withLock {
                val token = ensureTokenOrSilent() ?: return@withLock
                _actionState.update {
                    it.copy(
                        isSyncing = true,
                        progressLabel = "Uploading ${photo.displayName}...",
                        statusMessage = null,
                        errorMessage = null
                    )
                }
                val bytes = mediaGateway.readJpegBytesForDrive(photo).getOrElse { err ->
                    _actionState.update {
                        it.copy(isSyncing = false, progressLabel = "", errorMessage = err.localizedMessage)
                    }
                    return@withLock
                }
                driveRepository.uploadPhotoBackup(token, photo, bytes).fold(
                    onSuccess = { uploaded ->
                        val current = (_backupsState.value as? CloudBackupsUiState.Success)?.backups.orEmpty()
                        _backupsState.value = CloudBackupsUiState.Success(
                            (listOf(uploaded) + current).distinctBy { it.driveFileId }.sortedByDescending { it.capturedAtMillis }
                        )
                        _actionState.update {
                            it.copy(isSyncing = false, progressLabel = "", statusMessage = "Backed up ${photo.displayName} to Google Drive.")
                        }
                    },
                    onFailure = { err ->
                        handleDriveFailure(err, "Google Drive upload failed")
                    }
                )
            }
        }
    }

    fun restoreSingleBackupNow(backup: DrivePhotoBackup) {
        scope.launch {
            accountStore.unmarkSkippedForAutoRestore(backup.backupId)
            syncMutex.withLock {
                val token = ensureTokenOrSilent() ?: return@withLock
                _actionState.update {
                    it.copy(
                        isSyncing = true,
                        progressLabel = "Restoring ${backup.displayName}...",
                        statusMessage = null,
                        errorMessage = null
                    )
                }
                val bytes = driveRepository.downloadPhotoBytes(token, backup.driveFileId).getOrElse { err ->
                    handleDriveFailure(err, "Failed to download from Google Drive")
                    return@withLock
                }
                mediaGateway.restoreDrivePhotoToDevice(backup, bytes).fold(
                    onSuccess = { entity ->
                        photoRepository.insertPhoto(entity)
                        _actionState.update {
                            it.copy(isSyncing = false, progressLabel = "", statusMessage = "Restored ${backup.displayName} to gallery.")
                        }
                    },
                    onFailure = { err ->
                        _actionState.update {
                            it.copy(isSyncing = false, progressLabel = "", errorMessage = err.localizedMessage)
                        }
                    }
                )
            }
        }
    }

    fun deleteBackupNow(backup: DrivePhotoBackup) {
        scope.launch {
            syncMutex.withLock {
                val token = ensureTokenOrSilent() ?: return@withLock
                accountStore.markSkippedForAutoBackup(backup.backupId)
                driveRepository.deleteBackup(token, backup.driveFileId).fold(
                    onSuccess = {
                        val current = (_backupsState.value as? CloudBackupsUiState.Success)?.backups.orEmpty()
                        _backupsState.value = CloudBackupsUiState.Success(
                            current.filterNot { it.driveFileId == backup.driveFileId }
                        )
                        _actionState.update {
                            it.copy(statusMessage = "Removed backup from Google Drive.", errorMessage = null)
                        }
                    },
                    onFailure = { err ->
                        handleDriveFailure(err, "Failed to delete Google Drive backup")
                    }
                )
            }
        }
    }

    fun clearFeedback() {
        _actionState.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    private suspend fun syncNowInternal(forceRefreshList: Boolean) {
        syncMutex.withLock {
            if (!accountStore.accountFlow.first().isSignedIn) return@withLock
            if (!connectivityObserver.isOnline.value) return@withLock

            var token = ensureTokenOrSilent() ?: return@withLock

            if (forceRefreshList || _backupsState.value !is CloudBackupsUiState.Success) {
                if (_backupsState.value !is CloudBackupsUiState.Success) {
                    _backupsState.value = CloudBackupsUiState.Loading
                }
            }

            var listResult = driveRepository.listBackups(token)
            if (listResult.exceptionOrNull() is DriveUnauthorizedException) {
                // Attempt automatic silent token refresh once on 401/403
                val refreshed = GoogleDriveAuthHelper.tryGetDriveAccessTokenSilently(appContext)
                if (!refreshed.isNullOrBlank()) {
                    _accessToken.value = refreshed
                    token = refreshed
                    listResult = driveRepository.listBackups(token)
                }
            }

            val driveBackups = listResult.getOrElse { err ->
                if (err is DriveUnauthorizedException) {
                    _accessToken.value = null
                    _needsDriveConsent.value = true
                    _backupsState.value = CloudBackupsUiState.NeedsDrivePermission
                } else {
                    _backupsState.value = CloudBackupsUiState.Error(
                        err.localizedMessage ?: "Failed to sync with Google Drive"
                    )
                }
                return@withLock
            }.toMutableList()

            _backupsState.value = CloudBackupsUiState.Success(driveBackups.toList())

            val localPhotos = photoRepository.allPhotos.first()
            val skipBackupIds = accountStore.skipAutoBackupIdsFlow.first()
            val skipRestoreIds = accountStore.skipAutoRestoreIdsFlow.first()

            val existingDriveBackupIds = driveBackups.map { it.backupId }.toMutableSet()
            val existingLocalBackupIds = localPhotos.map {
                GoogleDriveBackupRepository.buildDeterministicBackupId(it.capturedAtMillis, it.displayName)
            }.toMutableSet()

            val pendingUploads = localPhotos.filter { photo ->
                val detId = GoogleDriveBackupRepository.buildDeterministicBackupId(photo.capturedAtMillis, photo.displayName)
                detId !in existingDriveBackupIds && detId !in skipBackupIds
            }

            val pendingRestores = driveBackups.filter { backup ->
                backup.backupId !in existingLocalBackupIds && backup.backupId !in skipRestoreIds
            }

            if (pendingUploads.isEmpty() && pendingRestores.isEmpty()) {
                _actionState.update {
                    it.copy(
                        isSyncing = false,
                        progressLabel = "",
                        statusMessage = if (driveBackups.isNotEmpty() || localPhotos.isNotEmpty()) {
                            "Auto-sync up to date (${driveBackups.size} in Drive)"
                        } else null,
                        errorMessage = null
                    )
                }
                return@withLock
            }

            var uploadedCount = 0
            var restoredCount = 0
            var failedCount = 0
            var authExpired = false

            if (pendingUploads.isNotEmpty()) {
                _actionState.update {
                    it.copy(
                        isSyncing = true,
                        progressLabel = "Auto-backing up 0/${pendingUploads.size}...",
                        statusMessage = null,
                        errorMessage = null
                    )
                }
                for ((index, photo) in pendingUploads.withIndex()) {
                    if (!connectivityObserver.isOnline.value) break
                    _actionState.update {
                        it.copy(progressLabel = "Auto-backing up ${index + 1}/${pendingUploads.size}: ${photo.displayName}")
                    }
                    val bytes = mediaGateway.readJpegBytesForDrive(photo).getOrNull()
                    if (bytes == null) {
                        failedCount++
                        continue
                    }
                    driveRepository.uploadPhotoBackup(token, photo, bytes).fold(
                        onSuccess = { item ->
                            uploadedCount++
                            existingDriveBackupIds.add(item.backupId)
                            driveBackups.add(0, item)
                            _backupsState.value = CloudBackupsUiState.Success(
                                driveBackups.distinctBy { it.driveFileId }.sortedByDescending { it.capturedAtMillis }
                            )
                        },
                        onFailure = { err ->
                            if (err is DriveUnauthorizedException) authExpired = true else failedCount++
                        }
                    )
                    if (authExpired) break
                }
            }

            if (!authExpired && pendingRestores.isNotEmpty()) {
                for ((index, backup) in pendingRestores.withIndex()) {
                    if (!connectivityObserver.isOnline.value) break
                    _actionState.update {
                        it.copy(
                            isSyncing = true,
                            progressLabel = "Auto-restoring ${index + 1}/${pendingRestores.size}: ${backup.displayName}"
                        )
                    }
                    val dlRes = driveRepository.downloadPhotoBytes(token, backup.driveFileId)
                    val bytes = dlRes.getOrElse { err ->
                        if (err is DriveUnauthorizedException) authExpired = true else failedCount++
                        null
                    }
                    if (authExpired) break
                    if (bytes != null) {
                        mediaGateway.restoreDrivePhotoToDevice(backup, bytes).fold(
                            onSuccess = { entity ->
                                photoRepository.insertPhoto(entity)
                                existingLocalBackupIds.add(backup.backupId)
                                restoredCount++
                            },
                            onFailure = { failedCount++ }
                        )
                    }
                }
            }

            if (authExpired) {
                _accessToken.value = null
                _needsDriveConsent.value = true
                _backupsState.value = CloudBackupsUiState.NeedsDrivePermission
                _actionState.update {
                    it.copy(
                        isSyncing = false,
                        progressLabel = "",
                        errorMessage = "Google Drive authorization expired. Grant access once to resume auto-sync."
                    )
                }
            } else {
                val summaryParts = buildList {
                    if (uploadedCount > 0) add("Auto-backed up $uploadedCount")
                    if (restoredCount > 0) add("Auto-restored $restoredCount")
                }
                _actionState.update {
                    it.copy(
                        isSyncing = false,
                        progressLabel = "",
                        statusMessage = if (summaryParts.isNotEmpty()) {
                            "${summaryParts.joinToString(" • ")} photo(s)"
                        } else it.statusMessage,
                        errorMessage = if (failedCount > 0) "$failedCount item(s) will retry when network stabilizes." else null
                    )
                }
            }
        }
    }

    private suspend fun ensureTokenOrSilent(): String? {
        _accessToken.value?.takeIf { it.isNotBlank() }?.let { return it }
        val silent = try {
            GoogleDriveAuthHelper.tryGetDriveAccessTokenSilently(appContext)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        return if (!silent.isNullOrBlank()) {
            _accessToken.value = silent
            _needsDriveConsent.value = false
            silent
        } else {
            _needsDriveConsent.value = true
            _backupsState.value = CloudBackupsUiState.NeedsDrivePermission
            null
        }
    }

    private fun handleDriveFailure(err: Throwable, fallbackMsg: String) {
        if (err is DriveUnauthorizedException) {
            _accessToken.value = null
            _needsDriveConsent.value = true
            _backupsState.value = CloudBackupsUiState.NeedsDrivePermission
        }
        _actionState.update {
            it.copy(
                isSyncing = false,
                progressLabel = "",
                errorMessage = err.localizedMessage ?: fallbackMsg
            )
        }
    }
}
