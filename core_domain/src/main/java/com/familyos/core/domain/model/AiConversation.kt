package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * AI conversation session scoped to a family/user.
 */
@Serializable
data class AiConversation(
    val id: String,
    val familyId: String,
    val userId: String,
    val title: String,
    val provider: String,
    val messages: List<AiMessage> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)
