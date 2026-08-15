package com.familyos.feature.notes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.familyos.core.domain.model.Note
import com.familyos.core.ui.components.FamilyLoading

/**
 * Note editor supporting text, photos, checklist items, and tags.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: Note?,
    errorMessage: String?,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onAddPhotoUrl: (String) -> Unit,
    onAddChecklistItem: (String) -> Unit,
    onToggleChecklist: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (String) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onToggleArchive: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (note == null) {
        FamilyLoading()
        return
    }

    var tagInput by remember(note.id) { mutableStateOf(note.tags.joinToString(", ")) }
    var checklistInput by remember { mutableStateOf("") }
    var photoInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (note.id.isBlank()) "New note" else "Edit note") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (note.id.isNotBlank()) {
                        IconButton(onClick = { onTogglePin(note.id, !note.isPinned) }) {
                            Icon(Icons.Default.PushPin, contentDescription = "Pin")
                        }
                        IconButton(onClick = { onToggleArchive(note.id, !note.isArchived) }) {
                            Icon(Icons.Default.Archive, contentDescription = "Archive")
                        }
                        IconButton(onClick = { onDelete(note.id); onBack() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    TextButton(onClick = onSave) { Text("Save") }
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
            OutlinedTextField(
                value = note.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = note.body,
                onValueChange = onBodyChange,
                label = { Text("Text") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
            )
            OutlinedTextField(
                value = tagInput,
                onValueChange = {
                    tagInput = it
                    onTagsChange(it.split(',').map { t -> t.trim() }.filter { t -> t.isNotEmpty() })
                },
                label = { Text("Tags") },
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Photos")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(note.photoUrls) { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(2.dp)
                            .fillMaxWidth(0.4f),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = photoInput,
                    onValueChange = { photoInput = it },
                    label = { Text("Photo URL") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                IconButton(
                    onClick = {
                        if (photoInput.isNotBlank()) {
                            onAddPhotoUrl(photoInput.trim())
                            photoInput = ""
                        }
                    },
                ) { Icon(Icons.Default.Add, contentDescription = "Add photo") }
            }

            Text("Checklist")
            note.checklist.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { onToggleChecklist(item.id) },
                    )
                    Text(item.text)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = checklistInput,
                    onValueChange = { checklistInput = it },
                    label = { Text("Checklist item") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                IconButton(
                    onClick = {
                        if (checklistInput.isNotBlank()) {
                            onAddChecklistItem(checklistInput.trim())
                            checklistInput = ""
                        }
                    },
                ) { Icon(Icons.Default.Add, contentDescription = "Add item") }
            }

            errorMessage?.let {
                Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
        }
    }
}
