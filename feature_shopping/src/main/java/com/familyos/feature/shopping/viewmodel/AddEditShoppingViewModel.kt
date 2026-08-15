package com.familyos.feature.shopping.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.ShoppingCategory
import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.shopping.GetShoppingItemUseCase
import com.familyos.core.domain.usecase.shopping.UpsertShoppingItemUseCase
import com.familyos.core.domain.util.Result
import com.familyos.feature.shopping.util.ShoppingUiCategories
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
 * Form state for create / edit shopping screens.
 */
data class AddEditShoppingUiState(
    val itemId: String? = null,
    val name: String = "",
    val quantity: String = "1",
    val unit: String = "",
    val category: ShoppingCategory = ShoppingCategory.PRODUCTS,
    val comment: String = "",
    val price: String = "",
    val photoUri: String = "",
    val categories: List<ShoppingCategory> = ShoppingUiCategories,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isEdit: Boolean = false,
)

/** Navigation events from the add/edit form. */
sealed interface AddEditShoppingEvent {
    data object Saved : AddEditShoppingEvent
}

/**
 * ViewModel for [com.familyos.feature.shopping.ui.edit.AddEditShoppingScreen].
 */
@HiltViewModel
class AddEditShoppingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getShoppingItem: GetShoppingItemUseCase,
    private val upsertShoppingItem: UpsertShoppingItemUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val itemId: String? = savedStateHandle["itemId"]

    private val _uiState = MutableStateFlow(
        AddEditShoppingUiState(itemId = itemId, isEdit = !itemId.isNullOrBlank()),
    )
    val uiState: StateFlow<AddEditShoppingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddEditShoppingEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var familyId: String? = null
    private var userId: String? = null
    private var existing: ShoppingItem? = null

    init {
        viewModelScope.launch {
            val user = getCurrentUser()
            val prefs = preferencesRepository.get()
            familyId = prefs.activeFamilyId ?: user?.familyId
            userId = user?.id
            val id = itemId
            if (!id.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = true) }
                when (val result = getShoppingItem(id)) {
                    is Result.Success -> {
                        existing = result.data
                        val item = result.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                name = item.title,
                                quantity = item.quantity.toString(),
                                unit = item.unit.orEmpty(),
                                category = item.category,
                                comment = item.notes.orEmpty(),
                                price = item.estimatedPrice?.toString().orEmpty(),
                                photoUri = item.photoUri.orEmpty(),
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.error.message)
                        }
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }
    fun onQuantityChange(value: String) = _uiState.update { it.copy(quantity = value) }
    fun onUnitChange(value: String) = _uiState.update { it.copy(unit = value) }
    fun onCategoryChange(value: ShoppingCategory) = _uiState.update { it.copy(category = value) }
    fun onCommentChange(value: String) = _uiState.update { it.copy(comment = value) }
    fun onPriceChange(value: String) = _uiState.update { it.copy(price = value) }
    fun onPhotoUriChange(value: String) = _uiState.update { it.copy(photoUri = value) }

    /** Validates and persists the form. */
    fun save() {
        val state = _uiState.value
        val family = familyId
        val creator = userId
        if (family.isNullOrBlank() || creator.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Family or user is not available") }
            return
        }
        val quantity = state.quantity.toDoubleOrNull()
        if (quantity == null || quantity <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Enter a valid quantity") }
            return
        }
        val price = state.price.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        if (state.price.isNotBlank() && price == null) {
            _uiState.update { it.copy(errorMessage = "Enter a valid price") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val base = existing
            val item = ShoppingItem(
                id = base?.id.orEmpty(),
                familyId = family,
                title = state.name.trim(),
                quantity = quantity,
                unit = state.unit.trim().ifBlank { null },
                category = state.category,
                status = base?.status ?: ShoppingStatus.ACTIVE,
                notes = state.comment.trim().ifBlank { null },
                estimatedPrice = price,
                currency = base?.currency ?: "EUR",
                photoUri = state.photoUri.trim().ifBlank { null },
                createdBy = base?.createdBy ?: creator,
                assignedTo = base?.assignedTo,
                purchasedBy = base?.purchasedBy,
                purchasedAt = base?.purchasedAt,
                createdAt = base?.createdAt ?: 0L,
                updatedAt = base?.updatedAt ?: 0L,
                isDeleted = false,
            )
            when (val result = upsertShoppingItem(item)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.emit(AddEditShoppingEvent.Saved)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }
}
