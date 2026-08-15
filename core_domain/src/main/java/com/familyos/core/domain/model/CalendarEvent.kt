package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Shared family calendar event.
 */
@Serializable
data class CalendarEvent(
    val id: String,
    val familyId: String,
    val title: String,
    val description: String? = null,
    val type: EventType = EventType.OTHER,
    val startAt: Long,
    val endAt: Long,
    val allDay: Boolean = false,
    val location: String? = null,
    val recurrence: RecurrenceRule? = null,
    val attendeeIds: List<String> = emptyList(),
    val createdBy: String,
    val reminderMinutes: Int? = 30,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)
