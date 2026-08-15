package com.familyos.core.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyos.core.data.local.entity.ShoppingEntity
import kotlinx.coroutines.flow.Flow

/** Data access for shopping items. */
@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping WHERE familyId = :familyId AND isDeleted = 0 AND (:status IS NULL OR status = :status) ORDER BY updatedAt DESC")
    fun observe(familyId: String, status: String?): Flow<List<ShoppingEntity>>

    @Query("SELECT * FROM shopping WHERE familyId = :familyId AND isDeleted = 0 AND (:status IS NULL OR status = :status) ORDER BY updatedAt DESC")
    fun paging(familyId: String, status: String?): PagingSource<Int, ShoppingEntity>

    @Query("""
        SELECT * FROM shopping
        WHERE familyId = :familyId AND isDeleted = 0
          AND (:status IS NULL OR status = :status)
          AND (:category IS NULL OR category = :category)
          AND (title LIKE '%' || :query || '%' OR IFNULL(notes,'') LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun search(familyId: String, query: String, category: String?, status: String?): Flow<List<ShoppingEntity>>

    @Query("SELECT * FROM shopping WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ShoppingEntity?

    @Upsert
    suspend fun upsert(entity: ShoppingEntity)

    @Query("UPDATE shopping SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("UPDATE shopping SET status = :status, purchasedBy = :purchasedBy, purchasedAt = :purchasedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markPurchased(id: String, status: String, purchasedBy: String, purchasedAt: Long, updatedAt: Long)

    @Query("UPDATE shopping SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)
}
