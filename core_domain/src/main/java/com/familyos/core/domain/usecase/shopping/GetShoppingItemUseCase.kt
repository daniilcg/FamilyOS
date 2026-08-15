package com.familyos.core.domain.usecase.shopping

import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.repository.ShoppingRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Loads a single shopping item by id. */
class GetShoppingItemUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
) {
    suspend operator fun invoke(id: String): Result<ShoppingItem> {
        if (id.isBlank()) return Result.failure(AppError.Validation("id required", "id"))
        return shoppingRepository.getById(id)
    }
}
