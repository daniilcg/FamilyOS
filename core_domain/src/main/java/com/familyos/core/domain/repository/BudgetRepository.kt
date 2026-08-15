package com.familyos.core.domain.repository

import androidx.paging.PagingData
import com.familyos.core.domain.model.BudgetCategory
import com.familyos.core.domain.model.BudgetSummary
import com.familyos.core.domain.model.BudgetTransaction
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Budget ledger persistence and aggregations.
 */
interface BudgetRepository {
    fun observeTransactions(familyId: String, from: Long, to: Long): Flow<List<BudgetTransaction>>
    fun pagingTransactions(familyId: String, category: BudgetCategory?): Flow<PagingData<BudgetTransaction>>
    suspend fun getById(id: String): Result<BudgetTransaction>
    suspend fun upsert(transaction: BudgetTransaction): Result<BudgetTransaction>
    suspend fun delete(id: String): Result<Unit>
    suspend fun summarize(familyId: String, from: Long, to: Long): Result<BudgetSummary>
}
