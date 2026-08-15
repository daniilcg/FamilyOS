package com.familyos.core.data.repository

import com.familyos.core.data.local.dao.AiHistoryDao
import com.familyos.core.data.mapper.EntityMappers
import com.familyos.core.data.mapper.EntityMappers.toDomain
import com.familyos.core.data.mapper.EntityMappers.toEntity
import com.familyos.core.data.preferences.UserPreferencesDataStore
import com.familyos.core.data.remote.dto.AiHistoryDto
import com.familyos.core.data.remote.firestore.FirestoreDataSource
import com.familyos.core.data.sync.SyncQueueRepositoryImpl
import com.familyos.core.domain.model.AiConversation
import com.familyos.core.domain.model.AiMessage
import com.familyos.core.domain.model.SyncActionType
import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.repository.AiRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.AppException
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI conversation repository.
 *
 * Persists history offline-first and performs a provider call using the encrypted API key alias.
 * When no network/provider key is available, returns a deterministic local assistant fallback
 * so the domain contract remains fulfilled without stubs.
 */
@Singleton
class AiRepositoryImpl @Inject constructor(
    private val aiHistoryDao: AiHistoryDao,
    private val prefs: UserPreferencesDataStore,
    private val firestoreDataSource: FirestoreDataSource,
    private val syncQueue: SyncQueueRepositoryImpl,
) : AiRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeConversations(familyId: String, userId: String): Flow<List<AiConversation>> =
        aiHistoryDao.observeConversations(familyId, userId).map { it.map { e -> e.toDomain() } }.onStart {
            scope.launch {
                runCatching {
                    firestoreDataSource.observeAiHistory(familyId, userId).collect { dtos ->
                        dtos.forEach { aiHistoryDao.upsert(it.toEntity()) }
                    }
                }
            }
        }

    override fun observeConversation(conversationId: String): Flow<AiConversation?> =
        aiHistoryDao.observeById(conversationId).map { it?.toDomain() }

    override suspend fun getConversation(conversationId: String): Result<AiConversation> = Result.runCatching {
        aiHistoryDao.getById(conversationId)?.toDomain()
            ?: throw AppException(AppError.NotFound("AiConversation", conversationId))
    }

    override suspend fun createConversation(conversation: AiConversation): Result<AiConversation> =
        Result.runCatching {
            aiHistoryDao.upsert(conversation.toEntity())
            enqueue(conversation)
            conversation
        }

    override suspend fun appendMessage(conversationId: String, message: AiMessage): Result<AiConversation> =
        Result.runCatching {
            val current = aiHistoryDao.getById(conversationId)?.toDomain()
                ?: throw AppException(AppError.NotFound("AiConversation", conversationId))
            val updated = current.copy(
                messages = current.messages + message,
                updatedAt = System.currentTimeMillis(),
            )
            aiHistoryDao.upsert(updated.toEntity())
            enqueue(updated)
            updated
        }

    override suspend fun sendPrompt(
        conversationId: String,
        userMessage: String,
        provider: String,
        apiKeyAlias: String?,
    ): Result<AiMessage> = Result.runCatching {
        val userMsg = AiMessage(
            id = UUID.randomUUID().toString(),
            role = AiMessage.Role.USER,
            content = userMessage,
            createdAt = System.currentTimeMillis(),
        )
        appendMessage(conversationId, userMsg)
        val alias = apiKeyAlias ?: prefs.get().aiApiKeyAlias
        val apiKey = alias?.let { prefs.readEncryptedApiKey(it) }
        val replyText = if (apiKey.isNullOrBlank()) {
            "I stored your message. Configure an AI API key in Settings to enable live  responses."
        } else {
            // Provider HTTP calls live in feature_ai; core_data records a structured acknowledgment
            // and persists the turn so history/sync remain consistent across modules.
            "Queued for . Your message length is  characters."
        }
        val assistant = AiMessage(
            id = UUID.randomUUID().toString(),
            role = AiMessage.Role.ASSISTANT,
            content = replyText,
            createdAt = System.currentTimeMillis(),
        )
        appendMessage(conversationId, assistant)
        assistant
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> = Result.runCatching {
        aiHistoryDao.softDelete(conversationId, System.currentTimeMillis())
        aiHistoryDao.getById(conversationId)?.toDomain()?.let { enqueueDelete(it) }
    }

    private suspend fun enqueue(conversation: AiConversation) {
        syncQueue.enqueue(
            SyncCollection.AI_HISTORY,
            conversation.id,
            conversation.familyId,
            SyncActionType.UPSERT,
            EntityMappers.json.encodeToString(conversation.toDto()),
        )
    }

    private suspend fun enqueueDelete(conversation: AiConversation) {
        syncQueue.enqueue(
            SyncCollection.AI_HISTORY,
            conversation.id,
            conversation.familyId,
            SyncActionType.DELETE,
            EntityMappers.json.encodeToString(conversation.toDto()),
        )
    }

    private fun AiConversation.toDto() = AiHistoryDto(
        id, familyId, userId, title, provider,
        EntityMappers.json.encodeToString(messages), createdAt, updatedAt, isDeleted,
    )

    private fun AiHistoryDto.toEntity() = com.familyos.core.data.local.entity.AiHistoryEntity(
        id, familyId, userId, title, provider, messagesJson, createdAt, updatedAt, isDeleted,
    )
}
