package com.familyos.core.domain.repository

import androidx.paging.PagingData
import com.familyos.core.domain.model.ChatMessage
import com.familyos.core.domain.model.ChatThread
import com.familyos.core.domain.model.MemberPresence
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Family chat threads and messages with realtime observation.
 */
interface ChatRepository {
    fun observeThreads(familyId: String): Flow<List<ChatThread>>
    fun observeMessages(threadId: String): Flow<List<ChatMessage>>
    fun pagingMessages(threadId: String): Flow<PagingData<ChatMessage>>
    fun observePresence(familyId: String): Flow<List<MemberPresence>>
    suspend fun getThread(threadId: String): Result<ChatThread>
    suspend fun createThread(thread: ChatThread): Result<ChatThread>
    suspend fun sendMessage(message: ChatMessage): Result<ChatMessage>
    suspend fun markRead(messageId: String, userId: String): Result<ChatMessage>
    suspend fun setOnline(familyId: String, userId: String, online: Boolean): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>
    suspend fun deleteThread(threadId: String): Result<Unit>
}
