package com.familyos.feature.budget.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.BudgetCategory
import com.familyos.core.domain.model.BudgetTransaction
import com.familyos.core.domain.repository.BudgetRepository
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.budget.UpsertBudgetTransactionUseCase
import com.familyos.core.domain.util.Result
import com.familyos.feature.budget.util.BudgetExpenseCategories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Form state for income / expense entry.
 */
data class AddTransactionUiState(
    val transactionId: String? = null,
    val title: String = "",
    val amount: String = "",
    val isIncome: Boolean = false,
    val category: BudgetCategory = BudgetCategory.FOOD,
    val categories: List<BudgetCategory> = BudgetExpenseCategories,
    val notes: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val currency: String = "EUR",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isEdit: Boolean = false,
)

sealed interface AddTransactionEvent {
    data object Saved : AddTransactionEvent
}

/**
 * ViewModel for [com.familyos.feature.budget.ui.AddTransactionScreen].
 */
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val upsertTransaction: UpsertBudgetTransactionUseCase,
    private val budgetRepository: BudgetRepository,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val routeId = savedStateHandle.get<String>("transactionId")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        AddTransactionUiState(transactionId = routeId, isEdit = routeId != null),
    )
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddTransactionEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var familyId: String? = null
    private var userId: String? = null
    private var existing: BudgetTransaction? = null

    init {
        viewModelScope.launch {
            val user = getCurrentUser()
            val prefs = preferencesRepository.get()
            familyId = prefs.activeFamilyId ?: user?.familyId
            userId = user?.id
            _uiState.update { it.copy(currency = prefs.currencyCode) }
            val id = routeId
            if (id != null) {
                _uiState.update { it.copy(isLoading = true) }
                when (val result = budgetRepository.getById(id)) {
                    is Result.Success -> {
                        existing = result.data
                        val tx = result.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                title = tx.title,
                                amount = tx.amount.toString(),
                                isIncome = tx.isIncome,
                                category = tx.category,
                                notes = tx.notes.orEmpty(),
                                occurredAt = tx.occurredAt,
                                currency = tx.currency,
                            )
                        }
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v, errorMessage = null) }
    fun onAmountChange(v: String) = _uiState.update { it.copy(amount = v) }
    fun onIncomeChange(v: Boolean) = _uiState.update {
        it.copy(
            isIncome = v,
            category = if (!v && it.category !in BudgetExpenseCategories) BudgetCategory.FOOD else it.category,
        )
    }
    fun onCategoryChange(v: BudgetCategory) = _uiState.update { it.copy(category = v) }
    fun onNotesChange(v: String) = _uiState.update { it.copy(notes = v) }
    fun onOccurredAtChange(v: Long) = _uiState.update { it.copy(occurredAt = v) }

    fun save() {
        val state = _uiState.value
        val family = familyId
        val creator = userId
        if (family.isNullOrBlank() || creator.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Family or user is not available") }
            return
        }
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Enter a valid amount") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val base = existing
            val tx = BudgetTransaction(
                id = base?.id.orEmpty(),
                familyId = family,
                title = state.title.trim(),
                amount = amount,
                currency = state.currency,
                category = if (state.isIncome) BudgetCategory.OTHER else state.category,
                isIncome = state.isIncome,
                notes = state.notes.trim().ifBlank { null },
                occurredAt = state.occurredAt,
                createdBy = base?.createdBy ?: creator,
                receiptDocumentId = base?.receiptDocumentId,
                createdAt = base?.createdAt ?: 0L,
                updatedAt = base?.updatedAt ?: 0L,
                isDeleted = false,
            )
            when (val result = upsertTransaction(tx)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.emit(AddTransactionEvent.Saved)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.error.message)
                }
            }
        }
    }
}
