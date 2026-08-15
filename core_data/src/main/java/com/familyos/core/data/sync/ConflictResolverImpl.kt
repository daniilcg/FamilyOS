package com.familyos.core.data.sync

import com.familyos.core.domain.logic.ConflictResolver
import com.familyos.core.domain.model.SyncConflict
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable wrapper around the domain [ConflictResolver] pure logic.
 */
@Singleton
class ConflictResolverImpl @Inject constructor() {

    /**
     * Resolves [conflict] and returns the winning JSON payload.
     */
    fun resolve(conflict: SyncConflict, preferMerge: Boolean = true): ConflictResolver.Resolution =
        ConflictResolver.resolve(conflict, preferMerge)
}
