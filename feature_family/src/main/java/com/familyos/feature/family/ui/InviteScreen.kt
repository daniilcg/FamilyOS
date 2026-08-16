package com.familyos.feature.family.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyos.core.ui.components.FamilyEmptyState
import com.familyos.feature.family.FamilyViewModel
import com.familyos.feature.family.qr.QrCodeGenerator
import com.familyos.feature.family.qr.QrCodeImage
import kotlinx.coroutines.launch
import com.familyos.core.ui.locale.rememberUiStrings

/**
 * Invite screen showing the current invite code and a generated QR image.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteScreen(
    onNavigateBack: () -> Unit,
    viewModel: FamilyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = rememberUiStrings()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val code = state.inviteCode ?: state.family?.inviteCode

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.inviteMembers) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = s.back)
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refreshInviteCode,
                        enabled = state.canManageRoles && !state.isLoading,
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = s.rotateInvite)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (code.isNullOrBlank()) {
            FamilyEmptyState(message = s.noInviteCode)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                Text(
                    text = s.shareCodeOrQr,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = s.anyoneCanJoin.format(state.family?.name ?: s.yourFamily),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(28.dp))
                QrCodeImage(payload = QrCodeGenerator.invitePayload(code))
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = code,
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        copyToClipboard(context, code, s.clipboardInviteLabel)
                        scope.launch { snackbar.showSnackbar(s.inviteCodeCopied) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.padding(6.dp))
                    Text(s.copyCode)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val link = QrCodeGenerator.invitePayload(code)
                        copyToClipboard(context, link, s.clipboardInviteLabel)
                        scope.launch { snackbar.showSnackbar(s.inviteLinkCopied) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(s.copyInviteLink)
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
