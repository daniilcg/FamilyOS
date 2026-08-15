package com.familyos.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.familyos.app.notifications.NotificationHelper
import com.familyos.app.workers.FamilyOsWorkScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * FamilyOS application entry point. Configures Timber, notification channels,
 * WorkManager with Hilt, and periodic background jobs.
 */
@HiltAndroidApp
class FamilyOsApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var workScheduler: FamilyOsWorkScheduler

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
        notificationHelper.createChannels()
        workScheduler.scheduleAll()
        Timber.i("FamilyOS application started")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO,
            )
            .build()
}

/**
 * Production Timber tree that drops verbose/debug logs.
 */
private class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < android.util.Log.INFO) return
        // Hook crash reporters here in production builds.
    }
}
