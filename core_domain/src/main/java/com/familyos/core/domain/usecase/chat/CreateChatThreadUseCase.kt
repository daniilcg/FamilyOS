package com.familyos.core.domain.usecase.chat

import com.familyos.core.domain.model.ChatThread
import com.familyos.core.domain.repository.ChatRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import java.util.UUID
import javax.inject.Inject

/** Creates a chat thread. */
class CreateChatThreadUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(thread: ChatThread): Result<ChatThread> {
        if (thread.familyId.isBlank()) return Result.failure(AppError.Validation("familyId required"))
        if (thread.title.isBlank()) return Result.failure(AppError.Validation("Title required", "title"))
        val now = System.currentTimeMillis()
        return chatRepository.createThread(
            thread.copy(
                id = thread.id.ifBlank { UUID.randomUUID().toString() },
                title = thread.title.trim(),
                createdAt = if (thread.createdAt == 0L) now else thread.createdAt,
                updatedAt = now,
            ),
        )
    }
}
