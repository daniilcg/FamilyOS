package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for notifications. */
@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["userId", "isRead"]),
        Index(value = ["userId", "createdAt"]),
    ],
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val familyId: String?,
    val type: String,
    val title: String,
    val body: String,
    val payloadJson: String?,
    val isRead: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
