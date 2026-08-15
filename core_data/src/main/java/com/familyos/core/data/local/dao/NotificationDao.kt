package com.familyos.core.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyos.core.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

/** Data access for notifications. */
@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun observe(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun paging(userId: String): PagingSource<Int, NotificationEntity>

    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND isRead = 0")
    fun observeUnreadCount(userId: String): Flow<Int>

    @Upsert
    suspend fun upsert(entity: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markRead(id: String, updatedAt: Long)

    @Query("UPDATE notifications SET isRead = 1, updatedAt = :updatedAt WHERE userId = :userId AND isRead = 0")
    suspend fun markAllRead(userId: String, updatedAt: Long)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun delete(id: String)
}
