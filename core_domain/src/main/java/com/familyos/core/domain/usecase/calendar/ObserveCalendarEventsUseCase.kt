package com.familyos.core.domain.usecase.calendar

import com.familyos.core.domain.logic.RecurrenceExpander
import com.familyos.core.domain.model.CalendarEvent
import com.familyos.core.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Observes events in a range, expanding recurrence rules. */
class ObserveCalendarEventsUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
) {
    operator fun invoke(familyId: String, rangeStart: Long, rangeEnd: Long): Flow<List<CalendarEvent>> =
        calendarRepository.observeEvents(familyId, rangeStart, rangeEnd).map { events ->
            events.flatMap { RecurrenceExpander.expandEvents(it, rangeStart, rangeEnd) }
                .sortedBy { it.startAt }
        }
}
