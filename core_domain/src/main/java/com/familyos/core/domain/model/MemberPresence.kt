package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Online presence snapshot for a family member in chat.
 */
@Serializable
data class MemberPresence(
    val userId: String,
    val displayName: String,
    val isOnline: Boolean,
    val lastSeenAt: Long? = null,
)
