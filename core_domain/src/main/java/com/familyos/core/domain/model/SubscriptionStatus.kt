package com.familyos.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Billing subscription lifecycle state.
 */
@Serializable
enum class SubscriptionStatus {
    NONE,
    ACTIVE,
    TRIALING,
    PAST_DUE,
    CANCELED,
    EXPIRED,
}

/**
 * Family billing snapshot.
 */
@Serializable
data class SubscriptionInfo(
    val familyId: String,
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val status: SubscriptionStatus = SubscriptionStatus.NONE,
    val productId: String? = null,
    val purchaseToken: String? = null,
    val expiresAt: Long? = null,
    val autoRenewing: Boolean = false,
    val updatedAt: Long = 0L,
) {
    /** True when the family currently has a paid entitlement. */
    val isPremium: Boolean
        get() = plan != SubscriptionPlan.FREE &&
            (status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIALING) &&
            (expiresAt == null || expiresAt > System.currentTimeMillis())
}
