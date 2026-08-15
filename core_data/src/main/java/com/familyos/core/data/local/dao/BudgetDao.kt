package com.familyos.core.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyos.core.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

/** Data access for budget transactions. */
@Dao
interface BudgetDao {
    @Query("""
        SELECT * FROM budgets
        WHERE familyId = :familyId AND isDeleted = 0
          AND occurredAt >= :from AND occurredAt < :to
        ORDER BY occurredAt DESC
    """)
    fun observePeriod(familyId: String, from: Long, to: Long): Flow<List<BudgetEntity>>

    @Query("""
        SELECT * FROM budgets
        WHERE familyId = :familyId AND isDeleted = 0
          AND (:category IS NULL OR category = :category)
        ORDER BY occurredAt DESC
    """)
    fun paging(familyId: String, category: String?): PagingSource<Int, BudgetEntity>

    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BudgetEntity?

    @Upsert
    suspend fun upsert(entity: BudgetEntity)

    @Query("UPDATE budgets SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query("""
        SELECT * FROM budgets
        WHERE familyId = :familyId AND isDeleted = 0
          AND occurredAt >= :from AND occurredAt < :to
    """)
    suspend fun listPeriod(familyId: String, from: Long, to: Long): List<BudgetEntity>
}
