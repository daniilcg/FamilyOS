package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for tasks. */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["familyId", "status"]),
        Index(value = ["familyId", "priority"]),
        Index(value = ["dueAt"]),
        Index(value = ["assigneeId"]),
    ],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    val title: String,
    val description: String?,
    val status: String,
    val priority: String,
    val startAt: Long?,
    val dueAt: Long?,
    val completedAt: Long?,
    val assigneeId: String?,
    val createdBy: String,
    val recurrenceJson: String?,
    val parentTaskId: String?,
    val photoUri: String?,
    val checklistJson: String?,
    val attachmentIdsCsv: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)
