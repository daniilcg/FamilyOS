package com.familyos.feature.budget.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.BudgetCategory
import com.familyos.core.domain.model.BudgetSummary
import com.familyos.core.domain.model.BudgetTransaction
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.budget.DeleteBudgetTransactionUseCase
import com.familyos.core.domain.usecase.budget.GetBudgetSummaryUseCase
import com.familyos.core.domain.usecase.budget.ObserveBudgetTransactionsUseCase
import com.familyos.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * UI state for budget home and report screens.
 */
data class BudgetUiState(
    val familyId: String? = null,
    val userId: String? = null,
    val currency: String = "EUR",
    val month: LocalDate = LocalDate.now().withDayOfMonth(1),
    val transactions: List<BudgetTransaction> = emptyList(),
    val summary: BudgetSummary? = null,
    val categoryFilter: BudgetCategory? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

sealed interface BudgetEvent {
    data class Message(val text: String) : BudgetEvent
}

/**
 * ViewModel for monthly budget balance, transactions, and reports.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val observeTransactions: ObserveBudgetTransactionsUseCase,
    private val getBudgetSummary: GetBudgetSummaryUseCase,
    private val deleteTransaction: DeleteBudgetTransactionUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BudgetEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val familyIdFlow = MutableStateFlow<String?>(null)
    private val monthFlow = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    private val categoryFilter = MutableStateFlow<BudgetCategory?>(null)

    init {
        viewModelScope.launch {
            val user = getCurrentUser()
            val prefs = preferencesRepository.get()
            familyIdFlow.value = prefs.activeFamilyId ?: user?.familyId
            _uiState.update {
                it.copy(
                    familyId = familyIdFlow.value,
                    userId = user?.id,
                    currency = prefs.currencyCode,
                )
            }
        }

        viewModelScope.launch {
            combine(familyIdFlow, monthFlow, categoryFilter) { familyId, month, category ->
                Triple(familyId, month, category)
            }.flatMapLatest { (familyId, month, category) ->
                if (familyId.isNullOrBlank()) {
                    flowOf(Triple(emptyList<BudgetTransaction>(), null as BudgetSummary?, Triple(familyId, month, category)))
                } else {
                    val (from, to) = monthRange(month)
                    observeTransactions(familyId, from, to).map { list ->
                        val summary = when (val result = getBudgetSummary(familyId, from, to)) {
                            is Result.Success -> result.data
                            is Result.Error -> null
                        }
                        val filtered = if (category == null) list else list.filter { it.category == category }
                        Triple(filtered.sortedByDescending { it.occurredAt }, summary, Triple(familyId, month, category))
                    }
                }
            }.collect { (transactions, summary, meta) ->
                _uiState.update {
                    it.copy(
                        transactions = transactions,
                        summary = summary,
                        month = meta.second,
                        categoryFilter = meta.third,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun shiftMonth(forward: Boolean) {
        monthFlow.value = if (forward) monthFlow.value.plusMonths(1) else monthFlow.value.minusMonths(1)
        _uiState.update { it.copy(isLoading = true) }
    }

    fun setCategoryFilter(category: BudgetCategory?) {
        categoryFilter.value = category
    }

    fun delete(transactionId: String) {
        viewModelScope.launch {
            when (val result = deleteTransaction(transactionId)) {
                is Result.Success -> _events.emit(BudgetEvent.Message("Transaction deleted"))
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun monthRange(month: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = month.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.with(TemporalAdjusters.lastDayOfMonth())
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli() - 1
        return start to end
    }
}
