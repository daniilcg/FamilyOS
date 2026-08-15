package com.familyos.feature.tasks.util

import com.familyos.core.domain.model.RecurrenceRule
import com.familyos.core.domain.model.TaskPriority
import com.familyos.core.domain.model.TaskStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun TaskStatus.label(): String = when (this) {
    TaskStatus.NEW -> "New"
    TaskStatus.IN_PROGRESS -> "In progress"
    TaskStatus.WAITING -> "Waiting"
    TaskStatus.DONE -> "Done"
    TaskStatus.CANCELLED -> "Cancelled"
    TaskStatus.OVERDUE -> "Overdue"
}

fun TaskPriority.label(): String = when (this) {
    TaskPriority.LOW -> "Low"
    TaskPriority.MEDIUM -> "Medium"
    TaskPriority.HIGH -> "High"
    TaskPriority.URGENT -> "Urgent"
}

fun RecurrenceRule.Frequency.label(): String = when (this) {
    RecurrenceRule.Frequency.DAILY -> "Daily"
    RecurrenceRule.Frequency.WEEKLY -> "Weekly"
    RecurrenceRule.Frequency.MONTHLY -> "Monthly"
    RecurrenceRule.Frequency.YEARLY -> "Yearly"
}

private val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withLocale(Locale.getDefault())

fun formatTaskEpoch(millis: Long?): String {
    if (millis == null || millis <= 0L) return "—"
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(formatter)
}

/** UI filter including computed OVERDUE. */
enum class TaskStatusFilter {
    ALL, NEW, IN_PROGRESS, WAITING, DONE, CANCELLED, OVERDUE
}

fun TaskStatusFilter.label(): String = when (this) {
    TaskStatusFilter.ALL -> "All"
    TaskStatusFilter.NEW -> "New"
    TaskStatusFilter.IN_PROGRESS -> "In progress"
    TaskStatusFilter.WAITING -> "Waiting"
    TaskStatusFilter.DONE -> "Done"
    TaskStatusFilter.CANCELLED -> "Cancelled"
    TaskStatusFilter.OVERDUE -> "Overdue"
}
