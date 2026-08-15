package com.familyos.core.domain.repository

import com.familyos.core.domain.model.PendingSyncAction
import com.familyos.core.domain.model.SyncActionType
import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.model.SyncConflict
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Offline sync queue and conflict surface.
 */
interface SyncRepository {
    fun observePendingActions(): Flow<List<PendingSyncAction>>
    fun observePendingCount(): Flow<Int>
    suspend fun enqueue(
        collection: SyncCollection,
        documentId: String,
        familyId: String?,
        actionType: SyncActionType,
        payloadJson: String,
    ): Result<PendingSyncAction>
    suspend fun processQueue(): Result<Int>
    suspend fun resolveConflict(conflict: SyncConflict, preferMerge: Boolean = true): Result<String>
    suspend fun clearCompleted(): Result<Unit>
}
