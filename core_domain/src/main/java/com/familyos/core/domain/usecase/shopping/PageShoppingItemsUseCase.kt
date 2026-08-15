package com.familyos.core.domain.usecase.shopping

import androidx.paging.PagingData
import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams paged shopping items for a family and optional status filter. */
class PageShoppingItemsUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
) {
    operator fun invoke(
        familyId: String,
        status: ShoppingStatus? = ShoppingStatus.ACTIVE,
    ): Flow<PagingData<ShoppingItem>> = shoppingRepository.pagingItems(familyId, status)
}
