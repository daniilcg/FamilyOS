package com.familyos.core.domain.usecase.home

import com.familyos.core.domain.model.PendingSyncAction
import com.familyos.core.domain.repository.SyncRepository
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes pending sync queue size. */
class ObservePendingSyncCountUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {
    operator fun invoke(): Flow<Int> = syncRepository.observePendingCount()
}

/** Triggers processing of the offline sync queue. */
class ProcessSyncQueueUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {
    suspend operator fun invoke(): Result<Int> = syncRepository.processQueue()
}

/** Observes pending sync actions. */
class ObservePendingSyncActionsUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {
    operator fun invoke(): Flow<List<PendingSyncAction>> = syncRepository.observePendingActions()
}
