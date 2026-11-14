package com.example.di

import android.content.Context
import com.example.camera.MediaStoreSaver
import com.example.camera.PhotoStampRenderer
import com.example.data.cloud.CloudMediaGateway
import com.example.data.cloud.GoogleDriveAccountStore
import com.example.data.cloud.GoogleDriveAutoSyncCoordinator
import com.example.data.cloud.GoogleDriveBackupRepository
import com.example.data.cloud.NetworkConnectivityObserver
import com.example.data.local.AppDatabase
import com.example.data.local.PhotoRepository
import com.example.data.local.StampPreferencesRepository
import com.example.map.MiniMapTileRenderer
import com.example.map.OfflineMapRepository
import com.example.sensors.LocationTelemetryManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class YvGpsCamContainer(context: Context) {
    private val appContext = context.applicationContext

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }
    val stampPreferencesRepository: StampPreferencesRepository by lazy {
        StampPreferencesRepository(appContext)
    }
    val photoRepository: PhotoRepository by lazy {
        PhotoRepository(appContext, database.capturedPhotoDao())
    }
    val offlineMapRepository: OfflineMapRepository by lazy {
        OfflineMapRepository(appContext, database.offlineRegionDao(), httpClient)
    }
    val miniMapTileRenderer: MiniMapTileRenderer by lazy {
        MiniMapTileRenderer(offlineMapRepository)
    }
    val locationTelemetryManager: LocationTelemetryManager by lazy {
        LocationTelemetryManager(appContext)
    }
    val photoStampRenderer: PhotoStampRenderer by lazy {
        PhotoStampRenderer(appContext)
    }
    val mediaStoreSaver: MediaStoreSaver by lazy {
        MediaStoreSaver(appContext, photoStampRenderer)
    }
    val googleDriveAccountStore: GoogleDriveAccountStore by lazy {
        GoogleDriveAccountStore(appContext)
    }
    val googleDriveBackupRepository: GoogleDriveBackupRepository by lazy {
        GoogleDriveBackupRepository(httpClient)
    }
    val networkConnectivityObserver: NetworkConnectivityObserver by lazy {
        NetworkConnectivityObserver(appContext)
    }
    val cloudMediaGateway: CloudMediaGateway by lazy {
        CloudMediaGateway(appContext)
    }
    val autoSyncCoordinator: GoogleDriveAutoSyncCoordinator by lazy {
        GoogleDriveAutoSyncCoordinator(
            appContext = appContext,
            accountStore = googleDriveAccountStore,
            driveRepository = googleDriveBackupRepository,
            photoRepository = photoRepository,
            mediaGateway = cloudMediaGateway,
            connectivityObserver = networkConnectivityObserver
        )
    }
}
