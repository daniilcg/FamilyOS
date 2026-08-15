package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local credential row for offline email/password (and local Google-linked) auth.
 */
@Entity(
    tableName = "auth_credentials",
    indices = [Index(value = ["email"], unique = true)],
)
data class AuthCredentialEntity(
    @PrimaryKey val userId: String,
    val email: String,
    val passwordSalt: String,
    val passwordHash: String,
    /** One of: EMAIL, GOOGLE, LOCAL */
    val provider: String,
    val createdAt: Long,
    val updatedAt: Long,
)
