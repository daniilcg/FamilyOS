package com.familyos.core.domain.usecase.shopping

import com.familyos.core.domain.model.ShoppingCategory
import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Searches and filters shopping items. */
class SearchShoppingItemsUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
) {
    operator fun invoke(
        familyId: String,
        query: String,
        category: ShoppingCategory? = null,
        status: ShoppingStatus? = ShoppingStatus.ACTIVE,
    ): Flow<List<ShoppingItem>> = shoppingRepository.search(familyId, query.trim(), category, status)
}
