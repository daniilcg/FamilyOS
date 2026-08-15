package com.familyos.feature.calendar.util

import com.familyos.core.domain.model.EventType
import com.familyos.core.locale.LocalizedLabels
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

fun EventType.label(): String = LocalizedLabels.eventType(name)

enum class CalendarViewMode { MONTH, WEEK, DAY, AGENDA }

fun CalendarViewMode.label(): String {
    val lang = Locale.getDefault().language.lowercase()
    return when (lang) {
        "ru" -> when (this) {
            CalendarViewMode.MONTH -> "Месяц"
            CalendarViewMode.WEEK -> "Неделя"
            CalendarViewMode.DAY -> "День"
            CalendarViewMode.AGENDA -> "Список"
        }
        "sr" -> when (this) {
            CalendarViewMode.MONTH -> "Mesec"
            CalendarViewMode.WEEK -> "Nedelja"
            CalendarViewMode.DAY -> "Dan"
            CalendarViewMode.AGENDA -> "Lista"
        }
        else -> when (this) {
            CalendarViewMode.MONTH -> "Month"
            CalendarViewMode.WEEK -> "Week"
            CalendarViewMode.DAY -> "Day"
            CalendarViewMode.AGENDA -> "Agenda"
        }
    }
}

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun formatEventDay(millis: Long): String {
    val dayFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM").withLocale(Locale.getDefault())
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dayFormatter)
}

fun formatEventTime(millis: Long): String {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withLocale(Locale.getDefault())
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFormatter)
}

fun LocalDate.startOfDayMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()

fun LocalDate.endOfDayMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
