package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Role of a member inside a family workspace.
 */
@Serializable
enum class FamilyRole {
    /** Full ownership including delete family and billing. */
    OWNER,

    /** Administrative privileges without ownership transfer. */
    ADMIN,

    /** Standard collaborative member. */
    MEMBER,

    /** Read-mostly guest access. */
    GUEST,
}
