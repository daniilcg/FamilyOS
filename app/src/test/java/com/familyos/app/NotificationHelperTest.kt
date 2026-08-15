package com.familyos.app

import com.familyos.app.notifications.NotificationHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for notification channel identifiers.
 */
class NotificationHelperTest {

    @Test
    fun channels_useStableIds() {
        assertThat(NotificationHelper.CHANNEL_GENERAL).isEqualTo("familyos_general")
        assertThat(NotificationHelper.CHANNEL_TASKS).isEqualTo("familyos_tasks")
        assertThat(NotificationHelper.CHANNEL_FAMILY).isEqualTo("familyos_family")
        assertThat(NotificationHelper.CHANNEL_SYNC).isEqualTo("familyos_sync")
    }
}
