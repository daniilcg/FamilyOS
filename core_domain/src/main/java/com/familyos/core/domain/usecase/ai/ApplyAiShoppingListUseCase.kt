package com.familyos.core.domain.usecase.ai

import com.familyos.core.domain.model.ShoppingCategory
import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.domain.usecase.shopping.UpsertShoppingItemUseCase
import com.familyos.core.domain.util.Result
import java.util.UUID
import javax.inject.Inject

/**
 * Applies a structured AI shopping-list payload by creating shopping items.
 */
class ApplyAiShoppingListUseCase @Inject constructor(
    private val upsertShoppingItem: UpsertShoppingItemUseCase,
) {
    data class AiShoppingLine(
        val title: String,
        val quantity: Double = 1.0,
        val unit: String? = null,
        val category: ShoppingCategory = ShoppingCategory.PRODUCTS,
    )

    suspend operator fun invoke(
        familyId: String,
        createdBy: String,
        lines: List<AiShoppingLine>,
    ): Result<List<ShoppingItem>> {
        val created = mutableListOf<ShoppingItem>()
        for (line in lines) {
            val result = upsertShoppingItem(
                ShoppingItem(
                    id = UUID.randomUUID().toString(),
                    familyId = familyId,
                    title = line.title,
                    quantity = line.quantity,
                    unit = line.unit,
                    category = line.category,
                    status = ShoppingStatus.ACTIVE,
                    createdBy = createdBy,
                ),
            )
            when (result) {
                is Result.Success -> created += result.data
                is Result.Error -> return result
            }
        }
        return Result.success(created)
    }
}
