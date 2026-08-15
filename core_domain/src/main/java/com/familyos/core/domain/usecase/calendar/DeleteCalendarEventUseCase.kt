package com.familyos.core.domain.usecase.calendar

import com.familyos.core.domain.repository.CalendarRepository
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Deletes a calendar event. */
class DeleteCalendarEventUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = calendarRepository.delete(id)
}
