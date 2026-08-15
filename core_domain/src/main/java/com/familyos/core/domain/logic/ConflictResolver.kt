package com.familyos.core.domain.logic

import com.familyos.core.domain.model.SyncConflict
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Pure conflict resolution strategies for offline-first sync.
 *
 * Default strategy is last-write-wins on `updatedAt`. For known mergeable
 * collections, field-level merge preserves non-conflicting keys.
 */
object ConflictResolver {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /** Outcome of resolving a [SyncConflict]. */
    data class Resolution(
        val winningPayloadJson: String,
        val strategy: Strategy,
        val winningUpdatedAt: Long,
    )

    /** Resolution strategy applied. */
    enum class Strategy {
        LAST_WRITE_WINS,
        FIELD_MERGE,
    }

    /**
     * Resolves [conflict] using last-write-wins, optionally merging JSON objects
     * when [preferMerge] is true and both payloads are objects.
     */
    fun resolve(conflict: SyncConflict, preferMerge: Boolean = true): Resolution {
        val localWins = conflict.localUpdatedAt >= conflict.remoteUpdatedAt
        if (!preferMerge) {
            return if (localWins) {
                Resolution(conflict.localPayloadJson, Strategy.LAST_WRITE_WINS, conflict.localUpdatedAt)
            } else {
                Resolution(conflict.remotePayloadJson, Strategy.LAST_WRITE_WINS, conflict.remoteUpdatedAt)
            }
        }

        val localElement = runCatching { json.parseToJsonElement(conflict.localPayloadJson) }.getOrNull()
        val remoteElement = runCatching { json.parseToJsonElement(conflict.remotePayloadJson) }.getOrNull()
        if (localElement is JsonObject && remoteElement is JsonObject) {
            val merged = mergeObjects(localElement, remoteElement, localWins)
            val winningUpdatedAt = maxOf(conflict.localUpdatedAt, conflict.remoteUpdatedAt)
            val withStamp = buildJsonObject {
                merged.forEach { (k, v) -> put(k, v) }
                put("updatedAt", JsonPrimitive(winningUpdatedAt))
            }
            return Resolution(withStamp.toString(), Strategy.FIELD_MERGE, winningUpdatedAt)
        }

        return if (localWins) {
            Resolution(conflict.localPayloadJson, Strategy.LAST_WRITE_WINS, conflict.localUpdatedAt)
        } else {
            Resolution(conflict.remotePayloadJson, Strategy.LAST_WRITE_WINS, conflict.remoteUpdatedAt)
        }
    }

    /**
     * Field-level merge: keys only present on one side are kept; conflicting keys
     * take the value from the newer side ([localIsNewer]).
     */
    fun mergeObjects(
        local: JsonObject,
        remote: JsonObject,
        localIsNewer: Boolean,
    ): JsonObject = buildJsonObject {
        val keys = local.keys + remote.keys
        for (key in keys) {
            val localValue = local[key]
            val remoteValue = remote[key]
            when {
                localValue == null && remoteValue != null -> put(key, remoteValue)
                remoteValue == null && localValue != null -> put(key, localValue)
                localValue is JsonObject && remoteValue is JsonObject ->
                    put(key, mergeObjects(localValue, remoteValue, localIsNewer))
                else -> put(key, if (localIsNewer) localValue!! else remoteValue!!)
            }
        }
    }

    /**
     * Picks the payload with the greater [updatedAt] among two optional stamps.
     */
    fun pickNewer(localUpdatedAt: Long, localJson: String, remoteUpdatedAt: Long, remoteJson: String): String =
        if (localUpdatedAt >= remoteUpdatedAt) localJson else remoteJson
}
