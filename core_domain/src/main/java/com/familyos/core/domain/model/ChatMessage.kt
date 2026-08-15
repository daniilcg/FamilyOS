package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Single message inside a [ChatThread].
 *
 * @property readBy User ids that have read this message (read receipts)
 * @property durationMs Voice duration when [type] is [MessageType.VOICE]
 */
@Serializable
data class ChatMessage(
    val id: String,
    val threadId: String,
    val familyId: String,
    val senderId: String,
    val type: MessageType = MessageType.TEXT,
    val body: String,
    val attachmentUrl: String? = null,
    val durationMs: Long? = null,
    val readBy: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)
