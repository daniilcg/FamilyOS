package com.familyos.feature.tasks.ui.list

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyos.core.domain.model.TaskPriority
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.domain.model.isOverdue
import com.familyos.core.ui.components.FamilyEmptyState
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.core.ui.theme.FamilyDanger
import com.familyos.core.ui.theme.FamilyOsTheme
import com.familyos.feature.tasks.util.TaskStatusFilter
import com.familyos.feature.tasks.util.formatTaskEpoch
import com.familyos.feature.tasks.util.label
import com.familyos.feature.tasks.viewmodel.TaskEvent
import com.familyos.feature.tasks.viewmodel.TaskListUiState
import com.familyos.feature.tasks.viewmodel.TaskViewModel
import com.familyos.core.ui.locale.rememberUiStrings

/**
 * Task list with status / priority / assignee filters and search.
 */
@Composable
fun TaskListScreen(
    onAddClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: TaskViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = rememberUiStrings()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is TaskEvent.Message) snackbar.showSnackbar(event.text)
        }
    }
    TaskListContent(
        state = state,
        snackbar = snackbar,
        onQueryChange = viewModel::onQueryChange,
        onStatusFilter = viewModel::onStatusFilter,
        onPriorityFilter = viewModel::onPriorityFilter,
        onAssigneeFilter = viewModel::onAssigneeFilter,
        onAddClick = onAddClick,
        onTaskClick = onTaskClick,
        onMarkDone = { viewModel.setStatus(it, TaskStatus.DONE) },
        onBack = onBack,
        onClearError = viewModel::clearError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TaskListContent(
    state: TaskListUiState,
    snackbar: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onStatusFilter: (TaskStatusFilter) -> Unit,
    onPriorityFilter: (TaskPriority?) -> Unit,
    onAssigneeFilter: (String?) -> Unit,
    onAddClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    onMarkDone: (String) -> Unit,
    onBack: (() -> Unit)?,
    onClearError: () -> Unit,
) {
    val s = rememberUiStrings()

    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        onClearError()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.tasksTitle) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = s.addTaskCd)
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.isLoading && state.tasks.isEmpty() -> FamilyLoading()
            state.familyId.isNullOrBlank() -> FamilyEmptyState(message = s.joinFamilyForTasks)
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(s.search) },
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TaskStatusFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.statusFilter == filter,
                            onClick = { onStatusFilter(filter) },
                            label = { Text(filter.label()) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.priorityFilter == null,
                        onClick = { onPriorityFilter(null) },
                        label = { Text(s.anyPriority) },
                    )
                    TaskPriority.entries.forEach { priority ->
                        FilterChip(
                            selected = state.priorityFilter == priority,
                            onClick = {
                                onPriorityFilter(
                                    if (state.priorityFilter == priority) null else priority,
                                )
                            },
                            label = { Text(priority.label()) },
                        )
                    }
                }
                if (state.members.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.assigneeFilter == null,
                            onClick = { onAssigneeFilter(null) },
                            label = { Text(s.anyone) },
                        )
                        state.members.forEach { member ->
                            FilterChip(
                                selected = state.assigneeFilter == member.userId,
                                onClick = {
                                    onAssigneeFilter(
                                        if (state.assigneeFilter == member.userId) null else member.userId,
                                    )
                                },
                                label = { Text(member.displayName) },
                            )
                        }
                    }
                }
                if (state.tasks.isEmpty()) {
                    FamilyEmptyState(message = s.noTasksMatch)
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.tasks, key = { it.id }) { task ->
                            val overdue = task.isOverdue()
                            Card(onClick = { onTaskClick(task.id) }, modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(task.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = buildString {
                                            append(if (overdue) s.overdue else task.status.label())
                                            append(" · ")
                                            append(task.priority.label())
                                            append(" · ${s.duePrefix} ")
                                            append(formatTaskEpoch(task.dueAt))
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (overdue) FamilyDanger else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        AssistChip(
                                            onClick = { onMarkDone(task.id) },
                                            label = { Text(s.done) },
                                            enabled = task.status != TaskStatus.DONE,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskListPreview() {
    FamilyOsTheme {
        TaskListContent(
            state = TaskListUiState(isLoading = false, familyId = "f1"),
            snackbar = remember { SnackbarHostState() },
            onQueryChange = {},
            onStatusFilter = {},
            onPriorityFilter = {},
            onAssigneeFilter = {},
            onAddClick = {},
            onTaskClick = {},
            onMarkDone = {},
            onBack = null,
            onClearError = {},
        )
    }
}
