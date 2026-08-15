package com.familyos.core.domain.usecase.notes

import com.familyos.core.domain.model.Note
import com.familyos.core.domain.repository.NoteRepository
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Loads a single note. */
class GetNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(id: String): Result<Note> = noteRepository.getById(id)
}
