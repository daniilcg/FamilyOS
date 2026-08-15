package com.familyos.core.domain.logic

import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.model.SyncConflict
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class ConflictResolverTest {

    @Test
    fun resolve_lastWriteWins_whenMergeDisabled() {
        val conflict = SyncConflict(
            collection = SyncCollection.TASKS,
            documentId = "t1",
            localUpdatedAt = 200L,
            remoteUpdatedAt = 100L,
            localPayloadJson = """{"title":"local"}""",
            remotePayloadJson = """{"title":"remote"}""",
        )

        val resolution = ConflictResolver.resolve(conflict, preferMerge = false)

        assertThat(resolution.strategy).isEqualTo(ConflictResolver.Strategy.LAST_WRITE_WINS)
        assertThat(resolution.winningPayloadJson).isEqualTo("""{"title":"local"}""")
        assertThat(resolution.winningUpdatedAt).isEqualTo(200L)
    }

    @Test
    fun resolve_fieldMerge_keepsNonConflictingKeys() {
        val conflict = SyncConflict(
            collection = SyncCollection.NOTES,
            documentId = "n1",
            localUpdatedAt = 50L,
            remoteUpdatedAt = 100L,
            localPayloadJson = """{"title":"local-title","notes":"only-local"}""",
            remotePayloadJson = """{"title":"remote-title","tags":"only-remote"}""",
        )

        val resolution = ConflictResolver.resolve(conflict, preferMerge = true)
        val merged = Json.parseToJsonElement(resolution.winningPayloadJson).jsonObject

        assertThat(resolution.strategy).isEqualTo(ConflictResolver.Strategy.FIELD_MERGE)
        assertThat(merged["title"]!!.jsonPrimitive.content).isEqualTo("remote-title")
        assertThat(merged["notes"]!!.jsonPrimitive.content).isEqualTo("only-local")
        assertThat(merged["tags"]!!.jsonPrimitive.content).isEqualTo("only-remote")
        assertThat(merged["updatedAt"]!!.jsonPrimitive.content.toLong()).isEqualTo(100L)
    }

    @Test
    fun pickNewer_prefersGreaterUpdatedAt() {
        val newer = ConflictResolver.pickNewer(
            localUpdatedAt = 10L,
            localJson = "local",
            remoteUpdatedAt = 20L,
            remoteJson = "remote",
        )
        assertThat(newer).isEqualTo("remote")
    }
}
