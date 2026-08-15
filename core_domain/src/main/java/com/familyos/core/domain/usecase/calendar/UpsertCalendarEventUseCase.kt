package com.familyos.core.domain.usecase.calendar

import com.familyos.core.domain.logic.RecurrenceExpander
import com.familyos.core.domain.model.CalendarEvent
import com.familyos.core.domain.repository.CalendarRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Creates or updates a calendar event. */
class UpsertCalendarEventUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository,
) {
    suspend operator fun invoke(event: CalendarEvent): Result<CalendarEvent> {
        if (event.title.isBlank()) return Result.failure(AppError.Validation("Title required", "title"))
        if (event.endAt < event.startAt) return Result.failure(AppError.Validation("endAt must be >= startAt"))
        if (event.familyId.isBlank()) return Result.failure(AppError.Validation("familyId required"))
        val now = System.currentTimeMillis()
        return calendarRepository.upsert(
            event.copy(
                id = event.id.ifBlank { UUID.randomUUID().toString() },
                title = event.title.trim(),
                updatedAt = now,
                createdAt = if (event.createdAt == 0L) now else event.createdAt,
            ),
        )
    }
}
