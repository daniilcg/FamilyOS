package com.familyos.feature.documents.ui

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.familyos.core.domain.model.DocumentType
import com.familyos.core.domain.model.FamilyDocument
import com.familyos.core.ui.components.FamilyEmptyState
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.feature.documents.viewmodel.DocumentsUiState
import java.text.DateFormat
import java.util.Date
import com.familyos.core.ui.locale.rememberUiStrings

/**
 * Lists encrypted family documents with type filters and search.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentListScreen(
    state: DocumentsUiState,
    onOpenDocument: (String) -> Unit,
    onImport: () -> Unit,
    onLock: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilterChange: (DocumentType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = rememberUiStrings()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(s.documentsTitle) },
                actions = {
                    IconButton(onClick = onLock) {
                        Icon(Icons.Default.Lock, contentDescription = s.lockVault)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onImport) {
                Icon(Icons.Default.Add, contentDescription = s.importDocument)
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text(s.searchTitlesTags) },
                singleLine = true,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.filterType == null,
                    onClick = { onFilterChange(null) },
                    label = { Text(s.all) },
                )
                DocumentType.entries.forEach { type ->
                    FilterChip(
                        selected = state.filterType == type,
                        onClick = { onFilterChange(type) },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            when {
                state.isLoading -> FamilyLoading()
                state.documents.isEmpty() -> FamilyEmptyState(s.noDocumentsYet)
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.documents, key = { it.id }) { doc ->
                        DocumentRow(doc) { onOpenDocument(doc.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentRow(document: FamilyDocument, onClick: () -> Unit) {
    val s = rememberUiStrings()

    ListItem(
        headlineContent = { Text(document.title) },
        supportingContent = {
            Text(
                "${document.type.name} · ${formatSize(document.sizeBytes)} · " +
                    DateFormat.getDateInstance().format(Date(document.createdAt)),
            )
        },
        leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
        trailingContent = {
            if (document.isEncrypted) {
                Text("AES-256", style = MaterialTheme.typography.labelLarge)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
