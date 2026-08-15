package com.familyos.feature.shopping.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.familyos.core.domain.model.ShoppingCategory
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.core.ui.theme.FamilyOsTheme
import com.familyos.feature.shopping.util.ShoppingUiCategories
import com.familyos.feature.shopping.util.label
import com.familyos.feature.shopping.viewmodel.AddEditShoppingEvent
import com.familyos.feature.shopping.viewmodel.AddEditShoppingUiState
import com.familyos.feature.shopping.viewmodel.AddEditShoppingViewModel

/**
 * Create or edit a shopping item.
 */
@Composable
fun AddEditShoppingScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddEditShoppingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AddEditShoppingEvent.Saved -> onDone()
            }
        }
    }

    AddEditShoppingContent(
        state = state,
        snackbar = snackbar,
        onNameChange = viewModel::onNameChange,
        onQuantityChange = viewModel::onQuantityChange,
        onUnitChange = viewModel::onUnitChange,
        onCategoryChange = viewModel::onCategoryChange,
        onCommentChange = viewModel::onCommentChange,
        onPriceChange = viewModel::onPriceChange,
        onPhotoUriChange = viewModel::onPhotoUriChange,
        onSave = viewModel::save,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddEditShoppingContent(
    state: AddEditShoppingUiState,
    snackbar: SnackbarHostState,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onCategoryChange: (ShoppingCategory) -> Unit,
    onCommentChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onPhotoUriChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    var categoryExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Edit item" else "Add item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.quantity,
                    onValueChange = onQuantityChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Quantity") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.unit,
                    onValueChange = onUnitChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Unit") },
                    placeholder = { Text("kg, pcs, L…") },
                    singleLine = true,
                )
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                ) {
                    OutlinedTextField(
                        value = state.category.label(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        state.categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.label()) },
                                onClick = {
                                    onCategoryChange(category)
                                    categoryExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = state.comment,
                    onValueChange = onCommentChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Comment") },
                    minLines = 2,
                )
                OutlinedTextField(
                    value = state.price,
                    onValueChange = onPriceChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Price") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.photoUri,
                    onValueChange = onPhotoUriChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Photo URI") },
                    placeholder = { Text("content:// or https://…") },
                    singleLine = true,
                )
                if (state.photoUri.isNotBlank()) {
                    AsyncImage(
                        model = state.photoUri,
                        contentDescription = "Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isSaving) "Saving…" else "Save")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddEditShoppingPreview() {
    FamilyOsTheme {
        AddEditShoppingContent(
            state = AddEditShoppingUiState(categories = ShoppingUiCategories),
            snackbar = remember { SnackbarHostState() },
            onNameChange = {},
            onQuantityChange = {},
            onUnitChange = {},
            onCategoryChange = {},
            onCommentChange = {},
            onPriceChange = {},
            onPhotoUriChange = {},
            onSave = {},
            onBack = {},
        )
    }
}
