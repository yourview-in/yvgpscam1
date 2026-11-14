package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.camera.ExifMetadataDetails
import com.example.camera.ExifMetadataReader
import com.example.data.cloud.GoogleDriveAccountStore
import com.example.data.cloud.GoogleDriveBackupRepository
import com.example.data.local.PhotoRepository
import com.example.data.local.StampPreferencesRepository
import com.example.data.model.CapturedPhotoEntity
import com.example.data.model.MapStyle
import com.example.data.model.StampConfig
import com.example.data.model.StampTemplate
import com.example.map.MiniMapTileRenderer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class GalleryViewMode {
    LIST,
    GRID,
    MAP
}

enum class DateFilterPreset(val label: String) {
    ALL("All Dates"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    SPECIFIC_DATE("Single Date"),
    CUSTOM_RANGE("Date Range")
}

data class GalleryDateFilter(
    val preset: DateFilterPreset = DateFilterPreset.ALL,
    val startMillis: Long? = null,
    val endMillis: Long? = null,
    val displayLabel: String = "All Dates"
)

class GalleryViewModel(
    private val appContext: Context,
    private val photoRepository: PhotoRepository,
    private val miniMapRenderer: MiniMapTileRenderer,
    private val stampPrefs: StampPreferencesRepository,
    private val driveAccountStore: GoogleDriveAccountStore? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _viewMode = MutableStateFlow(GalleryViewMode.GRID)
    val viewMode: StateFlow<GalleryViewMode> = _viewMode.asStateFlow()

    private val _dateFilter = MutableStateFlow(GalleryDateFilter())
    val dateFilter: StateFlow<GalleryDateFilter> = _dateFilter.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPhotoIds: StateFlow<Set<Long>> = _selectedPhotoIds.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchResults: Flow<List<CapturedPhotoEntity>> = _searchQuery
        .flatMapLatest { query -> photoRepository.searchPhotos(query) }

    val photos: StateFlow<List<CapturedPhotoEntity>> = combine(
        searchResults,
        _dateFilter
    ) { list, filter ->
        val start = filter.startMillis
        val end = filter.endMillis
        if (start == null && end == null) {
            list
        } else {
            list.filter { photo ->
                val afterStart = start == null || photo.capturedAtMillis >= start
                val beforeEnd = end == null || photo.capturedAtMillis <= end
                afterStart && beforeEnd
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stampConfig: StateFlow<StampConfig> = stampPrefs.stampConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StampConfig())

    private val _mapThumbnailCache = MutableStateFlow<Map<Long, Bitmap>>(emptyMap())
    val mapThumbnailCache: StateFlow<Map<Long, Bitmap>> = _mapThumbnailCache.asStateFlow()

    private val _renameStatusMessage = MutableStateFlow<String?>(null)
    val renameStatusMessage: StateFlow<String?> = _renameStatusMessage.asStateFlow()

    private val loadingIds = mutableSetOf<Long>()
    private var lastLoadedStyle: MapStyle? = null

    init {
        viewModelScope.launch {
            stampPrefs.stampConfig
                .map { it.mapStyle }
                .distinctUntilChanged()
                .collectLatest { style ->
                    if (lastLoadedStyle != null && lastLoadedStyle != style) {
                        loadingIds.clear()
                        _mapThumbnailCache.value = emptyMap()
                    }
                    lastLoadedStyle = style
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMapStyle(style: MapStyle) {
        viewModelScope.launch {
            stampPrefs.updateConfig { it.copy(mapStyle = style) }
        }
    }

    fun clearRenameStatus() {
        _renameStatusMessage.value = null
    }

    fun previewRenamedFilename(
        photo: CapturedPhotoEntity,
        template: String,
        indexOneBased: Int = 1
    ): String = PhotoRepository.formatTemplateFilename(photo, template, indexOneBased)

    fun renameSelectedPhotos(template: String) {
        val selected = photos.value.filter { _selectedPhotoIds.value.contains(it.id) }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            photoRepository.renamePhotosWithTemplate(selected, template)
                .onSuccess { count ->
                    _renameStatusMessage.value = "Renamed $count ${if (count == 1) "photo" else "photos"}"
                    clearSelection()
                }
                .onFailure { err ->
                    _renameStatusMessage.value = err.message ?: "Rename failed"
                }
        }
    }

    fun setViewMode(mode: GalleryViewMode) {
        _viewMode.value = mode
    }

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            GalleryViewMode.GRID -> GalleryViewMode.LIST
            GalleryViewMode.LIST -> GalleryViewMode.GRID
            GalleryViewMode.MAP -> GalleryViewMode.GRID
        }
    }

    fun enterSelectionMode(initialPhotoId: Long? = null) {
        _isSelectionMode.value = true
        if (initialPhotoId != null) {
            _selectedPhotoIds.update { it + initialPhotoId }
        }
    }

    fun togglePhotoSelection(photoId: Long) {
        _isSelectionMode.value = true
        _selectedPhotoIds.update { current ->
            if (current.contains(photoId)) current - photoId else current + photoId
        }
    }

    fun selectAllVisiblePhotos() {
        _isSelectionMode.value = true
        val visibleIds = photos.value.map { it.id }.toSet()
        _selectedPhotoIds.value = if (_selectedPhotoIds.value.size == visibleIds.size && visibleIds.isNotEmpty()) {
            emptySet()
        } else {
            visibleIds
        }
    }

    fun clearSelection() {
        _isSelectionMode.value = false
        _selectedPhotoIds.value = emptySet()
    }

    fun clearDateFilter() {
        _dateFilter.value = GalleryDateFilter()
    }

    fun applyTodayFilter() {
        val cal = Calendar.getInstance()
        val start = startOfLocalDay(cal.timeInMillis)
        val end = endOfLocalDay(cal.timeInMillis)
        _dateFilter.value = GalleryDateFilter(
            preset = DateFilterPreset.TODAY,
            startMillis = start,
            endMillis = end,
            displayLabel = "Today"
        )
    }

    fun applyLast7DaysFilter() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -6)
        }
        val start = startOfLocalDay(cal.timeInMillis)
        val end = endOfLocalDay(now)
        _dateFilter.value = GalleryDateFilter(
            preset = DateFilterPreset.LAST_7_DAYS,
            startMillis = start,
            endMillis = end,
            displayLabel = "Last 7 Days"
        )
    }

