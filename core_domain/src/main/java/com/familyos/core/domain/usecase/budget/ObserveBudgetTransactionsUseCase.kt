package com.familyos.core.domain.usecase.budget

import com.familyos.core.domain.model.BudgetTransaction
import com.familyos.core.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes transactions in a period. */
class ObserveBudgetTransactionsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
) {
    operator fun invoke(familyId: String, from: Long, to: Long): Flow<List<BudgetTransaction>> =
        budgetRepository.observeTransactions(familyId, from, to)
}
