package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Persisted notification delivered to a user.
 */
@Serializable
data class AppNotification(
    val id: String,
    val userId: String,
    val familyId: String?,
    val type: NotificationType,
    val title: String,
    val body: String,
    val payloadJson: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
