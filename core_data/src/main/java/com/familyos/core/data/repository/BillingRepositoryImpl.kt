package com.familyos.core.data.repository

import com.familyos.core.data.remote.dto.SubscriptionDto
import com.familyos.core.data.remote.firestore.FirestoreDataSource
import com.familyos.core.domain.model.BillingProducts
import com.familyos.core.domain.model.SubscriptionInfo
import com.familyos.core.domain.model.SubscriptionPlan
import com.familyos.core.domain.model.SubscriptionStatus
import com.familyos.core.domain.repository.BillingProductDetails
import com.familyos.core.domain.repository.BillingRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Billing repository backed by Firestore subscription documents.
 * Play Billing Client purchase UI is orchestrated in feature_billing;
 * this layer persists entitlements and exposes product metadata for the paywall.
 */
@Singleton
class BillingRepositoryImpl @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
) : BillingRepository {

    override fun observeSubscription(familyId: String): Flow<SubscriptionInfo> =
        firestoreDataSource.observeSubscription(familyId).map { dto ->
            dto?.toDomain() ?: SubscriptionInfo(familyId = familyId)
        }

    override fun observeIsPremium(familyId: String): Flow<Boolean> =
        observeSubscription(familyId).map { it.isPremium }

    override suspend fun getSubscription(familyId: String): Result<SubscriptionInfo> =
        Result.success(SubscriptionInfo(familyId = familyId))

    override suspend fun launchPurchase(familyId: String, productId: String): Result<Unit> =
        Result.runCatching {
            if (familyId.isBlank() || productId.isBlank()) {
                throw com.familyos.core.domain.util.AppException(
                    AppError.Validation("familyId and productId are required"),
                )
            }
            // Actual BillingClient.launchBillingFlow is invoked from feature_billing UI;
            // core_data validates inputs and acknowledges the launch request contract.
            Unit
        }

    override suspend fun purchase(
        familyId: String,
        plan: SubscriptionPlan,
        purchaseToken: String,
        productId: String,
    ): Result<SubscriptionInfo> = Result.runCatching {
        val info = SubscriptionInfo(
            familyId = familyId,
            plan = plan,
            status = SubscriptionStatus.ACTIVE,
            productId = productId,
            purchaseToken = purchaseToken,
            expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
            autoRenewing = true,
            updatedAt = System.currentTimeMillis(),
        )
        firestoreDataSource.upsertSubscription(info.toDto())
        info
    }

    override suspend fun grantDeveloperPremium(familyId: String): Result<SubscriptionInfo> =
        Result.runCatching {
            val info = SubscriptionInfo(
                familyId = familyId,
                plan = SubscriptionPlan.PREMIUM,
                status = SubscriptionStatus.ACTIVE,
                productId = "developer_lifetime",
                purchaseToken = "developer",
                expiresAt = null,
                autoRenewing = true,
                updatedAt = System.currentTimeMillis(),
            )
            firestoreDataSource.upsertSubscription(info.toDto())
            info
        }

    override suspend fun restorePurchases(familyId: String): Result<SubscriptionInfo> =
        Result.runCatching {
            val info = SubscriptionInfo(
                familyId = familyId,
                plan = SubscriptionPlan.PREMIUM,
                status = SubscriptionStatus.ACTIVE,
                updatedAt = System.currentTimeMillis(),
            )
            firestoreDataSource.upsertSubscription(info.toDto())
            info
        }

    override suspend fun queryProductDetails(): Result<List<BillingProductDetails>> =
        Result.success(
            BillingProducts.ALL.map { sku ->
                BillingProductDetails(
                    productId = sku,
                    title = sku.replace('_', ' '),
                    description = "FamilyOS subscription: $sku",
                    formattedPrice = when {
                        sku.contains("yearly") -> "€79.99"
                        else -> "€7.99"
                    },
                    billingPeriod = if (sku.contains("yearly")) "P1Y" else "P1M",
                )
            },
        )

    override suspend fun cancelLocally(familyId: String): Result<SubscriptionInfo> =
        Result.runCatching {
            val info = SubscriptionInfo(
                familyId = familyId,
                plan = SubscriptionPlan.FREE,
                status = SubscriptionStatus.CANCELED,
                autoRenewing = false,
                updatedAt = System.currentTimeMillis(),
            )
            firestoreDataSource.upsertSubscription(info.toDto())
            info
        }

    private fun SubscriptionInfo.toDto() = SubscriptionDto(
        familyId, plan.name, status.name, productId, purchaseToken, expiresAt, autoRenewing, updatedAt,
    )

    private fun SubscriptionDto.toDomain() = SubscriptionInfo(
        familyId,
        plan.toSubscriptionPlan(),
        runCatching { SubscriptionStatus.valueOf(status) }.getOrDefault(SubscriptionStatus.NONE),
        productId, purchaseToken, expiresAt, autoRenewing, updatedAt,
    )

    /** Maps stored / legacy plan names onto FREE / PREMIUM. */
    private fun String.toSubscriptionPlan(): SubscriptionPlan = when (uppercase()) {
        "PLUS", "FAMILY_PRO", "PREMIUM" -> SubscriptionPlan.PREMIUM
        else -> runCatching { SubscriptionPlan.valueOf(uppercase()) }.getOrDefault(SubscriptionPlan.FREE)
    }
}
