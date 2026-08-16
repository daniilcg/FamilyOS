package com.familyos.feature.family.ui

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyos.feature.family.FamilyEvent
import com.familyos.feature.family.FamilyViewModel
import com.familyos.feature.family.qr.QrCodeScanner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.familyos.core.ui.locale.rememberUiStrings

/**
 * Join an existing family via invite code or QR scan (CameraX + ZXing).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun JoinFamilyScreen(
    onNavigateBack: () -> Unit,
    onFamilyJoined: () -> Unit,
    viewModel: FamilyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = rememberUiStrings()
    val snackbar = remember { SnackbarHostState() }
    var showScanner by remember { mutableStateOf(false) }
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is FamilyEvent.FamilyReady) onFamilyJoined()
        }
    }
    LaunchedEffect(state.errorMessage, state.infoMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.infoMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.joinFamily) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = s.back)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        ) {
            Text(
                text = s.enterInviteCode,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = s.joinFamilyBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = state.inviteCodeInput,
                onValueChange = viewModel::onInviteCodeInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(s.inviteCode) },
                singleLine = true,
                enabled = !state.isLoading,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.joinFamily() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isLoading && state.inviteCodeInput.isNotBlank(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(s.joinWithCode)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = {
                    if (cameraPermission.status.isGranted) {
                        showScanner = !showScanner
                    } else {
                        cameraPermission.launchPermissionRequest()
                    }
                },
                enabled = !state.isLoading,
            ) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.padding(4.dp))
                Text(if (showScanner) s.hideScanner else s.scanQr)
            }

            if (!cameraPermission.status.isGranted && cameraPermission.status.shouldShowRationale) {
                Text(
                    text = s.cameraPermissionQr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (showScanner && cameraPermission.status.isGranted) {
                Spacer(modifier = Modifier.height(16.dp))
                QrCodeScanner(
                    onCodeScanned = { payload ->
                        showScanner = false
                        viewModel.onInviteQrScanned(payload)
                    },
                )
            }
        }
    }
}
