package com.familyos.core.domain.usecase.shopping

import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.repository.ShoppingRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Marks a shopping item as purchased. */
class MarkShoppingPurchasedUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
) {
    suspend operator fun invoke(id: String, purchasedBy: String): Result<ShoppingItem> {
        if (id.isBlank()) return Result.failure(AppError.Validation("id required", "id"))
        if (purchasedBy.isBlank()) return Result.failure(AppError.Unauthorized())
        return shoppingRepository.markPurchased(id, purchasedBy)
    }
}
