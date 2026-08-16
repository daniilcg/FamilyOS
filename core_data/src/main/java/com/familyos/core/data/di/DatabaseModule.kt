package com.familyos.core.data.di

import android.content.Context
import androidx.room.Room
import com.familyos.core.data.local.dao.AiHistoryDao
import com.familyos.core.data.local.dao.AuthCredentialDao
import com.familyos.core.data.local.dao.BudgetDao
import com.familyos.core.data.local.dao.ChatDao
import com.familyos.core.data.local.dao.DocumentDao
import com.familyos.core.data.local.dao.EventDao
import com.familyos.core.data.local.dao.FamilyDao
import com.familyos.core.data.local.dao.NoteDao
import com.familyos.core.data.local.dao.NotificationDao
import com.familyos.core.data.local.dao.PendingSyncDao
import com.familyos.core.data.local.dao.ShoppingDao
import com.familyos.core.data.local.dao.TaskDao
import com.familyos.core.data.local.dao.UserDao
import com.familyos.core.data.local.db.FamilyOsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the Room [FamilyOsDatabase] and DAO bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** Builds the singleton FamilyOS Room database. */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FamilyOsDatabase =
        Room.databaseBuilder(context, FamilyOsDatabase::class.java, "familyos.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()

    @Provides fun provideUserDao(db: FamilyOsDatabase): UserDao = db.userDao()
    @Provides fun provideAuthCredentialDao(db: FamilyOsDatabase): AuthCredentialDao = db.authCredentialDao()
    @Provides fun provideFamilyDao(db: FamilyOsDatabase): FamilyDao = db.familyDao()
    @Provides fun provideShoppingDao(db: FamilyOsDatabase): ShoppingDao = db.shoppingDao()
    @Provides fun provideTaskDao(db: FamilyOsDatabase): TaskDao = db.taskDao()
    @Provides fun provideEventDao(db: FamilyOsDatabase): EventDao = db.eventDao()
    @Provides fun provideBudgetDao(db: FamilyOsDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideDocumentDao(db: FamilyOsDatabase): DocumentDao = db.documentDao()
    @Provides fun provideNoteDao(db: FamilyOsDatabase): NoteDao = db.noteDao()
    @Provides fun provideChatDao(db: FamilyOsDatabase): ChatDao = db.chatDao()
    @Provides fun provideNotificationDao(db: FamilyOsDatabase): NotificationDao = db.notificationDao()
    @Provides fun provideAiHistoryDao(db: FamilyOsDatabase): AiHistoryDao = db.aiHistoryDao()
    @Provides fun providePendingSyncDao(db: FamilyOsDatabase): PendingSyncDao = db.pendingSyncDao()
}
