package com.familyos.core.domain.usecase.ai

import com.familyos.core.domain.model.TaskItem
import com.familyos.core.domain.model.TaskPriority
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.domain.usecase.tasks.UpsertTaskUseCase
import com.familyos.core.domain.util.Result
import java.util.UUID
import javax.inject.Inject

/**
 * Applies a structured AI task-set payload by creating tasks.
 */
class ApplyAiTaskSetUseCase @Inject constructor(
    private val upsertTask: UpsertTaskUseCase,
) {
    data class AiTaskLine(
        val title: String,
        val description: String? = null,
        val priority: TaskPriority = TaskPriority.MEDIUM,
        val dueAt: Long? = null,
    )

    suspend operator fun invoke(
        familyId: String,
        createdBy: String,
        lines: List<AiTaskLine>,
    ): Result<List<TaskItem>> {
        val created = mutableListOf<TaskItem>()
        for (line in lines) {
            val result = upsertTask(
                TaskItem(
                    id = UUID.randomUUID().toString(),
                    familyId = familyId,
                    title = line.title,
                    description = line.description,
                    status = TaskStatus.NEW,
                    priority = line.priority,
                    dueAt = line.dueAt,
                    createdBy = createdBy,
                ),
            )
            when (result) {
                is Result.Success -> created += result.data
                is Result.Error -> return result
            }
        }
        return Result.success(created)
    }
}
