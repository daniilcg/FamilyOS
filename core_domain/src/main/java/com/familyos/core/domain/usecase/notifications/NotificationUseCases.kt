package com.familyos.core.domain.usecase.notifications

import com.familyos.core.domain.model.AppNotification
import com.familyos.core.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import com.familyos.core.domain.util.Result

/** Observes notifications for a user. */
class ObserveNotificationsUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    operator fun invoke(userId: String): Flow<List<AppNotification>> =
        notificationRepository.observeNotifications(userId)
}

/** Observes unread notification count. */
class ObserveUnreadNotificationCountUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    operator fun invoke(userId: String): Flow<Int> = notificationRepository.observeUnreadCount(userId)
}

/** Marks one notification as read. */
class MarkNotificationReadUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = notificationRepository.markRead(id)
}

/** Marks all notifications as read for a user. */
class MarkAllNotificationsReadUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(userId: String): Result<Unit> = notificationRepository.markAllRead(userId)
}

/** Deletes a notification. */
class DeleteNotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = notificationRepository.delete(id)
}
