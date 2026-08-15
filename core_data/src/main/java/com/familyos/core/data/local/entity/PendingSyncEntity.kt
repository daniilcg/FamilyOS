package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for pending sync queue actions. */
@Entity(
    tableName = "pending_sync",
    indices = [
        Index(value = ["nextAttemptAt"]),
        Index(value = ["collection", "documentId"]),
    ],
)
data class PendingSyncEntity(
    @PrimaryKey val id: String,
    val collection: String,
    val documentId: String,
    val familyId: String?,
    val actionType: String,
    val payloadJson: String,
    val createdAt: Long,
    val attemptCount: Int,
    val lastError: String?,
    val nextAttemptAt: Long,
)
