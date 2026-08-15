package com.familyos.core.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyos.core.data.local.entity.ChatMessageEntity
import com.familyos.core.data.local.entity.ChatThreadEntity
import kotlinx.coroutines.flow.Flow

/** Data access for chat. */
@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_threads WHERE familyId = :familyId AND isDeleted = 0 ORDER BY IFNULL(lastMessageAt, createdAt) DESC")
    fun observeThreads(familyId: String): Flow<List<ChatThreadEntity>>

    @Query("SELECT * FROM chat_threads WHERE id = :id LIMIT 1")
    suspend fun getThread(id: String): ChatThreadEntity?

    @Upsert
    suspend fun upsertThread(entity: ChatThreadEntity)

    @Query("UPDATE chat_threads SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteThread(id: String, updatedAt: Long)

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId AND isDeleted = 0 ORDER BY createdAt ASC")
    fun observeMessages(threadId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun pagingMessages(threadId: String): PagingSource<Int, ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    suspend fun getMessage(id: String): ChatMessageEntity?

    @Upsert
    suspend fun upsertMessage(entity: ChatMessageEntity)

    @Query("UPDATE chat_messages SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteMessage(id: String, updatedAt: Long)

    @Query("""
        UPDATE chat_threads
        SET lastMessagePreview = :preview, lastMessageAt = :at, updatedAt = :at
        WHERE id = :threadId
    """)
    suspend fun updateThreadPreview(threadId: String, preview: String, at: Long)
}
