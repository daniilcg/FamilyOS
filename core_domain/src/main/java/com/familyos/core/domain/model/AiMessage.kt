package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * One turn in an AI assistant conversation.
 */
@Serializable
data class AiMessage(
    val id: String,
    val role: Role,
    val content: String,
    val createdAt: Long = 0L,
    val tokenCount: Int? = null,
) {
    /** Message authorship role. */
    @Serializable
    enum class Role {
        USER,
        ASSISTANT,
        SYSTEM,
    }
}
