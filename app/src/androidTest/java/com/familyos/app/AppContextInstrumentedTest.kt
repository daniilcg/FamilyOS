package com.familyos.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke test verifying the application context and package id.
 */
@RunWith(AndroidJUnit4::class)
class AppContextInstrumentedTest {

    @Test
    fun packageName_matchesApplicationId() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertThat(context.packageName).startsWith("com.familyos.app")
    }

    @Test
    fun appName_isFamilyOs() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = context.getString(R.string.app_name)
        assertThat(name).isEqualTo("FamilyOS")
    }
}
