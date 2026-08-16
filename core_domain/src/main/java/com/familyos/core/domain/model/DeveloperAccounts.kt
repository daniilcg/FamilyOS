package com.familyos.core.domain.model

/**
 * SEGAL COMMUNICATIONS developer allowlist.
 *
 * Any family whose OWNER signs in with one of these emails gets **lifetime Premium**.
 * Everyone they invite into that family inherits the same Premium (checked on each device
 * against the owner member email, and via the synced family subscription).
 *
 * Add more login emails here, then rebuild the APK.
 */
object DeveloperAccounts {
    val emails: Set<String> = setOf(
        "danielsegal.ca@gmail.com",
    )

    fun isDeveloper(email: String?): Boolean {
        val normalized = email?.trim()?.lowercase().orEmpty()
        if (normalized.isEmpty()) return false
        return emails.any { it.equals(normalized, ignoreCase = true) }
    }

    /**
     * True when this family is owned by a developer account.
     * Members of that family receive Premium even if they themselves are not developers.
     */
    fun isDeveloperOwnedFamily(
        currentUser: User?,
        family: Family?,
        members: List<FamilyMember>,
    ): Boolean {
        val owner = members.firstOrNull { it.role == FamilyRole.OWNER }
            ?: members.firstOrNull { it.userId == family?.ownerId }
        if (isDeveloper(owner?.email)) return true
        if (currentUser != null && family != null && currentUser.id == family.ownerId) {
            return isDeveloper(currentUser.email)
        }
        return false
    }
}
