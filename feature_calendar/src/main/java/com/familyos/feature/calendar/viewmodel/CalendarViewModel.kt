package com.familyos.feature.calendar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.CalendarEvent
import com.familyos.core.domain.model.EventType
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.calendar.DeleteCalendarEventUseCase
import com.familyos.core.domain.usecase.calendar.ObserveCalendarEventsUseCase
import com.familyos.core.domain.util.Result
import com.familyos.feature.calendar.util.CalendarViewMode
import com.familyos.feature.calendar.util.endOfDayMillis
import com.familyos.feature.calendar.util.startOfDayMillis
import com.familyos.feature.calendar.util.toLocalDate
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * UI state for month / week / day / agenda calendar surfaces.
 */
data class CalendarUiState(
    val familyId: String? = null,
    val userId: String? = null,
    val viewMode: CalendarViewMode = CalendarViewMode.MONTH,
    val selectedDate: LocalDate = LocalDate.now(),
    val visibleMonth: LocalDate = LocalDate.now().withDayOfMonth(1),
    val typeFilter: EventType? = null,
    val events: List<CalendarEvent> = emptyList(),
    val eventsByDay: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

sealed interface CalendarEventFeedback {
    data class Message(val text: String) : CalendarEventFeedback
}

/**
 * ViewModel for calendar navigation, range observation, and event deletion.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val observeEvents: ObserveCalendarEventsUseCase,
    private val deleteEvent: DeleteCalendarEventUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CalendarEventFeedback>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val familyIdFlow = MutableStateFlow<String?>(null)
    private val viewMode = MutableStateFlow(CalendarViewMode.MONTH)
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val typeFilter = MutableStateFlow<EventType?>(null)

    init {
        viewModelScope.launch {
            val user = getCurrentUser()
            val prefs = preferencesRepository.get()
            val familyId = prefs.activeFamilyId ?: user?.familyId
            familyIdFlow.value = familyId
            _uiState.update { it.copy(familyId = familyId, userId = user?.id) }
        }

        viewModelScope.launch {
            combine(familyIdFlow, viewMode, selectedDate, typeFilter) { familyId, mode, date, type ->
                Query(familyId, mode, date, type)
            }.flatMapLatest { q ->
                if (q.familyId.isNullOrBlank()) {
                    flowOf(emptyList<CalendarEvent>() to q)
                } else {
                    val (start, end) = rangeFor(q.mode, q.date)
                    observeEvents(q.familyId, start, end).map { list ->
                        val filtered = if (q.type == null) list else list.filter { it.type == q.type }
                        filtered to q
                    }
                }
            }.collect { (list, q) ->
                val byDay = list.groupBy { it.startAt.toLocalDate() }
                _uiState.update {
                    it.copy(
                        viewMode = q.mode,
                        selectedDate = q.date,
                        visibleMonth = q.date.withDayOfMonth(1),
                        typeFilter = q.type,
                        events = list.sortedBy { event -> event.startAt },
                        eventsByDay = byDay,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun setViewMode(mode: CalendarViewMode) {
        viewMode.value = mode
        _uiState.update { it.copy(isLoading = true) }
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun shiftPeriod(forward: Boolean) {
        val current = selectedDate.value
        val mode = viewMode.value
        selectedDate.value = when (mode) {
            CalendarViewMode.MONTH -> if (forward) current.plusMonths(1) else current.minusMonths(1)
            CalendarViewMode.WEEK -> if (forward) current.plusWeeks(1) else current.minusWeeks(1)
            CalendarViewMode.DAY, CalendarViewMode.AGENDA ->
                if (forward) current.plusDays(1) else current.minusDays(1)
        }
    }

    fun setTypeFilter(type: EventType?) {
        typeFilter.value = type
    }

    fun delete(eventId: String) {
        viewModelScope.launch {
            when (val result = deleteEvent(eventId)) {
                is Result.Success -> _events.emit(CalendarEventFeedback.Message("Event deleted"))
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private data class Query(
        val familyId: String?,
        val mode: CalendarViewMode,
        val date: LocalDate,
        val type: EventType?,
    )

    private fun rangeFor(mode: CalendarViewMode, date: LocalDate): Pair<Long, Long> = when (mode) {
        CalendarViewMode.DAY -> date.startOfDayMillis() to date.endOfDayMillis()
        CalendarViewMode.WEEK -> {
            val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            start.startOfDayMillis() to end.endOfDayMillis()
        }
        CalendarViewMode.MONTH -> {
            val start = date.withDayOfMonth(1)
            val end = date.with(TemporalAdjusters.lastDayOfMonth())
            start.startOfDayMillis() to end.endOfDayMillis()
        }
        CalendarViewMode.AGENDA -> {
            val end = date.plusDays(30)
            date.startOfDayMillis() to end.endOfDayMillis()
        }
    }
}
