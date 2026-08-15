package com.familyos.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.familyos.core.data.local.dao.NotificationDao
import com.familyos.core.data.mapper.EntityMappers
import com.familyos.core.data.mapper.EntityMappers.toDomain
import com.familyos.core.data.mapper.EntityMappers.toEntity
import com.familyos.core.data.remote.dto.NotificationDto
import com.familyos.core.data.remote.firestore.FirestoreDataSource
import com.familyos.core.data.sync.SyncQueueRepositoryImpl
import com.familyos.core.domain.model.AppNotification
import com.familyos.core.domain.model.SyncActionType
import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.repository.NotificationRepository
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

/** Offline-first notification repository. */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val syncQueue: SyncQueueRepositoryImpl,
) : NotificationRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeNotifications(userId: String): Flow<List<AppNotification>> =
        notificationDao.observe(userId).map { it.map { e -> e.toDomain() } }.onStart {
            scope.launch {
                runCatching {
                    firestoreDataSource.observeNotifications(userId).collect { dtos ->
                        dtos.forEach { notificationDao.upsert(it.toEntity()) }
                    }
                }
            }
        }

    override fun pagingNotifications(userId: String): Flow<PagingData<AppNotification>> =
        Pager(PagingConfig(Constants.DEFAULT_PAGE_SIZE)) { notificationDao.paging(userId) }
            .flow.map { it.map { e -> e.toDomain() } }

    override fun observeUnreadCount(userId: String): Flow<Int> = notificationDao.observeUnreadCount(userId)

    override suspend fun upsert(notification: AppNotification): Result<AppNotification> = Result.runCatching {
        notificationDao.upsert(notification.toEntity())
        syncQueue.enqueue(SyncCollection.NOTIFICATIONS, notification.id, notification.familyId,
            SyncActionType.UPSERT, EntityMappers.json.encodeToString(notification.toDto()))
        notification
    }

    override suspend fun markRead(id: String): Result<Unit> = Result.runCatching {
        notificationDao.markRead(id, System.currentTimeMillis())
    }

    override suspend fun markAllRead(userId: String): Result<Unit> = Result.runCatching {
        notificationDao.markAllRead(userId, System.currentTimeMillis())
    }

    override suspend fun delete(id: String): Result<Unit> = Result.runCatching {
        notificationDao.delete(id)
    }

    private fun AppNotification.toDto() = NotificationDto(
        id, userId, familyId, type.name, title, body, payloadJson, isRead, createdAt, updatedAt,
    )

    private fun NotificationDto.toEntity() = com.familyos.core.data.local.entity.NotificationEntity(
        id, userId, familyId, type, title, body, payloadJson, isRead, createdAt, updatedAt,
    )
}
