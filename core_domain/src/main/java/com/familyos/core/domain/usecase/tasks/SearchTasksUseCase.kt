package com.familyos.core.domain.usecase.tasks

import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.model.TaskPriority
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Searches tasks by free text and optional filters. */
class SearchTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    operator fun invoke(
        familyId: String,
        query: String,
        status: TaskStatus? = null,
        priority: TaskPriority? = null,
    ): Flow<List<TaskItem>> = taskRepository.search(familyId, query.trim(), status, priority)
}
