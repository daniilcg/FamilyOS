package com.familyos.feature.tasks.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.FamilyMember
import com.familyos.core.domain.model.RecurrenceRule
import com.familyos.core.domain.model.TaskChecklistItem
import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.model.TaskPriority
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.domain.model.isOverdue
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.family.ObserveFamilyMembersUseCase
import com.familyos.core.domain.usecase.tasks.DeleteTaskUseCase
import com.familyos.core.domain.usecase.tasks.GetTaskUseCase
import com.familyos.core.domain.usecase.tasks.UpdateTaskStatusUseCase
import com.familyos.core.domain.usecase.tasks.UpsertTaskUseCase
import com.familyos.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Shared form / detail state for task create, edit, and detail screens.
 */
data class TaskEditorUiState(
    val taskId: String? = null,
    val title: String = "",
    val description: String = "",
    val assigneeId: String? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.NEW,
    val startAt: Long? = null,
    val deadline: Long? = null,
    val photoUri: String = "",
    val attachmentsText: String = "",
    val checklist: List<TaskChecklistItem> = emptyList(),
    val recurrenceEnabled: Boolean = false,
    val recurrenceFrequency: RecurrenceRule.Frequency = RecurrenceRule.Frequency.WEEKLY,
    val recurrenceInterval: String = "1",
    val members: List<FamilyMember> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isEdit: Boolean = false,
    val isOverdue: Boolean = false,
)

sealed interface TaskEditorEvent {
    data object Saved : TaskEditorEvent
    data object Deleted : TaskEditorEvent
}

/**
 * ViewModel for task detail and add/edit flows.
 */
@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTask: GetTaskUseCase,
    private val upsertTask: UpsertTaskUseCase,
    private val updateTaskStatus: UpdateTaskStatusUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val observeMembers: ObserveFamilyMembersUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val routeTaskId: String? =
        savedStateHandle.get<String>("taskId")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        TaskEditorUiState(taskId = routeTaskId, isEdit = routeTaskId != null),
    )
    val uiState: StateFlow<TaskEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TaskEditorEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var familyId: String? = null
    private var userId: String? = null
    private var existing: TaskItem? = null

    init {
        viewModelScope.launch {
            val user = getCurrentUser()
            val prefs = preferencesRepository.get()
            familyId = prefs.activeFamilyId ?: user?.familyId
            userId = user?.id
            val fid = familyId
            if (!fid.isNullOrBlank()) {
                observeMembers(fid).collect { members ->
                    _uiState.update { it.copy(members = members) }
                }
            }
        }
        val id = routeTaskId
        if (id != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                when (val result = getTask(id)) {
                    is Result.Success -> bind(result.data)
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    private fun bind(task: TaskItem) {
        existing = task
        _uiState.update {
            it.copy(
                isLoading = false,
                taskId = task.id,
                isEdit = true,
                title = task.title,
                description = task.description.orEmpty(),
                assigneeId = task.assigneeId,
                priority = task.priority,
                status = task.status,
                startAt = task.startAt,
                deadline = task.dueAt,
                photoUri = task.photoUri.orEmpty(),
                attachmentsText = task.attachmentIds.joinToString(", "),
                checklist = task.checklist,
                recurrenceEnabled = task.recurrence != null,
                recurrenceFrequency = task.recurrence?.frequency ?: RecurrenceRule.Frequency.WEEKLY,
                recurrenceInterval = (task.recurrence?.interval ?: 1).toString(),
                isOverdue = task.isOverdue(),
            )
        }
    }

    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v, errorMessage = null) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }
    fun onAssigneeChange(v: String?) = _uiState.update { it.copy(assigneeId = v) }
    fun onPriorityChange(v: TaskPriority) = _uiState.update { it.copy(priority = v) }
    fun onStatusChange(v: TaskStatus) = _uiState.update { it.copy(status = v) }
    fun onStartAtChange(v: Long?) = _uiState.update { it.copy(startAt = v) }
    fun onDeadlineChange(v: Long?) = _uiState.update { it.copy(deadline = v) }
    fun onPhotoUriChange(v: String) = _uiState.update { it.copy(photoUri = v) }
    fun onAttachmentsChange(v: String) = _uiState.update { it.copy(attachmentsText = v) }
    fun onRecurrenceEnabled(v: Boolean) = _uiState.update { it.copy(recurrenceEnabled = v) }
    fun onRecurrenceFrequency(v: RecurrenceRule.Frequency) = _uiState.update { it.copy(recurrenceFrequency = v) }
    fun onRecurrenceInterval(v: String) = _uiState.update { it.copy(recurrenceInterval = v) }

    fun addChecklistItem(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _uiState.update {
            val next = it.checklist + TaskChecklistItem(
                id = UUID.randomUUID().toString(),
                text = trimmed,
                order = it.checklist.size,
            )
            it.copy(checklist = next)
        }
    }

    fun toggleChecklistItem(id: String) {
        _uiState.update { state ->
            state.copy(
                checklist = state.checklist.map { row ->
                    if (row.id == id) row.copy(isChecked = !row.isChecked) else row
                },
            )
        }
    }

    fun removeChecklistItem(id: String) {
        _uiState.update { state ->
            state.copy(checklist = state.checklist.filterNot { it.id == id })
        }
    }

    fun save() {
        val state = _uiState.value
        val family = familyId
        val creator = userId
        if (family.isNullOrBlank() || creator.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Family or user is not available") }
            return
        }
        val interval = state.recurrenceInterval.toIntOrNull()?.coerceAtLeast(1) ?: 1
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val base = existing
            val recurrence = if (state.recurrenceEnabled) {
                RecurrenceRule(frequency = state.recurrenceFrequency, interval = interval)
            } else {
                null
            }
            val attachments = state.attachmentsText
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val item = TaskItem(
                id = base?.id.orEmpty(),
                familyId = family,
                title = state.title.trim(),
                description = state.description.trim().ifBlank { null },
                status = state.status,
                priority = state.priority,
                startAt = state.startAt,
                dueAt = state.deadline,
                completedAt = if (state.status == TaskStatus.DONE) {
                    base?.completedAt ?: System.currentTimeMillis()
                } else {
                    null
                },
                assigneeId = state.assigneeId,
                createdBy = base?.createdBy ?: creator,
                recurrence = recurrence,
                parentTaskId = base?.parentTaskId,
                photoUri = state.photoUri.trim().ifBlank { null },
                checklist = state.checklist,
                attachmentIds = attachments,
                createdAt = base?.createdAt ?: 0L,
                updatedAt = base?.updatedAt ?: 0L,
                isDeleted = false,
            )
            when (val result = upsertTask(item)) {
                is Result.Success -> {
                    existing = result.data
                    _uiState.update { it.copy(isSaving = false, taskId = result.data.id, isEdit = true) }
                    _events.emit(TaskEditorEvent.Saved)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.error.message)
                }
            }
        }
    }

    fun applyStatus(status: TaskStatus) {
        val id = _uiState.value.taskId ?: return
        viewModelScope.launch {
            when (val result = updateTaskStatus(id, status)) {
                is Result.Success -> bind(result.data)
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun delete() {
        val id = _uiState.value.taskId ?: return
        viewModelScope.launch {
            when (val result = deleteTask(id)) {
                is Result.Success -> _events.emit(TaskEditorEvent.Deleted)
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }
}
