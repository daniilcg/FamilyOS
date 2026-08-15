package com.familyos.feature.budget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyos.core.ui.components.FamilyEmptyState
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.core.ui.theme.FamilyDanger
import com.familyos.core.ui.theme.FamilySuccess
import com.familyos.feature.budget.ui.charts.CategoryBarChart
import com.familyos.feature.budget.ui.charts.CategoryPieChart
import com.familyos.feature.budget.util.formatMoney
import com.familyos.feature.budget.util.formatMonth
import com.familyos.feature.budget.viewmodel.BudgetViewModel
import java.time.ZoneId

/**
 * Monthly report with balance statistics and Canvas charts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val monthStartMillis = state.month
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shiftMonth(false) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                    }
                    IconButton(onClick = { viewModel.shiftMonth(true) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading && state.summary == null -> FamilyLoading()
            state.familyId.isNullOrBlank() -> FamilyEmptyState(message = "Join a family to view reports.")
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(formatMonth(monthStartMillis), style = MaterialTheme.typography.titleLarge)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val summary = state.summary
                        Text("Statistics", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Balance ${formatMoney(summary?.balance ?: 0.0, state.currency)}",
                            color = if ((summary?.balance ?: 0.0) >= 0) FamilySuccess else FamilyDanger,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text("Total income: ${formatMoney(summary?.totalIncome ?: 0.0, state.currency)}")
                        Text("Total expense: ${formatMoney(summary?.totalExpense ?: 0.0, state.currency)}")
                        Text("Transactions: ${state.transactions.size}")
                    }
                }
                Text("Expenses by category (bars)", style = MaterialTheme.typography.titleMedium)
                CategoryBarChart(values = state.summary?.byCategory.orEmpty(), currency = state.currency)
                Text("Expenses by category (pie)", style = MaterialTheme.typography.titleMedium)
                CategoryPieChart(values = state.summary?.byCategory.orEmpty(), currency = state.currency)
            }
        }
    }
}
