package com.familyos.core.domain.usecase.tasks

import com.familyos.core.domain.logic.RecurrenceExpander
import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Observes tasks and expands recurring seeds into occurrences within [rangeStart, rangeEnd).
 */
class ObserveExpandedTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    operator fun invoke(
        familyId: String,
        rangeStart: Long,
        rangeEnd: Long,
        status: TaskStatus? = null,
    ): Flow<List<TaskItem>> =
        taskRepository.observeTasks(familyId, status).map { tasks ->
            tasks.flatMap { seed ->
                if (seed.recurrence == null) listOf(seed)
                else RecurrenceExpander.expandTasks(seed, rangeStart, rangeEnd)
            }.sortedWith(compareBy({ it.dueAt ?: Long.MAX_VALUE }, { it.priority }))
        }
}
