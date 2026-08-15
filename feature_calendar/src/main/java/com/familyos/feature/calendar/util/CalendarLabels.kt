package com.familyos.feature.calendar.util

import com.familyos.core.domain.model.EventType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Product event types shown in editors. */
val EventUiTypes: List<EventType> = listOf(
    EventType.BIRTHDAY,
    EventType.HOLIDAY,
    EventType.MEETING,
    EventType.TRIP,
    EventType.SCHOOL,
    EventType.VET,
    EventType.DOCTOR,
    EventType.BILL_PAYMENT,
    EventType.OTHER,
)

fun EventType.label(): String = when (this) {
    EventType.BIRTHDAY -> "Birthday"
    EventType.HOLIDAY -> "Holiday"
    EventType.MEETING -> "Meeting"
    EventType.TRIP -> "Trip"
    EventType.SCHOOL -> "School"
    EventType.VET -> "Vet"
    EventType.DOCTOR -> "Doctor"
    EventType.BILL_PAYMENT -> "Bill payment"
    EventType.OTHER -> "Other"
}

enum class CalendarViewMode { MONTH, WEEK, DAY, AGENDA }

fun CalendarViewMode.label(): String = when (this) {
    CalendarViewMode.MONTH -> "Month"
    CalendarViewMode.WEEK -> "Week"
    CalendarViewMode.DAY -> "Day"
    CalendarViewMode.AGENDA -> "Agenda"
}

private val dayFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM").withLocale(Locale.getDefault())
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withLocale(Locale.getDefault())

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun formatEventDay(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dayFormatter)

fun formatEventTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFormatter)

fun LocalDate.startOfDayMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

fun LocalDate.endOfDayMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
