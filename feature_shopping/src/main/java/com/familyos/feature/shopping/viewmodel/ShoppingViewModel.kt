package com.familyos.feature.shopping.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.familyos.core.domain.model.ShoppingCategory
import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.shopping.ArchiveShoppingItemUseCase
import com.familyos.core.domain.usecase.shopping.DeleteShoppingItemUseCase
import com.familyos.core.domain.usecase.shopping.MarkShoppingPurchasedUseCase
import com.familyos.core.domain.usecase.shopping.ObserveShoppingItemsUseCase
import com.familyos.core.domain.usecase.shopping.PageShoppingItemsUseCase
import com.familyos.core.domain.usecase.shopping.RestoreShoppingItemUseCase
import com.familyos.core.domain.usecase.shopping.SearchShoppingItemsUseCase
import com.familyos.core.domain.util.Result
import com.familyos.feature.shopping.util.ShoppingSort
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import javax.inject.Inject

/**
 * UI state for shopping list, history, and archive screens.
 */
data class ShoppingUiState(
    val familyId: String? = null,
    val userId: String? = null,
    val query: String = "",
    val categoryFilter: ShoppingCategory? = null,
    val sort: ShoppingSort = ShoppingSort.NEWEST,
    val groupByCategory: Boolean = true,
    val items: List<ShoppingItem> = emptyList(),
    val grouped: Map<ShoppingCategory, List<ShoppingItem>> = emptyMap(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val statusFilter: ShoppingStatus = ShoppingStatus.ACTIVE,
)

/**
 * One-shot shopping navigation / feedback events.
 */
sealed interface ShoppingEvent {
    data class Message(val text: String) : ShoppingEvent
}

/**
 * ViewModel driving shopping list search, filter, sort, grouping, paging, and item actions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val observeShoppingItems: ObserveShoppingItemsUseCase,
    private val searchShoppingItems: SearchShoppingItemsUseCase,
    private val pageShoppingItems: PageShoppingItemsUseCase,
    private val markPurchased: MarkShoppingPurchasedUseCase,
    private val archiveItem: ArchiveShoppingItemUseCase,
    private val restoreItem: RestoreShoppingItemUseCase,
    private val deleteItem: DeleteShoppingItemUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingUiState())
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ShoppingEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val query = MutableStateFlow("")
    private val categoryFilter = MutableStateFlow<ShoppingCategory?>(null)
    private val sort = MutableStateFlow(ShoppingSort.NEWEST)
    private val groupByCategory = MutableStateFlow(true)
    private val statusFilter = MutableStateFlow(ShoppingStatus.ACTIVE)
    private val familyIdFlow = MutableStateFlow<String?>(null)

    /**
     * Paged ACTIVE items for large lists. Client-side filters still apply via [uiState] list
     * when search/category filters are active.
     */
    val pagedItems: Flow<PagingData<ShoppingItem>> = familyIdFlow.flatMapLatest { familyId ->
        if (familyId.isNullOrBlank()) {
            flowOf(PagingData.empty())
        } else {
            pageShoppingItems(familyId, ShoppingStatus.ACTIVE)
        }
    }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            val user = getCurrentUser()
            val prefs = preferencesRepository.get()
            val familyId = prefs.activeFamilyId ?: user?.familyId
            familyIdFlow.value = familyId
            _uiState.update {
                it.copy(familyId = familyId, userId = user?.id)
            }
        }

        viewModelScope.launch {
            combine(
                combine(familyIdFlow, query, categoryFilter) { familyId, q, category ->
                    Triple(familyId, q, category)
                },
                combine(statusFilter, sort, groupByCategory) { status, sortMode, group ->
                    Triple(status, sortMode, group)
                },
            ) { identity, options ->
                ShoppingQuery(
                    familyId = identity.first,
                    query = identity.second,
                    category = identity.third,
                    status = options.first,
                    sort = options.second,
                    groupByCategory = options.third,
                )
            }.flatMapLatest { q ->
                if (q.familyId.isNullOrBlank()) {
                    flowOf(emptyList<ShoppingItem>() to q)
                } else if (q.query.isBlank() && q.category == null) {
                    observeShoppingItems(q.familyId, q.status).map { it to q }
                } else {
                    searchShoppingItems(q.familyId, q.query, q.category, q.status).map { it to q }
                }
            }.collect { (raw, q) ->
                val sorted = applySort(raw, q.sort)
                val grouped = if (q.groupByCategory) {
                    sorted.groupBy { normalizeCategory(it.category) }
                        .toSortedMap(compareBy { it.name })
                } else {
                    emptyMap()
                }
                _uiState.update {
                    it.copy(
                        query = q.query,
                        categoryFilter = q.category,
                        sort = q.sort,
                        groupByCategory = q.groupByCategory,
                        statusFilter = q.status,
                        items = sorted,
                        grouped = grouped,
                        isLoading = false,
                    )
                }
            }
        }
    }

    /** Updates free-text search. */
    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Sets category filter; null clears it. */
    fun onCategoryFilter(category: ShoppingCategory?) {
        categoryFilter.value = category
    }

    /** Updates sort mode. */
    fun onSortChange(value: ShoppingSort) {
        sort.value = value
    }

    /** Toggles category grouping on the list. */
    fun onGroupByCategoryChange(enabled: Boolean) {
        groupByCategory.value = enabled
    }

    /** Switches observed status (ACTIVE / PURCHASED / ARCHIVED). */
    fun onStatusFilter(status: ShoppingStatus) {
        statusFilter.value = status
        _uiState.update { it.copy(isLoading = true) }
    }

    /** Marks an item purchased by the current user. */
    fun purchase(itemId: String) {
        val userId = _uiState.value.userId ?: return emitError("Not signed in")
        viewModelScope.launch {
            when (val result = markPurchased(itemId, userId)) {
                is Result.Success -> _events.emit(ShoppingEvent.Message("Marked as purchased"))
                is Result.Error -> emitError(result.error.message)
            }
        }
    }

    /** Archives an item. */
    fun archive(itemId: String) {
        viewModelScope.launch {
            when (val result = archiveItem(itemId)) {
                is Result.Success -> _events.emit(ShoppingEvent.Message("Archived"))
                is Result.Error -> emitError(result.error.message)
            }
        }
    }

    /** Restores a purchased or archived item to ACTIVE. */
    fun restore(itemId: String) {
        viewModelScope.launch {
            when (val result = restoreItem(itemId)) {
                is Result.Success -> _events.emit(ShoppingEvent.Message("Restored to list"))
                is Result.Error -> emitError(result.error.message)
            }
        }
    }

    /** Soft-deletes an item. */
    fun delete(itemId: String) {
        viewModelScope.launch {
            when (val result = deleteItem(itemId)) {
                is Result.Success -> _events.emit(ShoppingEvent.Message("Deleted"))
                is Result.Error -> emitError(result.error.message)
            }
        }
    }

    /** Clears the last error message. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun emitError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    private data class ShoppingQuery(
        val familyId: String?,
        val query: String,
        val category: ShoppingCategory?,
        val status: ShoppingStatus,
        val sort: ShoppingSort,
        val groupByCategory: Boolean,
    )

    private fun normalizeCategory(category: ShoppingCategory): ShoppingCategory = category

    private fun applySort(items: List<ShoppingItem>, sort: ShoppingSort): List<ShoppingItem> =
        when (sort) {
            ShoppingSort.NAME_ASC -> items.sortedBy { it.title.lowercase() }
            ShoppingSort.NAME_DESC -> items.sortedByDescending { it.title.lowercase() }
            ShoppingSort.NEWEST -> items.sortedByDescending { it.createdAt }
            ShoppingSort.OLDEST -> items.sortedBy { it.createdAt }
            ShoppingSort.PRICE_ASC -> items.sortedBy { it.estimatedPrice ?: Double.MAX_VALUE }
            ShoppingSort.PRICE_DESC -> items.sortedByDescending { it.estimatedPrice ?: Double.MIN_VALUE }
            ShoppingSort.CATEGORY -> items.sortedWith(
                compareBy<ShoppingItem> { normalizeCategory(it.category).name }
                    .thenBy { it.title.lowercase() },
            )
        }
}
