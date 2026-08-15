package com.familyos.core.ui.components

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Hosts a system biometric prompt when [trigger] becomes true.
 *
 * Requires the hosting activity to be a [FragmentActivity].
 */
@Composable
fun BiometricPromptHost(
    trigger: Boolean,
    title: String,
    subtitle: String,
    negativeButtonText: String = "Cancel",
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    onDismissed: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }

    LaunchedEffect(trigger, title, subtitle) {
        if (!trigger) return@LaunchedEffect
        if (activity == null) {
            onError("Biometric host requires FragmentActivity")
            return@LaunchedEffect
        }
        val manager = BiometricManager.from(context)
        val canAuth = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("Biometric authentication is not available")
            return@LaunchedEffect
        }
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onDismissed()
                    } else {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    onError("Authentication failed")
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()
        // negative button is omitted when DEVICE_CREDENTIAL is allowed
        prompt.authenticate(info)
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is android.content.ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}
