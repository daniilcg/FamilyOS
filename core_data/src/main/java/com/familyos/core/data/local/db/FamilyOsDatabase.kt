package com.familyos.core.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
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
import com.familyos.core.data.local.entity.AiHistoryEntity
import com.familyos.core.data.local.entity.AuthCredentialEntity
import com.familyos.core.data.local.entity.BudgetEntity
import com.familyos.core.data.local.entity.ChatMessageEntity
import com.familyos.core.data.local.entity.ChatThreadEntity
import com.familyos.core.data.local.entity.ChecklistItemEntity
import com.familyos.core.data.local.entity.DocumentEntity
import com.familyos.core.data.local.entity.EventEntity
import com.familyos.core.data.local.entity.FamilyEntity
import com.familyos.core.data.local.entity.MemberEntity
import com.familyos.core.data.local.entity.NoteEntity
import com.familyos.core.data.local.entity.NotificationEntity
import com.familyos.core.data.local.entity.PendingSyncEntity
import com.familyos.core.data.local.entity.ShoppingEntity
import com.familyos.core.data.local.entity.TaskAttachmentEntity
import com.familyos.core.data.local.entity.TaskEntity
import com.familyos.core.data.local.entity.UserEntity

/**
 * FamilyOS Room database containing all offline-first entities.
 */
@Database(
    entities = [
        UserEntity::class,
        AuthCredentialEntity::class,
        FamilyEntity::class,
        MemberEntity::class,
        ShoppingEntity::class,
        TaskEntity::class,
        TaskAttachmentEntity::class,
        EventEntity::class,
        BudgetEntity::class,
        DocumentEntity::class,
        NoteEntity::class,
        ChecklistItemEntity::class,
        ChatThreadEntity::class,
        ChatMessageEntity::class,
        NotificationEntity::class,
        AiHistoryEntity::class,
        PendingSyncEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class FamilyOsDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun authCredentialDao(): AuthCredentialDao
    abstract fun familyDao(): FamilyDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun taskDao(): TaskDao
    abstract fun eventDao(): EventDao
    abstract fun budgetDao(): BudgetDao
    abstract fun documentDao(): DocumentDao
    abstract fun noteDao(): NoteDao
    abstract fun chatDao(): ChatDao
    abstract fun notificationDao(): NotificationDao
    abstract fun aiHistoryDao(): AiHistoryDao
    abstract fun pendingSyncDao(): PendingSyncDao
}
