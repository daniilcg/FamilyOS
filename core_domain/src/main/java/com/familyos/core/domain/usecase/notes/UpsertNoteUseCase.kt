package com.familyos.core.domain.usecase.notes

import com.familyos.core.domain.model.Note
import com.familyos.core.domain.repository.NoteRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Constants
import com.familyos.core.domain.util.Result
import java.util.UUID
import javax.inject.Inject

/** Creates or updates a note with validation. */
class UpsertNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(note: Note): Result<Note> {
        val title = note.title.trim()
        if (title.isEmpty() || title.length > Constants.MAX_NOTE_TITLE_LENGTH) {
            return Result.failure(AppError.Validation("Title is required", "title"))
        }
        if (note.familyId.isBlank()) {
            return Result.failure(AppError.Validation("familyId is required", "familyId"))
        }
        val now = System.currentTimeMillis()
        return noteRepository.upsert(
            note.copy(
                id = note.id.ifBlank { UUID.randomUUID().toString() },
                title = title,
                updatedAt = now,
                createdAt = if (note.createdAt == 0L) now else note.createdAt,
            ),
        )
    }
}
