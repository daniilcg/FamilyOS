package com.familyos.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyos.core.data.local.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

/** Data access for sync queue. */
@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync ORDER BY nextAttemptAt ASC, createdAt ASC")
    fun observeAll(): Flow<List<PendingSyncEntity>>

    @Query("SELECT COUNT(*) FROM pending_sync")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM pending_sync WHERE nextAttemptAt <= :now ORDER BY nextAttemptAt ASC LIMIT :limit")
    suspend fun dueActions(now: Long, limit: Int): List<PendingSyncEntity>

    @Upsert
    suspend fun upsert(entity: PendingSyncEntity)

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE pending_sync SET attemptCount = :attemptCount, lastError = :lastError, nextAttemptAt = :nextAttemptAt WHERE id = :id")
    suspend fun markAttempt(id: String, attemptCount: Int, lastError: String?, nextAttemptAt: Long)

    @Query("DELETE FROM pending_sync")
    suspend fun clearAll()
}
