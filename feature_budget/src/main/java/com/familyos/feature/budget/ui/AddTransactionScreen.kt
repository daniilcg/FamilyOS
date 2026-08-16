package com.familyos.feature.budget.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyos.core.domain.model.BudgetCategory
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.feature.budget.util.formatDay
import com.familyos.feature.budget.util.label
import com.familyos.feature.budget.viewmodel.AddTransactionEvent
import com.familyos.feature.budget.viewmodel.AddTransactionViewModel
import com.familyos.core.ui.locale.rememberUiStrings

/**
 * Create or edit an income / expense transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = rememberUiStrings()
    val snackbar = remember { SnackbarHostState() }
    var categoryExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is AddTransactionEvent.Saved) onDone() }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) s.editTransaction else s.addTransaction) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.isLoading) {
            FamilyLoading()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !state.isIncome,
                        onClick = { viewModel.onIncomeChange(false) },
                        label = { Text(s.expense) },
                    )
                    FilterChip(
                        selected = state.isIncome,
                        onClick = { viewModel.onIncomeChange(true) },
                        label = { Text(s.income) },
                    )
                }
                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.title) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = viewModel::onAmountChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("${s.amount} (${state.currency})") },
                    singleLine = true,
                )
                if (!state.isIncome) {
                    ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                        OutlinedTextField(
                            value = state.category.label(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(s.category) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                            state.categories.forEach { category: BudgetCategory ->
                                DropdownMenuItem(
                                    text = { Text(category.label()) },
                                    onClick = {
                                        viewModel.onCategoryChange(category)
                                        categoryExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                Text("${s.dateLabel}: ${formatDay(state.occurredAt)}")
                Button(onClick = { viewModel.onOccurredAtChange(System.currentTimeMillis()) }) {
                    Text(s.setToNow)
                }
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.notes) },
                    minLines = 2,
                )
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isSaving) s.saving else s.save)
                }
            }
        }
    }
}
