package com.familyos.feature.documents.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.familyos.core.domain.model.FamilyDocument
import com.familyos.core.ui.components.FamilyLoading
import java.text.DateFormat
import java.util.Date

/**
 * Shows encrypted document metadata and actions to open or delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    document: FamilyDocument?,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onDelete: (String) -> Unit,
    onOpenDecrypted: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(document?.title ?: "Document") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (document != null) {
                        IconButton(onClick = { onOpenDecrypted(document.id) }) {
                            Icon(Icons.Default.Download, contentDescription = "Open decrypted")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            isLoading && document == null -> FamilyLoading()
            document == null -> {
                Column(Modifier.padding(padding).padding(24.dp)) {
                    Text(errorMessage ?: "Document not found")
                }
            }
            else -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MetaRow("Type", document.type.name)
                    MetaRow("MIME", document.mimeType)
                    MetaRow("Size", "${document.sizeBytes} bytes")
                    MetaRow("Encrypted", if (document.isEncrypted) "AES-256" else "No")
                    MetaRow("Uploaded", DateFormat.getDateTimeInstance().format(Date(document.createdAt)))
                    MetaRow("Tags", document.tags.joinToString().ifBlank { "—" })
                    MetaRow("Storage", document.storagePath)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onOpenDecrypted(document.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Decrypt & open")
                    }
                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmDelete && document != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete document?") },
            text = { Text("This removes \"${document.title}\" from the family vault.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(document.id)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
