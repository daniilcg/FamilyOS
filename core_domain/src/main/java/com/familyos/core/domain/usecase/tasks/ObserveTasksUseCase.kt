package com.familyos.core.domain.usecase.tasks

import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes non-expanded tasks for a family. */
class ObserveTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    operator fun invoke(familyId: String, status: TaskStatus? = null): Flow<List<TaskItem>> =
        taskRepository.observeTasks(familyId, status)
}
