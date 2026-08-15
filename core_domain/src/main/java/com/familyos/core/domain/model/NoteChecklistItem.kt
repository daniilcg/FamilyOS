package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * A checklist row inside a note.
 */
@Serializable
data class NoteChecklistItem(
    val id: String,
    val text: String,
    val isChecked: Boolean = false,
    val order: Int = 0,
)
