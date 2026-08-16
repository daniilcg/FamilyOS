package com.familyos.feature.documents.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.familyos.core.domain.model.DocumentType
import com.familyos.feature.documents.viewmodel.DocumentsUiState
import com.familyos.core.ui.locale.rememberUiStrings

/**
 * Picks a PDF/DOCX/image file and imports it into the encrypted vault.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDocumentScreen(
    state: DocumentsUiState,
    onBack: () -> Unit,
    onImport: (title: String, type: DocumentType, mimeType: String, bytes: ByteArray, tags: List<String>) -> Unit,
    onConsumedSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = rememberUiStrings()

    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(DocumentType.OTHER) }
    var tagsText by remember { mutableStateOf("") }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingMime by remember { mutableStateOf("application/octet-stream") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val resolver = context.contentResolver
        pendingMime = resolver.getType(uri) ?: "application/octet-stream"
        selectedName = uri.lastPathSegment
        pendingBytes = resolver.openInputStream(uri)?.use { it.readBytes() }
        if (title.isBlank()) {
            title = selectedName?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Document"
        }
    }

    LaunchedEffect(state.importSuccess) {
        if (state.importSuccess) {
            onConsumedSuccess()
            onBack()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(s.importDocument) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.back)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    picker.launch(
                        arrayOf(
                            "application/pdf",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/msword",
                            "image/jpeg",
                            "image/png",
                            "image/webp",
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (selectedName == null) s.chooseFile else s.changeFile.format(selectedName))
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(s.title) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Text(s.type)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DocumentType.entries.forEach { entry ->
                    FilterChip(
                        selected = type == entry,
                        onClick = { type = entry },
                        label = { Text(entry.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }

            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = { Text(s.tagsComma) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val bytes = pendingBytes
                    if (bytes != null && title.isNotBlank()) {
                        onImport(
                            title,
                            type,
                            pendingMime,
                            bytes,
                            tagsText.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                        )
                    }
                },
                enabled = pendingBytes != null && title.isNotBlank() && !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isLoading) s.encrypting else s.importAes)
            }

            state.errorMessage?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
        }
    }
}
