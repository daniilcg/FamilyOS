package com.familyos.core.domain.repository

import androidx.paging.PagingData
import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.model.TaskPriority
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Task list persistence and queries.
 */
interface TaskRepository {
    fun observeTasks(familyId: String, status: TaskStatus? = null): Flow<List<TaskItem>>
    fun pagingTasks(familyId: String, status: TaskStatus?, priority: TaskPriority?): Flow<PagingData<TaskItem>>
    fun search(familyId: String, query: String, status: TaskStatus?, priority: TaskPriority?): Flow<List<TaskItem>>
    suspend fun getById(id: String): Result<TaskItem>
    suspend fun upsert(task: TaskItem): Result<TaskItem>
    suspend fun delete(id: String): Result<Unit>
    suspend fun updateStatus(id: String, status: TaskStatus): Result<TaskItem>
}
