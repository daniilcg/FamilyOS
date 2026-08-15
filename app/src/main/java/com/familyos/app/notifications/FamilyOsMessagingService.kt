package com.familyos.app.notifications

import com.familyos.core.domain.model.AppNotification
import com.familyos.core.domain.model.NotificationType
import com.familyos.core.domain.repository.NotificationRepository
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service that persists and displays remote notifications.
 */
@AndroidEntryPoint
class FamilyOsMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var getCurrentUser: GetCurrentUserUseCase

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        Timber.d("FCM token refreshed: %s…", token.take(12))
        // Token persistence is handled by the data layer when a user session is active.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Timber.d("FCM message from=%s data=%s", message.from, message.data)
        serviceScope.launch {
            val prefs = userPreferencesRepository.get()
            if (!prefs.notificationsEnabled) {
                Timber.d("Notifications disabled — dropping FCM message")
                return@launch
            }
            val user = getCurrentUser() ?: return@launch
            val title = message.notification?.title
                ?: message.data["title"]
                ?: getString(com.familyos.app.R.string.app_name)
            val body = message.notification?.body
                ?: message.data["body"]
                ?: ""
            val type = runCatching {
                NotificationType.valueOf(message.data["type"] ?: NotificationType.SYSTEM.name)
            }.getOrDefault(NotificationType.SYSTEM)
            val now = System.currentTimeMillis()
            val notification = AppNotification(
                id = message.messageId ?: UUID.randomUUID().toString(),
                userId = user.id,
                familyId = message.data["familyId"] ?: user.familyId,
                type = type,
                title = title,
                body = body,
                payloadJson = message.data["payload"],
                isRead = false,
                createdAt = now,
                updatedAt = now,
            )
            notificationRepository.upsert(notification)
            notificationHelper.show(notification)
        }
    }
}
