package com.example.data.cloud

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.driveAccountDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "yv_google_drive_account"
)

data class GoogleAccountProfile(
    val isSignedIn: Boolean = false,
    val email: String = "",
    val displayName: String = ""
)

class GoogleDriveAccountStore(private val context: Context) {

    private object Keys {
        val IS_SIGNED_IN = booleanPreferencesKey("is_signed_in")
        val EMAIL = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val SKIP_AUTO_BACKUP_IDS = stringSetPreferencesKey("skip_auto_backup_ids")
        val SKIP_AUTO_RESTORE_IDS = stringSetPreferencesKey("skip_auto_restore_ids")
    }

    val accountFlow: Flow<GoogleAccountProfile> = context.driveAccountDataStore.data.map { prefs ->
        val signedIn = prefs[Keys.IS_SIGNED_IN] ?: false
        GoogleAccountProfile(
            isSignedIn = signedIn,
            email = prefs[Keys.EMAIL].orEmpty(),
            displayName = prefs[Keys.DISPLAY_NAME].orEmpty()
        )
    }

    val skipAutoBackupIdsFlow: Flow<Set<String>> = context.driveAccountDataStore.data.map { prefs ->
        prefs[Keys.SKIP_AUTO_BACKUP_IDS].orEmpty()
    }

    val skipAutoRestoreIdsFlow: Flow<Set<String>> = context.driveAccountDataStore.data.map { prefs ->
        prefs[Keys.SKIP_AUTO_RESTORE_IDS].orEmpty()
    }

    suspend fun saveAccount(email: String, displayName: String) {
        context.driveAccountDataStore.edit { prefs ->
            prefs[Keys.IS_SIGNED_IN] = true
            prefs[Keys.EMAIL] = email
            prefs[Keys.DISPLAY_NAME] = displayName
        }
    }

    suspend fun markSkippedForAutoBackup(backupId: String) {
        context.driveAccountDataStore.edit { prefs ->
            val current = prefs[Keys.SKIP_AUTO_BACKUP_IDS].orEmpty()
            prefs[Keys.SKIP_AUTO_BACKUP_IDS] = current + backupId
        }
    }

    suspend fun unmarkSkippedForAutoBackup(backupId: String) {
        context.driveAccountDataStore.edit { prefs ->
            val current = prefs[Keys.SKIP_AUTO_BACKUP_IDS].orEmpty()
            if (current.contains(backupId)) {
                prefs[Keys.SKIP_AUTO_BACKUP_IDS] = current - backupId
            }
        }
    }

    suspend fun markSkippedForAutoRestore(backupId: String) {
        context.driveAccountDataStore.edit { prefs ->
            val current = prefs[Keys.SKIP_AUTO_RESTORE_IDS].orEmpty()
            prefs[Keys.SKIP_AUTO_RESTORE_IDS] = current + backupId
        }
    }

    suspend fun markSkippedForAutoRestore(backupIds: Collection<String>) {
        if (backupIds.isEmpty()) return
        context.driveAccountDataStore.edit { prefs ->
            val current = prefs[Keys.SKIP_AUTO_RESTORE_IDS].orEmpty()
            prefs[Keys.SKIP_AUTO_RESTORE_IDS] = current + backupIds
        }
    }

    suspend fun unmarkSkippedForAutoRestore(backupId: String) {
        context.driveAccountDataStore.edit { prefs ->
            val current = prefs[Keys.SKIP_AUTO_RESTORE_IDS].orEmpty()
            if (current.contains(backupId)) {
                prefs[Keys.SKIP_AUTO_RESTORE_IDS] = current - backupId
            }
        }
    }

    suspend fun clearAccount() {
        context.driveAccountDataStore.edit { prefs ->
            prefs[Keys.IS_SIGNED_IN] = false
            prefs.remove(Keys.EMAIL)
            prefs.remove(Keys.DISPLAY_NAME)
            prefs.remove(Keys.SKIP_AUTO_BACKUP_IDS)
            prefs.remove(Keys.SKIP_AUTO_RESTORE_IDS)
        }
    }
}
