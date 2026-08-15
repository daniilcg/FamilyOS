package com.familyos.core.domain.logic

import com.familyos.core.domain.model.CalendarEvent
import com.familyos.core.domain.model.RecurrenceRule
import com.familyos.core.domain.model.TaskItem
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.min

/**
 * Expands [RecurrenceRule] instances into concrete occurrence timestamps / task clones.
 */
object RecurrenceExpander {

    /**
     * Returns occurrence start epoch-millis between [rangeStart] (inclusive) and [rangeEnd] (exclusive)
     * for a series that begins at [seedStart].
     */
    fun expandOccurrences(
        seedStart: Long,
        rule: RecurrenceRule,
        rangeStart: Long,
        rangeEnd: Long,
        maxOccurrences: Int = 366,
    ): List<Long> {
        require(rule.interval >= 1) { "interval must be >= 1" }
        val results = ArrayList<Long>(min(64, maxOccurrences))
        var current = seedStart
        var emitted = 0
        var guard = 0
        val hardLimit = maxOccurrences * 4 + 32

        while (emitted < maxOccurrences && guard < hardLimit) {
            guard++
            if (rule.until != null && current >= rule.until) break
            if (rule.count != null && emitted >= rule.count) break
            if (current >= rangeEnd) break

            if (current >= rangeStart && matchesRule(current, seedStart, rule)) {
                results.add(current)
                emitted++
                if (rule.count != null && emitted >= rule.count) break
            }
            current = advance(current, rule)
            if (current <= seedStart && guard > 1) break
        }
        return results
    }

    /**
     * Materializes recurring [TaskItem] clones for the given window.
     * The seed task itself is included when it falls in range.
     */
    fun expandTasks(
        seed: TaskItem,
        rangeStart: Long,
        rangeEnd: Long,
    ): List<TaskItem> {
        val rule = seed.recurrence ?: return listOf(seed).filter {
            val due = it.dueAt ?: it.createdAt
            due in rangeStart until rangeEnd
        }
        val seedDue = seed.dueAt ?: seed.createdAt
        return expandOccurrences(seedDue, rule, rangeStart, rangeEnd).mapIndexed { index, due ->
            if (index == 0 && due == seedDue) seed
            else seed.copy(
                id = "${seed.id}_$due",
                dueAt = due,
                parentTaskId = seed.id,
                status = seed.status,
                completedAt = null,
            )
        }
    }

    /**
     * Materializes recurring [CalendarEvent] clones for the given window.
     */
    fun expandEvents(
        seed: CalendarEvent,
        rangeStart: Long,
        rangeEnd: Long,
    ): List<CalendarEvent> {
        val rule = seed.recurrence ?: return listOf(seed).filter {
            it.startAt < rangeEnd && it.endAt > rangeStart
        }
        val duration = (seed.endAt - seed.startAt).coerceAtLeast(0L)
        return expandOccurrences(seed.startAt, rule, rangeStart, rangeEnd).map { start ->
            if (start == seed.startAt) seed
            else seed.copy(
                id = "${seed.id}_$start",
                startAt = start,
                endAt = start + duration,
            )
        }
    }

    private fun matchesRule(candidate: Long, seedStart: Long, rule: RecurrenceRule): Boolean {
        if (rule.frequency != RecurrenceRule.Frequency.WEEKLY) return true
        if (rule.daysOfWeek.isEmpty()) return true
        val dow = Instant.ofEpochMilli(candidate).atZone(ZoneOffset.UTC).dayOfWeek.isoValue()
        return dow in rule.daysOfWeek
    }

    private fun advance(from: Long, rule: RecurrenceRule): Long {
        val zone = ZoneOffset.UTC
        val dateTime = Instant.ofEpochMilli(from).atZone(zone)
        return when (rule.frequency) {
            RecurrenceRule.Frequency.DAILY ->
                dateTime.plusDays(rule.interval.toLong()).toInstant().toEpochMilli()

            RecurrenceRule.Frequency.WEEKLY -> {
                if (rule.daysOfWeek.isEmpty()) {
                    dateTime.plusWeeks(rule.interval.toLong()).toInstant().toEpochMilli()
                } else {
                    nextWeeklyOccurrence(from, rule)
                }
            }

            RecurrenceRule.Frequency.MONTHLY -> {
                val day = rule.dayOfMonth ?: dateTime.dayOfMonth
                var next = dateTime.plusMonths(rule.interval.toLong())
                val clampedDay = min(day, next.toLocalDate().lengthOfMonth())
                next = next.withDayOfMonth(clampedDay)
                next.toInstant().toEpochMilli()
            }

            RecurrenceRule.Frequency.YEARLY ->
                dateTime.plusYears(rule.interval.toLong()).toInstant().toEpochMilli()
        }
    }

    private fun nextWeeklyOccurrence(from: Long, rule: RecurrenceRule): Long {
        val sortedDays = rule.daysOfWeek.distinct().sorted()
        val zone = ZoneOffset.UTC
        var day = Instant.ofEpochMilli(from).atZone(zone).toLocalDate().plusDays(1)
        var scanned = 0
        while (scanned < 14 * rule.interval + 7) {
            val iso = day.dayOfWeek.isoValue()
            if (iso in sortedDays) {
                val weeksFromStart = ChronoUnit.WEEKS.between(
                    Instant.ofEpochMilli(from).atZone(zone).toLocalDate().with(DayOfWeek.MONDAY),
                    day.with(DayOfWeek.MONDAY),
                )
                if (weeksFromStart % rule.interval == 0L || weeksFromStart == 0L) {
                    val time = Instant.ofEpochMilli(from).atZone(zone).toLocalTime()
                    return day.atTime(time).toInstant(zone).toEpochMilli()
                }
            }
            day = day.plusDays(1)
            scanned++
        }
        return Instant.ofEpochMilli(from).atZone(zone).plusWeeks(rule.interval.toLong())
            .toInstant().toEpochMilli()
    }

    private fun DayOfWeek.isoValue(): Int = value // Monday=1 .. Sunday=7
}
