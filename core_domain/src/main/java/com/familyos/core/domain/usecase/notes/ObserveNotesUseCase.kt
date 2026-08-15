package com.familyos.core.domain.usecase.notes

import com.familyos.core.domain.model.Note
import com.familyos.core.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes family notes. */
class ObserveNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke(familyId: String, includeArchived: Boolean = false): Flow<List<Note>> =
        noteRepository.observeNotes(familyId, includeArchived)
}
