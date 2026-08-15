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
) {

    /**
     * Creates a [GoogleSignInClient] configured for Firebase Auth with the
     * web client id from resources.
     */
    fun createClient(): GoogleSignInClient {
        val webClientId = context.getString(R.string.default_web_client_id)
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, options)
    }

    /** Returns the sign-in [Intent] for the Activity Result API. */
    fun getSignInIntent(): Intent = createClient().signInIntent

    /**
     * Parses an [ActivityResult] into a [GoogleSignInResult] containing an ID token.
     */
    fun parseResult(result: ActivityResult): GoogleSignInResult {
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            return GoogleSignInResult.Cancelled
        }
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val token = account?.idToken
            if (token.isNullOrBlank()) {
                GoogleSignInResult.Failure("Google ID token was empty")
            } else {
                GoogleSignInResult.Success(token)
            }
        } catch (e: ApiException) {
            Timber.w(e, "Google Sign-In failed with status %s", e.statusCode)
            GoogleSignInResult.Failure(e.message ?: "Google Sign-In failed")
        } catch (e: Exception) {
            Timber.e(e, "Unexpected Google Sign-In error")
            GoogleSignInResult.Failure(e.message ?: "Unexpected Google Sign-In error")
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
}
