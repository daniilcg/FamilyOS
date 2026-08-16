package com.familyos.app

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.familyos.app.notifications.NotificationHelper
import com.familyos.app.workers.FamilyOsWorkScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.system.exitProcess

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

    override fun onCreate() {
        val crashProcess = getProcessName().endsWith(":crash")
        if (!crashProcess) {
            installCrashHandler()
        }
        super.onCreate()
        if (crashProcess) return
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
        runCatching { notificationHelper.createChannels() }
            .onFailure { Timber.w(it, "Failed to create notification channels") }
        Handler(Looper.getMainLooper()).post {
            runCatching {
                FamilyOsWorkScheduler(WorkManager.getInstance(this)).scheduleAll()
            }.onFailure { Timber.w(it, "Failed to schedule workers") }
        }
        Timber.i("FamilyOS application started")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO,
            )
            .build()

    private fun installCrashHandler() {
        Thread.setDefaultUncaughtExceptionHandler { _, error ->
            runCatching {
                File(filesDir, "last_crash.txt").writeText(
                    error.toString() + "\n" + Log.getStackTraceString(error),
                )
            }
            runCatching {
                val intent = Intent(this, CrashActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(CrashActivity.EXTRA_CRASH, Log.getStackTraceString(error))
                }
                startActivity(intent)
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }
}

/**
 * Production Timber tree that drops verbose/debug logs.
 */
private class ReleaseTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < android.util.Log.INFO) return
    }
}
