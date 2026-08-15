package com.familyos.core.domain.repository

import androidx.paging.PagingData
import com.familyos.core.domain.model.DocumentType
import com.familyos.core.domain.model.FamilyDocument
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

/**
 * Encrypted document metadata and binary storage.
 */
interface DocumentRepository {
    fun observeDocuments(familyId: String, type: DocumentType? = null): Flow<List<FamilyDocument>>
    fun pagingDocuments(familyId: String, type: DocumentType?): Flow<PagingData<FamilyDocument>>
    fun search(familyId: String, query: String): Flow<List<FamilyDocument>>
    suspend fun getById(id: String): Result<FamilyDocument>
    suspend fun upload(
        familyId: String,
        title: String,
        type: DocumentType,
        mimeType: String,
        sizeBytes: Long,
        bytes: ByteArray,
        uploadedBy: String,
        tags: List<String> = emptyList(),
        encrypt: Boolean = true,
    ): Result<FamilyDocument>
    suspend fun openDecryptedStream(documentId: String): Result<InputStream>
    suspend fun delete(id: String): Result<Unit>
    suspend fun updateMetadata(document: FamilyDocument): Result<FamilyDocument>
}
