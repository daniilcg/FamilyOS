package com.familyos.core.domain.repository

import androidx.paging.PagingData
import com.familyos.core.domain.model.ShoppingCategory
import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Shopping list persistence and queries.
 */
interface ShoppingRepository {
    fun observeItems(familyId: String, status: ShoppingStatus? = null): Flow<List<ShoppingItem>>
    fun pagingItems(familyId: String, status: ShoppingStatus?): Flow<PagingData<ShoppingItem>>
    fun search(familyId: String, query: String, category: ShoppingCategory?, status: ShoppingStatus?): Flow<List<ShoppingItem>>
    suspend fun getById(id: String): Result<ShoppingItem>
    suspend fun upsert(item: ShoppingItem): Result<ShoppingItem>
    suspend fun delete(id: String): Result<Unit>
    suspend fun markPurchased(id: String, purchasedBy: String): Result<ShoppingItem>
    suspend fun archive(id: String): Result<ShoppingItem>
    suspend fun restore(id: String): Result<ShoppingItem>
}
