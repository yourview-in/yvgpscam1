package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.view.MotionEvent
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.GpsSignalState
import com.example.data.model.ScreenTapAction
import com.example.data.model.StampOrientationMode
import com.example.data.model.StampPosition
import com.example.data.model.VideoQualityLevel
import com.example.data.model.VolumeButtonAction
import com.example.ui.components.AdBannerSlot
import com.example.ui.components.CameraQuickToolsDrawer
import com.example.ui.components.DetectedQrActionBanner
import com.example.ui.components.DigitalHorizonLevelOverlay
import com.example.ui.components.StampPreviewCard
import com.example.ui.components.SystemBarsImmersiveEffect
import com.example.ui.components.launchPostCaptureAction
import com.example.ui.theme.FocusReticleGreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.ShutterGold
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.viewmodel.CameraAspectRatio
import com.example.ui.viewmodel.CameraCaptureMode
import com.example.ui.viewmodel.CameraViewModel
import com.example.util.QrCodeHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

private data class FocusReticleState(
    val point: Offset,
    val isFocused: Boolean?,
    val sequenceId: Int
)

enum class DeviceCaptureOrientation(
    val label: String,
    val angleDegrees: Float,
    val surfaceRotation: Int
) {
    PORTRAIT("PORTRAIT", 0f, Surface.ROTATION_0),
    LANDSCAPE_RIGHT("LANDSCAPE", -90f, Surface.ROTATION_270),
    PORTRAIT_UPSIDE_DOWN("PORTRAIT", 180f, Surface.ROTATION_180),
    LANDSCAPE_LEFT("LANDSCAPE", 90f, Surface.ROTATION_90);

    val isLandscape: Boolean
        get() = this == LANDSCAPE_LEFT || this == LANDSCAPE_RIGHT
}

internal fun resolveDeviceCaptureOrientation(
    sensorDegrees: Int,
    current: DeviceCaptureOrientation
): DeviceCaptureOrientation {
    if (sensorDegrees == OrientationEventListener.ORIENTATION_UNKNOWN || sensorDegrees < 0) {
        return current
    }
    val normalized = ((sensorDegrees % 360) + 360) % 360
    return when {
        normalized >= 330 || normalized <= 30 -> DeviceCaptureOrientation.PORTRAIT
        normalized in 60..120 -> DeviceCaptureOrientation.LANDSCAPE_RIGHT
        normalized in 150..210 -> DeviceCaptureOrientation.PORTRAIT_UPSIDE_DOWN
        normalized in 240..300 -> DeviceCaptureOrientation.LANDSCAPE_LEFT
        else -> current
    }
}

