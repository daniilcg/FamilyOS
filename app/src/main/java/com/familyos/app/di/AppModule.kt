package com.familyos.app.di

import android.content.Context
import androidx.work.WorkManager
import com.familyos.app.BuildConfig
import com.familyos.app.notifications.NotificationHelper
import com.familyos.app.workers.FamilyOsWorkScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Application-scoped bindings for WorkManager, notifications, and schedulers.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** OAuth Web client ID from local.properties / google-services.json parsing. */
    @Provides
    @Singleton
    @Named("googleWebClientId")
    fun provideGoogleWebClientId(): String = BuildConfig.GOOGLE_WEB_CLIENT_ID

    /** Provides the process [WorkManager] instance. */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    /** Provides notification channel / display helper. */
    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper =
        NotificationHelper(context)

    /** Provides the periodic work scheduler. */
    @Provides
    @Singleton
    fun provideWorkScheduler(workManager: WorkManager): FamilyOsWorkScheduler =
        FamilyOsWorkScheduler(workManager)
}
