package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.DrivePhotoBackup
import com.example.data.cloud.GoogleDriveAutoSyncCoordinator
import com.example.data.local.PhotoRepository
import com.example.data.model.CapturedPhotoEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

sealed interface CloudBackupsUiState {
    data object NeedsDrivePermission : CloudBackupsUiState
    data object Loading : CloudBackupsUiState
    data class Success(val backups: List<DrivePhotoBackup>) : CloudBackupsUiState
    data class Error(val message: String) : CloudBackupsUiState
}

data class CloudSyncActionState(
    val isSyncing: Boolean = false,
    val progressLabel: String = "",
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class CloudSyncViewModel(
    private val autoSyncCoordinator: GoogleDriveAutoSyncCoordinator,
    photoRepository: PhotoRepository
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = autoSyncCoordinator.isOnline
    val needsDriveConsent: StateFlow<Boolean> = autoSyncCoordinator.needsDriveConsent
    val accessToken: StateFlow<String?> = autoSyncCoordinator.accessToken
    val cloudBackupsState: StateFlow<CloudBackupsUiState> = autoSyncCoordinator.backupsState
    val actionState: StateFlow<CloudSyncActionState> = autoSyncCoordinator.actionState

    val localPhotos: StateFlow<List<CapturedPhotoEntity>> = photoRepository.allPhotos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    fun onAccessTokenGranted(token: String) {
        autoSyncCoordinator.onAccessTokenGranted(token)
    }

    fun onDriveAuthorizationError(message: String) {
        autoSyncCoordinator.onDriveAuthorizationError(message)
    }

    fun clearSession() {
        autoSyncCoordinator.clearSession()
    }

    fun refreshBackups() {
        autoSyncCoordinator.triggerSyncNow()
    }

    fun backupAllMissing() {
        autoSyncCoordinator.triggerSyncNow()
    }

    fun backupSinglePhoto(photo: CapturedPhotoEntity) {
        autoSyncCoordinator.backupSinglePhotoNow(photo)
    }

    fun restoreBackup(backup: DrivePhotoBackup) {
        autoSyncCoordinator.restoreSingleBackupNow(backup)
    }

    fun restoreAllMissing() {
        autoSyncCoordinator.triggerSyncNow()
    }

    fun deleteBackup(backup: DrivePhotoBackup) {
        autoSyncCoordinator.deleteBackupNow(backup)
    }

    fun clearFeedback() {
        autoSyncCoordinator.clearFeedback()
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
