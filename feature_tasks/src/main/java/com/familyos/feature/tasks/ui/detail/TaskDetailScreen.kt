package com.familyos.feature.tasks.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.core.ui.theme.FamilyDanger
import com.familyos.feature.tasks.util.formatTaskEpoch
import com.familyos.feature.tasks.util.label
import com.familyos.feature.tasks.viewmodel.TaskEditorEvent
import com.familyos.feature.tasks.viewmodel.TaskEditorViewModel

/**
 * Read-focused task detail with quick status actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: TaskEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TaskEditorEvent.Deleted -> onBack()
                TaskEditorEvent.Saved -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.taskId?.let { id ->
                        IconButton(onClick = { onEdit(id) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                        }
                    }
                    IconButton(onClick = viewModel::delete) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.isLoading) {
            FamilyLoading()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(state.title, style = MaterialTheme.typography.headlineMedium)
                if (state.isOverdue) {
                    Text("Overdue", color = FamilyDanger, style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    "${state.status.label()} · ${state.priority.label()}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text("Start: ${formatTaskEpoch(state.startAt)}")
                Text("Deadline: ${formatTaskEpoch(state.deadline)}")
                val assignee = state.members.firstOrNull { it.userId == state.assigneeId }?.displayName ?: "Unassigned"
                Text("Assignee: $assignee")
                if (state.description.isNotBlank()) {
                    Text(state.description, style = MaterialTheme.typography.bodyMedium)
                }
                if (state.photoUri.isNotBlank()) {
                    AsyncImage(
                        model = state.photoUri,
                        contentDescription = "Task photo",
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
                if (state.checklist.isNotEmpty()) {
                    Text("Checklist", style = MaterialTheme.typography.titleMedium)
                    state.checklist.forEach { row ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = row.isChecked, onCheckedChange = { viewModel.toggleChecklistItem(row.id) })
                            Text(row.text)
                        }
                    }
                }
                if (state.attachmentsText.isNotBlank()) {
                    Text("Attachments: ${state.attachmentsText}")
                }
                if (state.recurrenceEnabled) {
                    Text("Repeats ${state.recurrenceFrequency.label()} every ${state.recurrenceInterval}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { viewModel.applyStatus(TaskStatus.IN_PROGRESS) }, label = { Text("Start") })
                    AssistChip(onClick = { viewModel.applyStatus(TaskStatus.WAITING) }, label = { Text("Waiting") })
                    AssistChip(onClick = { viewModel.applyStatus(TaskStatus.DONE) }, label = { Text("Done") })
                    AssistChip(onClick = { viewModel.applyStatus(TaskStatus.CANCELLED) }, label = { Text("Cancel") })
                }
            }
        }
    }
}
