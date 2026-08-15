package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/** Relative priority for task ordering. */
@Serializable
enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT,
}
