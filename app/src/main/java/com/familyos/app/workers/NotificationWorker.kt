package com.familyos.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familyos.app.notifications.NotificationHelper
import com.familyos.core.domain.model.AppNotification
import com.familyos.core.domain.model.NotificationType
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.domain.repository.CalendarRepository
import com.familyos.core.domain.repository.NotificationRepository
import com.familyos.core.domain.repository.TaskRepository
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Scans for due tasks and upcoming events, then creates / displays reminder notifications.
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val calendarRepository: CalendarRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationHelper: NotificationHelper,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = userPreferencesRepository.get()
            if (!prefs.notificationsEnabled) {
                Timber.d("NotificationWorker skipped — notifications disabled")
                return Result.success()
            }
            val user = getCurrentUser() ?: return Result.success()
            val familyId = user.familyId ?: prefs.activeFamilyId ?: return Result.success()

            val now = System.currentTimeMillis()
            val horizon = now + 60L * 60L * 1000L // next hour
            val dayAhead = now + 24L * 60L * 60L * 1000L

            val openTasks = taskRepository.observeTasks(familyId, TaskStatus.NEW).first() +
                taskRepository.observeTasks(familyId, TaskStatus.IN_PROGRESS).first() +
                taskRepository.observeTasks(familyId, TaskStatus.WAITING).first()
            val dueSoon = openTasks.filter { task ->
                val due = task.dueAt ?: return@filter false
                due in (now - 15L * 60L * 1000L)..horizon
            }

            for (task in dueSoon) {
                val notification = AppNotification(
                    id = "task_due_${task.id}_${task.dueAt}",
                    userId = user.id,
                    familyId = familyId,
                    type = NotificationType.TASK_DUE,
                    title = "Task due soon",
                    body = task.title,
                    payloadJson = """{"taskId":"${task.id}"}""",
                    createdAt = now,
                    updatedAt = now,
                )
                notificationRepository.upsert(notification)
                notificationHelper.show(notification)
            }

            val events = calendarRepository.observeEvents(familyId, now, dayAhead).first()
            for (event in events) {
                val reminderMs = (event.reminderMinutes ?: 30) * 60L * 1000L
                val triggerAt = event.startAt - reminderMs
                if (triggerAt in (now - 15L * 60L * 1000L)..horizon) {
                    val notification = AppNotification(
                        id = "event_reminder_${event.id}_${event.startAt}",
                        userId = user.id,
                        familyId = familyId,
                        type = NotificationType.EVENT_REMINDER,
                        title = "Upcoming event",
                        body = event.title,
                        payloadJson = """{"eventId":"${event.id}"}""",
                        createdAt = now,
                        updatedAt = now,
                    )
                    notificationRepository.upsert(notification)
                    notificationHelper.show(notification)
                }
            }

            Timber.i(
                "NotificationWorker finished dueTasks=%d events=%d",
                dueSoon.size,
                events.size,
            )
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "NotificationWorker crashed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val UNIQUE_NAME = "familyos_notification_worker"
    }
}
