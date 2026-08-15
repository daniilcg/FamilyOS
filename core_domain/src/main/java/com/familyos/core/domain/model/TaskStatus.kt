package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Task workflow status.
 *
 * [OVERDUE] may be persisted after sync/workers mark missed deadlines,
 * or derived via [TaskItem.isOverdue] for display.
 */
@Serializable
enum class TaskStatus {
    NEW,
    IN_PROGRESS,
    WAITING,
    DONE,
    CANCELLED,
    OVERDUE,
}
