package com.familyos.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room entity for notes. */
@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["familyId", "isPinned"]),
        Index(value = ["familyId", "updatedAt"]),
    ],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val familyId: String,
    val title: String,
    val body: String,
    val colorHex: String?,
    val isPinned: Boolean,
    val isArchived: Boolean = false,
    val photoUrlsCsv: String = "",
    val tagsCsv: String,
    val createdBy: String,
    val updatedBy: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
)
