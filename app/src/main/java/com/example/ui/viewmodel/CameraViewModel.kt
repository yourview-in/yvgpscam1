package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.camera.MediaStoreSaver
import com.example.data.local.PhotoRepository
import com.example.data.local.StampPreferencesRepository
import com.example.data.model.CapturedPhotoEntity
import com.example.data.model.GpsSignalState
import com.example.data.model.GpsTelemetry
import com.example.data.model.ManualLocationItem
import com.example.data.model.MapStyle
import com.example.data.model.StampConfig
import com.example.data.model.StampLayoutTemplate
import com.example.data.model.StampPosition
import com.example.data.model.StampTemplate
import com.example.data.model.VolumeButtonAction
import com.example.map.MiniMapTileRenderer
import com.example.sensors.LocationTelemetryManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs

enum class CameraAspectRatio(val label: String, val ratioValue: Float) {
    RATIO_4_3("3:4", 3f / 4f),
    RATIO_16_9("16:9", 9f / 16f),
    RATIO_1_1("1:1", 1f)
}

enum class CameraCaptureMode(val label: String) {
    SHARE_PHOTO("SHARE PHOTO"),
    PHOTO("PHOTO"),
    VIDEO("VIDEO"),
    REPORTS("REPORTS")
}

data class CameraHudState(
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val aspectRatio: CameraAspectRatio = CameraAspectRatio.RATIO_4_3,
    val captureMode: CameraCaptureMode = CameraCaptureMode.PHOTO,
    val showGrid: Boolean = true,
    val showToolsDrawer: Boolean = false,
    val isRecordingVideo: Boolean = false,
    val recordingElapsedSeconds: Int = 0,
    val countdownRemaining: Int = 0,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 6f,
    val isCapturing: Boolean = false,
    val isFullScreenCamera: Boolean = false,
    val detectedQrText: String? = null,
    val statusMessage: String? = null
)

