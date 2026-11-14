package com.example.data.cloud

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface DriveAuthOutcome {
    data class TokenReady(val accessToken: String) : DriveAuthOutcome
    data class NeedsResolution(val intentSenderRequest: IntentSenderRequest) : DriveAuthOutcome
    data class Failed(val message: String) : DriveAuthOutcome
}

object GoogleDriveAuthHelper {
    private const val TAG = "GoogleDriveAuth"

    suspend fun attemptSilentGoogleSignIn(
        context: Context,
        credentialManager: CredentialManager
    ): GoogleAccountProfile? {
        val clientId = runCatching { context.getString(R.string.default_web_client_id) }.getOrNull()
            ?: return null
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            extractAccountProfile(result.credential)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun signInInteractive(
        activity: Activity,
        credentialManager: CredentialManager
    ): Result<GoogleAccountProfile?> {
        val clientId = runCatching { activity.getString(R.string.default_web_client_id) }.getOrNull()
            ?: return Result.failure(IllegalStateException("Google Sign-In configuration missing (default_web_client_id)."))

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            val profile = extractAccountProfile(result.credential)
                ?: return Result.failure(IllegalStateException("Unexpected Google credential response."))
            Result.success(profile)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelled by user", e)
            Result.success(null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    suspend fun tryGetDriveAccessTokenSilently(context: Context): String? {
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(GoogleDriveBackupRepository.DRIVE_FILE_SCOPE)))
                .build()
            val result: AuthorizationResult = Identity.getAuthorizationClient(context.applicationContext)
                .authorize(request)
                .awaitResult()
            if (!result.hasResolution()) {
                result.accessToken?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.d(TAG, "Silent Drive token unavailable: ${e.message}")
            null
        }
    }

    suspend fun requestDriveAccessToken(activity: Activity): DriveAuthOutcome {
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(GoogleDriveBackupRepository.DRIVE_FILE_SCOPE)))
                .build()
            val result: AuthorizationResult = Identity.getAuthorizationClient(activity)
                .authorize(request)
                .awaitResult()

            if (result.hasResolution()) {
                val pendingIntent = result.pendingIntent
                    ?: return DriveAuthOutcome.Failed("Google Drive consent prompt could not be opened.")
                DriveAuthOutcome.NeedsResolution(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            } else {
                val token = result.accessToken
                if (token.isNullOrBlank()) {
                    DriveAuthOutcome.Failed("Google Drive did not return an access token.")
                } else {
                    DriveAuthOutcome.TokenReady(token)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Drive authorization failed", e)
            DriveAuthOutcome.Failed(e.localizedMessage ?: "Failed to authorize Google Drive.")
        }
    }

    fun extractTokenFromResolutionIntent(activity: Activity, data: Intent?): Result<String> = runCatching {
        val result = Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(data)
        result.accessToken?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Google Drive permission was not granted.")
    }

    suspend fun signOut(
        credentialManager: CredentialManager,
        accountStore: GoogleDriveAccountStore
    ) {
        withContext(NonCancellable) {
            runCatching {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }.onFailure {
                Log.w(TAG, "Failed to clear credential state: ${it.message}")
            }
            accountStore.clearAccount()
        }
    }

    private fun extractAccountProfile(credential: androidx.credentials.Credential): GoogleAccountProfile? {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            return GoogleAccountProfile(
                isSignedIn = true,
                email = googleCred.id,
                displayName = googleCred.displayName ?: googleCred.givenName ?: "Google Account"
            )
        }
        return null
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { res ->
            if (cont.isActive) cont.resume(res)
        }
        addOnFailureListener { err ->
            if (cont.isActive) cont.resumeWithException(err)
        }
        addOnCanceledListener {
            if (cont.isActive) cont.cancel()
        }
    }
}
