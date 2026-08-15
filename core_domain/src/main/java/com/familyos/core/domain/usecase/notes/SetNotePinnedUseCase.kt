package com.familyos.core.domain.usecase.notes

import com.familyos.core.domain.model.Note
import com.familyos.core.domain.repository.NoteRepository
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Pins or unpins a note. */
class SetNotePinnedUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    suspend operator fun invoke(id: String, pinned: Boolean): Result<Note> =
        noteRepository.setPinned(id, pinned)
}
