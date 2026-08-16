package com.familyos.feature.family.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.familyos.core.domain.model.FamilyMember
import com.familyos.core.domain.model.FamilyRole
import com.familyos.core.ui.components.FamilyEmptyState
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.feature.family.FamilyViewModel
import com.familyos.core.ui.locale.rememberUiStrings

/**
 * Lists family members with Owner/Admin role management controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMembersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToInvite: () -> Unit,
    viewModel: FamilyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = rememberUiStrings()
    val snackbar = remember { SnackbarHostState() }

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
                title = { Text(state.family?.name ?: s.membersTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = s.back)
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToInvite) {
                        Text(s.invite)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.isLoading && state.members.isEmpty() -> {
                FamilyLoading()
            }
            state.family == null -> {
                FamilyEmptyState(message = s.notInFamilyYet)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    items(state.members, key = { it.id }) { member ->
                        MemberRow(
                            member = member,
                            canManage = state.canManageRoles &&
                                member.userId != state.currentUser?.id &&
                                member.role != FamilyRole.OWNER,
                            onRoleSelected = { role ->
                                viewModel.changeMemberRole(member.id, role)
                            },
                            onRemove = { viewModel.removeFamilyMember(member.id) },
                        )
                        HorizontalDivider()
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = viewModel::leaveCurrentFamily,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            Text(s.leaveFamily, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: FamilyMember,
    canManage: Boolean,
    onRoleSelected: (FamilyRole) -> Unit,
    onRemove: () -> Unit,
) {
    val s = rememberUiStrings()

    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!member.photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = member.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(member.displayName, style = MaterialTheme.typography.titleMedium)
            if (member.email.isNotBlank()) {
                Text(
                    member.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
        }
        AssistChip(
            onClick = { if (canManage) menuExpanded = true },
            label = { Text(member.role.name) },
            enabled = canManage,
        )
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            listOf(FamilyRole.ADMIN, FamilyRole.MEMBER, FamilyRole.GUEST).forEach { role ->
                DropdownMenuItem(
                    text = { Text(role.name) },
                    onClick = {
                        menuExpanded = false
                        onRoleSelected(role)
                    },
                )
            }
        }
        if (canManage) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = s.removeMember,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
