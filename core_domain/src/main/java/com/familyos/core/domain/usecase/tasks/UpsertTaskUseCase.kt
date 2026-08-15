package com.familyos.core.domain.usecase.tasks

import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.repository.TaskRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Constants
import com.familyos.core.domain.util.Result
import java.util.UUID
import javax.inject.Inject

/** Creates or updates a task after validation. */
class UpsertTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(task: TaskItem): Result<TaskItem> {
        val title = task.title.trim()
        if (title.isEmpty() || title.length > Constants.MAX_TASK_TITLE_LENGTH) {
            return Result.failure(AppError.Validation("Title is required", "title"))
        }
        if (task.familyId.isBlank()) {
            return Result.failure(AppError.Validation("familyId is required", "familyId"))
        }
        val now = System.currentTimeMillis()
        return taskRepository.upsert(
            task.copy(
                id = task.id.ifBlank { UUID.randomUUID().toString() },
                title = title,
                updatedAt = now,
                createdAt = if (task.createdAt == 0L) now else task.createdAt,
            ),
        )
    }
}