class CameraViewModel(
    private val locationManager: LocationTelemetryManager,
    private val stampPrefs: StampPreferencesRepository,
    private val photoRepository: PhotoRepository,
    private val miniMapRenderer: MiniMapTileRenderer,
    private val mediaStoreSaver: MediaStoreSaver
) : ViewModel() {

    val liveGpsTelemetry: StateFlow<GpsTelemetry> = locationManager.telemetry

    val horizonTiltDegrees: StateFlow<Pair<Float, Float>> = locationManager.horizonTiltDegrees

    val stampConfig: StateFlow<StampConfig> = stampPrefs.stampConfig.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StampConfig()
    )

    val telemetry: StateFlow<GpsTelemetry> = combine(
        locationManager.telemetry,
        stampConfig
    ) { live, cfg ->
        val manual = cfg.selectedManualLocation()
        if (manual != null) {
            live.copy(
                latitude = manual.latitude,
                longitude = manual.longitude,
                addressLine = manual.fullAddress,
                cityState = manual.shortName,
                country = "",
                signalState = GpsSignalState.LOCKED,
                lastUpdatedMillis = System.currentTimeMillis()
            )
        } else {
            live
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = locationManager.telemetry.value
    )

    val latestPhoto: StateFlow<CapturedPhotoEntity?> = photoRepository.allPhotos
        .combine(MutableStateFlow(Unit)) { list, _ -> list.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _hudState = MutableStateFlow(CameraHudState())
    val hudState: StateFlow<CameraHudState> = _hudState.asStateFlow()

    private val _miniMapBitmap = MutableStateFlow<Bitmap?>(null)
    val miniMapBitmap: StateFlow<Bitmap?> = _miniMapBitmap.asStateFlow()

    private val _volumeEvents = MutableSharedFlow<VolumeButtonAction>(extraBufferCapacity = 4)
    val volumeEvents: SharedFlow<VolumeButtonAction> = _volumeEvents.asSharedFlow()

    private val _postCaptureEvent = MutableSharedFlow<CapturedPhotoEntity>(extraBufferCapacity = 4)
    val postCaptureEvent: SharedFlow<CapturedPhotoEntity> = _postCaptureEvent.asSharedFlow()

    private var lastRenderedLat: Double? = null
    private var lastRenderedLon: Double? = null
    private var lastRenderedStyleKey: String? = null

    init {
        viewModelScope.launch {
            combine(telemetry, stampConfig) { tel, cfg -> tel to cfg }
                .collectLatest { (tel, cfg) ->
                    val styleKey = "${cfg.mapStyle}_${cfg.offlineOnlyMapMode}_${cfg.accentColor}_${cfg.template}_${cfg.layoutTemplate}"
                    val latChanged = tel.latitude != null && lastRenderedLat?.let { abs(it - tel.latitude) > 0.00025 } != false
                    val lonChanged = tel.longitude != null && lastRenderedLon?.let { abs(it - tel.longitude) > 0.00025 } != false
                    if (_miniMapBitmap.value == null || latChanged || lonChanged || styleKey != lastRenderedStyleKey) {
                        lastRenderedLat = tel.latitude
                        lastRenderedLon = tel.longitude
                        lastRenderedStyleKey = styleKey
                        val bmp = miniMapRenderer.renderMiniMapBitmap(
                            latitude = tel.latitude,
                            longitude = tel.longitude,
                            zoom = 15,
                            outputSizePx = 280,
                            mapStyle = cfg.mapStyle,
                            offlineOnly = cfg.offlineOnlyMapMode,
                            accentColorInt = cfg.accentColor.argbInt,
                            isLightCard = cfg.template == StampTemplate.CLASSIC_WHITE ||
                                cfg.layoutTemplate == StampLayoutTemplate.CLASSIC
                        )
                        _miniMapBitmap.value = bmp
                    }
                }
        }
    }

    fun onPermissionsUpdated(hasLocationPermission: Boolean) {
        locationManager.startLocationUpdates(hasLocationPermission)
        locationManager.startCompass()
    }

    fun onLifecyclePause() {
        locationManager.stopCompass()
    }

    fun toggleLensFacing() {
        _hudState.update {
            it.copy(
                lensFacing = if (it.lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                },
                zoomRatio = 1f
            )
        }
    }

    fun cycleFlashMode() {
        _hudState.update {
            val next = when (it.flashMode) {
                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                else -> ImageCapture.FLASH_MODE_OFF
            }
            it.copy(flashMode = next)
        }
    }

    fun cycleAspectRatio() {
        _hudState.update {
            val values = CameraAspectRatio.entries
            val next = values[(values.indexOf(it.aspectRatio) + 1) % values.size]
            it.copy(aspectRatio = next)
        }
    }

    fun setCaptureMode(mode: CameraCaptureMode) {
        _hudState.update { it.copy(captureMode = mode) }
    }

    fun toggleGrid() {
        _hudState.update { it.copy(showGrid = !it.showGrid) }
    }

    fun toggleToolsDrawer() {
        _hudState.update { it.copy(showToolsDrawer = !it.showToolsDrawer) }
    }

    fun closeToolsDrawer() {
        _hudState.update { it.copy(showToolsDrawer = false) }
    }

    fun setCountdownRemaining(seconds: Int) {
        _hudState.update { it.copy(countdownRemaining = seconds.coerceAtLeast(0)) }
    }

    fun setRecordingState(isRecording: Boolean, elapsedSeconds: Int = 0) {
        _hudState.update {
            it.copy(
                isRecordingVideo = isRecording,
                recordingElapsedSeconds = elapsedSeconds
            )
        }
    }

    fun updateDetectedQrCode(text: String?) {
        _hudState.update { it.copy(detectedQrText = text) }
    }

    fun toggleFullScreenCamera() {
        _hudState.update { it.copy(isFullScreenCamera = !it.isFullScreenCamera) }
    }

    fun setFullScreenCamera(enabled: Boolean) {
        _hudState.update { it.copy(isFullScreenCamera = enabled) }
    }

    fun setZoomRatio(ratio: Float) {
        _hudState.update { it.copy(zoomRatio = ratio.coerceIn(it.minZoomRatio, it.maxZoomRatio)) }
    }

    fun adjustZoomByDelta(delta: Float) {
        val current = _hudState.value.zoomRatio
        setZoomRatio(current + delta)
    }

    fun onHardwareVolumePressed(isUp: Boolean): Boolean {
        val action = stampConfig.value.volumeButtonAction
        if (action == VolumeButtonAction.OFF) return false
        if (action == VolumeButtonAction.ZOOM) {
            adjustZoomByDelta(if (isUp) 0.5f else -0.5f)
        } else {
            _volumeEvents.tryEmit(action)
        }
        return true
    }

    fun updateZoomBounds(minZoom: Float, maxZoom: Float) {
        val safeMin = minZoom.coerceIn(0.4f, 1f)
        val safeMax = maxZoom.coerceIn(1f, 10f)
        _hudState.update {
            it.copy(
                minZoomRatio = safeMin,
                maxZoomRatio = safeMax,
                zoomRatio = it.zoomRatio.coerceIn(safeMin, safeMax)
            )
        }
    }

    fun updateMaxZoom(maxZoom: Float) {
        updateZoomBounds(_hudState.value.minZoomRatio, maxZoom)
    }

    fun cycleStampTemplate() {
        viewModelScope.launch {
            stampPrefs.updateConfig { cfg ->
                val entries = StampLayoutTemplate.entries
                val next = entries[(entries.indexOf(cfg.layoutTemplate) + 1) % entries.size]
                val style = if (next == StampLayoutTemplate.CLASSIC) StampTemplate.CLASSIC_WHITE else StampTemplate.TACTICAL_DARK
                cfg.copy(layoutTemplate = next, template = style)
            }
        }
    }

    fun selectLayoutTemplate(layout: StampLayoutTemplate) {
        viewModelScope.launch {
            stampPrefs.updateConfig { cfg ->
                val nextTemplate = if (layout == StampLayoutTemplate.CLASSIC) {
                    StampTemplate.CLASSIC_WHITE
                } else {
                    cfg.template
                }
                cfg.copy(layoutTemplate = layout, template = nextTemplate)
            }
        }
    }

    fun cycleStampPosition() {
        viewModelScope.launch {
            stampPrefs.updateConfig { cfg ->
                val entries = StampPosition.entries
                val next = entries[(entries.indexOf(cfg.stampPosition) + 1) % entries.size]
                cfg.copy(stampPosition = next)
            }
        }
    }

    fun cycleMapStyle() {
        viewModelScope.launch {
            stampPrefs.updateConfig { cfg ->
                val entries = MapStyle.entries
                val next = entries[(entries.indexOf(cfg.mapStyle) + 1) % entries.size]
                cfg.copy(mapStyle = next)
            }
        }
    }

    fun cycleTimerDelay() {
        viewModelScope.launch {
            stampPrefs.updateConfig { cfg ->
                val next = when (cfg.timerDelaySeconds) {
                    0 -> 3
                    3 -> 10
                    else -> 0
                }
                cfg.copy(timerDelaySeconds = next)
            }
        }
    }

    fun updateStampConfig(transform: (StampConfig) -> StampConfig) {
        viewModelScope.launch {
            stampPrefs.updateConfig(transform)
        }
    }

    // Manual Location Actions
    fun selectCurrentGpsLocation() {
        updateStampConfig {
            it.copy(useManualLocation = false, selectedManualLocationId = null)
        }
    }

    fun selectManualLocation(id: String) {
        updateStampConfig {
            it.copy(useManualLocation = true, selectedManualLocationId = id)
        }
    }

    fun addManualLocation(
        shortName: String,
        fullAddress: String,
        latitude: Double,
        longitude: Double,
        activateImmediately: Boolean = true
    ) {
        val newItem = ManualLocationItem(
            id = UUID.randomUUID().toString(),
            shortName = shortName.trim(),
            fullAddress = fullAddress.trim(),
            latitude = latitude,
            longitude = longitude
        )
        updateStampConfig { cfg ->
            val updated = listOf(newItem) + cfg.parsedManualLocations()
            cfg.copy(
                manualLocationsJson = StampConfig.serializeManualLocations(updated),
                useManualLocation = if (activateImmediately) true else cfg.useManualLocation,
                selectedManualLocationId = if (activateImmediately) newItem.id else cfg.selectedManualLocationId
            )
        }
    }

    fun deleteManualLocation(id: String) {
        updateStampConfig { cfg ->
            val updated = cfg.parsedManualLocations().filterNot { it.id == id }
            val wasSelected = cfg.selectedManualLocationId == id
            cfg.copy(
                manualLocationsJson = StampConfig.serializeManualLocations(updated),
                useManualLocation = if (wasSelected) false else cfg.useManualLocation,
                selectedManualLocationId = if (wasSelected) null else cfg.selectedManualLocationId
            )
        }
    }

    private suspend fun obtainCaptureMiniMapBitmap(
        telemetry: GpsTelemetry,
        config: StampConfig
    ): Bitmap {
        val styleKey = "${config.mapStyle}_${config.offlineOnlyMapMode}_${config.accentColor}_${config.template}_${config.layoutTemplate}"
        val latSame = (telemetry.latitude == null && lastRenderedLat == null) ||
            (telemetry.latitude != null && lastRenderedLat != null && abs(lastRenderedLat!! - telemetry.latitude) <= 0.00025)
        val lonSame = (telemetry.longitude == null && lastRenderedLon == null) ||
            (telemetry.longitude != null && lastRenderedLon != null && abs(lastRenderedLon!! - telemetry.longitude) <= 0.00025)
        val cached = _miniMapBitmap.value
        if (cached != null && !cached.isRecycled && latSame && lonSame && styleKey == lastRenderedStyleKey) {
            return cached
        }
        return miniMapRenderer.renderMiniMapBitmap(
            latitude = telemetry.latitude,
            longitude = telemetry.longitude,
            zoom = 15,
            outputSizePx = 280,
            mapStyle = config.mapStyle,
            offlineOnly = config.offlineOnlyMapMode,
            accentColorInt = config.accentColor.argbInt,
            isLightCard = config.template == StampTemplate.CLASSIC_WHITE ||
                config.layoutTemplate == StampLayoutTemplate.CLASSIC
        )
    }

    fun saveCapturedPhoto(
        jpegBytes: ByteArray,
        isFrontCamera: Boolean,
        deviceOrientationDegrees: Int = 0,
        triggerShareImmediately: Boolean = false,
        onSaved: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            _hudState.update { it.copy(isCapturing = true) }
            val currentTel = telemetry.value
            val currentCfg = stampConfig.value
            val mapBmp = obtainCaptureMiniMapBitmap(currentTel, currentCfg)

            val result = mediaStoreSaver.saveStampedJpegBytes(
                jpegBytes = jpegBytes,
                telemetry = currentTel,
                config = currentCfg,
                miniMapBitmap = mapBmp,
                mirrorHorizontal = isFrontCamera && currentCfg.mirrorFrontCamera,
                isFrontCamera = isFrontCamera,
                deviceOrientationDegrees = deviceOrientationDegrees
            )

            result.onSuccess { entity ->
                val insertedId = photoRepository.insertPhoto(entity)
                val savedEntity = entity.copy(id = insertedId)
                if (currentCfg.showNumbering || (currentCfg.customFileNameEnabled && currentCfg.fnIncludeSequence)) {
                    stampPrefs.incrementSequenceNumber()
                }
                _hudState.update {
                    it.copy(
                        isCapturing = false,
                        statusMessage = "Saved ${entity.displayName}"
                    )
                }
                if (triggerShareImmediately || currentCfg.autoShareAfterCapture || currentCfg.emailAfterCapture) {
                    _postCaptureEvent.tryEmit(savedEntity)
                }
                onSaved(insertedId)
            }.onFailure { err ->
                _hudState.update {
                    it.copy(
                        isCapturing = false,
                        statusMessage = err.message ?: "Failed to save photo"
                    )
                }
            }
        }
    }

    fun saveRecordedVideo(videoUri: Uri, width: Int = 1080, height: Int = 1920) {
        viewModelScope.launch {
            val currentTel = telemetry.value
            val currentCfg = stampConfig.value
            val entity = mediaStoreSaver.buildRecordedVideoEntity(
                videoUri = videoUri,
                telemetry = currentTel,
                config = currentCfg,
                width = width,
                height = height
            )
            photoRepository.insertPhoto(entity)
            if (currentCfg.showNumbering || (currentCfg.customFileNameEnabled && currentCfg.fnIncludeSequence)) {
                stampPrefs.incrementSequenceNumber()
            }
            _hudState.update {
                it.copy(
                    isRecordingVideo = false,
                    recordingElapsedSeconds = 0,
                    statusMessage = "Video saved: ${entity.displayName}"
                )
            }
        }
    }

    fun stampImportedPhoto(sourceUri: Uri) {
        viewModelScope.launch {
            _hudState.update { it.copy(isCapturing = true) }
            val currentTel = telemetry.value
            val currentCfg = stampConfig.value
            val mapBmp = obtainCaptureMiniMapBitmap(currentTel, currentCfg)

            val result = mediaStoreSaver.stampAndSaveFromUri(
                sourceUri = sourceUri,
                telemetry = currentTel,
                config = currentCfg,
                miniMapBitmap = mapBmp
            )

            result.onSuccess { entity ->
                photoRepository.insertPhoto(entity)
                if (currentCfg.showNumbering || (currentCfg.customFileNameEnabled && currentCfg.fnIncludeSequence)) {
                    stampPrefs.incrementSequenceNumber()
                }
                _hudState.update {
                    it.copy(
                        isCapturing = false,
                        statusMessage = "Stamped & saved ${entity.displayName}"
                    )
                }
            }.onFailure { err ->
                _hudState.update {
                    it.copy(
                        isCapturing = false,
                        statusMessage = err.message ?: "Failed to stamp imported photo"
                    )
                }
            }
        }
    }

    fun setStatusMessage(msg: String) {
        _hudState.update { it.copy(statusMessage = msg) }
    }

    fun clearStatusMessage() {
        _hudState.update { it.copy(statusMessage = null) }
    }

    class Factory(
        private val locationManager: LocationTelemetryManager,
        private val stampPrefs: StampPreferencesRepository,
        private val photoRepository: PhotoRepository,
        private val miniMapRenderer: MiniMapTileRenderer,
        private val mediaStoreSaver: MediaStoreSaver
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CameraViewModel(
                locationManager,
                stampPrefs,
                photoRepository,
                miniMapRenderer,
                mediaStoreSaver
            ) as T
        }
    }
}
