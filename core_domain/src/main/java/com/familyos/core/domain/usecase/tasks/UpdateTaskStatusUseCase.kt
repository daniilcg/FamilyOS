package com.familyos.core.domain.usecase.tasks

import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.domain.repository.TaskRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Updates task status and completion timestamp. */
class UpdateTaskStatusUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(id: String, status: TaskStatus): Result<TaskItem> {
        if (id.isBlank()) return Result.failure(AppError.Validation("id required", "id"))
        return taskRepository.updateStatus(id, status)
    }
}
