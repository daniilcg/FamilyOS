package com.familyos.core.domain.usecase.budget

import com.familyos.core.domain.repository.BudgetRepository
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Deletes a budget transaction. */
class DeleteBudgetTransactionUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = budgetRepository.delete(id)
}
