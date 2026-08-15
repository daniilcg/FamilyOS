package com.familyos.core.domain.usecase.chat

import com.familyos.core.domain.model.ChatThread
import com.familyos.core.domain.repository.ChatRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import java.util.UUID
import javax.inject.Inject

/** Creates a family chat thread when none exists. */
class EnsureFamilyChatThreadUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(
        familyId: String,
        createdBy: String,
        participantIds: List<String>,
        title: String = "Family Chat",
    ): Result<ChatThread> {
        if (familyId.isBlank() || createdBy.isBlank()) {
            return Result.failure(AppError.Validation("familyId and createdBy are required"))
        }
        val now = System.currentTimeMillis()
        return chatRepository.createThread(
            ChatThread(
                id = UUID.randomUUID().toString(),
                familyId = familyId,
                title = title,
                participantIds = participantIds.distinct(),
                createdBy = createdBy,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}
