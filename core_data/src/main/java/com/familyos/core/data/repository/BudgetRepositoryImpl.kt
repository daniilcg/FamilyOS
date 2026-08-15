package com.familyos.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.familyos.core.data.local.dao.BudgetDao
import com.familyos.core.data.mapper.EntityMappers
import com.familyos.core.data.mapper.EntityMappers.toDomain
import com.familyos.core.data.mapper.EntityMappers.toEntity
import com.familyos.core.data.remote.dto.BudgetDto
import com.familyos.core.data.remote.firestore.FirestoreDataSource
import com.familyos.core.data.sync.SyncQueueRepositoryImpl
import com.familyos.core.domain.model.BudgetCategory
import com.familyos.core.domain.model.BudgetSummary
import com.familyos.core.domain.model.BudgetTransaction
import com.familyos.core.domain.model.SyncActionType
import com.familyos.core.domain.model.SyncCollection
import com.familyos.core.domain.repository.BudgetRepository
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

/** Offline-first budget repository. */
@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val syncQueue: SyncQueueRepositoryImpl,
) : BudgetRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeTransactions(familyId: String, from: Long, to: Long): Flow<List<BudgetTransaction>> =
        budgetDao.observePeriod(familyId, from, to).map { it.map { e -> e.toDomain() } }.onStart {
            scope.launch {
                runCatching {
                    firestoreDataSource.observeBudgets(familyId).collect { dtos ->
                        dtos.forEach { budgetDao.upsert(it.toEntity()) }
                    }
                }
            }
        }

    override fun pagingTransactions(familyId: String, category: BudgetCategory?): Flow<PagingData<BudgetTransaction>> =
        Pager(PagingConfig(Constants.DEFAULT_PAGE_SIZE)) {
            budgetDao.paging(familyId, category?.name)
        }.flow.map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: String): Result<BudgetTransaction> = Result.runCatching {
        budgetDao.getById(id)?.toDomain() ?: throw AppException(AppError.NotFound("BudgetTransaction", id))
    }

    override suspend fun upsert(transaction: BudgetTransaction): Result<BudgetTransaction> = Result.runCatching {
        budgetDao.upsert(transaction.toEntity())
        syncQueue.enqueue(SyncCollection.BUDGETS, transaction.id, transaction.familyId, SyncActionType.UPSERT,
            EntityMappers.json.encodeToString(transaction.toDto()))
        transaction
    }

    override suspend fun delete(id: String): Result<Unit> = Result.runCatching {
        budgetDao.softDelete(id, System.currentTimeMillis())
        budgetDao.getById(id)?.toDomain()?.let {
            syncQueue.enqueue(SyncCollection.BUDGETS, id, it.familyId, SyncActionType.DELETE,
                EntityMappers.json.encodeToString(it.toDto()))
        }
    }

    override suspend fun summarize(familyId: String, from: Long, to: Long): Result<BudgetSummary> =
        Result.runCatching {
            val items = budgetDao.listPeriod(familyId, from, to).map { it.toDomain() }
            val income = items.filter { it.isIncome }.sumOf { it.amount }
            val expense = items.filter { !it.isIncome }.sumOf { it.amount }
            val byCategory = items.filter { !it.isIncome }
                .groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
            BudgetSummary(
                familyId = familyId,
                periodStart = from,
                periodEnd = to,
                totalIncome = income,
                totalExpense = expense,
                balance = income - expense,
                byCategory = byCategory,
                currency = items.firstOrNull()?.currency ?: Constants.DEFAULT_CURRENCY,
            )
        }

    private fun BudgetTransaction.toDto() = BudgetDto(
        id, familyId, title, amount, currency, category.name, isIncome, notes, occurredAt,
        createdBy, receiptDocumentId, createdAt, updatedAt, isDeleted,
    )

    private fun BudgetDto.toEntity() = com.familyos.core.data.local.entity.BudgetEntity(
        id, familyId, title, amount, currency, category, isIncome, notes, occurredAt,
        createdBy, receiptDocumentId, createdAt, updatedAt, isDeleted,
    )
}
