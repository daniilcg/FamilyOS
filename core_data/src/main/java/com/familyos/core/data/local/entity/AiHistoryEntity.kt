package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for AI conversation history. */
@Entity(
    tableName = "ai_history",
    indices = [
        Index(value = ["familyId", "userId"]),
        Index(value = ["updatedAt"]),
    ],
)
data class AiHistoryEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    val userId: String,
    val title: String,
    val provider: String,
    val messagesJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)
