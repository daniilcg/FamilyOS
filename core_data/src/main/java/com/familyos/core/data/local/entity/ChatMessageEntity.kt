package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for chat threads. */
@Entity(tableName = "chat_threads", indices = [Index(value = ["familyId", "updatedAt"])])
data class ChatThreadEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    val title: String,
    val participantIdsCsv: String,
    val lastMessagePreview: String?,
    val lastMessageAt: Long?,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)

/** Room entity for chat messages. */
@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["threadId", "createdAt"]),
        Index(value = ["familyId"]),
    ],
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val familyId: String,
    val senderId: String,
    val type: String,
    val body: String,
    val attachmentUrl: String?,
    val durationMs: Long? = null,
    val readByCsv: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)
