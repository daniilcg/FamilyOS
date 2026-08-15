package com.familyos.core.domain.repository

import com.familyos.core.domain.model.CalendarEvent
import com.familyos.core.domain.model.EventType
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Family calendar persistence.
 */
interface CalendarRepository {
    fun observeEvents(familyId: String, rangeStart: Long, rangeEnd: Long): Flow<List<CalendarEvent>>
    fun observeByType(familyId: String, type: EventType): Flow<List<CalendarEvent>>
    suspend fun getById(id: String): Result<CalendarEvent>
    suspend fun upsert(event: CalendarEvent): Result<CalendarEvent>
    suspend fun delete(id: String): Result<Unit>
}
