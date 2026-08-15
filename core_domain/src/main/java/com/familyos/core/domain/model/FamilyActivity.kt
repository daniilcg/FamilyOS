package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Activity feed entry for the family home dashboard.
 */
@Serializable
data class FamilyActivity(
    val id: String,
    val familyId: String,
    val actorId: String,
    val actorName: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val summary: String,
    val createdAt: Long = 0L,
)
