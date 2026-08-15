package com.familyos.feature.auth.google

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.familyos.feature.auth.R
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Result of a Google Sign-In attempt.
 */
sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult
    data class Failure(val message: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
}

/**
 * Builds Google Sign-In clients and extracts ID tokens from activity results.
 */
@Singleton
class GoogleSignInHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("googleWebClientId") private val webClientIdOverride: String,
) {

    /**
     * Resolves the OAuth Web client ID from override / app resources / feature fallback.
     */
    fun resolveWebClientId(): String {
        if (webClientIdOverride.isNotBlank() && !isPlaceholderWebClientId(webClientIdOverride)) {
            return webClientIdOverride.trim()
        }
        val appResId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName,
        )
        val fromApp = if (appResId != 0) {
            runCatching { context.getString(appResId) }.getOrNull().orEmpty()
        } else {
            ""
        }
        val fromFeature = runCatching { context.getString(R.string.default_web_client_id) }
            .getOrNull()
            .orEmpty()
        return listOf(fromApp, fromFeature)
            .firstOrNull { id -> id.isNotBlank() && !isPlaceholderWebClientId(id) }
            ?: fromApp.ifBlank { fromFeature }
    }

    /** True when Firebase / Google OAuth is still using placeholder values. */
    fun isMisconfigured(): Boolean = isPlaceholderWebClientId(resolveWebClientId())

    /**
     * Creates a [GoogleSignInClient] configured for Firebase Auth with the
     * web client id from resources.
     */
    fun createClient(): GoogleSignInClient {
        val webClientId = resolveWebClientId()
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    /** Returns the sign-in [Intent] for the Activity Result API. */
    fun getSignInIntent(): Intent {
        if (isMisconfigured()) {
            Timber.e("Google Sign-In web client id is a placeholder — replace google-services.json")
        }
        return createClient().signInIntent
    }

    /**
     * Parses an [ActivityResult] into a [GoogleSignInResult] containing an ID token.
     */
    fun parseResult(result: ActivityResult): GoogleSignInResult {
        if (isMisconfigured()) {
            return GoogleSignInResult.Failure(SETUP_MESSAGE)
        }
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            return GoogleSignInResult.Cancelled
        }
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val token = account?.idToken
            if (token.isNullOrBlank()) {
                GoogleSignInResult.Failure(
                    "Google не вернул ID token. В Firebase Console добавьте Web client ID " +
                        "(Authentication → Google → Web SDK configuration).",
                )
            } else {
                GoogleSignInResult.Success(token)
            }
        } catch (e: ApiException) {
            Timber.w(e, "Google Sign-In failed with status %s", e.statusCode)
            val message = when (e.statusCode) {
                10 -> SETUP_MESSAGE // DEVELOPER_ERROR
                12500 -> "Ошибка Google Play Services. Обновите Google Play на устройстве."
                7 -> "Нет сети."
                else -> e.message ?: "Google Sign-In failed (code ${e.statusCode})"
            }
            GoogleSignInResult.Failure(message)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected Google Sign-In error")
            GoogleSignInResult.Failure(e.message ?: SETUP_MESSAGE)
        }
    }

    /** Signs out of the Google account on this device. */
    fun signOut() {
        try {
            createClient().signOut()
        } catch (e: Exception) {
            Timber.w(e, "Google sign-out failed")
        }
    }

    companion object {
        const val SETUP_MESSAGE =
            "Google Sign-In не настроен. Положите реальный app/google-services.json из Firebase " +
                "и укажите Web client ID (client_type: 3). См. README → Firebase setup."

        fun isPlaceholderWebClientId(value: String): Boolean {
            val trimmed = value.trim()
            return trimmed.isEmpty() ||
                trimmed.contains("REPLACE", ignoreCase = true) ||
                trimmed.contains("000000000000", ignoreCase = true) ||
                trimmed.contains("Placeholder", ignoreCase = true) ||
                trimmed == "null"
        }
    }
}
