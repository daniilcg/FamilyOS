package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for [com.familyos.core.domain.model.User]. */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true), Index(value = ["familyId"])],
)
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val phoneNumber: String?,
    val familyId: String?,
    val preferredLanguage: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isEmailVerified: Boolean,
)
