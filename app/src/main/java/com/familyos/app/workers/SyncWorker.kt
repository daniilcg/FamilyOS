package com.familyos.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familyos.core.domain.usecase.home.ProcessSyncQueueUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Periodically drains the offline-first sync queue to Firestore.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val processSyncQueue: ProcessSyncQueueUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            when (val outcome = processSyncQueue()) {
                is com.familyos.core.domain.util.Result.Success -> {
                    Timber.i("SyncWorker processed %d actions", outcome.data)
                    Result.success()
                }
                is com.familyos.core.domain.util.Result.Error -> {
                    Timber.w("SyncWorker failed: %s", outcome.error.message)
                    if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker crashed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val UNIQUE_NAME = "familyos_sync_worker"
    }
}
