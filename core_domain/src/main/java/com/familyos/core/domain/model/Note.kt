package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Shared family note with optional checklist, photos, tags, and archive state.
 */
@Serializable
data class Note(
    val id: String,
    val familyId: String,
    val title: String,
    val body: String = "",
    val checklist: List<NoteChecklistItem> = emptyList(),
    val photoUrls: List<String> = emptyList(),
    val colorHex: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdBy: String,
    val updatedBy: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)
