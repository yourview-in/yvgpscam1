package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.di.YvGpsCamContainer
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.CloudSyncRouteScreen
import com.example.ui.screens.FileNameScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.ImageOnMapScreen
import com.example.ui.screens.LocationsScreen
import com.example.ui.screens.OfflineMapsScreen
import com.example.ui.screens.PdfReportScreen
import com.example.ui.screens.PhotoDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StampStudioScreen
import com.example.ui.screens.TemplatePickerScreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.theme.YvGpsCamTheme
import com.example.ui.viewmodel.CameraViewModel
import com.example.ui.viewmodel.GalleryViewModel
import com.example.ui.viewmodel.OfflineMapsViewModel
import kotlinx.serialization.Serializable

@Serializable object CameraRoute
@Serializable object GalleryRoute
@Serializable object StampStudioRoute
@Serializable object TemplatePickerRoute
@Serializable object LocationsRoute
@Serializable object SettingsRoute
@Serializable object ImageOnMapRoute
@Serializable object OfflineMapsRoute
@Serializable object CloudSyncRoute
@Serializable object FileNameRoute
@Serializable data class PdfReportRoute(val initialPhotoIdsCsv: String = "")
@Serializable data class PhotoDetailRoute(val photoId: Long)

private data class TopLevelDestination(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

internal fun resolveScreenOrientationForRoute(isMainCameraPage: Boolean): Int =
    if (isMainCameraPage) {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private class VolumeInterceptorHolder {
    var onVolumeKey: ((Boolean) -> Boolean)? = null
}

class MainActivity : ComponentActivity() {
    private val volumeInterceptor = VolumeInterceptorHolder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            requestedOrientation = resolveScreenOrientationForRoute(isMainCameraPage = true)
        }
        enableEdgeToEdge()
        val container = YvGpsCamContainer(applicationContext)
        container.autoSyncCoordinator.start()
        setContent {
            YvGpsCamApp(
                container = container,
                appContext = applicationContext,
                volumeInterceptor = volumeInterceptor
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val handled = volumeInterceptor.onVolumeKey?.invoke(keyCode == KeyEvent.KEYCODE_VOLUME_UP) ?: false
            if (handled) return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
private fun YvGpsCamApp(
    container: YvGpsCamContainer,
    appContext: Context,
    volumeInterceptor: VolumeInterceptorHolder
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val localContext = LocalContext.current
    val hostActivity = remember(localContext) { localContext.findActivity() }
    val isMainCameraPage = currentDestination == null || currentDestination.hasRoute<CameraRoute>()

    LaunchedEffect(isMainCameraPage, hostActivity) {
        hostActivity?.requestedOrientation = resolveScreenOrientationForRoute(isMainCameraPage)
    }

    val cameraViewModel: CameraViewModel = viewModel(
        factory = remember {
            CameraViewModel.Factory(
                container.locationTelemetryManager,
                container.stampPreferencesRepository,
                container.photoRepository,
                container.miniMapTileRenderer,
                container.mediaStoreSaver
            )
        }
    )
    val galleryViewModel: GalleryViewModel = viewModel(
        factory = remember {
            GalleryViewModel.Factory(
                appContext,
                container.photoRepository,
                container.miniMapTileRenderer,
                container.stampPreferencesRepository,
                container.googleDriveAccountStore
            )
        }
    )
    val offlineMapsViewModel: OfflineMapsViewModel = viewModel(
        factory = remember {
            OfflineMapsViewModel.Factory(
                container.offlineMapRepository,
                container.stampPreferencesRepository
            )
        }
    )

    val stampConfig by cameraViewModel.stampConfig.collectAsStateWithLifecycle()

    DisposableEffect(isMainCameraPage, cameraViewModel) {
        volumeInterceptor.onVolumeKey = if (isMainCameraPage) {
            { isUp -> cameraViewModel.onHardwareVolumePressed(isUp) }
        } else {
            null
        }
        onDispose {
            volumeInterceptor.onVolumeKey = null
        }
    }

    val destinations = remember {
        listOf(
            TopLevelDestination(CameraRoute, "Camera", Icons.Filled.CameraAlt, Icons.Outlined.CameraAlt, "nav_camera"),
            TopLevelDestination(GalleryRoute, "Gallery", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary, "nav_gallery"),
            TopLevelDestination(TemplatePickerRoute, "Templates", Icons.Filled.Tune, Icons.Outlined.Tune, "nav_studio"),
            TopLevelDestination(OfflineMapsRoute, "Offline", Icons.Filled.OfflinePin, Icons.Outlined.OfflinePin, "nav_offline"),
            TopLevelDestination(CloudSyncRoute, "Drive", Icons.Filled.CloudUpload, Icons.Outlined.CloudUpload, "nav_cloud")
        )
    }

    val isFullHeightSubScreen = isMainCameraPage ||
        currentDestination?.hasRoute<PhotoDetailRoute>() == true ||
        currentDestination?.hasRoute<FileNameRoute>() == true ||
        currentDestination?.hasRoute<SettingsRoute>() == true ||
        currentDestination?.hasRoute<LocationsRoute>() == true ||
        currentDestination?.hasRoute<ImageOnMapRoute>() == true ||
        currentDestination?.hasRoute<PdfReportRoute>() == true ||
        currentDestination?.hasRoute<StampStudioRoute>() == true ||
        currentDestination?.hasRoute<TemplatePickerRoute>() == true

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    YvGpsCamTheme(themeMode = stampConfig.appThemeMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 700.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (!useNavRail && !isFullHeightSubScreen) {
                        NavigationBar(
                            containerColor = ObsidianSurface,
                            tonalElevation = 0.dp
                        ) {
                            destinations.forEach { dest ->
                                val selected = when (dest.route) {
                                    is CameraRoute -> currentDestination?.hasRoute<CameraRoute>() == true
                                    is GalleryRoute -> currentDestination?.hasRoute<GalleryRoute>() == true
                                    is TemplatePickerRoute -> currentDestination?.hasRoute<TemplatePickerRoute>() == true
                                    is OfflineMapsRoute -> currentDestination?.hasRoute<OfflineMapsRoute>() == true
                                    is CloudSyncRoute -> currentDestination?.hasRoute<CloudSyncRoute>() == true
                                    else -> false
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ObsidianBg,
                                        selectedTextColor = TelemetryEmerald,
                                        indicatorColor = TelemetryEmerald,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail && !isFullHeightSubScreen) {
                        NavigationRail(
                            containerColor = ObsidianSurface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            destinations.forEach { dest ->
                                val selected = when (dest.route) {
                                    is CameraRoute -> currentDestination?.hasRoute<CameraRoute>() == true
                                    is GalleryRoute -> currentDestination?.hasRoute<GalleryRoute>() == true
                                    is TemplatePickerRoute -> currentDestination?.hasRoute<TemplatePickerRoute>() == true
                                    is OfflineMapsRoute -> currentDestination?.hasRoute<OfflineMapsRoute>() == true
                                    is CloudSyncRoute -> currentDestination?.hasRoute<CloudSyncRoute>() == true
                                    else -> false
                                }
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ObsidianBg,
                                        selectedTextColor = TelemetryEmerald,
                                        indicatorColor = TelemetryEmerald
                                    ),
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = CameraRoute,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable<CameraRoute> {
                            CameraScreen(
                                viewModel = cameraViewModel,
                                onOpenGallery = { navigateTopLevel(GalleryRoute) },
                                onOpenTemplates = { navController.navigate(TemplatePickerRoute) },
                                onOpenLocations = { navController.navigate(LocationsRoute) },
                                onOpenFileName = { navController.navigate(FileNameRoute) },
                                onOpenSettings = { navController.navigate(SettingsRoute) },
                                onOpenImageOnMap = { navController.navigate(ImageOnMapRoute) },
                                onOpenPdfReport = { navController.navigate(PdfReportRoute()) },
                                onOpenCloudSync = { navigateTopLevel(CloudSyncRoute) }
                            )
                        }
                        composable<GalleryRoute> {
                            GalleryScreen(
                                galleryViewModel = galleryViewModel,
                                offlineOnlyMapMode = stampConfig.offlineOnlyMapMode,
                                onPhotoClick = { photoId ->
                                    navController.navigate(PhotoDetailRoute(photoId))
                                },
                                onGoToCamera = { navigateTopLevel(CameraRoute) },
                                onOpenImageOnMap = { navController.navigate(ImageOnMapRoute) },
                                onOpenPdfReport = { selectedIds ->
                                    navController.navigate(PdfReportRoute(selectedIds.joinToString(",")))
                                },
                                onOpenCloudSync = { navigateTopLevel(CloudSyncRoute) }
                            )
                        }
                        composable<TemplatePickerRoute> {
                            TemplatePickerScreen(
                                viewModel = cameraViewModel,
                                onBack = { navController.popBackStack() },
                                onCustomizeAdvance = { navController.navigate(StampStudioRoute) }
                            )
                        }
                        composable<StampStudioRoute> {
                            StampStudioScreen(
                                viewModel = cameraViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable<LocationsRoute> {
                            LocationsScreen(
                                viewModel = cameraViewModel,
                                onBack = { navController.popBackStack() },
                                onOpenOfflineMaps = { navigateTopLevel(OfflineMapsRoute) }
                            )
                        }
                        composable<SettingsRoute> {
                            SettingsScreen(
                                viewModel = cameraViewModel,
                                onBack = { navController.popBackStack() },
                                onOpenPdfReport = { navController.navigate(PdfReportRoute()) },
                                onOpenImageOnMap = { navController.navigate(ImageOnMapRoute) },
                                onOpenOfflineMaps = { navigateTopLevel(OfflineMapsRoute) },
                                onOpenCloudSync = { navigateTopLevel(CloudSyncRoute) }
                            )
                        }
                        composable<ImageOnMapRoute> {
                            ImageOnMapScreen(
                                galleryViewModel = galleryViewModel,
                                cameraViewModel = cameraViewModel,
                                onBack = { navController.popBackStack() },
                                onInspectPhoto = { photoId ->
                                    navController.navigate(PhotoDetailRoute(photoId))
                                }
                            )
                        }
                        composable<PdfReportRoute> { backStack ->
                            val route = backStack.toRoute<PdfReportRoute>()
                            val initialIds = remember(route.initialPhotoIdsCsv) {
                                route.initialPhotoIdsCsv
                                    .split(",")
                                    .mapNotNull { it.trim().toLongOrNull() }
                                    .toSet()
                            }
                            PdfReportScreen(
                                galleryViewModel = galleryViewModel,
                                cameraViewModel = cameraViewModel,
                                initialSelectedPhotoIds = initialIds,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable<FileNameRoute> {
                            FileNameScreen(
                                viewModel = cameraViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable<OfflineMapsRoute> {
                            OfflineMapsScreen(
                                offlineViewModel = offlineMapsViewModel,
                                cameraViewModel = cameraViewModel
                            )
                        }
                        composable<CloudSyncRoute> {
                            CloudSyncRouteScreen(
                                photoRepository = container.photoRepository,
                                autoSyncCoordinator = container.autoSyncCoordinator,
                                accountStore = container.googleDriveAccountStore,
                                onOpenCamera = { navigateTopLevel(CameraRoute) }
                            )
                        }
                        composable<PhotoDetailRoute> { backStack ->
                            val route = backStack.toRoute<PhotoDetailRoute>()
                            PhotoDetailScreen(
                                photoId = route.photoId,
                                galleryViewModel = galleryViewModel,
                                offlineOnlyMapMode = stampConfig.offlineOnlyMapMode,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
