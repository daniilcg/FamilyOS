package com.familyos.core.domain.usecase.documents

import com.familyos.core.domain.model.FamilyDocument
import com.familyos.core.domain.repository.DocumentRepository
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Loads a single document by id. */
class GetDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    suspend operator fun invoke(id: String): Result<FamilyDocument> = documentRepository.getById(id)
}
