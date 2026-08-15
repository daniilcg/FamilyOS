package com.familyos.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyos.core.data.local.entity.AiHistoryEntity
import kotlinx.coroutines.flow.Flow

/** Data access for AI history. */
@Dao
interface AiHistoryDao {
    @Query("SELECT * FROM ai_history WHERE familyId = :familyId AND userId = :userId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeConversations(familyId: String, userId: String): Flow<List<AiHistoryEntity>>

    @Query("SELECT * FROM ai_history WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<AiHistoryEntity?>

    @Query("SELECT * FROM ai_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AiHistoryEntity?

    @Upsert
    suspend fun upsert(entity: AiHistoryEntity)

    @Query("UPDATE ai_history SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)
}
