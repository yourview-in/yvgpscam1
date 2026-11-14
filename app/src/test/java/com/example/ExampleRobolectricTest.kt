package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.camera.PhotoStampRenderer
import com.example.data.local.AppDatabase
import com.example.data.model.CoordinateFormat
import com.example.data.model.GpsSignalState
import com.example.data.model.GpsTelemetry
import com.example.data.model.MapStyle
import com.example.data.model.StampAccentColor
import com.example.data.model.StampConfig
import com.example.data.model.StampFont
import com.example.data.model.StampPosition
import com.example.data.model.StampSize
import com.example.data.model.StampTemplate
import com.example.map.MiniMapTileRenderer
import com.example.map.OfflineMapRepository
import com.example.map.toGoogleMapType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [ShadowPdfDocument::class])
class ExampleRobolectricTest {

    @Test
    fun `app name matches YVGPSCAM`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("YVGPSCAM", appName)
    }

    @Test
    fun `telemetry formats decimal and dms coordinates accurately`() {
        val telemetry = GpsTelemetry(
            latitude = 37.422094,
            longitude = -122.084057,
            signalState = GpsSignalState.LOCKED
        )
        val dd = telemetry.formatCoordinates(CoordinateFormat.DECIMAL_DEGREES)
        val dms = telemetry.formatCoordinates(CoordinateFormat.DMS)
        assertTrue(dd.contains("37.422094° N"))
        assertTrue(dd.contains("122.084057° W"))
        assertTrue(dms.contains("37°25'"))
        assertEquals("NE", GpsTelemetry.azimuthToCardinal(45f))
    }

    @Test
    fun `offline map tile estimator and vector fallback work offline`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val offlineRepo = OfflineMapRepository(context, db.offlineRegionDao())

        val tiles = offlineRepo.estimateTilesForRegion(
            centerLat = 37.422094,
            centerLon = -122.084057,
            radiusKm = 1.0,
            zoomMin = 14,
            zoomMax = 15
        )
        assertTrue(tiles.isNotEmpty())

        // Write a dummy cached tile and verify disk detection
        offlineRepo.saveTileBytes(15, 5240, 12664, byteArrayOf(1, 2, 3, 4))
        assertTrue(offlineRepo.hasCachedTile(15, 5240, 12664))

        val miniMapRenderer = MiniMapTileRenderer(offlineRepo)
        for (style in listOf(MapStyle.STANDARD, MapStyle.SATELLITE, MapStyle.TERRAIN, MapStyle.HYBRID)) {
            val mapBmp = miniMapRenderer.renderMiniMapBitmap(
                latitude = 37.422094,
                longitude = -122.084057,
                mapStyle = style,
                offlineOnly = true
            )
            assertNotNull(mapBmp)
            assertEquals(260, mapBmp.width)
            mapBmp.recycle()
        }
    }

    @Test
    fun `photo stamp renderer stamps logo and telemetry across positions and styles`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val renderer = PhotoStampRenderer(context)
        val source = Bitmap.createBitmap(1080, 1440, Bitmap.Config.ARGB_8888)
        val miniMap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val telemetry = GpsTelemetry(
            latitude = 37.422094,
            longitude = -122.084057,
            altitudeMeters = 18.0,
            accuracyMeters = 3.5f,
            compassAzimuth = 120f,
            compassCardinal = "SE",
            addressLine = "1600 Amphitheatre Pkwy",
            cityState = "Mountain View, CA",
            country = "USA",
            signalState = GpsSignalState.LOCKED
        )

        val config = StampConfig(
            template = StampTemplate.TACTICAL_DARK,
            stampFont = StampFont.SPACE_GROTESK,
            accentColor = StampAccentColor.YV_GOLD,
            stampSize = StampSize.LARGE,
            stampPosition = StampPosition.TOP_RIGHT,
            customNote = "Field Check"
        )

        val stamped = renderer.stampBitmap(source, telemetry, config, miniMap)
        assertEquals(1080, stamped.width)
        assertEquals(1440, stamped.height)
        stamped.recycle()
        source.recycle()
        miniMap.recycle()
    }

    @Test
    fun `brand logo loads by default and gallery viewMode and dateFilter update`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val logoBmp = com.example.camera.LogoBitmapProvider.loadStampLogoBitmap(context, 128)
        assertEquals(128, logoBmp.width)
        logoBmp.recycle()

        val db = AppDatabase.getInstance(context)
        val photoRepo = com.example.data.local.PhotoRepository(context, db.capturedPhotoDao())
        val offlineRepo = OfflineMapRepository(context, db.offlineRegionDao())
        val galleryVm = com.example.ui.viewmodel.GalleryViewModel(
            context,
            photoRepo,
            MiniMapTileRenderer(offlineRepo),
            com.example.data.local.StampPreferencesRepository(context)
        )
        assertEquals(com.example.ui.viewmodel.GalleryViewMode.GRID, galleryVm.viewMode.value)
        galleryVm.toggleViewMode()
        assertEquals(com.example.ui.viewmodel.GalleryViewMode.LIST, galleryVm.viewMode.value)

        galleryVm.applyTodayFilter()
        assertEquals(com.example.ui.viewmodel.DateFilterPreset.TODAY, galleryVm.dateFilter.value.preset)
        assertNotNull(galleryVm.dateFilter.value.startMillis)
        galleryVm.clearDateFilter()
        assertEquals(com.example.ui.viewmodel.DateFilterPreset.ALL, galleryVm.dateFilter.value.preset)
    }

    @Test
    fun `stamp and logo opacity configuration renders and gallery multi-select updates state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val renderer = PhotoStampRenderer(context)
        val source = Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
        val miniMap = Bitmap.createBitmap(180, 180, Bitmap.Config.ARGB_8888)
        val telemetry = GpsTelemetry(
            latitude = 37.422094,
            longitude = -122.084057,
            altitudeMeters = 12.0,
            accuracyMeters = 4.0f,
            compassAzimuth = 90f,
            compassCardinal = "E",
            addressLine = "1600 Amphitheatre Pkwy",
            cityState = "Mountain View, CA",
            country = "USA",
            signalState = GpsSignalState.LOCKED
        )

        val translucentConfig = StampConfig(
            stampOpacity = 0.45f,
            logoOpacity = 0.50f
        )
        assertEquals(0.45f, translucentConfig.stampOpacity, 0.001f)
        assertEquals(0.50f, translucentConfig.logoOpacity, 0.001f)

        val stamped = renderer.stampBitmap(source, telemetry, translucentConfig, miniMap)
        assertEquals(800, stamped.width)
        assertEquals(1000, stamped.height)
        stamped.recycle()
        source.recycle()
        miniMap.recycle()

        val db = AppDatabase.getInstance(context)
        val photoRepo = com.example.data.local.PhotoRepository(context, db.capturedPhotoDao())
        val offlineRepo = OfflineMapRepository(context, db.offlineRegionDao())
        val galleryVm = com.example.ui.viewmodel.GalleryViewModel(
            context,
            photoRepo,
            MiniMapTileRenderer(offlineRepo),
            com.example.data.local.StampPreferencesRepository(context)
        )

        assertFalse(galleryVm.isSelectionMode.value)
        assertTrue(galleryVm.selectedPhotoIds.value.isEmpty())

        galleryVm.enterSelectionMode(101L)
        assertTrue(galleryVm.isSelectionMode.value)
        assertEquals(setOf(101L), galleryVm.selectedPhotoIds.value)

        galleryVm.togglePhotoSelection(202L)
        assertEquals(setOf(101L, 202L), galleryVm.selectedPhotoIds.value)

        galleryVm.togglePhotoSelection(101L)
        assertEquals(setOf(202L), galleryVm.selectedPhotoIds.value)

        galleryVm.clearSelection()
        assertFalse(galleryVm.isSelectionMode.value)
        assertTrue(galleryVm.selectedPhotoIds.value.isEmpty())
    }

    @Test
    fun `batch rename template formats date and coordinate placeholders accurately`() {
        val samplePhoto = com.example.data.model.CapturedPhotoEntity(
            id = 7L,
            mediaUri = "content://media/external/images/media/7",
            displayName = "IMG_001.jpg",
            capturedAtMillis = 1763141040000L, // Fixed deterministic UTC timestamp
            latitude = 37.422094,
            longitude = -122.084057,
            altitudeMeters = 18.0,
            accuracyMeters = 3.5f,
            compassAzimuth = 90f,
            compassCardinal = "E",
            addressLine = "1600 Amphitheatre Pkwy",
            cityState = "Mountain View, CA",
            country = "USA",
            customNote = "",
            templateName = "TACTICAL_DARK",
            width = 1080,
            height = 1440
        )

        val formatted = com.example.data.local.PhotoRepository.formatTemplateFilename(
            photo = samplePhoto,
            template = "Site_{date}_{lat}_{lon}_{city}_{index}",
            indexOneBased = 3
        )
        assertTrue(formatted.startsWith("Site_"))
        assertTrue(formatted.contains("37.4221N"))
        assertTrue(formatted.contains("122.0841W"))
        assertTrue(formatted.contains("Mountain_View_CA"))
        assertTrue(formatted.endsWith("_03.jpg"))
    }

    @Test
    fun `google drive backup repository builds compact metadata and parses drive file list`() {
        val photo = com.example.data.model.CapturedPhotoEntity(
            id = 12L,
            mediaUri = "content://media/external/images/media/12",
            displayName = "Site_Survey_12.jpg",
            capturedAtMillis = 1763141040000L,
            latitude = 37.422094,
            longitude = -122.084057,
            altitudeMeters = 24.5,
            accuracyMeters = 2.8f,
            compassAzimuth = 135f,
            compassCardinal = "SE",
            addressLine = "1600 Amphitheatre Pkwy",
            cityState = "Mountain View, CA",
            country = "USA",
            customNote = "Tower inspection",
            templateName = "TACTICAL_DARK",
            width = 1080,
            height = 1440
        )

        val backupId = com.example.data.cloud.GoogleDriveBackupRepository
            .buildDeterministicBackupId(photo.capturedAtMillis, photo.displayName)
        val metaJson = com.example.data.cloud.GoogleDriveBackupRepository
            .buildUploadMetadataJson("folder_abc", photo.displayName, backupId, photo)

        assertEquals(photo.displayName, metaJson.getString("name"))
        assertEquals("folder_abc", metaJson.getJSONArray("parents").getString(0))
        val props = metaJson.getJSONObject("appProperties")
        assertEquals(backupId, props.getString("bid"))
        assertEquals("Mountain View, CA", props.getString("city"))

        val fakeResponse = org.json.JSONObject().apply {
            put(
                "files",
                org.json.JSONArray().put(
                    org.json.JSONObject().apply {
                        put("id", "drive_file_99")
                        put("name", photo.displayName)
                        put("size", "482910")
                        put("appProperties", props)
                    }
                )
            )
        }.toString()

        val parsed = com.example.data.cloud.GoogleDriveBackupRepository.parseBackupsResponse(fakeResponse)
        assertEquals(1, parsed.size)
        val item = parsed.first()
        assertEquals("drive_file_99", item.driveFileId)
        assertEquals(backupId, item.backupId)
        assertEquals(37.422094, item.latitude!!, 0.0001)
        assertEquals(-122.084057, item.longitude!!, 0.0001)
        assertEquals("Mountain View, CA", item.cityState)
        assertEquals("Tower inspection", item.customNote)
    }

    @Test
    fun `camera viewmodel toggles flash mode and front back lens cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val offlineRepo = OfflineMapRepository(context, db.offlineRegionDao())
        val stampPrefs = com.example.data.local.StampPreferencesRepository(context)
        val photoRepo = com.example.data.local.PhotoRepository(context, db.capturedPhotoDao())
        val vm = com.example.ui.viewmodel.CameraViewModel(
            locationManager = com.example.sensors.LocationTelemetryManager(context),
            stampPrefs = stampPrefs,
            photoRepository = photoRepo,
            miniMapRenderer = MiniMapTileRenderer(offlineRepo),
            mediaStoreSaver = com.example.camera.MediaStoreSaver(context, PhotoStampRenderer(context))
        )

        assertEquals(androidx.camera.core.ImageCapture.FLASH_MODE_OFF, vm.hudState.value.flashMode)
        vm.cycleFlashMode()
        assertEquals(androidx.camera.core.ImageCapture.FLASH_MODE_ON, vm.hudState.value.flashMode)
        vm.cycleFlashMode()
        assertEquals(androidx.camera.core.ImageCapture.FLASH_MODE_AUTO, vm.hudState.value.flashMode)
        vm.cycleFlashMode()
        assertEquals(androidx.camera.core.ImageCapture.FLASH_MODE_OFF, vm.hudState.value.flashMode)

        assertEquals(androidx.camera.core.CameraSelector.LENS_FACING_BACK, vm.hudState.value.lensFacing)
        vm.toggleLensFacing()
        assertEquals(androidx.camera.core.CameraSelector.LENS_FACING_FRONT, vm.hudState.value.lensFacing)
        vm.toggleLensFacing()
        assertEquals(androidx.camera.core.CameraSelector.LENS_FACING_BACK, vm.hudState.value.lensFacing)

        assertFalse(vm.hudState.value.isFullScreenCamera)
        vm.toggleFullScreenCamera()
        assertTrue(vm.hudState.value.isFullScreenCamera)
        vm.setFullScreenCamera(false)
        assertFalse(vm.hudState.value.isFullScreenCamera)
    }

    @Test
    fun `google drive account store persists login and auto sync skip sets`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = com.example.data.cloud.GoogleDriveAccountStore(context)
        store.clearAccount()
        store.saveAccount("field.user@gmail.com", "Field User")
        store.markSkippedForAutoBackup("b_100_photo")
        store.markSkippedForAutoRestore("b_200_photo")

        val profile = store.accountFlow.first()
        assertTrue(profile.isSignedIn)
        assertEquals("field.user@gmail.com", profile.email)
        assertTrue(store.skipAutoBackupIdsFlow.first().contains("b_100_photo"))
        assertTrue(store.skipAutoRestoreIdsFlow.first().contains("b_200_photo"))

        store.unmarkSkippedForAutoBackup("b_100_photo")
        assertFalse(store.skipAutoBackupIdsFlow.first().contains("b_100_photo"))

        store.clearAccount()
        assertFalse(store.accountFlow.first().isSignedIn)
    }

    @Test
    fun `drive folder uses YVGPSCam and google maps helpers map styles and api keys safely`() {
        assertEquals(
            "YVGPSCam",
            com.example.data.cloud.GoogleDriveBackupRepository.BACKUP_FOLDER_NAME
        )

        assertEquals(
            com.google.maps.android.compose.MapType.NORMAL,
            MapStyle.STANDARD.toGoogleMapType()
        )
        assertEquals(
            com.google.maps.android.compose.MapType.SATELLITE,
            MapStyle.SATELLITE.toGoogleMapType()
        )
        assertEquals(
            com.google.maps.android.compose.MapType.TERRAIN,
            MapStyle.TERRAIN.toGoogleMapType()
        )
        assertEquals(
            com.google.maps.android.compose.MapType.HYBRID,
            MapStyle.HYBRID.toGoogleMapType()
        )

        assertFalse(com.example.map.GoogleMapsConfig.isApiKeyConfigured(""))
        assertFalse(com.example.map.GoogleMapsConfig.isApiKeyConfigured("MY_MAPS_API_KEY"))
        assertFalse(com.example.map.GoogleMapsConfig.isApiKeyConfigured("null"))
        assertTrue(com.example.map.GoogleMapsConfig.isApiKeyConfigured("AIzaSyRealDemoKeyValue"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val offlineRepo = OfflineMapRepository(context, db.offlineRegionDao())
        val galleryVm = com.example.ui.viewmodel.GalleryViewModel(
            context,
            com.example.data.local.PhotoRepository(context, db.capturedPhotoDao()),
            MiniMapTileRenderer(offlineRepo),
            com.example.data.local.StampPreferencesRepository(context)
        )
        galleryVm.setViewMode(com.example.ui.viewmodel.GalleryViewMode.MAP)
        assertEquals(com.example.ui.viewmodel.GalleryViewMode.MAP, galleryVm.viewMode.value)
        galleryVm.toggleViewMode()
        assertEquals(com.example.ui.viewmodel.GalleryViewMode.GRID, galleryVm.viewMode.value)
    }

    @Test
    fun `device capture orientation resolver detects portrait landscape and applies hysteresis`() {
        var current = com.example.ui.screens.DeviceCaptureOrientation.PORTRAIT
        current = com.example.ui.screens.resolveDeviceCaptureOrientation(5, current)
        assertEquals(com.example.ui.screens.DeviceCaptureOrientation.PORTRAIT, current)
        assertFalse(current.isLandscape)

        // Transition zone (45 deg) retains portrait cleanly without jitter
        current = com.example.ui.screens.resolveDeviceCaptureOrientation(45, current)
        assertEquals(com.example.ui.screens.DeviceCaptureOrientation.PORTRAIT, current)

        // Tilted to 90 deg right -> Landscape
        current = com.example.ui.screens.resolveDeviceCaptureOrientation(90, current)
        assertEquals(com.example.ui.screens.DeviceCaptureOrientation.LANDSCAPE_RIGHT, current)
        assertTrue(current.isLandscape)
        assertEquals("LANDSCAPE", current.label)

        // Transition zone retains landscape cleanly
        current = com.example.ui.screens.resolveDeviceCaptureOrientation(45, current)
        assertEquals(com.example.ui.screens.DeviceCaptureOrientation.LANDSCAPE_RIGHT, current)

        // Tilted to 270 deg left -> Landscape
        current = com.example.ui.screens.resolveDeviceCaptureOrientation(270, current)
        assertEquals(com.example.ui.screens.DeviceCaptureOrientation.LANDSCAPE_LEFT, current)
        assertTrue(current.isLandscape)

        // Unknown sensor reading (-1) preserves current orientation
        current = com.example.ui.screens.resolveDeviceCaptureOrientation(-1, current)
        assertEquals(com.example.ui.screens.DeviceCaptureOrientation.LANDSCAPE_LEFT, current)
    }

    @Test
    fun `brand logo remains permanently enabled and batch delete removes selected photos`(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val stampPrefs = com.example.data.local.StampPreferencesRepository(context)
        stampPrefs.updateConfig { it.copy(showLogo = false, logoOpacity = 0.85f) }
        val persistedConfig = stampPrefs.stampConfig.first()
        assertTrue(persistedConfig.showLogo)
        assertEquals(0.85f, persistedConfig.logoOpacity, 0.01f)

        val renderer = PhotoStampRenderer(context)
        val source = Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
        val telemetry = GpsTelemetry(
            latitude = 37.422094,
            longitude = -122.084057,
            signalState = GpsSignalState.LOCKED
        )
        val stamped = renderer.stampBitmap(source, telemetry, StampConfig(showLogo = false), null)
        assertEquals(800, stamped.width)
        stamped.recycle()
        source.recycle()

        val db = AppDatabase.getInstance(context)
        val dao = db.capturedPhotoDao()
        val photoRepo = com.example.data.local.PhotoRepository(context, dao)

        val id1 = photoRepo.insertPhoto(
            com.example.data.model.CapturedPhotoEntity(
                mediaUri = "file:///tmp/test_del_1.jpg",
                displayName = "YV_Del_1.jpg",
                capturedAtMillis = 1763141041000L,
                latitude = 37.42,
                longitude = -122.08,
                altitudeMeters = 10.0,
                accuracyMeters = 3f,
                compassAzimuth = 0f,
                compassCardinal = "N",
                addressLine = "Addr 1",
                cityState = "City 1",
                country = "USA",
                customNote = "",
                templateName = "TACTICAL_DARK",
                width = 800,
                height = 1000
            )
        )
        val id2 = photoRepo.insertPhoto(
            com.example.data.model.CapturedPhotoEntity(
                mediaUri = "file:///tmp/test_del_2.jpg",
                displayName = "YV_Del_2.jpg",
                capturedAtMillis = 1763141042000L,
                latitude = 37.43,
                longitude = -122.09,
                altitudeMeters = 12.0,
                accuracyMeters = 3f,
                compassAzimuth = 90f,
                compassCardinal = "E",
                addressLine = "Addr 2",
                cityState = "City 2",
                country = "USA",
                customNote = "",
                templateName = "TACTICAL_DARK",
                width = 800,
                height = 1000
            )
        )
        val id3 = photoRepo.insertPhoto(
            com.example.data.model.CapturedPhotoEntity(
                mediaUri = "file:///tmp/test_keep_3.jpg",
                displayName = "YV_Keep_3.jpg",
                capturedAtMillis = 1763141043000L,
                latitude = 37.44,
                longitude = -122.10,
                altitudeMeters = 15.0,
                accuracyMeters = 3f,
                compassAzimuth = 180f,
                compassCardinal = "S",
                addressLine = "Addr 3",
                cityState = "City 3",
                country = "USA",
                customNote = "",
                templateName = "TACTICAL_DARK",
                width = 800,
                height = 1000
            )
        )

        val stored = dao.getAllPhotos().first()
        val toDelete = stored.filter { it.id == id1 || it.id == id2 }
        assertEquals(2, toDelete.size)

        val result = photoRepo.deletePhotos(toDelete)
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())

        val remainingIds = dao.getAllPhotos().first().map { it.id }.toSet()
        assertFalse(remainingIds.contains(id1))
        assertFalse(remainingIds.contains(id2))
        assertTrue(remainingIds.contains(id3))

        // Clean up remaining test row
        photoRepo.deletePhotos(dao.getAllPhotos().first().filter { it.id == id3 })
        Unit
    }

    @Test
    fun `main camera route locks portrait orientation while secondary screens stay rotatable`() {
        assertEquals(
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            resolveScreenOrientationForRoute(isMainCameraPage = true)
        )
        assertEquals(
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            resolveScreenOrientationForRoute(isMainCameraPage = false)
        )
    }

    @Test
    fun `plus code country flag emoji and zoom bounds compute accurately`() {
        val telemetry = GpsTelemetry(
            latitude = 37.422094,
            longitude = -122.084057,
            country = "USA",
            countryCode = "US",
            magneticFieldMicroTesla = 418f,
            signalState = GpsSignalState.LOCKED
        )
        val plus = telemetry.plusCode
        assertNotNull(plus)
        assertTrue(plus!!.contains("+"))
        assertTrue(plus.length >= 10)
        assertEquals("🇺🇸", telemetry.countryFlagEmoji)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val offlineRepo = OfflineMapRepository(context, db.offlineRegionDao())
        val vm = com.example.ui.viewmodel.CameraViewModel(
            locationManager = com.example.sensors.LocationTelemetryManager(context),
            stampPrefs = com.example.data.local.StampPreferencesRepository(context),
            photoRepository = com.example.data.local.PhotoRepository(context, db.capturedPhotoDao()),
            miniMapRenderer = MiniMapTileRenderer(offlineRepo),
            mediaStoreSaver = com.example.camera.MediaStoreSaver(context, PhotoStampRenderer(context))
        )
        vm.updateZoomBounds(0.5f, 8f)
        vm.setZoomRatio(0.5f)
        assertEquals(0.5f, vm.hudState.value.minZoomRatio, 0.01f)
        assertEquals(0.5f, vm.hudState.value.zoomRatio, 0.01f)
    }

    @Test
    fun `custom filename builder formats selected parts and sequence number increments cleanly`(): Unit = runBlocking {
        val telemetry = GpsTelemetry(
            latitude = 64.545127,
            longitude = -19.091384,
            cityState = "Southern Region",
            country = "Iceland",
            addressLine = "Massa Marittima",
            signalState = GpsSignalState.LOCKED
        )
        val config = StampConfig(
            customFileNameEnabled = true,
            fnIncludeDateTime = true,
            fnIncludeSequence = true,
            photoSequenceNumber = 7,
            fnIncludeCustom1 = true,
            fnCustomText1 = "YVGPSCAM",
            fnIncludeCustom2 = true,
            fnCustomText2 = "SurveyA",
            fnIncludeFullAddress = true,
            fnIncludeLatLon = true,
            fnIncludePlusCode = true
        )
        val generated = com.example.camera.MediaStoreSaver.buildCustomFileName(
            config = config,
            telemetry = telemetry,
            timestampMillis = 1763141040000L
        )
        assertTrue(generated.endsWith(".jpg"))
        assertTrue(generated.contains("_7_"))
        assertTrue(generated.contains("YVGPSCAM"))
        assertTrue(generated.contains("SurveyA"))
        assertTrue(generated.contains("64.545127_-19.091384"))

        val fallbackConfig = config.copy(customFileNameEnabled = false)
        val defaultName = com.example.camera.MediaStoreSaver.buildCustomFileName(
            config = fallbackConfig,
            telemetry = telemetry,
            timestampMillis = 1763141040000L
        )
        assertTrue(defaultName.startsWith("YVGPSCAM_"))
        assertTrue(defaultName.endsWith(".jpg"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val stampPrefs = com.example.data.local.StampPreferencesRepository(context)
        val beforeSeq = stampPrefs.stampConfig.first().photoSequenceNumber
        stampPrefs.incrementSequenceNumber()
        val afterSeq = stampPrefs.stampConfig.first().photoSequenceNumber
        assertEquals(beforeSeq + 1, afterSeq)

        assertTrue(telemetry.formatTimeZoneOffset(1763141040000L).startsWith("GMT "))
        Unit
    }

    @Test
    fun `all 6 layout templates and QR helper render without error`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val renderer = PhotoStampRenderer(context)
        val source = Bitmap.createBitmap(1080, 1440, Bitmap.Config.ARGB_8888)
        val miniMap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val telemetry = GpsTelemetry(
            latitude = 17.385044,
            longitude = 78.486671,
            altitudeMeters = 505.0,
            accuracyMeters = 3.2f,
            compassAzimuth = 225f,
            compassCardinal = "SW",
            addressLine = "Nagole - Bandlaguda Rd, Hyderabad",
            cityState = "Hyderabad, Telangana",
            country = "India",
            signalState = GpsSignalState.LOCKED
        )

        val qrUrl = com.example.util.QrCodeHelper.buildLocationQrUrl(telemetry.latitude, telemetry.longitude)
        assertTrue(qrUrl.contains("17.385044"))
        val qrBmp = com.example.util.QrCodeHelper.generateQrBitmap(qrUrl, 160)
        assertEquals(160, qrBmp.width)
        qrBmp.recycle()

        for (layout in com.example.data.model.StampLayoutTemplate.entries) {
            val cfg = StampConfig(
                layoutTemplate = layout,
                reportingLabel = "Checked In",
                personName = "Surveyor YV",
                showPersonName = true,
                showCameraIndicator = true
            )
            val stamped = renderer.stampBitmap(source, telemetry, cfg, miniMap, isFrontCamera = true)
            assertEquals(1080, stamped.width)
            assertEquals(1440, stamped.height)
            stamped.recycle()
        }
        source.recycle()
        miniMap.recycle()
    }

    @Test
    fun `custom filename ordering manual locations settings persistence and hidden ads work properly`(): Unit = runBlocking {
        assertFalse(com.example.ui.components.AdSlotConfig.AD_SLOT_ENABLED)

        val telemetry = GpsTelemetry(
            latitude = 17.385044,
            longitude = 78.486671,
            cityState = "Hyderabad",
            country = "India",
            signalState = GpsSignalState.LOCKED
        )
        val customOrder = listOf(
            com.example.data.model.FileNameField.CUSTOM_3,
            com.example.data.model.FileNameField.NOTE,
            com.example.data.model.FileNameField.TIME_ZONE,
            com.example.data.model.FileNameField.SEQUENCE
        )
        val orderedCfg = StampConfig(
            customFileNameEnabled = true,
            fnIncludeDateTime = false,
            fnIncludeSequence = true,
            photoSequenceNumber = 42,
            fnIncludeCustom1 = false,
            fnIncludeCustom3 = true,
            fnCustomText3 = "SubstationB",
            fnIncludeLatLon = false,
            fnIncludeTimeZone = true,
            fnIncludeNote = true,
            customNote = "Inspected",
            fnFieldOrderSerialized = StampConfig.serializeFnFieldOrder(customOrder)
        )
        val name = com.example.camera.MediaStoreSaver.buildCustomFileName(orderedCfg, telemetry, 1763141040000L)
        assertTrue(name.startsWith("SubstationB_Inspected_"))
        assertTrue(name.endsWith("_42.jpg"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val stampPrefs = com.example.data.local.StampPreferencesRepository(context)
        val manualList = listOf(
            com.example.data.model.ManualLocationItem("id_1", "Plant Gate", "Industrial Area", 17.40, 78.50)
        )
        stampPrefs.updateConfig {
            it.copy(
                appThemeMode = com.example.data.model.AppThemeMode.LIGHT,
                photoQuality = com.example.data.model.PhotoQualityLevel.MEDIUM,
                videoQuality = com.example.data.model.VideoQualityLevel.UHD_4K,
                volumeButtonAction = com.example.data.model.VolumeButtonAction.ZOOM,
                screenTapAction = com.example.data.model.ScreenTapAction.CAPTURE,
                stampOrientation = com.example.data.model.StampOrientationMode.LANDSCAPE,
                autoSaveOriginalPhoto = true,
                showLogo = false, // Must remain locked true
                useManualLocation = true,
                selectedManualLocationId = "id_1",
                manualLocationsJson = StampConfig.serializeManualLocations(manualList)
            )
        }
        val persisted = stampPrefs.stampConfig.first()
        assertEquals(com.example.data.model.AppThemeMode.LIGHT, persisted.appThemeMode)
        assertEquals(com.example.data.model.PhotoQualityLevel.MEDIUM, persisted.photoQuality)
        assertEquals(com.example.data.model.VideoQualityLevel.UHD_4K, persisted.videoQuality)
        assertEquals(com.example.data.model.VolumeButtonAction.ZOOM, persisted.volumeButtonAction)
        assertEquals(com.example.data.model.ScreenTapAction.CAPTURE, persisted.screenTapAction)
        assertEquals(com.example.data.model.StampOrientationMode.LANDSCAPE, persisted.stampOrientation)
        assertTrue(persisted.autoSaveOriginalPhoto)
        assertTrue(persisted.showLogo)
        assertEquals("Plant Gate", persisted.selectedManualLocation()?.shortName)

        // Verify PDF report creation
        val samplePhoto = com.example.data.model.CapturedPhotoEntity(
            id = 1L,
            mediaUri = "file:///tmp/non_existent.jpg",
            displayName = "YV_Site_1.jpg",
            capturedAtMillis = 1763141040000L,
            latitude = 17.385044,
            longitude = 78.486671,
            altitudeMeters = 505.0,
            accuracyMeters = 3.2f,
            compassAzimuth = 90f,
            compassCardinal = "E",
            addressLine = "Nagole Rd",
            cityState = "Hyderabad",
            country = "India",
            customNote = "Pole check",
            templateName = "ADVANCE",
            width = 1080,
            height = 1440
        )
        val pdfResult = com.example.util.PdfReportGenerator.generateReport(
            context = context,
            title = "Site Audit",
            inspectorName = "Tester",
            summaryNote = "OK",
            photos = listOf(samplePhoto)
        )
        assertTrue(pdfResult.exceptionOrNull()?.stackTraceToString() ?: "PDF failed", pdfResult.isSuccess)
        val report = pdfResult.getOrNull()!!
        assertTrue(report.file.exists())
        assertTrue(report.fileSizeBytes > 0L)
        assertEquals(1, report.pageCount)
        report.file.delete()
        Unit
    }
}

@Implements(android.graphics.pdf.PdfDocument::class)
class ShadowPdfDocument {
    private var renderedPages = 0

    @Implementation
    fun __constructor__() {}

    @Implementation
    fun startPage(pageInfo: android.graphics.pdf.PdfDocument.PageInfo): android.graphics.pdf.PdfDocument.Page {
        val bitmap = Bitmap.createBitmap(
            pageInfo.pageWidth,
            pageInfo.pageHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bitmap)
        val ctor = android.graphics.pdf.PdfDocument.Page::class.java.declaredConstructors.first {
            it.parameterCount == 2
        }
        ctor.isAccessible = true
        renderedPages++
        return ctor.newInstance(canvas, pageInfo) as android.graphics.pdf.PdfDocument.Page
    }

    @Implementation
    fun finishPage(page: android.graphics.pdf.PdfDocument.Page) {}

    @Implementation
    fun writeTo(out: java.io.OutputStream) {
        out.write("%PDF-1.4\n%%YVGPSCAM Robolectric Test Pages: $renderedPages\n%%EOF\n".toByteArray())
    }

    @Implementation
    fun close() {}
}
