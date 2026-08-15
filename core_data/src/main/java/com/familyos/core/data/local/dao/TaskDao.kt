package com.familyos.core.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyos.core.data.local.entity.TaskAttachmentEntity
import com.familyos.core.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/** Data access for tasks. */
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE familyId = :familyId AND isDeleted = 0 AND (:status IS NULL OR status = :status) ORDER BY dueAt IS NULL, dueAt ASC, priority DESC")
    fun observe(familyId: String, status: String?): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks
        WHERE familyId = :familyId AND isDeleted = 0
          AND (:status IS NULL OR status = :status)
          AND (:priority IS NULL OR priority = :priority)
        ORDER BY dueAt IS NULL, dueAt ASC
    """)
    fun paging(familyId: String, status: String?, priority: String?): PagingSource<Int, TaskEntity>

    @Query("""
        SELECT * FROM tasks
        WHERE familyId = :familyId AND isDeleted = 0
          AND (:status IS NULL OR status = :status)
          AND (:priority IS NULL OR priority = :priority)
          AND (title LIKE '%' || :query || '%' OR IFNULL(description,'') LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun search(familyId: String, query: String, status: String?, priority: String?): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaskEntity?

    @Upsert
    suspend fun upsert(entity: TaskEntity)

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, completedAt: Long?, updatedAt: Long)

    @Upsert
    suspend fun upsertAttachment(entity: TaskAttachmentEntity)

    @Query("SELECT * FROM task_attachments WHERE taskId = :taskId")
    fun observeAttachments(taskId: String): Flow<List<TaskAttachmentEntity>>

    @Query("DELETE FROM task_attachments WHERE id = :id")
    suspend fun deleteAttachment(id: String)
}
