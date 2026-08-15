package com.familyos.core.domain.usecase.chat

import com.familyos.core.domain.model.ChatMessage
import com.familyos.core.domain.model.MessageType
import com.familyos.core.domain.repository.ChatRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Constants
import com.familyos.core.domain.util.Result
import java.util.UUID
import javax.inject.Inject

/** Sends a text, photo, or voice chat message. */
class SendChatMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    suspend operator fun invoke(message: ChatMessage): Result<ChatMessage> {
        if (message.familyId.isBlank() || message.threadId.isBlank() || message.senderId.isBlank()) {
            return Result.failure(AppError.Validation("familyId, threadId, and senderId are required"))
        }
        when (message.type) {
            MessageType.TEXT -> {
                val body = message.body.trim()
                if (body.isEmpty() || body.length > Constants.MAX_CHAT_MESSAGE_LENGTH) {
                    return Result.failure(AppError.Validation("Message text is required", "body"))
                }
            }
            MessageType.IMAGE, MessageType.VOICE, MessageType.FILE -> {
                if (message.attachmentUrl.isNullOrBlank()) {
                    return Result.failure(AppError.Validation("Attachment is required", "attachmentUrl"))
                }
            }
            MessageType.SYSTEM -> Unit
        }
        val now = System.currentTimeMillis()
        return chatRepository.sendMessage(
            message.copy(
                id = message.id.ifBlank { UUID.randomUUID().toString() },
                body = message.body.trim(),
                createdAt = if (message.createdAt == 0L) now else message.createdAt,
                updatedAt = now,
            ),
        )
    }
}
