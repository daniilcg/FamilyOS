package com.familyos.feature.shopping.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyos.core.domain.model.ShoppingStatus
import com.familyos.core.ui.components.FamilyEmptyState
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.feature.shopping.ui.components.ShoppingItemRow
import com.familyos.feature.shopping.viewmodel.ShoppingEvent
import com.familyos.feature.shopping.viewmodel.ShoppingViewModel
import com.familyos.core.ui.locale.rememberUiStrings

/**
 * History of purchased shopping items with restore / archive / delete actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingHistoryScreen(
    onBack: () -> Unit,
    onEditClick: (String) -> Unit,
    viewModel: ShoppingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val s = rememberUiStrings()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onStatusFilter(ShoppingStatus.PURCHASED)
        viewModel.events.collect { event ->
            when (event) {
                is ShoppingEvent.Message -> snackbar.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s.purchaseHistory) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.isLoading && state.items.isEmpty() -> FamilyLoading()
            state.items.isEmpty() -> FamilyEmptyState(message = s.noPurchasedYet)
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    ShoppingItemRow(
                        item = item,
                        onClick = { onEditClick(item.id) },
                        onRestore = { viewModel.restore(item.id) },
                        onArchive = { viewModel.archive(item.id) },
                        onDelete = { viewModel.delete(item.id) },
                    )
                }
            }
        }
    }
}
