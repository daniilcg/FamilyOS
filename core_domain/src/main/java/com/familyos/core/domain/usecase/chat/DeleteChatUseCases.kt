package com.familyos.core.domain.usecase.chat

import com.familyos.core.domain.repository.ChatRepository
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Deletes a chat message. */
class DeleteChatMessageUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(messageId: String): Result<Unit> = chatRepository.deleteMessage(messageId)
}

/** Deletes a chat thread. */
class DeleteChatThreadUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    suspend operator fun invoke(threadId: String): Result<Unit> = chatRepository.deleteThread(threadId)
}
