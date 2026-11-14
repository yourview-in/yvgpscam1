package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.StampPreferencesRepository
import com.example.data.model.OfflineRegionEntity
import com.example.map.DownloadProgress
import com.example.map.OfflineMapRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OfflineMapsUiState(
    val diskCacheBytes: Long = 0L,
    val diskTileCount: Int = 0,
    val downloadProgress: DownloadProgress = DownloadProgress(),
    val feedbackMessage: String? = null
)

class OfflineMapsViewModel(
    private val offlineMapRepository: OfflineMapRepository,
    private val stampPrefs: StampPreferencesRepository
) : ViewModel() {

    val savedRegions: StateFlow<List<OfflineRegionEntity>> = offlineMapRepository.savedRegions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(OfflineMapsUiState())
    val uiState: StateFlow<OfflineMapsUiState> = _uiState.asStateFlow()

    init {
        refreshStorageStats()
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            val bytes = offlineMapRepository.getTotalDiskCacheBytes()
            val tiles = offlineMapRepository.getTotalCachedTileCount()
            _uiState.update { it.copy(diskCacheBytes = bytes, diskTileCount = tiles) }
        }
    }

    fun estimateTileCount(lat: Double, lon: Double, radiusKm: Double, zoomMin: Int, zoomMax: Int): Int {
        return offlineMapRepository.estimateTilesForRegion(lat, lon, radiusKm, zoomMin, zoomMax).size
    }

    fun downloadRegion(
        regionName: String,
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double,
        zoomMin: Int = 14,
        zoomMax: Int = 16
    ) {
        if (_uiState.value.downloadProgress.isDownloading) return
        viewModelScope.launch {
            val result = offlineMapRepository.downloadRegion(
                regionName = regionName,
                centerLat = centerLat,
                centerLon = centerLon,
                radiusKm = radiusKm,
                zoomMin = zoomMin,
                zoomMax = zoomMax,
                onProgress = { progress ->
                    _uiState.update { it.copy(downloadProgress = progress) }
                }
            )
            result.onSuccess { region ->
                _uiState.update {
                    it.copy(
                        downloadProgress = DownloadProgress(isDownloading = false),
                        feedbackMessage = "Downloaded ${region.tileCount} tiles for ${region.name}"
                    )
                }
                refreshStorageStats()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        downloadProgress = DownloadProgress(isDownloading = false, errorMessage = err.message),
                        feedbackMessage = err.message ?: "Offline download failed"
                    )
                }
            }
        }
    }

    fun deleteRegion(region: OfflineRegionEntity) {
        viewModelScope.launch {
            offlineMapRepository.deleteRegion(region)
            refreshStorageStats()
            _uiState.update { it.copy(feedbackMessage = "Removed offline region ${region.name}") }
        }
    }

    fun setOfflineOnlyMode(enabled: Boolean) {
        viewModelScope.launch {
            stampPrefs.updateConfig { it.copy(offlineOnlyMapMode = enabled) }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    class Factory(
        private val offlineMapRepository: OfflineMapRepository,
        private val stampPrefs: StampPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OfflineMapsViewModel(offlineMapRepository, stampPrefs) as T
        }
    }
}
