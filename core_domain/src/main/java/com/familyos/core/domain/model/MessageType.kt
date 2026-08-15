package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/** Chat message payload type. */
@Serializable
enum class MessageType {
    TEXT,
    IMAGE,
    VOICE,
    FILE,
    SYSTEM,
}
