package com.familyos.core.domain.usecase.billing

import com.familyos.core.domain.repository.BillingRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Launches a Play Billing purchase flow for a product id. */
class LaunchPurchaseUseCase @Inject constructor(
    private val billingRepository: BillingRepository,
) {
    suspend operator fun invoke(familyId: String, productId: String): Result<Unit> {
        if (familyId.isBlank() || productId.isBlank()) {
            return Result.failure(AppError.Validation("familyId and productId are required"))
        }
        return billingRepository.launchPurchase(familyId, productId)
    }
}
