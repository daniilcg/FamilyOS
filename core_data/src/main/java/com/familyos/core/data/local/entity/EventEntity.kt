package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for calendar events. */
@Entity(
    tableName = "events",
    indices = [
        Index(value = ["familyId", "startAt", "endAt"]),
        Index(value = ["familyId", "type"]),
    ],
)
data class EventEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    val title: String,
    val description: String?,
    val type: String,
    val startAt: Long,
    val endAt: Long,
    val allDay: Boolean,
    val location: String?,
    val recurrenceJson: String?,
    val attendeeIdsCsv: String,
    val createdBy: String,
    val reminderMinutes: Int?,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)