    fun applySpecificUtcDate(utcMidnightMillis: Long) {
        val localDayMillis = utcMidnightToLocalMidnight(utcMidnightMillis)
        val start = startOfLocalDay(localDayMillis)
        val end = endOfLocalDay(localDayMillis)
        val label = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(start))
        _dateFilter.value = GalleryDateFilter(
            preset = DateFilterPreset.SPECIFIC_DATE,
            startMillis = start,
            endMillis = end,
            displayLabel = label
        )
    }

    fun applyCustomUtcDateRange(startUtcMillis: Long, endUtcMillis: Long) {
        val localStart = startOfLocalDay(utcMidnightToLocalMidnight(minOf(startUtcMillis, endUtcMillis)))
        val localEnd = endOfLocalDay(utcMidnightToLocalMidnight(maxOf(startUtcMillis, endUtcMillis)))
        val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
        val label = "${fmt.format(Date(localStart))} – ${fmt.format(Date(localEnd))}"
        _dateFilter.value = GalleryDateFilter(
            preset = DateFilterPreset.CUSTOM_RANGE,
            startMillis = localStart,
            endMillis = localEnd,
            displayLabel = label
        )
    }

    fun getPhotoFlow(id: Long): Flow<CapturedPhotoEntity?> = photoRepository.getPhotoById(id)

    fun ensureMapThumbnailLoaded(
        photo: CapturedPhotoEntity,
        offlineOnly: Boolean = false,
        mapStyle: MapStyle = stampConfig.value.mapStyle
    ) {
        if (_mapThumbnailCache.value.containsKey(photo.id) || loadingIds.contains(photo.id)) return
        loadingIds.add(photo.id)
        val cfg = stampConfig.value
        viewModelScope.launch {
            val bmp = miniMapRenderer.renderMiniMapBitmap(
                latitude = photo.latitude,
                longitude = photo.longitude,
                zoom = 15,
                outputSizePx = 220,
                mapStyle = mapStyle,
                offlineOnly = offlineOnly,
                accentColorInt = cfg.accentColor.argbInt,
                isLightCard = cfg.template == StampTemplate.CLASSIC_WHITE
            )
            if (stampConfig.value.mapStyle == mapStyle) {
                _mapThumbnailCache.value = _mapThumbnailCache.value + (photo.id to bmp)
            }
            loadingIds.remove(photo.id)
        }
    }

    suspend fun loadExifDetails(uriString: String): ExifMetadataDetails {
        return ExifMetadataReader.readFromUri(appContext, uriString)
    }

    fun deletePhoto(photo: CapturedPhotoEntity, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            val detId = GoogleDriveBackupRepository.buildDeterministicBackupId(
                photo.capturedAtMillis,
                photo.displayName
            )
            driveAccountStore?.markSkippedForAutoRestore(detId)
            photoRepository.deletePhoto(photo)
            onDeleted()
        }
    }

    fun deleteSelectedPhotos(onDeleted: (Set<Long>) -> Unit = {}) {
        val selectedIds = _selectedPhotoIds.value
        val selected = photos.value.filter { selectedIds.contains(it.id) }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val detIds = selected.map { photo ->
                GoogleDriveBackupRepository.buildDeterministicBackupId(
                    photo.capturedAtMillis,
                    photo.displayName
                )
            }
            driveAccountStore?.markSkippedForAutoRestore(detIds)
            photoRepository.deletePhotos(selected)
                .onSuccess { count ->
                    clearSelection()
                    _renameStatusMessage.value = "Deleted $count ${if (count == 1) "photo" else "photos"}"
                    onDeleted(selected.map { it.id }.toSet())
                }
                .onFailure { err ->
                    _renameStatusMessage.value = err.message ?: "Delete failed"
                }
        }
    }

    private fun utcMidnightToLocalMidnight(utcMillis: Long): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val localCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return localCal.timeInMillis
    }

    private fun startOfLocalDay(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun endOfLocalDay(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    class Factory(
        private val appContext: Context,
        private val photoRepository: PhotoRepository,
        private val miniMapRenderer: MiniMapTileRenderer,
        private val stampPrefs: StampPreferencesRepository,
        private val driveAccountStore: GoogleDriveAccountStore? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GalleryViewModel(appContext, photoRepository, miniMapRenderer, stampPrefs, driveAccountStore) as T
        }
    }
}
