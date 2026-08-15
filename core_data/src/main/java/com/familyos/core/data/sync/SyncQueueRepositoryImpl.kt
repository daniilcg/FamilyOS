package com.familyos.core.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.familyos.core.data.local.dao.PendingSyncDao
import com.familyos.core.data.local.entity.PendingSyncEntity
import com.familyos.core.data.mapper.EntityMappers.toDomain
import com.familyos.core.data.mapper.EntityMappers.toEntity
import com.familyos.core.data.remote.firestore.FirestoreDataSource
import com.familyos.core.data.worker.SyncWorker
import com.familyos.core.domain.model.PendingSyncAction
import com.familyos.core.domain.model.SyncActionType
import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.model.SyncConflict
import com.familyos.core.domain.repository.SyncRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Constants
import com.familyos.core.domain.util.Result
import com.familyos.core.logging.FamilyOsLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first sync queue: Room enqueue + WorkManager drain + Firestore push.
 */
@Singleton
class SyncQueueRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingSyncDao: PendingSyncDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val conflictResolver: ConflictResolverImpl,
) : SyncRepository {

    override fun observePendingActions(): Flow<List<PendingSyncAction>> =
        pendingSyncDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observePendingCount(): Flow<Int> = pendingSyncDao.observeCount()

    override suspend fun enqueue(
        collection: SyncCollection,
        documentId: String,
        familyId: String?,
        actionType: SyncActionType,
        payloadJson: String,
    ): Result<PendingSyncAction> = Result.runCatching {
        val entity = PendingSyncEntity(
            id = UUID.randomUUID().toString(),
            collection = collection.name,
            documentId = documentId,
            familyId = familyId,
            actionType = actionType.name,
            payloadJson = payloadJson,
            createdAt = System.currentTimeMillis(),
            attemptCount = 0,
            lastError = null,
            nextAttemptAt = System.currentTimeMillis(),
        )
        pendingSyncDao.upsert(entity)
        scheduleWorker()
        entity.toDomain()
    }

    override suspend fun processQueue(): Result<Int> = Result.runCatching {
        val due = pendingSyncDao.dueActions(System.currentTimeMillis(), Constants.SYNC_BATCH_SIZE)
        var processed = 0
        for (action in due) {
            try {
                when (SyncActionType.valueOf(action.actionType)) {
                    SyncActionType.UPSERT -> firestoreDataSource.upsertRaw(
                        collection = collectionPath(action.collection),
                        familyId = action.familyId,
                        documentId = action.documentId,
                        payloadJson = action.payloadJson,
                    )
                    SyncActionType.DELETE -> firestoreDataSource.deleteRaw(
                        collection = collectionPath(action.collection),
                        familyId = action.familyId,
                        documentId = action.documentId,
                    )
                }
                pendingSyncDao.delete(action.id)
                processed++
            } catch ( thr: Throwable) {
                FamilyOsLog.w("SyncQueue", "Failed sync ${action.id}: ${thr.message}", thr)
                val attempts = action.attemptCount + 1
                val delayMs = (attempts * attempts * 5_000L).coerceAtMost(15 * 60_000L)
                pendingSyncDao.markAttempt(
                    id = action.id,
                    attemptCount = attempts,
                    lastError = thr.message,
                    nextAttemptAt = System.currentTimeMillis() + delayMs,
                )
            }
        }
        processed
    }

    override suspend fun resolveConflict(conflict: SyncConflict, preferMerge: Boolean): Result<String> =
        Result.runCatching {
            val resolution = conflictResolver.resolve(conflict, preferMerge)
            firestoreDataSource.upsertRaw(
                collection = collectionPath(conflict.collection.name),
                familyId = null,
                documentId = conflict.documentId,
                payloadJson = resolution.winningPayloadJson,
            )
            resolution.winningPayloadJson
        }

    override suspend fun clearCompleted(): Result<Unit> = Result.success(Unit)

    /** Schedules a unique one-time sync worker when network is available. */
    fun scheduleWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun collectionPath(collectionName: String): String = when (collectionName) {
        SyncCollection.USERS.name -> Constants.COLLECTION_USERS
        SyncCollection.FAMILIES.name -> Constants.COLLECTION_FAMILIES
        SyncCollection.MEMBERS.name -> Constants.COLLECTION_MEMBERS
        SyncCollection.SHOPPING.name -> Constants.COLLECTION_SHOPPING
        SyncCollection.TASKS.name -> Constants.COLLECTION_TASKS
        SyncCollection.EVENTS.name -> Constants.COLLECTION_EVENTS
        SyncCollection.BUDGETS.name -> Constants.COLLECTION_BUDGETS
        SyncCollection.DOCUMENTS.name -> Constants.COLLECTION_DOCUMENTS
        SyncCollection.NOTES.name -> Constants.COLLECTION_NOTES
        SyncCollection.CHAT.name -> Constants.COLLECTION_CHAT
        SyncCollection.MESSAGES.name -> Constants.COLLECTION_MESSAGES
        SyncCollection.NOTIFICATIONS.name -> Constants.COLLECTION_NOTIFICATIONS
        SyncCollection.AI_HISTORY.name -> Constants.COLLECTION_AI_HISTORY
        else -> collectionName.lowercase()
    }
}
