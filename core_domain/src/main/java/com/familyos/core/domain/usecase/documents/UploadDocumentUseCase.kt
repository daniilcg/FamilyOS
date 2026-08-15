package com.familyos.core.domain.usecase.documents

import com.familyos.core.domain.model.DocumentType
import com.familyos.core.domain.model.FamilyDocument
import com.familyos.core.domain.repository.DocumentRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Constants
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Uploads and optionally encrypts a family document. */
class UploadDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    suspend operator fun invoke(
        familyId: String,
        title: String,
        type: DocumentType,
        mimeType: String,
        bytes: ByteArray,
        uploadedBy: String,
        tags: List<String> = emptyList(),
        encrypt: Boolean = true,
    ): Result<FamilyDocument> {
        if (familyId.isBlank()) return Result.failure(AppError.Validation("familyId required"))
        if (title.isBlank()) return Result.failure(AppError.Validation("Title required", "title"))
        if (bytes.isEmpty()) return Result.failure(AppError.Validation("File is empty"))
        if (bytes.size.toLong() > Constants.DOCUMENT_MAX_BYTES) {
            return Result.failure(AppError.Validation("File exceeds 25MB limit"))
        }
        return documentRepository.upload(
            familyId = familyId,
            title = title.trim(),
            type = type,
            mimeType = mimeType,
            sizeBytes = bytes.size.toLong(),
            bytes = bytes,
            uploadedBy = uploadedBy,
            tags = tags,
            encrypt = encrypt,
        )
    }
}
