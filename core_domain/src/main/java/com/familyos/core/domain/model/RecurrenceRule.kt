package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Recurrence pattern for repeating tasks and events.
 *
 * @property frequency Base unit of recurrence
 * @property interval Multiplier for [frequency] (e.g. every 2 weeks)
 * @property daysOfWeek Weekdays when frequency is WEEKLY (1=Mon .. 7=Sun)
 * @property dayOfMonth Day of month when frequency is MONTHLY
 * @property until Exclusive end epoch millis; null means infinite
 * @property count Maximum occurrences; null means unlimited
 */
@Serializable
data class RecurrenceRule(
    val frequency: Frequency = Frequency.WEEKLY,
    val interval: Int = 1,
    val daysOfWeek: List<Int> = emptyList(),
    val dayOfMonth: Int? = null,
    val until: Long? = null,
    val count: Int? = null,
) {
    /** Supported recurrence frequencies. */
    @Serializable
    enum class Frequency {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY,
    }
}
