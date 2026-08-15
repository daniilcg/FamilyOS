package com.familyos.feature.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.FamilyMember
import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.model.TaskPriority
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.domain.model.isOverdue
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.family.ObserveFamilyMembersUseCase
import com.familyos.core.domain.usecase.tasks.DeleteTaskUseCase
import com.familyos.core.domain.usecase.tasks.ObserveTasksUseCase
import com.familyos.core.domain.usecase.tasks.SearchTasksUseCase
import com.familyos.core.domain.usecase.tasks.UpdateTaskStatusUseCase
import com.familyos.core.domain.util.Result
import com.familyos.feature.tasks.util.TaskStatusFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the task list screen.
 */
data class TaskListUiState(
    val familyId: String? = null,
    val userId: String? = null,
    val query: String = "",
    val statusFilter: TaskStatusFilter = TaskStatusFilter.ALL,
    val priorityFilter: TaskPriority? = null,
    val assigneeFilter: String? = null,
    val tasks: List<TaskItem> = emptyList(),
    val members: List<FamilyMember> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

sealed interface TaskEvent {
    data class Message(val text: String) : TaskEvent
}

/**
 * ViewModel for task list filtering, search, and status actions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val observeTasks: ObserveTasksUseCase,
    private val searchTasks: SearchTasksUseCase,
    private val updateTaskStatus: UpdateTaskStatusUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val observeMembers: ObserveFamilyMembersUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskListUiState())
    val uiState: StateFlow<TaskListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TaskEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val query = MutableStateFlow("")
    private val statusFilter = MutableStateFlow(TaskStatusFilter.ALL)
    private val priorityFilter = MutableStateFlow<TaskPriority?>(null)
    private val assigneeFilter = MutableStateFlow<String?>(null)
    private val familyIdFlow = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val user = getCurrentUser()
            val prefs = preferencesRepository.get()
            val familyId = prefs.activeFamilyId ?: user?.familyId
            familyIdFlow.value = familyId
            _uiState.update { it.copy(familyId = familyId, userId = user?.id) }
        }

        viewModelScope.launch {
            familyIdFlow.flatMapLatest { familyId ->
                if (familyId.isNullOrBlank()) flowOf(emptyList()) else observeMembers(familyId)
            }.collect { members ->
                _uiState.update { it.copy(members = members) }
            }
        }

        viewModelScope.launch {
            combine(
                combine(familyIdFlow, query, statusFilter) { f, q, s -> Triple(f, q, s) },
                combine(priorityFilter, assigneeFilter) { p, a -> p to a },
            ) { identity, extras ->
                Filter(
                    familyId = identity.first,
                    query = identity.second,
                    status = identity.third,
                    priority = extras.first,
                    assigneeId = extras.second,
                )
            }.flatMapLatest { filter ->
                if (filter.familyId.isNullOrBlank()) {
                    flowOf(emptyList<TaskItem>() to filter)
                } else {
                    val domainStatus = filter.status.toDomainStatus()
                    val source = if (filter.query.isBlank()) {
                        observeTasks(filter.familyId, domainStatus)
                    } else {
                        searchTasks(filter.familyId, filter.query, domainStatus, filter.priority)
                    }
                    source.map { list -> applyClientFilters(list, filter) to filter }
                }
            }.collect { (tasks, filter) ->
                _uiState.update {
                    it.copy(
                        query = filter.query,
                        statusFilter = filter.status,
                        priorityFilter = filter.priority,
                        assigneeFilter = filter.assigneeId,
                        tasks = tasks,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onStatusFilter(value: TaskStatusFilter) {
        statusFilter.value = value
        _uiState.update { it.copy(isLoading = true) }
    }

    fun onPriorityFilter(value: TaskPriority?) {
        priorityFilter.value = value
    }

    fun onAssigneeFilter(value: String?) {
        assigneeFilter.value = value
    }

    fun setStatus(taskId: String, status: TaskStatus) {
        viewModelScope.launch {
            when (val result = updateTaskStatus(taskId, status)) {
                is Result.Success -> _events.emit(TaskEvent.Message("Status updated"))
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun delete(taskId: String) {
        viewModelScope.launch {
            when (val result = deleteTask(taskId)) {
                is Result.Success -> _events.emit(TaskEvent.Message("Task deleted"))
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private data class Filter(
        val familyId: String?,
        val query: String,
        val status: TaskStatusFilter,
        val priority: TaskPriority?,
        val assigneeId: String?,
    )

    private fun TaskStatusFilter.toDomainStatus(): TaskStatus? = when (this) {
        TaskStatusFilter.ALL, TaskStatusFilter.OVERDUE -> null
        TaskStatusFilter.NEW -> TaskStatus.NEW
        TaskStatusFilter.IN_PROGRESS -> TaskStatus.IN_PROGRESS
        TaskStatusFilter.WAITING -> TaskStatus.WAITING
        TaskStatusFilter.DONE -> TaskStatus.DONE
        TaskStatusFilter.CANCELLED -> TaskStatus.CANCELLED
    }

    private fun applyClientFilters(tasks: List<TaskItem>, filter: Filter): List<TaskItem> {
        val now = System.currentTimeMillis()
        return tasks
            .asSequence()
            .filter { filter.priority == null || it.priority == filter.priority }
            .filter { filter.assigneeId == null || it.assigneeId == filter.assigneeId }
            .filter {
                when (filter.status) {
                    TaskStatusFilter.OVERDUE -> it.isOverdue(now)
                    TaskStatusFilter.NEW -> it.status == TaskStatus.NEW
                    TaskStatusFilter.WAITING -> it.status == TaskStatus.WAITING
                    TaskStatusFilter.ALL -> true
                    else -> true
                }
            }
            .sortedWith(
                compareBy<TaskItem> { it.status == TaskStatus.DONE || it.status == TaskStatus.CANCELLED }
                    .thenByDescending { it.priority.ordinal }
                    .thenBy { it.dueAt ?: Long.MAX_VALUE },
            )
            .toList()
    }
}
