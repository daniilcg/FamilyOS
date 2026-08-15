package com.familyos.feature.calendar.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.CalendarEvent
import com.familyos.core.domain.model.EventType
import com.familyos.core.domain.model.RecurrenceRule
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.calendar.GetCalendarEventUseCase
import com.familyos.core.domain.usecase.calendar.UpsertCalendarEventUseCase
import com.familyos.core.domain.util.Result
import com.familyos.feature.calendar.util.EventUiTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Form state for creating and editing calendar events.
 */
data class EventEditorUiState(
    val eventId: String? = null,
    val title: String = "",
    val description: String = "",
    val type: EventType = EventType.OTHER,
    val types: List<EventType> = EventUiTypes,
    val allDay: Boolean = false,
    val location: String = "",
    val startAt: Long = System.currentTimeMillis(),
    val endAt: Long = System.currentTimeMillis() + 60L * 60L * 1000L,
    val reminderMinutes: String = "30",
    val recurrenceEnabled: Boolean = false,
    val recurrenceFrequency: RecurrenceRule.Frequency = RecurrenceRule.Frequency.YEARLY,
    val recurrenceInterval: String = "1",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isEdit: Boolean = false,
)

sealed interface EventEditorEvent {
    data object Saved : EventEditorEvent
}

/**
 * ViewModel for [com.familyos.feature.calendar.ui.editor.EventEditorScreen].
 */
@HiltViewModel
class EventEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEvent: GetCalendarEventUseCase,
    private val upsertEvent: UpsertCalendarEventUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val routeId = savedStateHandle.get<String>("eventId")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(EventEditorUiState(eventId = routeId, isEdit = routeId != null))
    val uiState: StateFlow<EventEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EventEditorEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var familyId: String? = null
    private var userId: String? = null
    private var existing: CalendarEvent? = null

    init {
        viewModelScope.launch {
            val user = getCurrentUser()
            val prefs = preferencesRepository.get()
            familyId = prefs.activeFamilyId ?: user?.familyId
            userId = user?.id
            val id = routeId
            if (id != null) {
                _uiState.update { it.copy(isLoading = true) }
                when (val result = getEvent(id)) {
                    is Result.Success -> {
                        existing = result.data
                        val event = result.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                title = event.title,
                                description = event.description.orEmpty(),
                                type = event.type,
                                allDay = event.allDay,
                                location = event.location.orEmpty(),
                                startAt = event.startAt,
                                endAt = event.endAt,
                                reminderMinutes = (event.reminderMinutes ?: 30).toString(),
                                recurrenceEnabled = event.recurrence != null,
                                recurrenceFrequency = event.recurrence?.frequency ?: RecurrenceRule.Frequency.YEARLY,
                                recurrenceInterval = (event.recurrence?.interval ?: 1).toString(),
                            )
                        }
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v, errorMessage = null) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }
    fun onTypeChange(v: EventType) = _uiState.update { it.copy(type = v) }
    fun onAllDayChange(v: Boolean) = _uiState.update { it.copy(allDay = v) }
    fun onLocationChange(v: String) = _uiState.update { it.copy(location = v) }
    fun onStartAtChange(v: Long) = _uiState.update { it.copy(startAt = v) }
    fun onEndAtChange(v: Long) = _uiState.update { it.copy(endAt = v) }
    fun onReminderChange(v: String) = _uiState.update { it.copy(reminderMinutes = v) }
    fun onRecurrenceEnabled(v: Boolean) = _uiState.update { it.copy(recurrenceEnabled = v) }
    fun onRecurrenceFrequency(v: RecurrenceRule.Frequency) = _uiState.update { it.copy(recurrenceFrequency = v) }
    fun onRecurrenceInterval(v: String) = _uiState.update { it.copy(recurrenceInterval = v) }

    /** Sets start/end to the given local date, preserving time-of-day when not all-day. */
    fun setDate(date: LocalDate) {
        val zone = ZoneId.systemDefault()
        val state = _uiState.value
        val start = if (state.allDay) {
            date.atStartOfDay(zone).toInstant().toEpochMilli()
        } else {
            val time = java.time.Instant.ofEpochMilli(state.startAt).atZone(zone).toLocalTime()
            date.atTime(time).atZone(zone).toInstant().toEpochMilli()
        }
        val end = if (state.allDay) {
            date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        } else {
            val time = java.time.Instant.ofEpochMilli(state.endAt).atZone(zone).toLocalTime()
                .let { if (it == LocalTime.MIDNIGHT) LocalTime.of(1, 0) else it }
            date.atTime(time).atZone(zone).toInstant().toEpochMilli()
        }
        _uiState.update { it.copy(startAt = start, endAt = end) }
    }

    fun save() {
        val state = _uiState.value
        val family = familyId
        val creator = userId
        if (family.isNullOrBlank() || creator.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Family or user is not available") }
            return
        }
        val reminder = state.reminderMinutes.toIntOrNull()
        val interval = state.recurrenceInterval.toIntOrNull()?.coerceAtLeast(1) ?: 1
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val base = existing
            val event = CalendarEvent(
                id = base?.id.orEmpty(),
                familyId = family,
                title = state.title.trim(),
                description = state.description.trim().ifBlank { null },
                type = state.type,
                startAt = state.startAt,
                endAt = state.endAt,
                allDay = state.allDay,
                location = state.location.trim().ifBlank { null },
                recurrence = if (state.recurrenceEnabled) {
                    RecurrenceRule(frequency = state.recurrenceFrequency, interval = interval)
                } else {
                    null
                },
                attendeeIds = base?.attendeeIds.orEmpty(),
                createdBy = base?.createdBy ?: creator,
                reminderMinutes = reminder,
                createdAt = base?.createdAt ?: 0L,
                updatedAt = base?.updatedAt ?: 0L,
                isDeleted = false,
            )
            when (val result = upsertEvent(event)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.emit(EventEditorEvent.Saved)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.error.message)
                }
            }
        }
    }
}
