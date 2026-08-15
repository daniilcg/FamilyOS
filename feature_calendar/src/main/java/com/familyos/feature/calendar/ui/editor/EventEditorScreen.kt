package com.familyos.feature.calendar.ui.editor

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyos.core.domain.model.EventType
import com.familyos.core.domain.model.RecurrenceRule
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.feature.calendar.util.formatEventDay
import com.familyos.feature.calendar.util.formatEventTime
import com.familyos.feature.calendar.util.label
import com.familyos.feature.calendar.viewmodel.EventEditorEvent
import com.familyos.feature.calendar.viewmodel.EventEditorViewModel
import java.time.LocalDate

/**
 * Create / edit calendar event form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: EventEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var typeExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it is EventEditorEvent.Saved) onDone() }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Edit event" else "New event") },
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
                    value = state.title,
                    onValueChange = viewModel::onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description") },
                    minLines = 2,
                )
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    OutlinedTextField(
                        value = state.type.label(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        state.types.forEach { type: EventType ->
                            DropdownMenuItem(
                                text = { Text(type.label()) },
                                onClick = {
                                    viewModel.onTypeChange(type)
                                    typeExpanded = false
                                },
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("All day")
                    Switch(checked = state.allDay, onCheckedChange = viewModel::onAllDayChange)
                }
                Text("Starts ${formatEventDay(state.startAt)} ${if (state.allDay) "" else formatEventTime(state.startAt)}")
                Text("Ends ${formatEventDay(state.endAt)} ${if (state.allDay) "" else formatEventTime(state.endAt)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.setDate(LocalDate.now()) }) { Text("Today") }
                    Button(onClick = { viewModel.setDate(LocalDate.now().plusDays(1)) }) { Text("Tomorrow") }
                    Button(onClick = {
                        viewModel.onStartAtChange(System.currentTimeMillis())
                        viewModel.onEndAtChange(System.currentTimeMillis() + 3_600_000L)
                    }) { Text("Now +1h") }
                }
                OutlinedTextField(
                    value = state.location,
                    onValueChange = viewModel::onLocationChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Location") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.reminderMinutes,
                    onValueChange = viewModel::onReminderChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Reminder (minutes before)") },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Recurrence")
                    Switch(checked = state.recurrenceEnabled, onCheckedChange = viewModel::onRecurrenceEnabled)
                }
                if (state.recurrenceEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecurrenceRule.Frequency.entries.forEach { frequency ->
                            FilterChip(
                                selected = state.recurrenceFrequency == frequency,
                                onClick = { viewModel.onRecurrenceFrequency(frequency) },
                                label = { Text(frequency.name.lowercase().replaceFirstChar { it.titlecase() }) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.recurrenceInterval,
                        onValueChange = viewModel::onRecurrenceInterval,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Interval") },
                        singleLine = true,
                    )
                }
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isSaving) "Saving…" else "Save")
                }
            }
        }
    }
}
