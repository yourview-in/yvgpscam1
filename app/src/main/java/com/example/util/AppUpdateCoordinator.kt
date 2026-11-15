package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.example.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

data class AppUpdateInfoState(
    val currentVersionName: String,
    val currentVersionCode: Int,
    val availableVersionCode: Int,
    val availableVersionLabel: String,
    val releaseNotes: String = "Includes performance upgrades, sharper map rendering, and latest GPS telemetry improvements.",
    val isImmediateRequired: Boolean = false,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgressPercent: Int? = null,
    val storeUrl: String? = null,
    val playAppUpdateInfo: AppUpdateInfo? = null
)

class AppUpdateCoordinator(private val appContext: Context) {

    private val prefs by lazy {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val appUpdateManager: AppUpdateManager? by lazy {
        runCatching { AppUpdateManagerFactory.create(appContext) }.getOrNull()
    }

    private val _updateState = MutableStateFlow<AppUpdateInfoState?>(null)
    val updateState: StateFlow<AppUpdateInfoState?> = _updateState.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private var listenerRegistered = false

    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val total = state.totalBytesToDownload().coerceAtLeast(1L)
                val pct = ((state.bytesDownloaded() * 100L) / total).toInt().coerceIn(0, 100)
                _updateState.value = _updateState.value?.copy(
                    isDownloading = true,
                    downloadProgressPercent = pct
                )
            }
            InstallStatus.DOWNLOADED -> {
                _updateState.value = (_updateState.value ?: AppUpdateInfoState(
                    currentVersionName = BuildConfig.VERSION_NAME,
                    currentVersionCode = BuildConfig.VERSION_CODE,
                    availableVersionCode = BuildConfig.VERSION_CODE + 1,
                    availableVersionLabel = "Latest",
                    isDownloaded = true
                )).copy(
                    isDownloading = false,
                    downloadProgressPercent = 100,
                    isDownloaded = true
                )
            }
            InstallStatus.INSTALLED -> {
                _updateState.value = null
                unregisterListenerIfNeeded()
            }
            InstallStatus.FAILED, InstallStatus.CANCELED -> {
                _updateState.value = _updateState.value?.copy(
                    isDownloading = false,
                    downloadProgressPercent = null
                )
            }
            else -> Unit
        }
    }

    fun checkForUpdates(
        forceShowIfDismissed: Boolean = false,
        onUpToDate: () -> Unit = {}
    ) {
        val manager = appUpdateManager
        if (manager == null) {
            onUpToDate()
            return
        }

        _isChecking.value = true
        runCatching {
            manager.appUpdateInfo
                .addOnSuccessListener { info ->
                    _isChecking.value = false
                    if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        _updateState.value = AppUpdateInfoState(
                            currentVersionName = BuildConfig.VERSION_NAME,
                            currentVersionCode = BuildConfig.VERSION_CODE,
                            availableVersionCode = info.availableVersionCode(),
                            availableVersionLabel = "Build ${info.availableVersionCode()}",
                            isDownloaded = true,
                            playAppUpdateInfo = info
                        )
                        return@addOnSuccessListener
                    }

                    if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                        val availableCode = info.availableVersionCode()
                        val staleness = info.clientVersionStalenessDays() ?: 0
                        val immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                        val flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                        if (immediateAllowed || flexibleAllowed) {
                            val dismissedCode = prefs.getInt(KEY_DISMISSED_VERSION_CODE, -1)
                            val isMandatory = staleness >= MANDATORY_STALENESS_DAYS && immediateAllowed
                            if (forceShowIfDismissed || isMandatory || dismissedCode != availableCode) {
                                _updateState.value = AppUpdateInfoState(
                                    currentVersionName = BuildConfig.VERSION_NAME,
                                    currentVersionCode = BuildConfig.VERSION_CODE,
                                    availableVersionCode = availableCode,
                                    availableVersionLabel = "Build $availableCode",
                                    isImmediateRequired = isMandatory,
                                    playAppUpdateInfo = info
                                )
                            }
                            return@addOnSuccessListener
                        }
                    }

                    if (forceShowIfDismissed) {
                        onUpToDate()
                    }
                }
                .addOnFailureListener {
                    _isChecking.value = false
                    if (forceShowIfDismissed) {
                        onUpToDate()
                    }
                }
        }.onFailure {
            _isChecking.value = false
            if (forceShowIfDismissed) {
                onUpToDate()
            }
        }
    }

    fun onResumeCheck(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val manager = appUpdateManager ?: return
        runCatching {
            manager.appUpdateInfo.addOnSuccessListener { info ->
                when {
                    info.installStatus() == InstallStatus.DOWNLOADED -> {
                        _updateState.value = (_updateState.value ?: AppUpdateInfoState(
                            currentVersionName = BuildConfig.VERSION_NAME,
                            currentVersionCode = BuildConfig.VERSION_CODE,
                            availableVersionCode = info.availableVersionCode(),
                            availableVersionLabel = "Build ${info.availableVersionCode()}",
                            playAppUpdateInfo = info
                        )).copy(
                            isDownloaded = true,
                            isDownloading = false,
                            playAppUpdateInfo = info
                        )
                    }
                    info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        runCatching {
                            manager.startUpdateFlowForResult(
                                info,
                                launcher,
                                AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)
                            )
                        }
                    }
                }
            }
        }
    }

    fun startUpdate(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val current = _updateState.value ?: return
        val manager = appUpdateManager
        val playInfo = current.playAppUpdateInfo

        if (current.isDownloaded && manager != null) {
            completeFlexibleUpdate()
            return
        }

        if (manager != null && playInfo != null) {
            val updateType = when {
                current.isImmediateRequired && playInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                playInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                else -> AppUpdateType.IMMEDIATE
            }
            if (updateType == AppUpdateType.FLEXIBLE && !listenerRegistered) {
                manager.registerListener(installStateListener)
                listenerRegistered = true
            }
            val started = runCatching {
                manager.startUpdateFlowForResult(
                    playInfo,
                    launcher,
                    AppUpdateOptions.defaultOptions(updateType)
                )
            }.getOrDefault(false)
            if (started) {
                if (updateType == AppUpdateType.FLEXIBLE) {
                    _updateState.value = current.copy(isDownloading = true)
                }
                return
            }
        }

        openStoreListing(activity, current.storeUrl)
    }

    fun completeFlexibleUpdate() {
        runCatching { appUpdateManager?.completeUpdate() }
        _updateState.value = null
        unregisterListenerIfNeeded()
    }

    fun dismissUpdateForNow() {
        val current = _updateState.value ?: return
        if (current.isImmediateRequired) return
        prefs.edit().putInt(KEY_DISMISSED_VERSION_CODE, current.availableVersionCode).apply()
        _updateState.value = null
    }

    fun evaluateManifestJson(
        manifestJson: String,
        currentVersionCode: Int = BuildConfig.VERSION_CODE,
        currentVersionName: String = BuildConfig.VERSION_NAME,
        forceShow: Boolean = false
    ): AppUpdateInfoState? {
        return runCatching {
            val root = JSONObject(manifestJson)
            val remoteCode = root.optInt("versionCode", 0)
            if (remoteCode <= currentVersionCode) return null
            val mandatory = root.optBoolean("mandatory", false)
            val dismissedCode = prefs.getInt(KEY_DISMISSED_VERSION_CODE, -1)
            if (!forceShow && !mandatory && dismissedCode == remoteCode) return null

            val state = AppUpdateInfoState(
                currentVersionName = currentVersionName,
                currentVersionCode = currentVersionCode,
                availableVersionCode = remoteCode,
                availableVersionLabel = root.optString("versionName", "Build $remoteCode").ifBlank { "Build $remoteCode" },
                releaseNotes = root.optString(
                    "releaseNotes",
                    "Includes performance upgrades, sharper map rendering, and latest GPS telemetry improvements."
                ),
                isImmediateRequired = mandatory,
                storeUrl = root.optString("storeUrl", "").ifBlank { null }
            )
            _updateState.value = state
            state
        }.getOrNull()
    }

    fun openStoreListing(context: Context = appContext, customUrl: String? = null) {
        val targetUrl = customUrl?.takeIf { it.isNotBlank() }
        val playMarketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(targetUrl ?: "market://details?id=${appContext.packageName}")
        ).apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching { context.startActivity(playMarketIntent) }.isSuccess
        if (!launched) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(targetUrl ?: "https://play.google.com/store/apps/details?id=${appContext.packageName}")
            ).apply {
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(webIntent) }
        }
    }

    fun dispose() {
        unregisterListenerIfNeeded()
    }

    private fun unregisterListenerIfNeeded() {
        if (listenerRegistered) {
            runCatching { appUpdateManager?.unregisterListener(installStateListener) }
            listenerRegistered = false
        }
    }

    companion object {
        private const val PREFS_NAME = "yvgpscam_updates"
        private const val KEY_DISMISSED_VERSION_CODE = "dismissed_version_code"
        private const val MANDATORY_STALENESS_DAYS = 7
    }
}
