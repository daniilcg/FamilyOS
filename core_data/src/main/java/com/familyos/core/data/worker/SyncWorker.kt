package com.familyos.core.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familyos.core.data.sync.SyncQueueRepositoryImpl
import com.familyos.core.logging.FamilyOsLog
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker that drains the offline sync queue when connectivity returns.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncQueueRepository: SyncQueueRepositoryImpl,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val processed = syncQueueRepository.processQueue()
            FamilyOsLog.i(UNIQUE_NAME, "Processed ${processed.getOrNull() ?: 0} sync actions")
            Result.success()
        } catch (t: Throwable) {
            FamilyOsLog.e(UNIQUE_NAME, "Sync worker failed", t)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "familyos_sync_queue"
    }
}
