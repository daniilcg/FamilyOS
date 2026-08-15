package com.familyos.core.domain.repository

import com.familyos.core.domain.model.AiConversation
import com.familyos.core.domain.model.AiMessage
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * AI conversation history and prompt submission.
 */
interface AiRepository {
    fun observeConversations(familyId: String, userId: String): Flow<List<AiConversation>>
    fun observeConversation(conversationId: String): Flow<AiConversation?>
    suspend fun getConversation(conversationId: String): Result<AiConversation>
    suspend fun createConversation(conversation: AiConversation): Result<AiConversation>
    suspend fun appendMessage(conversationId: String, message: AiMessage): Result<AiConversation>
    suspend fun sendPrompt(conversationId: String, userMessage: String, provider: String, apiKeyAlias: String?): Result<AiMessage>
    suspend fun deleteConversation(conversationId: String): Result<Unit>
}
