package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Authenticated application user profile.
 *
 * @property id Firebase Auth UID
 * @property email Account email address
 * @property displayName Human-readable name
 * @property photoUrl Optional avatar URL
 * @property phoneNumber Optional phone
 * @property familyId Active family membership id
 * @property preferredLanguage BCP-47 language tag
 * @property createdAt Epoch millis when the profile was created
 * @property updatedAt Epoch millis of last profile update
 * @property isEmailVerified Whether Firebase email verification succeeded
 */
@Serializable
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val phoneNumber: String? = null,
    val familyId: String? = null,
    val preferredLanguage: String = "en",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isEmailVerified: Boolean = false,
)
