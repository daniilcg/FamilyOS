package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Membership binding a [User] to a [Family] with a [FamilyRole].
 */
@Serializable
data class FamilyMember(
    val id: String,
    val familyId: String,
    val userId: String,
    val displayName: String,
    val photoUrl: String? = null,
    val email: String = "",
    val role: FamilyRole = FamilyRole.MEMBER,
    val joinedAt: Long = 0L,
    val updatedAt: Long = 0L,
)
