package com.familyos.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyos.core.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

/** Data access for calendar events. */
@Dao
interface EventDao {
    @Query("""
        SELECT * FROM events
        WHERE familyId = :familyId AND isDeleted = 0
          AND startAt < :rangeEnd AND endAt > :rangeStart
        ORDER BY startAt ASC
    """)
    fun observeRange(familyId: String, rangeStart: Long, rangeEnd: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE familyId = :familyId AND isDeleted = 0 AND type = :type ORDER BY startAt ASC")
    fun observeByType(familyId: String, type: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EventEntity?

    @Upsert
    suspend fun upsert(entity: EventEntity)

    @Query("UPDATE events SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)
}
