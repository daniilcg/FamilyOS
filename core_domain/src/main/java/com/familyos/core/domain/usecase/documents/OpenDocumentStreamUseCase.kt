package com.familyos.core.domain.usecase.documents

import com.familyos.core.domain.repository.DocumentRepository
import com.familyos.core.domain.util.Result
import java.io.InputStream
import javax.inject.Inject

/** Opens a decrypted stream for a vault document after unlock. */
class OpenDocumentStreamUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    suspend operator fun invoke(documentId: String): Result<InputStream> =
        documentRepository.openDecryptedStream(documentId)
}
