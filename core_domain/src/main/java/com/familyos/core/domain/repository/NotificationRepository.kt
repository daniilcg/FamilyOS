package com.familyos.core.domain.repository

import androidx.paging.PagingData
import com.familyos.core.domain.model.AppNotification
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * In-app notifications for a user.
 */
interface NotificationRepository {
    fun observeNotifications(userId: String): Flow<List<AppNotification>>
    fun pagingNotifications(userId: String): Flow<PagingData<AppNotification>>
    fun observeUnreadCount(userId: String): Flow<Int>
    suspend fun upsert(notification: AppNotification): Result<AppNotification>
    suspend fun markRead(id: String): Result<Unit>
    suspend fun markAllRead(userId: String): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}
