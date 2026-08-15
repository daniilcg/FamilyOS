package com.familyos.core.domain.logic

import com.familyos.core.domain.model.RecurrenceRule
import com.familyos.core.domain.model.TaskItem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class RecurrenceExpanderTest {

    @Test
    fun expandOccurrences_daily_emitsIntervalSteps() {
        val seed = Instant.parse("2026-01-01T10:00:00Z").toEpochMilli()
        val rangeEnd = Instant.parse("2026-01-05T10:00:00Z").toEpochMilli()
        val rule = RecurrenceRule(frequency = RecurrenceRule.Frequency.DAILY, interval = 1)

        val occurrences = RecurrenceExpander.expandOccurrences(seed, rule, seed, rangeEnd)

        assertThat(occurrences).hasSize(4)
        assertThat(occurrences.first()).isEqualTo(seed)
        assertThat(occurrences.last()).isEqualTo(Instant.parse("2026-01-04T10:00:00Z").toEpochMilli())
    }

    @Test
    fun expandOccurrences_respectsCount() {
        val seed = Instant.parse("2026-03-01T08:00:00Z").toEpochMilli()
        val rangeEnd = Instant.parse("2026-04-01T08:00:00Z").toEpochMilli()
        val rule = RecurrenceRule(
            frequency = RecurrenceRule.Frequency.DAILY,
            interval = 1,
            count = 3,
        )

        val occurrences = RecurrenceExpander.expandOccurrences(seed, rule, seed, rangeEnd)

        assertThat(occurrences).hasSize(3)
    }

    @Test
    fun expandTasks_withoutRule_returnsSeedWhenInRange() {
        val seed = TaskItem(
            id = "t1",
            familyId = "f1",
            title = "One-off",
            createdBy = "u1",
            dueAt = 1_000L,
            createdAt = 1_000L,
        )

        val expanded = RecurrenceExpander.expandTasks(seed, 0L, 2_000L)

        assertThat(expanded).containsExactly(seed)
    }

    @Test
    fun expandTasks_withWeeklyRule_clonesOccurrences() {
        val seedStart = Instant.parse("2026-08-03T12:00:00Z").toEpochMilli() // Monday
        val seed = TaskItem(
            id = "t-weekly",
            familyId = "f1",
            title = "Weekly chore",
            createdBy = "u1",
            dueAt = seedStart,
            createdAt = seedStart,
            recurrence = RecurrenceRule(
                frequency = RecurrenceRule.Frequency.WEEKLY,
                interval = 1,
                daysOfWeek = listOf(1),
            ),
        )
        val rangeEnd = Instant.ofEpochMilli(seedStart)
            .atZone(ZoneOffset.UTC)
            .plusWeeks(3)
            .toInstant()
            .toEpochMilli()

        val expanded = RecurrenceExpander.expandTasks(seed, seedStart, rangeEnd)

        assertThat(expanded.size).isAtLeast(2)
        assertThat(expanded.first().id).isEqualTo("t-weekly")
        assertThat(expanded.drop(1).all { it.parentTaskId == "t-weekly" }).isTrue()
    }
}
