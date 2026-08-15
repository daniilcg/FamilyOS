package com.familyos.core.domain.usecase.documents

import com.familyos.core.domain.repository.DocumentRepository
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Soft-deletes a family document. */
class DeleteDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = documentRepository.delete(id)
}
