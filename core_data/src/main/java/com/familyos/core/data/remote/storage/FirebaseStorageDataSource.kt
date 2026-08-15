package com.familyos.core.data.remote.storage

import android.net.Uri
import com.familyos.core.domain.util.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Storage helper for avatars, documents, and attachments.
 */
@Singleton
class FirebaseStorageDataSource @Inject constructor(
    private val storage: FirebaseStorage,
) {
    /**
     * Uploads [bytes] to [path] and returns the download URL string.
     */
    suspend fun uploadBytes(
        path: String,
        bytes: ByteArray,
        mimeType: String,
    ): String {
        val ref = storage.reference.child(path)
        val metadata = StorageMetadata.Builder().setContentType(mimeType).build()
        ref.putBytes(bytes, metadata).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Uploads a local file [uri] to [path].
     */
    suspend fun uploadUri(path: String, uri: Uri, mimeType: String): String {
        val ref = storage.reference.child(path)
        val metadata = StorageMetadata.Builder().setContentType(mimeType).build()
        ref.putFile(uri, metadata).await()
        return ref.downloadUrl.await().toString()
    }

    /**
     * Downloads file bytes from [path].
     */
    suspend fun downloadBytes(path: String, maxBytes: Long = Constants.DOCUMENT_MAX_BYTES): ByteArray {
        val ref = storage.reference.child(path)
        return ref.getBytes(maxBytes).await()
    }

    /**
     * Opens an [InputStream] over downloaded bytes for [path].
     */
    suspend fun openStream(path: String): InputStream =
        ByteArrayInputStream(downloadBytes(path))

    /**
     * Deletes the object at [path] if it exists.
     */
    suspend fun delete(path: String) {
        runCatching { storage.reference.child(path).delete().await() }
    }

    /**
     * Builds a deterministic storage path for a family document.
     */
    fun documentPath(familyId: String, documentId: String, fileName: String): String =
        "${Constants.STORAGE_DOCUMENTS}/$familyId/$documentId/$fileName"

    /**
     * Builds a storage path for a user avatar.
     */
    fun avatarPath(userId: String): String =
        "${Constants.STORAGE_AVATARS}/$userId.jpg"

    /**
     * Builds a storage path for a task attachment.
     */
    fun attachmentPath(familyId: String, taskId: String, attachmentId: String, fileName: String): String =
        "${Constants.STORAGE_ATTACHMENTS}/$familyId/$taskId/$attachmentId/$fileName"
}
