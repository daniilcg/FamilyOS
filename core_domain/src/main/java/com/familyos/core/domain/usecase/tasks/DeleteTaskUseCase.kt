package com.familyos.core.domain.usecase.tasks

import com.familyos.core.domain.repository.TaskRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Deletes a task by id. */
class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        if (id.isBlank()) return Result.failure(AppError.Validation("id required", "id"))
        return taskRepository.delete(id)
    }
}
