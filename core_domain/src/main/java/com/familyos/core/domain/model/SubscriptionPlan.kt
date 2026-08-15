package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Available subscription tiers.
 */
@Serializable
enum class SubscriptionPlan {
    FREE,
    PREMIUM,
}

/**
 * Play Billing product identifiers used by FamilyOS.
 */
object BillingProducts {
    const val PREMIUM_MONTHLY = "familyos_premium_monthly"
    const val PREMIUM_YEARLY = "familyos_premium_yearly"

    /** All known SKU ids. */
    val ALL: Set<String> = setOf(PREMIUM_MONTHLY, PREMIUM_YEARLY)
}

/**
 * FREE vs Premium entitlement limits.
 */
object EntitlementLimits {
    const val FREE_MAX_MEMBERS = 5
    const val FREE_MAX_FAMILIES = 1
    const val FREE_MAX_STORAGE_BYTES = 2L * 1024L * 1024L * 1024L
    const val PREMIUM_MAX_STORAGE_BYTES = 50L * 1024L * 1024L * 1024L
}
