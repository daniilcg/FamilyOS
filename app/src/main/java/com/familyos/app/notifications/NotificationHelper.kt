package com.familyos.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.familyos.app.MainActivity
import com.familyos.app.R
import com.familyos.core.domain.model.AppNotification
import com.familyos.core.domain.model.NotificationType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates notification channels and displays local / FCM-backed notifications.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Ensures all FamilyOS notification channels exist (idempotent).
     */
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(
                CHANNEL_GENERAL,
                context.getString(R.string.channel_general_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.channel_general_desc) },
            NotificationChannel(
                CHANNEL_TASKS,
                context.getString(R.string.channel_tasks_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.channel_tasks_desc) },
            NotificationChannel(
                CHANNEL_FAMILY,
                context.getString(R.string.channel_family_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.channel_family_desc) },
            NotificationChannel(
                CHANNEL_SYNC,
                context.getString(R.string.channel_sync_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.channel_sync_desc) },
        )
        manager.createNotificationChannels(channels)
    }

    /**
     * Shows a notification for a domain [AppNotification].
     */
    fun show(notification: AppNotification) {
        val channel = channelFor(notification.type)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ID, notification.id)
            putExtra(EXTRA_NOTIFICATION_TYPE, notification.type.name)
        }
        val pending = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(
                if (channel == CHANNEL_TASKS) {
                    NotificationCompat.PRIORITY_HIGH
                } else {
                    NotificationCompat.PRIORITY_DEFAULT
                },
            )
        NotificationManagerCompat.from(context).notify(notification.id.hashCode(), builder.build())
    }

    /**
     * Shows a simple title/body notification on [channelId].
     */
    fun showSimple(
        id: Int,
        title: String,
        body: String,
        channelId: String = CHANNEL_GENERAL,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun channelFor(type: NotificationType): String = when (type) {
        NotificationType.NEW_TASK,
        NotificationType.TASK_ASSIGNED,
        NotificationType.TASK_DUE,
        -> CHANNEL_TASKS
        NotificationType.MEMBER_JOINED,
        NotificationType.FAMILY_INVITE,
        NotificationType.CHAT_MESSAGE,
        -> CHANNEL_FAMILY
        NotificationType.SYSTEM -> CHANNEL_SYNC
        else -> CHANNEL_GENERAL
    }

    companion object {
        const val CHANNEL_GENERAL = "familyos_general"
        const val CHANNEL_TASKS = "familyos_tasks"
        const val CHANNEL_FAMILY = "familyos_family"
        const val CHANNEL_SYNC = "familyos_sync"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_NOTIFICATION_TYPE = "extra_notification_type"
    }
}
