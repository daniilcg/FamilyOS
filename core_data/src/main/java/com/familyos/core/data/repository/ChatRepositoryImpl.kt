package com.familyos.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.familyos.core.data.local.dao.ChatDao
import com.familyos.core.data.mapper.EntityMappers
import com.familyos.core.data.mapper.EntityMappers.toDomain
import com.familyos.core.data.mapper.EntityMappers.toEntity
import com.familyos.core.data.remote.dto.ChatMessageDto
import com.familyos.core.data.remote.dto.ChatThreadDto
import com.familyos.core.data.remote.firestore.FirestoreDataSource
import com.familyos.core.data.sync.SyncQueueRepositoryImpl
import com.familyos.core.domain.model.ChatMessage
import com.familyos.core.domain.model.ChatThread
import com.familyos.core.domain.model.SyncActionType
import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.repository.ChatRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.AppException
import com.familyos.core.domain.util.Constants
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/** Offline-first chat repository. */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val syncQueue: SyncQueueRepositoryImpl,
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeThreads(familyId: String): Flow<List<ChatThread>> =
        chatDao.observeThreads(familyId).map { it.map { e -> e.toDomain() } }.onStart {
            scope.launch {
                runCatching {
                    firestoreDataSource.observeChatThreads(familyId).collect { dtos ->
                        dtos.forEach { chatDao.upsertThread(it.toEntity()) }
                    }
                }
            }
        }

    override fun observeMessages(threadId: String): Flow<List<ChatMessage>> =
        chatDao.observeMessages(threadId).map { it.map { e -> e.toDomain() } }.onStart {
            scope.launch {
                val thread = chatDao.getThread(threadId) ?: return@launch
                runCatching {
                    firestoreDataSource.observeChatMessages(thread.familyId, threadId).collect { dtos ->
                        dtos.forEach { chatDao.upsertMessage(it.toEntity()) }
                    }
                }
            }
        }

    override fun pagingMessages(threadId: String): Flow<PagingData<ChatMessage>> =
        Pager(PagingConfig(Constants.DEFAULT_PAGE_SIZE)) { chatDao.pagingMessages(threadId) }
            .flow.map { it.map { e -> e.toDomain() } }

    override suspend fun getThread(threadId: String): Result<ChatThread> = Result.runCatching {
        chatDao.getThread(threadId)?.toDomain() ?: throw AppException(AppError.NotFound("ChatThread", threadId))
    }

    override suspend fun createThread(thread: ChatThread): Result<ChatThread> = Result.runCatching {
        chatDao.upsertThread(thread.toEntity())
        syncQueue.enqueue(SyncCollection.CHAT, thread.id, thread.familyId, SyncActionType.UPSERT,
            EntityMappers.json.encodeToString(thread.toDto()))
        thread
    }

    override suspend fun sendMessage(message: ChatMessage): Result<ChatMessage> = Result.runCatching {
        chatDao.upsertMessage(message.toEntity())
        chatDao.updateThreadPreview(message.threadId, message.body.take(120), message.createdAt)
        syncQueue.enqueue(SyncCollection.MESSAGES, message.id, message.familyId, SyncActionType.UPSERT,
            EntityMappers.json.encodeToString(message.toDto()))
        message
    }

    override fun observePresence(familyId: String): Flow<List<com.familyos.core.domain.model.MemberPresence>> =
        kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun markRead(messageId: String, userId: String): Result<ChatMessage> =
        Result.runCatching {
            val existing = chatDao.getMessage(messageId)?.toDomain()
                ?: throw AppException(AppError.NotFound("ChatMessage", messageId))
            val updated = existing.copy(
                readBy = (existing.readBy + userId).distinct(),
                updatedAt = System.currentTimeMillis(),
            )
            chatDao.upsertMessage(updated.toEntity())
            syncQueue.enqueue(
                SyncCollection.MESSAGES,
                messageId,
                updated.familyId,
                SyncActionType.UPSERT,
                EntityMappers.json.encodeToString(updated.toDto()),
            )
            updated
        }

    override suspend fun setOnline(familyId: String, userId: String, online: Boolean): Result<Unit> =
        Result.success(Unit)

    override suspend fun deleteMessage(messageId: String): Result<Unit> = Result.runCatching {
        chatDao.softDeleteMessage(messageId, System.currentTimeMillis())
        chatDao.getMessage(messageId)?.toDomain()?.let {
            syncQueue.enqueue(SyncCollection.MESSAGES, messageId, it.familyId, SyncActionType.DELETE,
                EntityMappers.json.encodeToString(it.toDto()))
        }
    }

    override suspend fun deleteThread(threadId: String): Result<Unit> = Result.runCatching {
        chatDao.softDeleteThread(threadId, System.currentTimeMillis())
        chatDao.getThread(threadId)?.toDomain()?.let {
            syncQueue.enqueue(SyncCollection.CHAT, threadId, it.familyId, SyncActionType.DELETE,
                EntityMappers.json.encodeToString(it.toDto()))
        }
    }

    private fun ChatThread.toDto() = ChatThreadDto(
        id, familyId, title, participantIds, lastMessagePreview, lastMessageAt,
        createdBy, createdAt, updatedAt, isDeleted,
    )

    private fun ChatThreadDto.toEntity() = com.familyos.core.data.local.entity.ChatThreadEntity(
        id, familyId, title, participantIds.joinToString(","), lastMessagePreview, lastMessageAt,
        createdBy, createdAt, updatedAt, isDeleted,
    )

    private fun ChatMessage.toDto() = ChatMessageDto(
        id, threadId, familyId, senderId, type.name, body, attachmentUrl, createdAt, updatedAt, isDeleted,
    )

    private fun ChatMessageDto.toEntity() = com.familyos.core.data.local.entity.ChatMessageEntity(
        id = id,
        threadId = threadId,
        familyId = familyId,
        senderId = senderId,
        type = type,
        body = body,
        attachmentUrl = attachmentUrl,
        durationMs = null,
        readByCsv = "",
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )
}
