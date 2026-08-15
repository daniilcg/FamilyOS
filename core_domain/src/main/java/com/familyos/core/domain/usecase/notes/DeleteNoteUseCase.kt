package com.familyos.core.domain.usecase.notes

import com.familyos.core.domain.repository.NoteRepository
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Deletes a note. */
class DeleteNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = noteRepository.delete(id)
}
