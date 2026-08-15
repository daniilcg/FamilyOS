package com.familyos.core.domain.usecase.tasks

import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.repository.TaskRepository
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Loads a single task by id. */
class GetTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(id: String): Result<TaskItem> = taskRepository.getById(id)
}
