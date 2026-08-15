package com.familyos.core.data.di

import com.familyos.core.data.repository.AiRepositoryImpl
import com.familyos.core.data.repository.AuthRepositoryImpl
import com.familyos.core.data.repository.BudgetRepositoryImpl
import com.familyos.core.data.repository.CalendarRepositoryImpl
import com.familyos.core.data.repository.ChatRepositoryImpl
import com.familyos.core.data.repository.DocumentRepositoryImpl
import com.familyos.core.data.repository.FamilyRepositoryImpl
import com.familyos.core.data.repository.NoteRepositoryImpl
import com.familyos.core.data.repository.NotificationRepositoryImpl
import com.familyos.core.data.repository.ShoppingRepositoryImpl
import com.familyos.core.data.repository.TaskRepositoryImpl
import com.familyos.core.data.repository.UserPreferencesRepositoryImpl
import com.familyos.core.data.sync.SyncQueueRepositoryImpl
import com.familyos.core.domain.repository.AiRepository
import com.familyos.core.domain.repository.AuthRepository
import com.familyos.core.domain.repository.BudgetRepository
import com.familyos.core.domain.repository.CalendarRepository
import com.familyos.core.domain.repository.ChatRepository
import com.familyos.core.domain.repository.DocumentRepository
import com.familyos.core.domain.repository.FamilyRepository
import com.familyos.core.domain.repository.NoteRepository
import com.familyos.core.domain.repository.NotificationRepository
import com.familyos.core.domain.repository.ShoppingRepository
import com.familyos.core.domain.repository.SyncRepository
import com.familyos.core.domain.repository.TaskRepository
import com.familyos.core.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds domain repository interfaces to offline-first implementations.
 * BillingRepository is bound in feature_billing (Play Billing client).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindFamilyRepository(impl: FamilyRepositoryImpl): FamilyRepository
    @Binds @Singleton abstract fun bindShoppingRepository(impl: ShoppingRepositoryImpl): ShoppingRepository
    @Binds @Singleton abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository
    @Binds @Singleton abstract fun bindCalendarRepository(impl: CalendarRepositoryImpl): CalendarRepository
    @Binds @Singleton abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository
    @Binds @Singleton abstract fun bindDocumentRepository(impl: DocumentRepositoryImpl): DocumentRepository
    @Binds @Singleton abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository
    @Binds @Singleton abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
    @Binds @Singleton abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
    @Binds @Singleton abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository
    @Binds @Singleton abstract fun bindSyncRepository(impl: SyncQueueRepositoryImpl): SyncRepository
    @Binds @Singleton abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository
}
