package com.familyos.core.domain.usecase.calendar

import com.familyos.core.domain.model.CalendarEvent
import com.familyos.core.domain.repository.CalendarRepository
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Loads a calendar event by id. */
class GetCalendarEventUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
) {
    suspend operator fun invoke(id: String): Result<CalendarEvent> = calendarRepository.getById(id)
}
