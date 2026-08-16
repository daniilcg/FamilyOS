package com.familyos.core.domain.repository

import com.familyos.core.domain.model.SubscriptionInfo
import com.familyos.core.domain.model.SubscriptionPlan
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Subscription / Play Billing facade.
 */
interface BillingRepository {
    fun observeSubscription(familyId: String): Flow<SubscriptionInfo>
    fun observeIsPremium(familyId: String): Flow<Boolean>
    suspend fun getSubscription(familyId: String): Result<SubscriptionInfo>
    suspend fun launchPurchase(familyId: String, productId: String): Result<Unit>
    suspend fun purchase(
        familyId: String,
        plan: SubscriptionPlan,
        purchaseToken: String,
        productId: String,
    ): Result<SubscriptionInfo>
    suspend fun grantDeveloperPremium(familyId: String): Result<SubscriptionInfo>
    suspend fun restorePurchases(familyId: String): Result<SubscriptionInfo>
    suspend fun queryProductDetails(): Result<List<BillingProductDetails>>
    suspend fun cancelLocally(familyId: String): Result<SubscriptionInfo>
}

/**
 * Play Billing product details for the paywall UI.
 */
data class BillingProductDetails(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val billingPeriod: String,
)
