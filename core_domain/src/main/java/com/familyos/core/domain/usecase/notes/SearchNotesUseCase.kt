package com.familyos.core.domain.usecase.notes

import com.familyos.core.domain.model.Note
import com.familyos.core.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Searches notes by title, body, and tags. */
class SearchNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository,
) {
    operator fun invoke(familyId: String, query: String, includeArchived: Boolean = false): Flow<List<Note>> =
        noteRepository.search(familyId, query, includeArchived)
}
