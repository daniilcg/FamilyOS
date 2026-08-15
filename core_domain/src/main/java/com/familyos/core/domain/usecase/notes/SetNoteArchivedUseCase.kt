package com.familyos.core.domain.usecase.notes

import com.familyos.core.domain.model.Note
import com.familyos.core.domain.repository.NoteRepository
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Archives or restores a note. */
class SetNoteArchivedUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(id: String, archived: Boolean): Result<Note> =
        noteRepository.setArchived(id, archived)
}
