package com.familyos.core.domain.usecase.chat

import com.familyos.core.domain.model.ChatMessage
import com.familyos.core.domain.repository.ChatRepository
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Marks a message as read by the given user (read receipt). */
class MarkMessageReadUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(messageId: String, userId: String): Result<ChatMessage> =
        chatRepository.markRead(messageId, userId)
}
