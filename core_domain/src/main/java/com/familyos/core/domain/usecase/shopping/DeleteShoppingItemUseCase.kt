package com.familyos.core.domain.usecase.shopping

import com.familyos.core.domain.repository.ShoppingRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Soft-deletes a shopping item. */
class DeleteShoppingItemUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        if (id.isBlank()) return Result.failure(AppError.Validation("id required", "id"))
        return shoppingRepository.delete(id)
    }
}
