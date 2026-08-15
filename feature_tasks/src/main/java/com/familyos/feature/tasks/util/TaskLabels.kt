package com.familyos.feature.tasks.util

import com.familyos.core.domain.model.RecurrenceRule
import com.familyos.core.domain.model.TaskPriority
import com.familyos.core.domain.model.TaskStatus
import com.familyos.core.locale.LocalizedLabels
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun TaskStatus.label(): String = LocalizedLabels.taskStatus(name)

fun TaskPriority.label(): String = LocalizedLabels.taskPriority(name)

fun RecurrenceRule.Frequency.label(): String = LocalizedLabels.recurrence(name)

fun formatTaskEpoch(millis: Long?): String {
    if (millis == null || millis <= 0L) return "—"
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withLocale(Locale.getDefault())
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(formatter)
}

/** UI filter including computed OVERDUE. */
enum class TaskStatusFilter {
    ALL, NEW, IN_PROGRESS, WAITING, DONE, CANCELLED, OVERDUE
}

fun TaskStatusFilter.label(): String = LocalizedLabels.taskStatus(name)
