package com.familyos.feature.profile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.core.ui.theme.FamilyDanger
import com.familyos.feature.profile.ProfileEvent
import com.familyos.feature.profile.ProfileViewModel
import com.familyos.core.ui.locale.rememberUiStrings

/**
 * Profile screen showing avatar, name, email, and delete-account action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = rememberUiStrings()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is ProfileEvent.AccountDeleted) onAccountDeleted()
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

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.setDeleteConfirmVisible(false) },
            title = { Text(s.deleteAccountQ) },
            text = {
                Text(s.deleteAccountBody)
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteAccount) {
                    Text(s.delete, color = FamilyDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setDeleteConfirmVisible(false) }) {
                    Text(s.cancel)
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.profileTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = s.back)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.isLoading && state.user == null) {
            FamilyLoading()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val photoUrl = state.user?.photoUrl
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = s.avatar,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = s.avatar,
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = viewModel::onDisplayNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.displayName) },
                    singleLine = true,
                    enabled = !state.isSaving,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.user?.email.orEmpty(),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.email) },
                    singleLine = true,
                    enabled = false,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = viewModel::saveProfile,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                ) {
                    Text(if (state.isSaving) s.saving else s.saveChanges)
                }
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedButton(
                    onClick = { viewModel.setDeleteConfirmVisible(true) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(s.deleteAccount, color = FamilyDanger)
                }
            }
        }
    }
}
