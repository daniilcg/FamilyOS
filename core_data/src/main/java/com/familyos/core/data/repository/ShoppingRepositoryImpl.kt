package com.familyos.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.familyos.core.data.local.dao.ShoppingDao
import com.familyos.core.data.mapper.EntityMappers
import com.familyos.core.data.mapper.EntityMappers.toDomain
import com.familyos.core.data.mapper.EntityMappers.toEntity
import com.familyos.core.data.remote.dto.ShoppingDto
import com.familyos.core.data.remote.firestore.FirestoreDataSource
import com.familyos.core.data.sync.SyncQueueRepositoryImpl
import com.familyos.core.domain.model.ShoppingCategory
import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.domain.model.SyncActionType
import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.repository.ShoppingRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.AppException
import com.familyos.core.domain.util.Constants
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first shopping repository: Room write → sync enqueue → Firestore listen.
 */
@Singleton
class ShoppingRepositoryImpl @Inject constructor(
    private val shoppingDao: ShoppingDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val syncQueue: SyncQueueRepositoryImpl,
) : ShoppingRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeItems(familyId: String, status: ShoppingStatus?): Flow<List<ShoppingItem>> =
        shoppingDao.observe(familyId, status?.name).map { it.map { e -> e.toDomain() } }.onStart {
            scope.launch { mirrorRemote(familyId) }
        }

    override fun pagingItems(familyId: String, status: ShoppingStatus?): Flow<PagingData<ShoppingItem>> =
        Pager(PagingConfig(pageSize = Constants.DEFAULT_PAGE_SIZE)) {
            shoppingDao.paging(familyId, status?.name)
        }.flow.map { paging -> paging.map { it.toDomain() } }

    override fun search(
        familyId: String,
        query: String,
        category: ShoppingCategory?,
        status: ShoppingStatus?,
    ): Flow<List<ShoppingItem>> =
        shoppingDao.search(familyId, query, category?.name, status?.name)
            .map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: String): Result<ShoppingItem> = Result.runCatching {
        shoppingDao.getById(id)?.toDomain() ?: throw AppException(AppError.NotFound("ShoppingItem", id))
    }

    override suspend fun upsert(item: ShoppingItem): Result<ShoppingItem> = Result.runCatching {
        shoppingDao.upsert(item.toEntity())
        enqueue(item)
        item
    }

    override suspend fun delete(id: String): Result<Unit> = Result.runCatching {
        val now = System.currentTimeMillis()
        shoppingDao.softDelete(id, now)
        val item = shoppingDao.getById(id)?.toDomain()
        if (item != null) {
            syncQueue.enqueue(
                SyncCollection.SHOPPING,
                id,
                item.familyId,
                SyncActionType.DELETE,
                EntityMappers.json.encodeToString(item.toDto()),
            )
        }
    }

    override suspend fun markPurchased(id: String, purchasedBy: String): Result<ShoppingItem> =
        Result.runCatching {
            val now = System.currentTimeMillis()
            shoppingDao.markPurchased(id, ShoppingStatus.PURCHASED.name, purchasedBy, now, now)
            val item = shoppingDao.getById(id)?.toDomain()
                ?: throw AppException(AppError.NotFound("ShoppingItem", id))
            enqueue(item)
            item
        }

    override suspend fun archive(id: String): Result<ShoppingItem> = Result.runCatching {
        val now = System.currentTimeMillis()
        shoppingDao.updateStatus(id, ShoppingStatus.ARCHIVED.name, now)
        val item = shoppingDao.getById(id)?.toDomain()
            ?: throw AppException(AppError.NotFound("ShoppingItem", id))
        enqueue(item)
        item
    }

    override suspend fun restore(id: String): Result<ShoppingItem> = Result.runCatching {
        val now = System.currentTimeMillis()
        shoppingDao.updateStatus(id, ShoppingStatus.ACTIVE.name, now)
        val item = shoppingDao.getById(id)?.toDomain()
            ?: throw AppException(AppError.NotFound("ShoppingItem", id))
        enqueue(item)
        item
    }

    private suspend fun enqueue(item: ShoppingItem) {
        syncQueue.enqueue(
            SyncCollection.SHOPPING,
            item.id,
            item.familyId,
            SyncActionType.UPSERT,
            EntityMappers.json.encodeToString(item.toDto()),
        )
    }

    private suspend fun mirrorRemote(familyId: String) {
        runCatching {
            firestoreDataSource.observeShopping(familyId).collect { dtos ->
                dtos.forEach { shoppingDao.upsert(it.toEntity()) }
            }
        }
    }

    private fun ShoppingItem.toDto() = ShoppingDto(
        id = id,
        familyId = familyId,
        title = title,
        quantity = quantity,
        unit = unit,
        category = category.name,
        status = status.name,
        notes = notes,
        estimatedPrice = estimatedPrice,
        currency = currency,
        photoUri = photoUri,
        createdBy = createdBy,
        assignedTo = assignedTo,
        purchasedBy = purchasedBy,
        purchasedAt = purchasedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )

    private fun ShoppingDto.toEntity() = com.familyos.core.data.local.entity.ShoppingEntity(
        id = id,
        familyId = familyId,
        title = title,
        quantity = quantity,
        unit = unit,
        category = category,
        status = status,
        notes = notes,
        estimatedPrice = estimatedPrice,
        currency = currency,
        photoUri = photoUri,
        createdBy = createdBy,
        assignedTo = assignedTo,
        purchasedBy = purchasedBy,
        purchasedAt = purchasedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isDeleted = isDeleted,
    )
}
