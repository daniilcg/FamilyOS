package com.familyos.core.data.auth

import android.content.Context
import com.familyos.core.domain.repository.AuthModeProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects whether a real Firebase project is present (not the shipped placeholder).
 */
@Singleton
class FirebaseAvailability @Inject constructor(
    @ApplicationContext private val context: Context,
) : AuthModeProvider {

    @Volatile
    private var cached: Boolean? = null

    override fun isCloudAuthAvailable(): Boolean {
        cached?.let { return it }
        val available = Companion.isCloudAuthAvailable(context)
        cached = available
        return available
    }

    companion object {
        /**
         * True only if FirebaseApp can init and the config is not the placeholder stub.
         */
        fun isCloudAuthAvailable(context: Context): Boolean = try {
            val app = runCatching { FirebaseApp.getInstance() }.getOrElse {
                FirebaseApp.initializeApp(context) ?: return false
            }
            isRealFirebaseOptions(app.options)
        } catch (e: Exception) {
            Timber.d(e, "Firebase cloud auth unavailable")
            false
        }

        fun isRealFirebaseOptions(options: FirebaseOptions): Boolean {
            val apiKey = options.apiKey.orEmpty()
            val projectNumber = options.gcmSenderId.orEmpty()
            if (apiKey.isBlank()) return false
            if (apiKey.contains("Placeholder", ignoreCase = true)) return false
            if (projectNumber == "000000000000" || projectNumber.isBlank()) return false
            return true
        }
    }
}
