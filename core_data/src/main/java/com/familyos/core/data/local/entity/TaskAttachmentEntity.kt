package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for task file attachments metadata. */
@Entity(
    tableName = "task_attachments",
    indices = [Index(value = ["taskId"]), Index(value = ["familyId"])],
)
data class TaskAttachmentEntity(
    @PrimaryKey val id: String,
    val taskId: String,
    val familyId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val storagePath: String,
    val downloadUrl: String?,
    val createdAt: Long,
)
