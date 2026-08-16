package com.familyos.feature.documents.ui

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.familyos.feature.documents.viewmodel.DocumentsUiState
import com.familyos.core.ui.locale.rememberUiStrings
import com.familyos.core.locale.AppLocale
import com.familyos.core.locale.UiStringsCatalog

/**
 * PIN + biometric lock screen shown before vault access.
 */
@Composable
fun DocumentLockScreen(
    state: DocumentsUiState,
    onSetupPin: (String) -> Unit,
    onUnlockPin: (String) -> Unit,
    onBiometricSuccess: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val s = rememberUiStrings()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (state.pinConfigured) s.unlockVaultTitle else s.setVaultPinTitle,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = s.vaultSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 12) pin = it.filter { c -> c.isDigit() } },
            label = { Text(s.pinLabel) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        if (!state.pinConfigured) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 12) confirmPin = it.filter { c -> c.isDigit() } },
                label = { Text(s.confirmPin) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (pin.length < 4) {
                        Toast.makeText(context, s.pinTooShort, Toast.LENGTH_SHORT).show()
                    } else if (pin != confirmPin) {
                        Toast.makeText(context, s.pinsDoNotMatch, Toast.LENGTH_SHORT).show()
                    } else {
                        onSetupPin(pin)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(s.createPin)
            }
        } else {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onUnlockPin(pin) },
                modifier = Modifier.fillMaxWidth(),
                enabled = pin.length >= 4,
            ) {
                Text(s.unlock)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    launchBiometric(
                        context = context,
                        onSuccess = onBiometricSuccess,
                        onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                    )
                },
                enabled = state.biometricEnabled || canUseBiometric(context),
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  ${s.useBiometric}")
            }
            Spacer(Modifier.height(16.dp))
            RowToggle(
                checked = state.biometricEnabled,
                onCheckedChange = onBiometricToggle,
            )
        }

        state.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun RowToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val s = rememberUiStrings()
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(s.enableBiometricUnlock)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun canUseBiometric(context: android.content.Context): Boolean {
    val manager = BiometricManager.from(context)
    return manager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
    ) == BiometricManager.BIOMETRIC_SUCCESS
}

private fun launchBiometric(
    context: android.content.Context,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
) {
    val strings = UiStringsCatalog.forLang(AppLocale.currentLanguage())
    val activity = context as? FragmentActivity
    if (activity == null) {
        onError(strings.biometricHostError)
        return
    }
    val executor = ContextCompat.getMainExecutor(context)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                onError(strings.biometricFailed)
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(strings.unlockVaultTitle)
        .setSubtitle(strings.biometricSubtitle)
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
        .build()
    prompt.authenticate(info)
}
