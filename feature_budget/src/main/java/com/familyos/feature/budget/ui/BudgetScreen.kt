package com.familyos.feature.budget.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyos.core.ui.components.FamilyEmptyState
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.core.ui.theme.FamilyDanger
import com.familyos.core.ui.theme.FamilyOsTheme
import com.familyos.core.ui.theme.FamilySuccess
import com.familyos.feature.budget.ui.charts.CategoryBarChart
import com.familyos.feature.budget.util.BudgetExpenseCategories
import com.familyos.feature.budget.util.formatDay
import com.familyos.feature.budget.util.formatMoney
import com.familyos.feature.budget.util.formatMonth
import com.familyos.feature.budget.util.label
import com.familyos.feature.budget.viewmodel.BudgetEvent
import com.familyos.feature.budget.viewmodel.BudgetUiState
import com.familyos.feature.budget.viewmodel.BudgetViewModel
import java.time.ZoneId

/**
 * Budget home with balance, monthly transactions, and category bars.
 */
@Composable
fun BudgetScreen(
    onAddClick: () -> Unit,
    onReportClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: BudgetViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is BudgetEvent.Message) snackbar.showSnackbar(it.text) }
    }
    BudgetContent(
        state = state,
        snackbar = snackbar,
        onShiftMonth = viewModel::shiftMonth,
        onCategoryFilter = viewModel::setCategoryFilter,
        onAddClick = onAddClick,
        onReportClick = onReportClick,
        onTransactionClick = onTransactionClick,
        onDelete = viewModel::delete,
        onBack = onBack,
        onClearError = viewModel::clearError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetContent(
    state: BudgetUiState,
    snackbar: SnackbarHostState,
    onShiftMonth: (Boolean) -> Unit,
    onCategoryFilter: (com.familyos.core.domain.model.BudgetCategory?) -> Unit,
    onAddClick: () -> Unit,
    onReportClick: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: (() -> Unit)?,
    onClearError: () -> Unit,
) {
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            onClearError()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onReportClick) {
                        Icon(Icons.Outlined.Assessment, contentDescription = "Report")
                    }
                    IconButton(onClick = { onShiftMonth(false) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    IconButton(onClick = { onShiftMonth(true) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add transaction")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.isLoading && state.transactions.isEmpty() && state.summary == null ->
                FamilyLoading()
            state.familyId.isNullOrBlank() -> FamilyEmptyState(message = "Join a family to track shared finances.")
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    val monthMillis = state.month
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    Text(formatMonth(monthMillis), style = MaterialTheme.typography.titleMedium)
                }
                item {
                    val summary = state.summary
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Balance", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = formatMoney(summary?.balance ?: 0.0, state.currency),
                                style = MaterialTheme.typography.headlineMedium,
                                color = if ((summary?.balance ?: 0.0) >= 0) FamilySuccess else FamilyDanger,
                            )
                            Text("Income ${formatMoney(summary?.totalIncome ?: 0.0, state.currency)}")
                            Text("Expense ${formatMoney(summary?.totalExpense ?: 0.0, state.currency)}")
                        }
                    }
                }
                item {
                    CategoryBarChart(
                        values = state.summary?.byCategory.orEmpty(),
                        currency = state.currency,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.categoryFilter == null,
                            onClick = { onCategoryFilter(null) },
                            label = { Text("All") },
                        )
                        BudgetExpenseCategories.forEach { category ->
                            FilterChip(
                                selected = state.categoryFilter == category,
                                onClick = {
                                    onCategoryFilter(if (state.categoryFilter == category) null else category)
                                },
                                label = { Text(category.label()) },
                            )
                        }
                    }
                }
                if (state.transactions.isEmpty()) {
                    item { FamilyEmptyState(message = "No transactions this month.") }
                } else {
                    items(state.transactions, key = { it.id }) { tx ->
                        Card(onClick = { onTransactionClick(tx.id) }, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(tx.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${if (tx.isIncome) "Income" else tx.category.label()} · ${formatDay(tx.occurredAt)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                                Text(
                                    text = (if (tx.isIncome) "+" else "−") + formatMoney(tx.amount, tx.currency),
                                    color = if (tx.isIncome) FamilySuccess else FamilyDanger,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "Delete",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clickable { onDelete(tx.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BudgetPreview() {
    FamilyOsTheme {
        BudgetContent(
            state = BudgetUiState(isLoading = false, familyId = "f1"),
            snackbar = remember { SnackbarHostState() },
            onShiftMonth = {},
            onCategoryFilter = {},
            onAddClick = {},
            onReportClick = {},
            onTransactionClick = {},
            onDelete = {},
            onBack = null,
            onClearError = {},
        )
    }
}
