package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * A family chat conversation thread.
 */
@Serializable
data class ChatThread(
    val id: String,
    val familyId: String,
    val title: String,
    val participantIds: List<String> = emptyList(),
    val lastMessagePreview: String? = null,
    val lastMessageAt: Long? = null,
    val createdBy: String,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)
