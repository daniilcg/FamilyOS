package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/** Uploaded document classification for the family vault. */
@Serializable
enum class DocumentType {
    PASSPORT,
    INSURANCE,
    WARRANTY,
    CONTRACT,
    CERTIFICATE,
    MEDICAL,
    OTHER,
}
