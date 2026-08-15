package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Encrypted family document metadata. Binary content lives in Firebase Storage / local cipher store.
 */
@Serializable
data class FamilyDocument(
    val id: String,
    val familyId: String,
    val title: String,
    val type: DocumentType = DocumentType.OTHER,
    val mimeType: String,
    val sizeBytes: Long,
    val storagePath: String,
    val downloadUrl: String? = null,
    val checksumSha256: String? = null,
    val isEncrypted: Boolean = true,
    val tags: List<String> = emptyList(),
    val uploadedBy: String,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
)
