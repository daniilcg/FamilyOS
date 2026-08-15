package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for family documents. */
@Entity(
    tableName = "documents",
    indices = [
        Index(value = ["familyId", "type"]),
        Index(value = ["familyId", "title"]),
    ],
)
data class DocumentEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    val title: String,
    val type: String,
    val mimeType: String,
    val sizeBytes: Long,
    val storagePath: String,
    val downloadUrl: String?,
    val checksumSha256: String?,
    val isEncrypted: Boolean,
    val tagsCsv: String,
    val uploadedBy: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)
