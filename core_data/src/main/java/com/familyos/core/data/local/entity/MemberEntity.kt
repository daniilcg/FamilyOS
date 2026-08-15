package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for [com.familyos.core.domain.model.FamilyMember]. */
@Entity(
    tableName = "members",
    indices = [
        Index(value = ["familyId"]),
        Index(value = ["userId"]),
        Index(value = ["familyId", "userId"], unique = true),
    ],
)
data class MemberEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    val userId: String,
    val displayName: String,
    val photoUrl: String?,
    val email: String,
    val role: String,
    val joinedAt: Long,
    val updatedAt: Long,
)
