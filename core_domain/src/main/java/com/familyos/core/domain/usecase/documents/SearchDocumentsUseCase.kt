package com.familyos.core.domain.usecase.documents

import com.familyos.core.domain.model.FamilyDocument
import com.familyos.core.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Searches document titles and tags. */
class SearchDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    operator fun invoke(familyId: String, query: String): Flow<List<FamilyDocument>> =
        documentRepository.search(familyId, query)
}
