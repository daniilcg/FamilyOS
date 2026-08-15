package com.familyos.core.domain.usecase.ai

import com.familyos.core.domain.model.AiConversation
import com.familyos.core.domain.model.AiMessage
import com.familyos.core.domain.repository.AiRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Constants
import com.familyos.core.domain.util.Result
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Creates an AI conversation. */
class CreateAiConversationUseCase @Inject constructor(private val aiRepository: AiRepository) {
    suspend operator fun invoke(conversation: AiConversation): Result<AiConversation> {
        if (conversation.familyId.isBlank() || conversation.userId.isBlank()) {
            return Result.failure(AppError.Validation("familyId and userId required"))
        }
        val now = System.currentTimeMillis()
        return aiRepository.createConversation(
            conversation.copy(
                id = conversation.id.ifBlank { UUID.randomUUID().toString() },
                title = conversation.title.ifBlank { "New chat" },
                createdAt = if (conversation.createdAt == 0L) now else conversation.createdAt,
                updatedAt = now,
            ),
        )
    }
}

/** Sends a prompt and returns the assistant reply. */
class SendAiPromptUseCase @Inject constructor(private val aiRepository: AiRepository) {
    suspend operator fun invoke(
        conversationId: String,
        userMessage: String,
        provider: String,
        apiKeyAlias: String?,
    ): Result<AiMessage> {
        if (conversationId.isBlank()) return Result.failure(AppError.Validation("conversationId required"))
        val body = userMessage.trim()
        if (body.isEmpty() || body.length > Constants.MAX_AI_MESSAGE_LENGTH) {
            return Result.failure(AppError.Validation("Message required", "userMessage"))
        }
        return aiRepository.sendPrompt(conversationId, body, provider, apiKeyAlias)
    }
}

/** Observes AI conversations for a user in a family. */
class ObserveAiConversationsUseCase @Inject constructor(private val aiRepository: AiRepository) {
    operator fun invoke(familyId: String, userId: String): Flow<List<AiConversation>> =
        aiRepository.observeConversations(familyId, userId)
}

/** Observes a single AI conversation. */
class ObserveAiConversationUseCase @Inject constructor(private val aiRepository: AiRepository) {
    operator fun invoke(conversationId: String): Flow<AiConversation?> =
        aiRepository.observeConversation(conversationId)
}

/** Deletes an AI conversation. */
class DeleteAiConversationUseCase @Inject constructor(private val aiRepository: AiRepository) {
    suspend operator fun invoke(conversationId: String): Result<Unit> =
        aiRepository.deleteConversation(conversationId)
}
