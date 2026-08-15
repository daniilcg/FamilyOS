package com.familyos.core.domain.usecase.documents

import com.familyos.core.domain.model.DocumentType
import com.familyos.core.domain.model.FamilyDocument
import com.familyos.core.domain.repository.DocumentRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Constants
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes family documents, optionally filtered by type. */
class ObserveDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    operator fun invoke(familyId: String, type: DocumentType? = null): Flow<List<FamilyDocument>> =
        documentRepository.observeDocuments(familyId, type)
}
