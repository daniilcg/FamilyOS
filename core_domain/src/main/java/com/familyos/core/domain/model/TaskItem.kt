package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/** Checklist row attached to a task. */
@Serializable
data class TaskChecklistItem(
    val id: String,
    val text: String,
    val isChecked: Boolean = false,
    val order: Int = 0,
)

/**
 * Family task / chore item.
 */
@Serializable
data class TaskItem(
    val id: String,
    val familyId: String,
    val title: String,
    val description: String? = null,
    val status: TaskStatus = TaskStatus.NEW,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val startAt: Long? = null,
    val dueAt: Long? = null,
    val completedAt: Long? = null,
    val assigneeId: String? = null,
    val createdBy: String,
    val recurrence: RecurrenceRule? = null,
    val parentTaskId: String? = null,
    val photoUri: String? = null,
    val checklist: List<TaskChecklistItem> = emptyList(),
    val attachmentIds: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)

/** True when status is [TaskStatus.OVERDUE] or the due date has passed while still open. */
fun TaskItem.isOverdue(now: Long = System.currentTimeMillis()): Boolean {
    if (status == TaskStatus.DONE || status == TaskStatus.CANCELLED) return false
    if (status == TaskStatus.OVERDUE) return true
    val due = dueAt ?: return false
    return due < now
}
