package com.familyos.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.familyos.core.data.local.dao.DocumentDao
import com.familyos.core.data.mapper.EntityMappers
import com.familyos.core.data.mapper.EntityMappers.toDomain
import com.familyos.core.data.mapper.EntityMappers.toEntity
import com.familyos.core.data.preferences.UserPreferencesDataStore
import com.familyos.core.data.remote.dto.DocumentDto
import com.familyos.core.data.remote.firestore.FirestoreDataSource
import com.familyos.core.data.remote.storage.FirebaseStorageDataSource
import com.familyos.core.data.security.Aes256Cipher
import com.familyos.core.data.sync.SyncQueueRepositoryImpl
import com.familyos.core.domain.model.DocumentType
import com.familyos.core.domain.model.FamilyDocument
import com.familyos.core.domain.model.SyncActionType
import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.repository.DocumentRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.AppException
import com.familyos.core.domain.util.Constants
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Offline-first encrypted document repository. */
@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao,
    private val storage: FirebaseStorageDataSource,
    private val cipher: Aes256Cipher,
    private val prefs: UserPreferencesDataStore,
    private val firestoreDataSource: FirestoreDataSource,
    private val syncQueue: SyncQueueRepositoryImpl,
) : DocumentRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeDocuments(familyId: String, type: DocumentType?): Flow<List<FamilyDocument>> =
        documentDao.observe(familyId, type?.name).map { it.map { e -> e.toDomain() } }.onStart {
            scope.launch {
                runCatching {
                    firestoreDataSource.observeDocuments(familyId).collect { dtos ->
                        dtos.forEach { documentDao.upsert(it.toEntity()) }
                    }
                }
            }
        }

    override fun pagingDocuments(familyId: String, type: DocumentType?): Flow<PagingData<FamilyDocument>> =
        Pager(PagingConfig(Constants.DEFAULT_PAGE_SIZE)) {
            documentDao.paging(familyId, type?.name)
        }.flow.map { it.map { e -> e.toDomain() } }

    override fun search(familyId: String, query: String): Flow<List<FamilyDocument>> =
        documentDao.search(familyId, query).map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: String): Result<FamilyDocument> = Result.runCatching {
        documentDao.getById(id)?.toDomain() ?: throw AppException(AppError.NotFound("Document", id))
    }

    override suspend fun upload(
        familyId: String,
        title: String,
        type: DocumentType,
        mimeType: String,
        sizeBytes: Long,
        bytes: ByteArray,
        uploadedBy: String,
        tags: List<String>,
        encrypt: Boolean,
    ): Result<FamilyDocument> = Result.runCatching {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val payload = if (encrypt) {
            val key = cipher.generateKey()
            prefs.storeEncryptedApiKey("doc_key_", cipher.encodeBase64(key))
            cipher.encrypt(bytes, key)
        } else bytes
        val path = storage.documentPath(familyId, id, title.replace(' ', '_'))
        val url = storage.uploadBytes(path, payload, if (encrypt) "application/octet-stream" else mimeType)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val doc = FamilyDocument(
            id = id,
            familyId = familyId,
            title = title,
            type = type,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            storagePath = path,
            downloadUrl = url,
            checksumSha256 = digest,
            isEncrypted = encrypt,
            tags = tags,
            uploadedBy = uploadedBy,
            createdAt = now,
            updatedAt = now,
        )
        documentDao.upsert(doc.toEntity())
        syncQueue.enqueue(SyncCollection.DOCUMENTS, id, familyId, SyncActionType.UPSERT,
            EntityMappers.json.encodeToString(doc.toDto()))
        doc
    }

    override suspend fun openDecryptedStream(documentId: String): Result<InputStream> = Result.runCatching {
        val doc = documentDao.getById(documentId)?.toDomain()
            ?: throw AppException(AppError.NotFound("Document", documentId))
        val encrypted = storage.downloadBytes(doc.storagePath)
        val plain = if (doc.isEncrypted) {
            val keyB64 = prefs.readEncryptedApiKey("doc_key_")
                ?: throw AppException(AppError.Local("Missing encryption key for document"))
            cipher.decrypt(encrypted, cipher.decodeBase64(keyB64))
        } else encrypted
        ByteArrayInputStream(plain)
    }

    override suspend fun delete(id: String): Result<Unit> = Result.runCatching {
        val doc = documentDao.getById(id)?.toDomain()
        documentDao.softDelete(id, System.currentTimeMillis())
        if (doc != null) {
            runCatching { storage.delete(doc.storagePath) }
            syncQueue.enqueue(SyncCollection.DOCUMENTS, id, doc.familyId, SyncActionType.DELETE,
                EntityMappers.json.encodeToString(doc.toDto()))
        }
    }

    override suspend fun updateMetadata(document: FamilyDocument): Result<FamilyDocument> = Result.runCatching {
        val updated = document.copy(updatedAt = System.currentTimeMillis())
        documentDao.upsert(updated.toEntity())
        syncQueue.enqueue(SyncCollection.DOCUMENTS, updated.id, updated.familyId, SyncActionType.UPSERT,
            EntityMappers.json.encodeToString(updated.toDto()))
        updated
    }

    private fun FamilyDocument.toDto() = DocumentDto(
        id, familyId, title, type.name, mimeType, sizeBytes, storagePath, downloadUrl,
        checksumSha256, isEncrypted, tags, uploadedBy, createdAt, updatedAt, isDeleted,
    )

    private fun DocumentDto.toEntity() = com.familyos.core.data.local.entity.DocumentEntity(
        id, familyId, title, type, mimeType, sizeBytes, storagePath, downloadUrl,
        checksumSha256, isEncrypted, tags.joinToString(","), uploadedBy, createdAt, updatedAt, isDeleted,
    )
}
