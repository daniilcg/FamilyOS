package com.familyos.core.domain.usecase.budget

import com.familyos.core.domain.model.BudgetTransaction
import com.familyos.core.domain.repository.BudgetRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import java.util.UUID
import javax.inject.Inject

/** Creates or updates a budget transaction. */
class UpsertBudgetTransactionUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    suspend operator fun invoke(tx: BudgetTransaction): Result<BudgetTransaction> {
        if (tx.title.isBlank()) return Result.failure(AppError.Validation("Title required", "title"))
        if (tx.amount <= 0.0) return Result.failure(AppError.Validation("Amount must be positive", "amount"))
        if (tx.familyId.isBlank()) return Result.failure(AppError.Validation("familyId required"))
        val now = System.currentTimeMillis()
        return budgetRepository.upsert(
            tx.copy(
                id = tx.id.ifBlank { UUID.randomUUID().toString() },
                title = tx.title.trim(),
                updatedAt = now,
                createdAt = if (tx.createdAt == 0L) now else tx.createdAt,
            ),
        )
    }
}
