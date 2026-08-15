package com.familyos.core.domain.usecase.budget

import com.familyos.core.domain.model.BudgetSummary
import com.familyos.core.domain.repository.BudgetRepository
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Computes a budget summary for a period. */
class GetBudgetSummaryUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    suspend operator fun invoke(familyId: String, from: Long, to: Long): Result<BudgetSummary> =
        budgetRepository.summarize(familyId, from, to)
}
