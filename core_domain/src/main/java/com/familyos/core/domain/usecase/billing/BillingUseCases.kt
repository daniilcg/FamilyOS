package com.familyos.core.domain.usecase.billing

import com.familyos.core.domain.model.SubscriptionInfo
import com.familyos.core.domain.model.SubscriptionPlan
import com.familyos.core.domain.repository.BillingRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Observes subscription status for a family. */
class ObserveSubscriptionUseCase @Inject constructor(private val billingRepository: BillingRepository) {
    operator fun invoke(familyId: String): Flow<SubscriptionInfo> =
        billingRepository.observeSubscription(familyId)
}

/** Records a successful Play Billing purchase. */
class PurchaseSubscriptionUseCase @Inject constructor(private val billingRepository: BillingRepository) {
    suspend operator fun invoke(
        familyId: String,
        plan: SubscriptionPlan,
        purchaseToken: String,
        productId: String,
    ): Result<SubscriptionInfo> {
        if (familyId.isBlank()) return Result.failure(AppError.Validation("familyId required"))
        if (purchaseToken.isBlank()) return Result.failure(AppError.Billing("Missing purchase token"))
        return billingRepository.purchase(familyId, plan, purchaseToken, productId)
    }
}

/** Restores previous purchases. */
class RestorePurchasesUseCase @Inject constructor(private val billingRepository: BillingRepository) {
    suspend operator fun invoke(familyId: String): Result<SubscriptionInfo> =
        billingRepository.restorePurchases(familyId)
}