private val CameraBarBg = Color(0xFF15181C)

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onOpenGallery: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenLocations: () -> Unit,
    onOpenFileName: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenImageOnMap: () -> Unit = {},
    onOpenPdfReport: () -> Unit = {},
    onOpenCloudSync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val stampConfig by viewModel.stampConfig.collectAsStateWithLifecycle()
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()
    val miniMapBitmap by viewModel.miniMapBitmap.collectAsStateWithLifecycle()
    val latestPhoto by viewModel.latestPhoto.collectAsStateWithLifecycle()
    val horizonTilt by viewModel.horizonTiltDegrees.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasCameraPermission = grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        hasAudioPermission = grants[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionsUpdated(hasLocationPermission)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    val photoImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.stampImportedPhoto(uri)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.onPermissionsUpdated(true)
        }
    }

    DisposableEffect(lifecycleOwner, hasLocationPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                hasAudioPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                viewModel.onPermissionsUpdated(hasLocationPermission)
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.onLifecyclePause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showNoteDialog by remember { mutableStateOf(false) }
    var noteDraft by remember(stampConfig.customNote) { mutableStateOf(stampConfig.customNote) }
    var brandDraft by remember(stampConfig.customBrandLabel) { mutableStateOf(stampConfig.customBrandLabel) }
    var shutterFlash by remember { mutableStateOf(false) }
    var focusReticle by remember { mutableStateOf<FocusReticleState?>(null) }
    var focusSequence by remember { mutableStateOf(0) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    LaunchedEffect(shutterFlash) {
        if (shutterFlash) {
            delay(130)
            shutterFlash = false
        }
    }

    LaunchedEffect(focusReticle?.sequenceId) {
        val currentSeq = focusReticle?.sequenceId ?: return@LaunchedEffect
        delay(1600)
        if (focusReticle?.sequenceId == currentSeq) {
            focusReticle = null
        }
    }

    LaunchedEffect(hudState.lensFacing) {
        focusReticle = null
    }

    LaunchedEffect(hudState.statusMessage) {
        if (hudState.statusMessage != null) {
            delay(2400)
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(hudState.isRecordingVideo) {
        if (!hudState.isRecordingVideo) return@LaunchedEffect
        var elapsed = 0
        while (true) {
            viewModel.setRecordingState(isRecording = true, elapsedSeconds = elapsed)
            delay(1000)
            elapsed += 1
        }
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val videoCapture = remember(stampConfig.videoQuality) {
        val qualitySelector = when (stampConfig.videoQuality) {
            VideoQualityLevel.UHD_4K -> QualitySelector.from(
                Quality.UHD,
                FallbackStrategy.higherQualityOrLowerThan(Quality.FHD)
            )
            VideoQualityLevel.FHD_1080P -> QualitySelector.from(
                Quality.FHD,
                FallbackStrategy.higherQualityOrLowerThan(Quality.HD)
            )
            VideoQualityLevel.HD_720P -> QualitySelector.from(Quality.HD)
        }
        val recorder = Recorder.Builder().setQualitySelector(qualitySelector).build()
        VideoCapture.withOutput(recorder)
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val qrAnalysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var lastQrScanMs by remember { mutableLongStateOf(0L) }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
            cameraExecutor.shutdown()
            qrAnalysisExecutor.shutdown()
        }
    }

    var activeCamera by remember { mutableStateOf<Camera?>(null) }

    val configuration = LocalConfiguration.current
    var deviceOrientation by remember {
        mutableStateOf(
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                DeviceCaptureOrientation.LANDSCAPE_LEFT
            } else {
                DeviceCaptureOrientation.PORTRAIT
            }
        )
    }

    DisposableEffect(context) {
        val orientationListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                val next = resolveDeviceCaptureOrientation(orientation, deviceOrientation)
                if (next != deviceOrientation) {
                    deviceOrientation = next
                }
            }
        }
        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable()
        }
        onDispose { orientationListener.disable() }
    }

    val effectiveStampOrientationAngle = remember(deviceOrientation, stampConfig.stampOrientation) {
        when (stampConfig.stampOrientation) {
            StampOrientationMode.AUTOMATIC -> deviceOrientation.angleDegrees
            StampOrientationMode.PORTRAIT -> 0f
            StampOrientationMode.LANDSCAPE -> 90f
        }
    }
    val effectiveStampOrientationDegrees = remember(effectiveStampOrientationAngle) {
        effectiveStampOrientationAngle.roundToInt()
    }

    LaunchedEffect(hudState.flashMode) {
        imageCapture.flashMode = hudState.flashMode
    }

    LaunchedEffect(deviceOrientation) {
        imageCapture.targetRotation = deviceOrientation.surfaceRotation
        videoCapture.targetRotation = deviceOrientation.surfaceRotation
    }

    LaunchedEffect(hudState.zoomRatio, activeCamera) {
        activeCamera?.cameraControl?.setZoomRatio(hudState.zoomRatio)
    }

    // Collect post-capture Auto-Share / Email / Share Mode actions
    LaunchedEffect(Unit) {
        viewModel.postCaptureEvent.collectLatest { entity ->
            val cfg = viewModel.stampConfig.value
            launchPostCaptureAction(
                context = context,
                entity = entity,
                emailRecipient = cfg.defaultReportEmail,
                preferEmail = cfg.emailAfterCapture && !cfg.autoShareAfterCapture
            )
        }
    }

    fun executePhotoShutterNow() {
        shutterFlash = true
        val shareNow = hudState.captureMode == CameraCaptureMode.SHARE_PHOTO
        val front = hudState.lensFacing == CameraSelector.LENS_FACING_FRONT
        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()
                    viewModel.saveCapturedPhoto(
                        jpegBytes = bytes,
                        isFrontCamera = front,
                        deviceOrientationDegrees = effectiveStampOrientationDegrees,
                        triggerShareImmediately = shareNow
                    )
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModel.setStatusMessage(exception.message ?: "Capture failed")
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    fun toggleVideoRecording() {
        val currentRec = activeRecording
        if (currentRec != null) {
            currentRec.stop()
            activeRecording = null
            viewModel.setRecordingState(isRecording = false, elapsedSeconds = 0)
            return
        }

        val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "YVGPSCAM_$fileTimestamp.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/YVGPSCAM")
            }
        }
        val outputOptions = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        val pending = videoCapture.output.prepareRecording(context, outputOptions).apply {
            if (stampConfig.recordVideoWithVoice && hasAudioPermission) {
                withAudioEnabled()
            }
        }

        viewModel.setRecordingState(isRecording = true, elapsedSeconds = 0)
        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize) {
                activeRecording = null
                if (!event.hasError()) {
                    viewModel.saveRecordedVideo(event.outputResults.outputUri)
                } else {
                    viewModel.setRecordingState(isRecording = false, elapsedSeconds = 0)
                    viewModel.setStatusMessage("Video recording stopped (${event.error})")
                }
            }
        }
    }

    fun triggerPrimaryShutter() {
        if (!hasCameraPermission || hudState.isCapturing || hudState.countdownRemaining > 0) return
        if (hudState.captureMode == CameraCaptureMode.VIDEO) {
            toggleVideoRecording()
            return
        }
        val delaySec = stampConfig.timerDelaySeconds
        if (delaySec <= 0) {
            executePhotoShutterNow()
        } else {
            scope.launch {
                for (rem in delaySec downTo 1) {
                    viewModel.setCountdownRemaining(rem)
                    delay(1000)
                }
                viewModel.setCountdownRemaining(0)
                executePhotoShutterNow()
            }
        }
    }

    // Hardware Volume Button Capture Listener
    LaunchedEffect(Unit) {
        viewModel.volumeEvents.collectLatest { action ->
            if (action == VolumeButtonAction.PHOTO_CAPTURE) {
                triggerPrimaryShutter()
            }
        }
    }

    val isFullScreenCamera = hudState.isFullScreenCamera && hasCameraPermission
    SystemBarsImmersiveEffect(immersive = isFullScreenCamera)
    BackHandler(enabled = hudState.showToolsDrawer || isFullScreenCamera) {
        if (hudState.showToolsDrawer) {
            viewModel.closeToolsDrawer()
        } else {
            viewModel.setFullScreenCamera(false)
        }
    }

    // 6-Icon Top Toolbar matching IMG_4851 / IMG_4859
    val topIconToolbar: @Composable () -> Unit = {
        val flashIcon = when (hudState.flashMode) {
            ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
            ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
            else -> Icons.Default.FlashOff
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isFullScreenCamera) CameraBarBg.copy(alpha = 0.72f) else CameraBarBg)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 6.dp)
                    .testTag("camera_quick_settings_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Expandable Quick Tools Grid (3x3 drawer)
                IconButton(
                    onClick = { viewModel.toggleToolsDrawer() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("camera_tools_drawer_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Toggle camera tools",
                        tint = if (hudState.showToolsDrawer) ShutterGold else Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }

                // 2. Flash Mode
                IconButton(
                    onClick = { viewModel.cycleFlashMode() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("flash_toggle_button")
                ) {
                    Icon(
                        imageVector = flashIcon,
                        contentDescription = stringResource(R.string.toggle_flash),
                        tint = if (hudState.flashMode != ImageCapture.FLASH_MODE_OFF) ShutterGold else Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }

                // 3. Map Icon right beside Flash -> Loads Full-Page Google Map ("Image on Map")
                IconButton(
                    onClick = onOpenImageOnMap,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("camera_image_on_map_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddLocationAlt,
                        contentDescription = "Open full-page Google Map",
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }

                // 4. Switch Camera
                IconButton(
                    onClick = { viewModel.toggleLensFacing() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("switch_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = stringResource(R.string.switch_camera),
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }

                // 5. Settings Gear -> Full Settings Screen
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("camera_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open camera and app settings",
                        tint = Color.White,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = hudState.showToolsDrawer,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                CameraQuickToolsDrawer(
                    hudState = hudState,
                    config = stampConfig,
                    onCycleRatio = { viewModel.cycleAspectRatio() },
                    onToggleGrid = { viewModel.toggleGrid() },
                    onCycleTimer = { viewModel.cycleTimerDelay() },
                    onToggleFullView = {
                        viewModel.toggleFullScreenCamera()
                        viewModel.closeToolsDrawer()
                    },
                    onToggleMirror = {
                        viewModel.updateStampConfig { it.copy(mirrorFrontCamera = !it.mirrorFrontCamera) }
                    },
                    onToggleVoice = {
                        val nextVoice = !stampConfig.recordVideoWithVoice
                        viewModel.updateStampConfig { it.copy(recordVideoWithVoice = nextVoice) }
                        if (nextVoice && !hasAudioPermission) {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onToggleLevel = {
                        viewModel.updateStampConfig { it.copy(showDigitalLevel = !it.showDigitalLevel) }
                    },
                    onCycleStampPosition = { viewModel.cycleStampPosition() },
                    onOpenNoteDialog = {
                        viewModel.closeToolsDrawer()
                        showNoteDialog = true
                    },
                    onImportPhoto = {
                        viewModel.closeToolsDrawer()
                        photoImportLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onOpenDriveSync = {
                        viewModel.closeToolsDrawer()
                        onOpenCloudSync()
                    }
                )
            }
        }
    }

    val livePreviewContent = remember(context) {
        movableContentOf { previewModifier: Modifier ->
            val previewView = remember {
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }

            LaunchedEffect(
                hudState.lensFacing,
                hudState.captureMode,
                stampConfig.qrDetectionEnabled,
                videoCapture
            ) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    runCatching {
                        val provider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val selector = CameraSelector.Builder()
                            .requireLensFacing(hudState.lensFacing)
                            .build()
                        provider.unbindAll()

                        val cam = if (hudState.captureMode == CameraCaptureMode.VIDEO) {
                            imageAnalysis.clearAnalyzer()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                videoCapture
                            )
                        } else if (stampConfig.qrDetectionEnabled) {
                            imageAnalysis.setAnalyzer(qrAnalysisExecutor) { proxy ->
                                val now = System.currentTimeMillis()
                                if (now - lastQrScanMs >= 700L) {
                                    lastQrScanMs = now
                                    val plane = proxy.planes.firstOrNull()
                                    if (plane != null) {
                                        val buf = plane.buffer
                                        val bytes = ByteArray(buf.remaining())
                                        buf.get(bytes)
                                        val decoded = QrCodeHelper.decodeQrFromYPlane(
                                            yBytes = bytes,
                                            width = proxy.width,
                                            height = proxy.height
                                        )
                                        if (decoded != null) {
                                            viewModel.updateDetectedQrCode(decoded)
                                        }
                                    }
                                }
                                proxy.close()
                            }
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                imageCapture,
                                imageAnalysis
                            )
                        } else {
                            imageAnalysis.clearAnalyzer()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                imageCapture
                            )
                        }

                        activeCamera = cam
                        cam.cameraInfo.zoomState.value?.let { zState ->
                            viewModel.updateZoomBounds(zState.minZoomRatio, zState.maxZoomRatio)
                        }
                    }
                }, ContextCompat.getMainExecutor(context))
            }

            AndroidView(
                factory = {
                    previewView.apply {
                        setOnTouchListener { v, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                when (stampConfig.screenTapAction) {
                                    ScreenTapAction.CAPTURE -> {
                                        triggerPrimaryShutter()
                                    }

                                    ScreenTapAction.FOCUS -> {
                                        val tapOffset = Offset(event.x, event.y)
                                        focusSequence += 1
                                        val seq = focusSequence
                                        focusReticle = FocusReticleState(
                                            point = tapOffset,
                                            isFocused = null,
                                            sequenceId = seq
                                        )
                                        val factory = meteringPointFactory
                                        val point = factory.createPoint(event.x, event.y)
                                        val action = FocusMeteringAction.Builder(
                                            point,
                                            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                        )
                                            .setAutoCancelDuration(4, TimeUnit.SECONDS)
                                            .build()

                                        val future = activeCamera?.cameraControl?.startFocusAndMetering(action)
                                        future?.addListener(
                                            {
                                                val focused = runCatching { future.get().isFocusSuccessful }
                                                    .getOrDefault(true)
                                                if (focusReticle?.sequenceId == seq) {
                                                    focusReticle = focusReticle?.copy(isFocused = focused)
                                                }
                                            },
                                            ContextCompat.getMainExecutor(context)
                                        )
                                    }

                                    ScreenTapAction.OFF -> Unit
                                }
                                v.performClick()
                            }
                            true
                        }
                    }
                },
                modifier = previewModifier
            )
        }
    }

    val zoomOptions = remember(hudState.minZoomRatio, hudState.maxZoomRatio) {
        buildList {
            if (hudState.minZoomRatio <= 0.7f) add(0.5f)
            add(1f)
            if (hudState.maxZoomRatio >= 2f) add(2f)
            if (hudState.minZoomRatio > 0.7f && hudState.maxZoomRatio >= 3f) add(3f)
        }
    }

    val viewfinderOverlays: @Composable (Boolean) -> Unit = { fullScreenOverlay ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val topInsetPad = if (fullScreenOverlay) 68.dp else 8.dp
            val bottomControlsReserve = if (fullScreenOverlay) 156.dp else 0.dp
            val isFrontCam = hudState.lensFacing == CameraSelector.LENS_FACING_FRONT

            if (hudState.showGrid) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineColor = Color.White.copy(alpha = 0.16f)
                    drawLine(lineColor, Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height), strokeWidth = 0.9.dp.toPx())
                    drawLine(lineColor, Offset(size.width * 2f / 3f, 0f), Offset(size.width * 2f / 3f, size.height), strokeWidth = 0.9.dp.toPx())
                    drawLine(lineColor, Offset(0f, size.height / 3f), Offset(size.width, size.height / 3f), strokeWidth = 0.9.dp.toPx())
                    drawLine(lineColor, Offset(0f, size.height * 2f / 3f), Offset(size.width, size.height * 2f / 3f), strokeWidth = 0.9.dp.toPx())
                }
            }

            if (stampConfig.showDigitalLevel) {
                DigitalHorizonLevelOverlay(
                    pitchDeg = horizonTilt.first,
                    rollDeg = horizonTilt.second,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val activeReticle = focusReticle
                if (activeReticle != null) {
                    TapFocusReticleOverlay(
                        reticle = activeReticle,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CenterFocusBracketsGuide(modifier = Modifier.align(Alignment.Center))
                }
            }

            // Countdown Overlay (3s / 10s Timer)
            if (hudState.countdownRemaining > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(92.dp)
                        .border(2.dp, ShutterGold, CircleShape)
                        .testTag("camera_timer_countdown_badge")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = hudState.countdownRemaining.toString(),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ShutterGold
                        )
                    }
                }
            }

            // Shutter Flash
            AnimatedVisibility(
                visible = shutterFlash,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.55f))
                )
            }

            // Top-Left: Orientation Pill OR Active Video Recording Timer
            if (hudState.isRecordingVideo) {
                val mins = hudState.recordingElapsedSeconds / 60
                val secs = hudState.recordingElapsedSeconds % 60
                Surface(
                    color = Color(0xFFD92D20),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = topInsetPad)
                        .testTag("video_recording_timer_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Text(
                            text = String.format(Locale.US, "REC %02d:%02d", mins, secs),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                ViewfinderOrientationStatusPill(
                    orientation = deviceOrientation,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 10.dp, top = topInsetPad)
                )
            }

            // Top-Right: Compact GPS Signal Status Pill
            CompactGpsStatusPill(
                signalState = telemetry.signalState,
                accuracyMeters = telemetry.accuracyMeters,
                offlineOnly = stampConfig.offlineOnlyMapMode,
                onTapRequestPermission = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                hasLocationPermission = hasLocationPermission,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = topInsetPad)
            )

            // Detected Live QR Code Banner
            val qrText = hudState.detectedQrText
            if (qrText != null && stampConfig.qrDetectionEnabled) {
                DetectedQrActionBanner(
                    qrText = qrText,
                    context = context,
                    onDismiss = { viewModel.updateDetectedQrCode(null) },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topInsetPad + 32.dp)
                )
            }

            // Stamp Preview Card + Floating Zoom Pill
            val isTopStamp = stampConfig.stampPosition == StampPosition.TOP_LEFT ||
                stampConfig.stampPosition == StampPosition.TOP_CENTER ||
                stampConfig.stampPosition == StampPosition.TOP_RIGHT

            if (isTopStamp) {
                val topAlign = when (stampConfig.stampPosition) {
                    StampPosition.TOP_LEFT -> Alignment.TopStart
                    StampPosition.TOP_RIGHT -> Alignment.TopEnd
                    else -> Alignment.TopCenter
                }
                Box(
                    modifier = Modifier
                        .align(topAlign)
                        .padding(horizontal = 8.dp, vertical = topInsetPad + 34.dp)
                ) {
                    StampPreviewCard(
                        telemetry = telemetry,
                        config = stampConfig,
                        miniMapBitmap = miniMapBitmap,
                        isFrontCamera = isFrontCam,
                        onClick = { viewModel.cycleStampTemplate() }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = bottomControlsReserve + 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isTopStamp) {
                    val horizontalAlign = when (stampConfig.stampPosition) {
                        StampPosition.BOTTOM_LEFT -> Alignment.Start
                        StampPosition.BOTTOM_RIGHT -> Alignment.End
                        else -> Alignment.CenterHorizontally
                    }
                    Box(modifier = Modifier.align(horizontalAlign)) {
                        StampPreviewCard(
                            telemetry = telemetry,
                            config = stampConfig,
                            miniMapBitmap = miniMapBitmap,
                            isFrontCamera = isFrontCam,
                            onClick = { viewModel.cycleStampTemplate() }
                        )
                    }
                }

                ZoomSelectorPill(
                    zoomOptions = zoomOptions,
                    currentZoom = hudState.zoomRatio,
                    onSelectZoom = { viewModel.setZoomRatio(it) }
                )
            }
        }
    }

    // Bottom Camera Deck matching IMG_4847 / IMG_4851
    val bottomCameraDeck: @Composable () -> Unit = {
        val visibleModes = remember(stampConfig.showReportsModeOnCamera) {
            buildList {
                add(CameraCaptureMode.SHARE_PHOTO)
                add(CameraCaptureMode.PHOTO)
                add(CameraCaptureMode.VIDEO)
                if (stampConfig.showReportsModeOnCamera) {
                    add(CameraCaptureMode.REPORTS)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isFullScreenCamera) CameraBarBg.copy(alpha = 0.78f) else CameraBarBg)
                .navigationBarsPadding()
                .padding(top = 6.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            AnimatedVisibility(visible = hudState.statusMessage != null) {
                Surface(
                    color = ObsidianSurfaceHigh,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .border(1.dp, ShutterGold.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                ) {
                    Text(
                        text = hudState.statusMessage.orEmpty(),
                        color = ShutterGold,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            // Capture Mode Strip: SHARE PHOTO | PHOTO | VIDEO | REPORTS
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                visibleModes.forEach { mode ->
                    val isSelected = hudState.captureMode == mode
                    Surface(
                        color = if (isSelected) ShutterGold else Color.Transparent,
                        shape = RoundedCornerShape(7.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .clickable {
                                if (mode == CameraCaptureMode.REPORTS) {
                                    onOpenPdfReport()
                                } else {
                                    viewModel.setCaptureMode(mode)
                                }
                            }
                            .testTag("camera_mode_${mode.name.lowercase()}")
                    ) {
                        Text(
                            text = mode.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isSelected) Color(0xFF141619) else Color.White.copy(alpha = 0.82f),
                            letterSpacing = 0.4.sp,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 5-Item Bottom Action Row: Collection | Locations | Golden Shutter/Video | File Name | Template
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Collection (circular latest-photo thumbnail -> Gallery)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .width(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onOpenGallery)
                        .padding(vertical = 4.dp)
                        .testTag("camera_gallery_thumb_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ObsidianSurfaceHigh)
                            .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (latestPhoto != null) {
                            AsyncImage(
                                model = latestPhoto!!.mediaUri,
                                contentDescription = "Open latest capture collection",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Open photo collection",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = "Collection",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 2. Locations (-> Locations Screen)
                BottomDeckIconAction(
                    icon = Icons.Outlined.LocationOn,
                    label = "Locations",
                    onClick = onOpenLocations,
                    modifier = Modifier.testTag("camera_locations_button")
                )

                // 3. Center Golden Yellow Shutter / Video Record Button
                val shutterScale by animateFloatAsState(
                    targetValue = if (hudState.isCapturing || hudState.isRecordingVideo) 0.90f else 1f,
                    animationSpec = spring(),
                    label = "shutterScale"
                )
                val isVideoMode = hudState.captureMode == CameraCaptureMode.VIDEO
                val innerColor = when {
                    !hasCameraPermission -> ObsidianSurfaceHigh
                    isVideoMode && hudState.isRecordingVideo -> Color(0xFFE53935)
                    isVideoMode -> Color(0xFFFF3B30)
                    else -> ShutterGold
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(shutterScale)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                        .padding(5.dp)
                        .clip(if (isVideoMode && hudState.isRecordingVideo) RoundedCornerShape(14.dp) else CircleShape)
                        .background(innerColor)
                        .clickable(enabled = hasCameraPermission && !hudState.isCapturing) {
                            triggerPrimaryShutter()
                        }
                        .testTag("capture_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (hudState.isCapturing) {
                        CircularProgressIndicator(
                            color = Color(0xFF141619),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(26.dp)
                        )
                    } else if (isVideoMode && hudState.isRecordingVideo) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White)
                        )
                    }
                }

                // 4. File Name (-> Custom File Name Screen)
                BottomDeckIconAction(
                    icon = Icons.AutoMirrored.Outlined.NoteAdd,
                    label = "File Name",
                    onClick = onOpenFileName,
                    modifier = Modifier.testTag("camera_edit_note_btn")
                )

                // 5. Template (-> Visual 6-Template Picker Screen)
                BottomDeckIconAction(
                    icon = Icons.Outlined.GridView,
                    label = "Template",
                    onClick = onOpenTemplates,
                    modifier = Modifier.testTag("camera_open_templates_btn")
                )
            }

            AdBannerSlot(placementId = "camera")
        }
    }

    // Root Scaffolded Camera Layout
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isFullScreenCamera) {
            Box(modifier = Modifier.fillMaxSize()) {
                livePreviewContent(Modifier.fillMaxSize())
                viewfinderOverlays(true)
            }
            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                topIconToolbar()
            }
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                bottomCameraDeck()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                topIconToolbar()

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (!hasCameraPermission) {
                        PermissionOnboardingCard(
                            onRequestPermissions = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.CAMERA,
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            onOpenSettings = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            onImportPhoto = {
                                photoImportLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    } else {
                        val previewFrameModifier = when (hudState.aspectRatio) {
                            CameraAspectRatio.RATIO_1_1 -> Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                            CameraAspectRatio.RATIO_16_9 -> Modifier
                                .fillMaxSize()
                            CameraAspectRatio.RATIO_4_3 -> Modifier.fillMaxSize()
                        }
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = previewFrameModifier) {
                                livePreviewContent(Modifier.fillMaxSize())
                            }
                            viewfinderOverlays(false)
                        }
                    }
                }

                bottomCameraDeck()
            }
        }
    }

    // Quick Note & Brand Label Dialog
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Stamp Note & Brand Label") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = brandDraft,
                        onValueChange = { brandDraft = it.take(24) },
                        label = { Text("Brand Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it.take(80) },
                        label = { Text("Stamp Note") },
                        placeholder = { Text("e.g., Site survey complete") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateStampConfig {
                            it.copy(
                                customBrandLabel = brandDraft.trim().ifBlank { "YVGPSCAM" },
                                customNote = noteDraft.trim()
                            )
                        }
                        showNoteDialog = false
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
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BottomDeckIconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .width(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ZoomSelectorPill(
    zoomOptions: List<Float>,
    currentZoom: Float,
    onSelectZoom: (Float) -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.65f),
        shape = CircleShape,
        modifier = Modifier.testTag("camera_zoom_selector_pill")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)
        ) {
            zoomOptions.forEach { zoomVal ->
                val selected = abs(currentZoom - zoomVal) < 0.2f
                val label = when {
                    zoomVal < 1f -> "0.5"
                    zoomVal == 1f -> "1X"
                    else -> "${zoomVal.toInt()}"
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (selected) ShutterGold else Color.Transparent)
                        .clickable { onSelectZoom(zoomVal) }
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color(0xFF141619) else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterFocusBracketsGuide(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .size(112.dp)
            .testTag("camera_center_focus_brackets")
    ) {
        val len = 24.dp.toPx()
        val stroke = 2.6.dp.toPx()
        val color = FocusReticleGreen.copy(alpha = 0.9f)
        drawLine(color, Offset(0f, 0f), Offset(len, 0f), strokeWidth = stroke, cap = StrokeCap.Square)
        drawLine(color, Offset(0f, 0f), Offset(0f, len), strokeWidth = stroke, cap = StrokeCap.Square)
        drawLine(color, Offset(size.width, 0f), Offset(size.width - len, 0f), strokeWidth = stroke, cap = StrokeCap.Square)
        drawLine(color, Offset(size.width, 0f), Offset(size.width, len), strokeWidth = stroke, cap = StrokeCap.Square)
        drawLine(color, Offset(0f, size.height), Offset(len, size.height), strokeWidth = stroke, cap = StrokeCap.Square)
        drawLine(color, Offset(0f, size.height), Offset(0f, size.height - len), strokeWidth = stroke, cap = StrokeCap.Square)
        drawLine(color, Offset(size.width, size.height), Offset(size.width - len, size.height), strokeWidth = stroke, cap = StrokeCap.Square)
        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - len), strokeWidth = stroke, cap = StrokeCap.Square)
    }
}

@Composable
private fun TapFocusReticleOverlay(
    reticle: FocusReticleState,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember(reticle.sequenceId) { Animatable(1.28f) }
    LaunchedEffect(reticle.sequenceId) {
        scaleAnim.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f)
        )
    }

    val reticleColor = when (reticle.isFocused) {
        true -> FocusReticleGreen
        false -> Color.White
        null -> ShutterGold
    }
    val statusLabel = when (reticle.isFocused) {
        true -> "AF LOCK"
        false -> "METERED"
        null -> "FOCUS"
    }

    BoxWithConstraints(modifier = modifier.testTag("camera_focus_reticle")) {
        val density = LocalDensity.current
        val maxW = with(density) { maxWidth.toPx() }
        val maxH = with(density) { maxHeight.toPx() }
        val badgeW = with(density) { 64.dp.toPx() }
        val badgeH = with(density) { 20.dp.toPx() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = scaleAnim.value
            val halfBox = 44.dp.toPx() * scale
            val bracketLen = 18.dp.toPx() * scale
            val strokePx = 2.6.dp.toPx()
            val cx = reticle.point.x
            val cy = reticle.point.y

            val corners = listOf(
                Pair(Offset(cx - halfBox, cy - halfBox), Pair(Offset(1f, 0f), Offset(0f, 1f))),
                Pair(Offset(cx + halfBox, cy - halfBox), Pair(Offset(-1f, 0f), Offset(0f, 1f))),
                Pair(Offset(cx - halfBox, cy + halfBox), Pair(Offset(1f, 0f), Offset(0f, -1f))),
                Pair(Offset(cx + halfBox, cy + halfBox), Pair(Offset(-1f, 0f), Offset(0f, -1f)))
            )
            corners.forEach { (corner, dirs) ->
                val (dirX, dirY) = dirs
                drawLine(reticleColor, corner, Offset(corner.x + dirX.x * bracketLen, corner.y), strokeWidth = strokePx, cap = StrokeCap.Square)
                drawLine(reticleColor, corner, Offset(corner.x, corner.y + dirY.y * bracketLen), strokeWidth = strokePx, cap = StrokeCap.Square)
            }
        }

        val desiredX = reticle.point.x - badgeW / 2f
        val belowY = with(density) { reticle.point.y + 50.dp.toPx() }
        val aboveY = with(density) { reticle.point.y - 50.dp.toPx() - badgeH }
        val labelOffsetX = desiredX.coerceIn(8f, (maxW - badgeW - 8f).coerceAtLeast(8f)).roundToInt()
        val labelOffsetY = (if (belowY + badgeH + 8f <= maxH) belowY else aboveY)
            .coerceIn(8f, (maxH - badgeH - 8f).coerceAtLeast(8f))
            .roundToInt()

        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .offset { IntOffset(labelOffsetX, labelOffsetY) }
                .border(0.75.dp, reticleColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
        ) {
            Text(
                text = statusLabel,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = reticleColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun CompactGpsStatusPill(
    signalState: GpsSignalState,
    accuracyMeters: Float?,
    offlineOnly: Boolean,
    hasLocationPermission: Boolean,
    onTapRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (gpsIcon, gpsColor, gpsLabel) = when (signalState) {
        GpsSignalState.LOCKED -> Triple(Icons.Default.GpsFixed, TelemetryEmerald, "±${accuracyMeters?.roundToInt() ?: 0}m")
        GpsSignalState.COARSE -> Triple(Icons.Default.GpsNotFixed, ShutterGold, "±${accuracyMeters?.roundToInt() ?: 0}m")
        GpsSignalState.ACQUIRING -> Triple(Icons.Default.GpsNotFixed, ShutterGold, "GPS SYNC")
        GpsSignalState.PERMISSION_DENIED -> Triple(Icons.Default.GpsOff, MaterialTheme.colorScheme.error, "TAP GPS")
    }

    Surface(
        color = Color.Black.copy(alpha = 0.56f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .border(1.dp, gpsColor.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .then(if (!hasLocationPermission) Modifier.clickable(onClick = onTapRequestPermission) else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(gpsIcon, contentDescription = "GPS status", tint = gpsColor, modifier = Modifier.size(12.dp))
            Text(
                text = gpsLabel,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = gpsColor
            )
            if (offlineOnly) {
                Icon(Icons.Default.OfflinePin, contentDescription = "Offline map mode", tint = ShutterGold, modifier = Modifier.size(11.dp))
            }
        }
    }
}

@Composable
private fun ViewfinderOrientationStatusPill(
    orientation: DeviceCaptureOrientation,
    modifier: Modifier = Modifier
) {
    val isLandscape = orientation.isLandscape
    val accent = if (isLandscape) ShutterGold else Color.White
    val borderColor = if (isLandscape) ShutterGold.copy(alpha = 0.78f) else Color.White.copy(alpha = 0.28f)
    val animatedAngle by animateFloatAsState(
        targetValue = orientation.angleDegrees,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
        label = "viewfinderOrientationAngle"
    )

    Surface(
        color = Color.Black.copy(alpha = 0.56f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .testTag("viewfinder_orientation_status")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ScreenRotation,
                contentDescription = "Auto-rotate active",
                tint = accent.copy(alpha = 0.85f),
                modifier = Modifier.size(12.dp)
            )
            Icon(
                imageVector = Icons.Default.StayCurrentPortrait,
                contentDescription = if (isLandscape) "Phone held in landscape orientation" else "Phone held in portrait orientation",
                tint = accent,
                modifier = Modifier
                    .size(13.dp)
                    .graphicsLayer { rotationZ = animatedAngle }
            )
            Text(
                text = orientation.label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
private fun PermissionOnboardingCard(
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onImportPhoto: () -> Unit
) {
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
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
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Text(
                text = stringResource(R.string.permission_title_camera),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.permission_desc_camera),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = ShutterGold, contentColor = Color(0xFF141619)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grant_permissions_button")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.grant_permissions), fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onImportPhoto,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stamp Photo")
                }
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Settings")
                }
            }
        }
    }
}
