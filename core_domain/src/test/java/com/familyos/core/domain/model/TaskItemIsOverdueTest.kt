package com.familyos.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TaskItemIsOverdueTest {

    private fun task(
        status: TaskStatus = TaskStatus.NEW,
        dueAt: Long? = 1_000L,
    ) = TaskItem(
        id = "t1",
        familyId = "f1",
        title = "Test",
        createdBy = "u1",
        status = status,
        dueAt = dueAt,
    )

    @Test
    fun overdueStatus_isAlwaysOverdue() {
        assertThat(task(status = TaskStatus.OVERDUE, dueAt = null).isOverdue(now = 5_000L)).isTrue()
    }

    @Test
    fun pastDue_openStatuses_areOverdue() {
        assertThat(task(status = TaskStatus.NEW).isOverdue(now = 5_000L)).isTrue()
        assertThat(task(status = TaskStatus.IN_PROGRESS).isOverdue(now = 5_000L)).isTrue()
        assertThat(task(status = TaskStatus.WAITING).isOverdue(now = 5_000L)).isTrue()
    }

    @Test
    fun doneAndCancelled_neverOverdue() {
        assertThat(task(status = TaskStatus.DONE).isOverdue(now = 5_000L)).isFalse()
        assertThat(task(status = TaskStatus.CANCELLED).isOverdue(now = 5_000L)).isFalse()
        assertThat(task(status = TaskStatus.DONE, dueAt = null).isOverdue(now = 5_000L)).isFalse()
    }

    @Test
    fun futureDue_isNotOverdue() {
        assertThat(task(status = TaskStatus.NEW, dueAt = 10_000L).isOverdue(now = 5_000L)).isFalse()
    }

    @Test
    fun missingDue_withoutOverdueStatus_isNotOverdue() {
        assertThat(task(status = TaskStatus.NEW, dueAt = null).isOverdue(now = 5_000L)).isFalse()
    }
}
