package com.familyos.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.familyos.core.data.local.dao.NoteDao
import com.familyos.core.data.mapper.EntityMappers
import com.familyos.core.data.mapper.EntityMappers.toDomain
import com.familyos.core.data.mapper.EntityMappers.toEntity
import com.familyos.core.data.remote.dto.NoteDto
import com.familyos.core.data.remote.firestore.FirestoreDataSource
import com.familyos.core.data.sync.SyncQueueRepositoryImpl
import com.familyos.core.domain.model.Note
import com.familyos.core.domain.model.NoteChecklistItem
import com.familyos.core.domain.model.SyncActionType
import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.repository.NoteRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.AppException
import com.familyos.core.domain.util.Constants
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

/** Offline-first notes repository. */
@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val syncQueue: SyncQueueRepositoryImpl,
) : NoteRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeNotes(familyId: String, includeArchived: Boolean): Flow<List<Note>> =
        noteDao.observe(familyId).map { entities ->
            entities.map { e -> e.toDomain() }.filter { includeArchived || !it.isArchived }
        }.onStart {
            scope.launch {
                runCatching {
                    firestoreDataSource.observeNotes(familyId).collect { dtos ->
                        dtos.forEach { dto ->
                            noteDao.upsert(dto.toEntity())
                            val checklist = runCatching {
                                EntityMappers.json.decodeFromString<List<NoteChecklistItem>>(dto.checklistJson)
                            }.getOrDefault(emptyList())
                            noteDao.replaceChecklist(dto.id, checklist.map { it.toEntity(dto.id) })
                        }
                    }
                }
            }
        }

    override fun pagingNotes(familyId: String, includeArchived: Boolean): Flow<PagingData<Note>> =
        Pager(PagingConfig(Constants.DEFAULT_PAGE_SIZE)) { noteDao.paging(familyId) }
            .flow.map { paging -> paging.map { it.toDomain() } }

    override fun search(familyId: String, query: String, includeArchived: Boolean): Flow<List<Note>> =
        noteDao.search(familyId, query).map { list ->
            list.map { e -> e.toDomain() }.filter { includeArchived || !it.isArchived }
        }

    override suspend fun getById(id: String): Result<Note> = Result.runCatching {
        val entity = noteDao.getById(id) ?: throw AppException(AppError.NotFound("Note", id))
        entity.toDomain(noteDao.getChecklist(id))
    }

    override suspend fun upsert(note: Note): Result<Note> = Result.runCatching {
        noteDao.upsert(note.toEntity())
        noteDao.replaceChecklist(note.id, note.checklist.map { it.toEntity(note.id) })
        syncQueue.enqueue(
            SyncCollection.NOTES,
            note.id,
            note.familyId,
            SyncActionType.UPSERT,
            EntityMappers.json.encodeToString(note.toDto()),
        )
        note
    }

    override suspend fun delete(id: String): Result<Unit> = Result.runCatching {
        noteDao.softDelete(id, System.currentTimeMillis())
        noteDao.getById(id)?.let { entity ->
            val note = entity.toDomain(noteDao.getChecklist(id))
            syncQueue.enqueue(
                SyncCollection.NOTES,
                id,
                note.familyId,
                SyncActionType.DELETE,
                EntityMappers.json.encodeToString(note.toDto()),
            )
        }
    }

    override suspend fun setPinned(id: String, pinned: Boolean): Result<Note> = Result.runCatching {
        noteDao.setPinned(id, pinned, System.currentTimeMillis())
        val entity = noteDao.getById(id) ?: throw AppException(AppError.NotFound("Note", id))
        val note = entity.toDomain(noteDao.getChecklist(id))
        syncQueue.enqueue(
            SyncCollection.NOTES,
            id,
            note.familyId,
            SyncActionType.UPSERT,
            EntityMappers.json.encodeToString(note.toDto()),
        )
        note
    }

    override suspend fun setArchived(id: String, archived: Boolean): Result<Note> = Result.runCatching {
        val existing = noteDao.getById(id)?.toDomain(noteDao.getChecklist(id))
            ?: throw AppException(AppError.NotFound("Note", id))
        val note = existing.copy(isArchived = archived, updatedAt = System.currentTimeMillis())
        noteDao.upsert(note.toEntity())
        syncQueue.enqueue(
            SyncCollection.NOTES,
            id,
            note.familyId,
            SyncActionType.UPSERT,
            EntityMappers.json.encodeToString(note.toDto()),
        )
        note
    }

    private fun Note.toDto() = NoteDto(
        id, familyId, title, body, EntityMappers.json.encodeToString(checklist), colorHex,
        isPinned, tags, createdBy, updatedBy, createdAt, updatedAt, isDeleted,
    )

    private fun NoteDto.toEntity() = com.familyos.core.data.local.entity.NoteEntity(
        id = id,
        familyId = familyId,
        title = title,
        body = body,
        colorHex = colorHex,
        isPinned = isPinned,
        isArchived = false,
        photoUrlsCsv = "",
        tagsCsv = tags.joinToString(","),
        createdBy = createdBy,
        updatedBy = updatedBy,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )
}
