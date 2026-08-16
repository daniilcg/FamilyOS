package com.familyos.feature.shopping.ui.list

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.ui.components.FamilyEmptyState
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.core.ui.theme.FamilyOsTheme
import com.familyos.feature.shopping.ui.components.ShoppingItemRow
import com.familyos.feature.shopping.util.ShoppingSort
import com.familyos.feature.shopping.util.ShoppingUiCategories
import com.familyos.feature.shopping.util.label
import com.familyos.feature.shopping.viewmodel.ShoppingEvent
import com.familyos.feature.shopping.viewmodel.ShoppingUiState
import com.familyos.feature.shopping.viewmodel.ShoppingViewModel
import com.familyos.core.ui.locale.rememberUiStrings

/**
 * Active shopping list with search, category filter, sort, and category grouping.
 */
@Composable
fun ShoppingListScreen(
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onHistoryClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: ShoppingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = rememberUiStrings()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onStatusFilter(ShoppingStatus.ACTIVE)
        viewModel.events.collect { event ->
            when (event) {
                is ShoppingEvent.Message -> snackbar.showSnackbar(event.text)
            }
        }
    }

    ShoppingListContent(
        state = state,
        snackbar = snackbar,
        onQueryChange = viewModel::onQueryChange,
        onCategoryFilter = viewModel::onCategoryFilter,
        onSortChange = viewModel::onSortChange,
        onGroupByCategoryChange = viewModel::onGroupByCategoryChange,
        onAddClick = onAddClick,
        onEditClick = onEditClick,
        onHistoryClick = onHistoryClick,
        onArchiveClick = onArchiveClick,
        onPurchase = viewModel::purchase,
        onArchive = viewModel::archive,
        onDelete = viewModel::delete,
        onBack = onBack,
        onClearError = viewModel::clearError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShoppingListContent(
    state: ShoppingUiState,
    snackbar: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onCategoryFilter: (com.familyos.core.domain.model.ShoppingCategory?) -> Unit,
    onSortChange: (ShoppingSort) -> Unit,
    onGroupByCategoryChange: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onHistoryClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onPurchase: (String) -> Unit,
    onArchive: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: (() -> Unit)?,
    onClearError: () -> Unit,
) {
    val s = rememberUiStrings()

    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        onClearError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.shoppingTitle) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Outlined.History, contentDescription = s.history)
                    }
                    IconButton(onClick = onArchiveClick) {
                        Icon(Icons.Outlined.Archive, contentDescription = s.archive)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = s.addItemCd)
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.isLoading && state.items.isEmpty() -> FamilyLoading()
            state.familyId.isNullOrBlank() -> FamilyEmptyState(message = s.joinFamilyForShopping)
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(s.search) },
                    placeholder = { Text(s.shoppingSearchHint) },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.categoryFilter == null,
                        onClick = { onCategoryFilter(null) },
                        label = { Text(s.all) },
                    )
                    ShoppingUiCategories.forEach { category ->
                        FilterChip(
                            selected = state.categoryFilter == category,
                            onClick = {
                                onCategoryFilter(
                                    if (state.categoryFilter == category) null else category,
                                )
                            },
                            label = { Text(category.label()) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { sortMenuExpanded = true }) {
                        Text("${s.sortPrefix}: ${state.sort.label()}")
                    }
                    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                        ShoppingSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label()) },
                                onClick = {
                                    onSortChange(option)
                                    sortMenuExpanded = false
                                },
                            )
                        }
                    }
                    FilterChip(
                        selected = state.groupByCategory,
                        onClick = { onGroupByCategoryChange(!state.groupByCategory) },
                        label = { Text(s.group) },
                    )
                }
                if (state.items.isEmpty()) {
                    FamilyEmptyState(message = s.noItemsYet)
                } else if (state.groupByCategory && state.grouped.isNotEmpty()) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.grouped.forEach { (category, items) ->
                            item(key = "header_${category.name}") {
                                Text(
                                    text = category.label(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                )
                            }
                            items(items, key = { it.id }) { item ->
                                ShoppingItemRow(
                                    item = item,
                                    onClick = { onEditClick(item.id) },
                                    onPurchase = { onPurchase(item.id) },
                                    onArchive = { onArchive(item.id) },
                                    onDelete = { onDelete(item.id) },
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            ShoppingItemRow(
                                item = item,
                                onClick = { onEditClick(item.id) },
                                onPurchase = { onPurchase(item.id) },
                                onArchive = { onArchive(item.id) },
                                onDelete = { onDelete(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShoppingListPreview() {
    FamilyOsTheme {
        ShoppingListContent(
            state = ShoppingUiState(isLoading = false, familyId = "f1"),
            snackbar = remember { SnackbarHostState() },
            onQueryChange = {},
            onCategoryFilter = {},
            onSortChange = {},
            onGroupByCategoryChange = {},
            onAddClick = {},
            onEditClick = {},
            onHistoryClick = {},
            onArchiveClick = {},
            onPurchase = {},
            onArchive = {},
            onDelete = {},
            onBack = null,
            onClearError = {},
        )
    }
}
