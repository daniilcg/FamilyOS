package com.familyos.core.domain.usecase.documents

import com.familyos.core.domain.model.DocumentType
import com.familyos.core.domain.model.FamilyDocument
import com.familyos.core.domain.repository.DocumentRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Constants
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Uploads and optionally encrypts a document into the family vault. */
class ImportDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
) {
    private val allowedMimePrefixes = listOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml",
        "application/msword",
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp",
    )

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
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(AppError.Validation("Title is required", "title"))
        }
        if (familyId.isBlank() || uploadedBy.isBlank()) {
            return Result.failure(AppError.Validation("familyId and uploadedBy are required"))
        }
        if (bytes.isEmpty()) {
            return Result.failure(AppError.Validation("Document bytes are empty", "bytes"))
        }
        if (bytes.size.toLong() > Constants.DOCUMENT_MAX_BYTES) {
            return Result.failure(AppError.Validation("Document exceeds maximum size", "bytes"))
        }
        val mimeOk = allowedMimePrefixes.any { mimeType.startsWith(it, ignoreCase = true) }
        if (!mimeOk) {
            return Result.failure(
                AppError.Validation("Only PDF, DOCX, JPG, PNG, and WEBP are supported", "mimeType"),
            )
        }
        return documentRepository.upload(
            familyId = familyId,
            title = trimmed,
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
