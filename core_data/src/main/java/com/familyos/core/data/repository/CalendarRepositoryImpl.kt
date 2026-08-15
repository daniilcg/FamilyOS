package com.familyos.core.data.repository

import com.familyos.core.data.local.dao.EventDao
import com.familyos.core.data.mapper.EntityMappers
import com.familyos.core.data.mapper.EntityMappers.toDomain
import com.familyos.core.data.mapper.EntityMappers.toEntity
import com.familyos.core.data.remote.dto.EventDto
import com.familyos.core.data.remote.firestore.FirestoreDataSource
import com.familyos.core.data.sync.SyncQueueRepositoryImpl
import com.familyos.core.domain.model.CalendarEvent
import com.familyos.core.domain.model.EventType
import com.familyos.core.domain.model.SyncActionType
import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.repository.CalendarRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.AppException
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/** Offline-first calendar repository. */
@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val syncQueue: SyncQueueRepositoryImpl,
) : CalendarRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeEvents(familyId: String, rangeStart: Long, rangeEnd: Long): Flow<List<CalendarEvent>> =
        eventDao.observeRange(familyId, rangeStart, rangeEnd).map { it.map { e -> e.toDomain() } }.onStart {
            scope.launch {
                runCatching {
                    firestoreDataSource.observeEvents(familyId).collect { dtos ->
                        dtos.forEach { eventDao.upsert(it.toEntity()) }
                    }
                }
            }
        }

    override fun observeByType(familyId: String, type: EventType): Flow<List<CalendarEvent>> =
        eventDao.observeByType(familyId, type.name).map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: String): Result<CalendarEvent> = Result.runCatching {
        eventDao.getById(id)?.toDomain() ?: throw AppException(AppError.NotFound("Event", id))
    }

    override suspend fun upsert(event: CalendarEvent): Result<CalendarEvent> = Result.runCatching {
        eventDao.upsert(event.toEntity())
        syncQueue.enqueue(SyncCollection.EVENTS, event.id, event.familyId, SyncActionType.UPSERT,
            EntityMappers.json.encodeToString(event.toDto()))
        event
    }

    override suspend fun delete(id: String): Result<Unit> = Result.runCatching {
        val now = System.currentTimeMillis()
        eventDao.softDelete(id, now)
        eventDao.getById(id)?.toDomain()?.let {
            syncQueue.enqueue(SyncCollection.EVENTS, id, it.familyId, SyncActionType.DELETE,
                EntityMappers.json.encodeToString(it.toDto()))
        }
    }

    private fun CalendarEvent.toDto() = EventDto(
        id, familyId, title, description, type.name, startAt, endAt, allDay, location,
        recurrence?.let { EntityMappers.json.encodeToString(it) }, attendeeIds, createdBy,
        reminderMinutes, createdAt, updatedAt, isDeleted,
    )

    private fun EventDto.toEntity() = com.familyos.core.data.local.entity.EventEntity(
        id, familyId, title, description, type, startAt, endAt, allDay, location,
        recurrenceJson, attendeeIds.joinToString(","), createdBy, reminderMinutes,
        createdAt, updatedAt, isDeleted,
    )
}
