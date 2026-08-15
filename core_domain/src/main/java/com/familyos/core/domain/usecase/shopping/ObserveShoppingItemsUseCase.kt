package com.familyos.core.domain.usecase.shopping

import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes shopping items for a family. */
class ObserveShoppingItemsUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
) {
    operator fun invoke(familyId: String, status: ShoppingStatus? = ShoppingStatus.ACTIVE): Flow<List<ShoppingItem>> =
        shoppingRepository.observeItems(familyId, status)
}
