package com.familyos.feature.tasks.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyos.core.domain.model.RecurrenceRule
import com.familyos.core.domain.model.TaskPriority
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.feature.tasks.util.formatTaskEpoch
import com.familyos.feature.tasks.util.label
import com.familyos.feature.tasks.viewmodel.TaskEditorEvent
import com.familyos.feature.tasks.viewmodel.TaskEditorViewModel
import com.familyos.core.ui.locale.rememberUiStrings

/**
 * Create or edit a task including checklist, recurrence, and attachments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: TaskEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = rememberUiStrings()
    val snackbar = remember { SnackbarHostState() }
    var checklistDraft by remember { mutableStateOf("") }
    var assigneeExpanded by remember { mutableStateOf(false) }
    var priorityExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TaskEditorEvent.Saved, TaskEditorEvent.Deleted -> onDone()
            }
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) s.editTask else s.addTask) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
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
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.title) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.description) },
                    minLines = 3,
                )
                ExposedDropdownMenuBox(expanded = assigneeExpanded, onExpandedChange = { assigneeExpanded = it }) {
                    val assigneeName = state.members.firstOrNull { it.userId == state.assigneeId }?.displayName ?: s.unassigned
                    OutlinedTextField(
                        value = assigneeName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(s.assigneeLabel) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(assigneeExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = assigneeExpanded, onDismissRequest = { assigneeExpanded = false }) {
                        DropdownMenuItem(text = { Text(s.unassigned) }, onClick = {
                            viewModel.onAssigneeChange(null)
                            assigneeExpanded = false
                        })
                        state.members.forEach { member ->
                            DropdownMenuItem(text = { Text(member.displayName) }, onClick = {
                                viewModel.onAssigneeChange(member.userId)
                                assigneeExpanded = false
                            })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = priorityExpanded, onExpandedChange = { priorityExpanded = it }) {
                    OutlinedTextField(
                        value = state.priority.label(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(s.priority) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(priorityExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }) {
                        TaskPriority.entries.forEach { priority ->
                            DropdownMenuItem(text = { Text(priority.label()) }, onClick = {
                                viewModel.onPriorityChange(priority)
                                priorityExpanded = false
                            })
                        }
                    }
                }
                ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                    OutlinedTextField(
                        value = state.status.label(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(s.status) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        TaskStatus.entries.forEach { status ->
                            DropdownMenuItem(text = { Text(status.label()) }, onClick = {
                                viewModel.onStatusChange(status)
                                statusExpanded = false
                            })
                        }
                    }
                }
                Text("${s.startLabel}: ${formatTaskEpoch(state.startAt)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.onStartAtChange(System.currentTimeMillis()) }) { Text(s.setStartNow) }
                    Button(onClick = { viewModel.onStartAtChange(null) }) { Text(s.clear) }
                }
                Text("${s.deadlineLabel}: ${formatTaskEpoch(state.deadline)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        viewModel.onDeadlineChange(System.currentTimeMillis() + 24L * 60L * 60L * 1000L)
                    }) { Text(s.dueTomorrow) }
                    Button(onClick = { viewModel.onDeadlineChange(null) }) { Text(s.clear) }
                }
                OutlinedTextField(
                    value = state.photoUri,
                    onValueChange = viewModel::onPhotoUriChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.photoUri) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.attachmentsText,
                    onValueChange = viewModel::onAttachmentsChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.attachmentsHint) },
                )
                Text(s.checklist)
                state.checklist.forEach { row ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = row.isChecked, onCheckedChange = { viewModel.toggleChecklistItem(row.id) })
                        Text(row.text, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.removeChecklistItem(row.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = s.remove)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = checklistDraft,
                        onValueChange = { checklistDraft = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(s.checklistItem) },
                        singleLine = true,
                    )
                    Button(onClick = {
                        viewModel.addChecklistItem(checklistDraft)
                        checklistDraft = ""
                    }) { Text(s.add) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(s.recurrence)
                    Switch(checked = state.recurrenceEnabled, onCheckedChange = viewModel::onRecurrenceEnabled)
                }
                if (state.recurrenceEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecurrenceRule.Frequency.entries.forEach { frequency ->
                            FilterChip(
                                selected = state.recurrenceFrequency == frequency,
                                onClick = { viewModel.onRecurrenceFrequency(frequency) },
                                label = { Text(frequency.label()) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.recurrenceInterval,
                        onValueChange = viewModel::onRecurrenceInterval,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(s.customInterval) },
                        singleLine = true,
                    )
                }
                Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.isSaving) s.saving else s.save)
                }
            }
        }
    }
}
