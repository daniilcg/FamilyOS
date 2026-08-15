package com.familyos.core.domain.usecase.shopping

import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.domain.repository.ShoppingRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Constants
import com.familyos.core.domain.util.Result
import java.util.UUID
import javax.inject.Inject

/** Creates or updates a shopping item after validation. */
class UpsertShoppingItemUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
) {
    suspend operator fun invoke(item: ShoppingItem): Result<ShoppingItem> {
        val title = item.title.trim()
        if (title.isEmpty() || title.length > Constants.MAX_SHOPPING_TITLE_LENGTH) {
            return Result.failure(AppError.Validation("Title is required", "title"))
        }
        if (item.familyId.isBlank()) {
            return Result.failure(AppError.Validation("familyId is required", "familyId"))
        }
        val now = System.currentTimeMillis()
        val normalized = item.copy(
            id = item.id.ifBlank { UUID.randomUUID().toString() },
            title = title,
            updatedAt = now,
            createdAt = if (item.createdAt == 0L) now else item.createdAt,
        )
        return shoppingRepository.upsert(normalized)
    }
}
