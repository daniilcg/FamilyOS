package com.familyos.app

import com.familyos.app.workers.NotificationWorker
import com.familyos.app.workers.RecurringTaskWorker
import com.familyos.app.workers.SyncWorker
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for WorkManager unique work naming contracts.
 */
class WorkerContractTest {

    @Test
    fun syncWorker_hasStableUniqueName() {
        assertThat(SyncWorker.UNIQUE_NAME).isEqualTo("familyos_sync_worker")
    }

    @Test
    fun recurringTaskWorker_hasStableUniqueName() {
        assertThat(RecurringTaskWorker.UNIQUE_NAME).isEqualTo("familyos_recurring_task_worker")
    }

    @Test
    fun notificationWorker_hasStableUniqueName() {
        assertThat(NotificationWorker.UNIQUE_NAME).isEqualTo("familyos_notification_worker")
    }
}
