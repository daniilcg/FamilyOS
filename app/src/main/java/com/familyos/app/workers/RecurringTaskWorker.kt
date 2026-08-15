package com.familyos.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familyos.core.domain.logic.RecurrenceExpander
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.domain.repository.TaskRepository
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import com.familyos.core.domain.util.Result as DomainResult

/**
 * Materializes upcoming occurrences for recurring tasks into the local task store.
 */
@HiltWorker
class RecurringTaskWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val user = getCurrentUser()
            val familyId = user?.familyId
                ?: userPreferencesRepository.get().activeFamilyId
                ?: return Result.success()

            val now = System.currentTimeMillis()
            val rangeEnd = Instant.ofEpochMilli(now)
                .atZone(ZoneOffset.UTC)
                .plus(14, ChronoUnit.DAYS)
                .toInstant()
                .toEpochMilli()

            val seeds = taskRepository.observeTasks(familyId, status = null).first()
                .filter {
                    it.recurrence != null &&
                        !it.isDeleted &&
                        it.status != TaskStatus.DONE &&
                        it.status != TaskStatus.CANCELLED
                }

            var created = 0
            for (seed in seeds) {
                val occurrences = RecurrenceExpander.expandTasks(seed, now, rangeEnd)
                    .filter { it.id != seed.id }
                for (occurrence in occurrences) {
                    // Persist concrete occurrences without the recurrence rule to avoid re-expansion loops.
                    val materialised = occurrence.copy(recurrence = null)
                    when (val upsert = taskRepository.upsert(materialised)) {
                        is DomainResult.Success -> created++
                        is DomainResult.Error -> {
                            Timber.w(
                                "Failed to materialise occurrence %s: %s",
                                materialised.id,
                                upsert.error.message,
                            )
                        }
                    }
                }
            }
            Timber.i("RecurringTaskWorker materialised %d occurrences", created)
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "RecurringTaskWorker crashed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val UNIQUE_NAME = "familyos_recurring_task_worker"
    }
}
