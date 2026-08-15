package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Sync operation kinds queued for WorkManager processing.
 */
@Serializable
enum class SyncActionType {
    UPSERT,
    DELETE,
}

/**
 * Collection identifiers used by the offline sync queue.
 */
@Serializable
enum class SyncCollection {
    USERS,
    FAMILIES,
    MEMBERS,
    SHOPPING,
    TASKS,
    EVENTS,
    BUDGETS,
    DOCUMENTS,
    NOTES,
    CHAT,
    MESSAGES,
    NOTIFICATIONS,
    AI_HISTORY,
}

/**
 * Locally queued mutation waiting to be pushed to Firestore.
 */
@Serializable
data class PendingSyncAction(
    val id: String,
    val collection: SyncCollection,
    val documentId: String,
    val familyId: String?,
    val actionType: SyncActionType,
    val payloadJson: String,
    val createdAt: Long,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val nextAttemptAt: Long = 0L,
)

/**
 * Describes a local/remote version conflict for a synced document.
 */
@Serializable
data class SyncConflict(
    val collection: SyncCollection,
    val documentId: String,
    val localUpdatedAt: Long,
    val remoteUpdatedAt: Long,
    val localPayloadJson: String,
    val remotePayloadJson: String,
)
