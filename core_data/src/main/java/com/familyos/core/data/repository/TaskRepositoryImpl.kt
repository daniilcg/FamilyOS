package com.familyos.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.familyos.core.data.local.dao.TaskDao
import com.familyos.core.data.mapper.EntityMappers
import com.familyos.core.data.mapper.EntityMappers.toDomain
import com.familyos.core.data.mapper.EntityMappers.toEntity
import com.familyos.core.data.remote.dto.TaskDto
import com.familyos.core.data.remote.firestore.FirestoreDataSource
import com.familyos.core.data.sync.SyncQueueRepositoryImpl
import com.familyos.core.domain.model.SyncActionType
import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.model.TaskPriority
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.domain.repository.TaskRepository
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

/** Offline-first task repository. */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val syncQueue: SyncQueueRepositoryImpl,
) : TaskRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeTasks(familyId: String, status: TaskStatus?): Flow<List<TaskItem>> =
        taskDao.observe(familyId, status?.name).map { it.map { e -> e.toDomain() } }.onStart {
            scope.launch {
                runCatching {
                    firestoreDataSource.observeTasks(familyId).collect { dtos ->
                        dtos.forEach { taskDao.upsert(it.toEntity()) }
                    }
                }
            }
        }

    override fun pagingTasks(familyId: String, status: TaskStatus?, priority: TaskPriority?): Flow<PagingData<TaskItem>> =
        Pager(PagingConfig(Constants.DEFAULT_PAGE_SIZE)) {
            taskDao.paging(familyId, status?.name, priority?.name)
        }.flow.map { it.map { e -> e.toDomain() } }

    override fun search(familyId: String, query: String, status: TaskStatus?, priority: TaskPriority?): Flow<List<TaskItem>> =
        taskDao.search(familyId, query, status?.name, priority?.name).map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: String): Result<TaskItem> = Result.runCatching {
        taskDao.getById(id)?.toDomain() ?: throw AppException(AppError.NotFound("Task", id))
    }

    override suspend fun upsert(task: TaskItem): Result<TaskItem> = Result.runCatching {
        taskDao.upsert(task.toEntity())
        enqueue(task)
        task
    }

    override suspend fun delete(id: String): Result<Unit> = Result.runCatching {
        val now = System.currentTimeMillis()
        taskDao.softDelete(id, now)
        taskDao.getById(id)?.toDomain()?.let { enqueueDelete(it) }
    }

    override suspend fun updateStatus(id: String, status: TaskStatus): Result<TaskItem> = Result.runCatching {
        val now = System.currentTimeMillis()
        val completedAt = if (status == TaskStatus.DONE) now else null
        taskDao.updateStatus(id, status.name, completedAt, now)
        val task = taskDao.getById(id)?.toDomain() ?: throw AppException(AppError.NotFound("Task", id))
        enqueue(task)
        task
    }

    private suspend fun enqueue(task: TaskItem) {
        syncQueue.enqueue(SyncCollection.TASKS, task.id, task.familyId, SyncActionType.UPSERT,
            EntityMappers.json.encodeToString(task.toDto()))
    }

    private suspend fun enqueueDelete(task: TaskItem) {
        syncQueue.enqueue(SyncCollection.TASKS, task.id, task.familyId, SyncActionType.DELETE,
            EntityMappers.json.encodeToString(task.toDto()))
    }

    private fun TaskItem.toDto() = TaskDto(
        id = id,
        familyId = familyId,
        title = title,
        description = description,
        status = status.name,
        priority = priority.name,
        startAt = startAt,
        dueAt = dueAt,
        completedAt = completedAt,
        assigneeId = assigneeId,
        createdBy = createdBy,
        recurrenceJson = recurrence?.let { EntityMappers.json.encodeToString(it) },
        parentTaskId = parentTaskId,
        photoUri = photoUri,
        checklistJson = if (checklist.isEmpty()) null else EntityMappers.json.encodeToString(checklist),
        attachmentIds = attachmentIds,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    private fun TaskDto.toEntity() = com.familyos.core.data.local.entity.TaskEntity(
        id = id,
        familyId = familyId,
        title = title,
        description = description,
        status = status,
        priority = priority,
        startAt = startAt,
        dueAt = dueAt,
        completedAt = completedAt,
        assigneeId = assigneeId,
        createdBy = createdBy,
        recurrenceJson = recurrenceJson,
        parentTaskId = parentTaskId,
        photoUri = photoUri,
        checklistJson = checklistJson,
        attachmentIdsCsv = attachmentIds.joinToString(","),
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )
}
