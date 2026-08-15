package com.familyos.feature.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyos.core.domain.model.CalendarEvent
import com.familyos.core.ui.components.FamilyEmptyState
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.core.ui.theme.FamilyOsTheme
import com.familyos.feature.calendar.util.CalendarViewMode
import com.familyos.feature.calendar.util.EventUiTypes
import com.familyos.feature.calendar.util.formatEventDay
import com.familyos.feature.calendar.util.formatEventTime
import com.familyos.feature.calendar.util.label
import com.familyos.feature.calendar.viewmodel.CalendarEventFeedback
import com.familyos.feature.calendar.viewmodel.CalendarUiState
import com.familyos.feature.calendar.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendar hub hosting Month, Week, Day, and Agenda views.
 */
@Composable
fun CalendarScreen(
    onAddClick: () -> Unit,
    onEventClick: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is CalendarEventFeedback.Message) snackbar.showSnackbar(event.text)
        }
    }
    CalendarContent(
        state = state,
        snackbar = snackbar,
        onViewMode = viewModel::setViewMode,
        onSelectDate = viewModel::selectDate,
        onShift = viewModel::shiftPeriod,
        onTypeFilter = viewModel::setTypeFilter,
        onAddClick = onAddClick,
        onEventClick = onEventClick,
        onDelete = viewModel::delete,
        onBack = onBack,
        onClearError = viewModel::clearError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarContent(
    state: CalendarUiState,
    snackbar: SnackbarHostState,
    onViewMode: (CalendarViewMode) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onShift: (Boolean) -> Unit,
    onTypeFilter: (com.familyos.core.domain.model.EventType?) -> Unit,
    onAddClick: () -> Unit,
    onEventClick: (String) -> Unit,
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
                title = { Text("Calendar") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onShift(false) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                    }
                    IconButton(onClick = { onShift(true) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Filled.Add, contentDescription = "Add event")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.isLoading && state.events.isEmpty() -> FamilyLoading()
            state.familyId.isNullOrBlank() -> FamilyEmptyState(message = "Join a family to use the shared calendar.")
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CalendarViewMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.viewMode == mode,
                            onClick = { onViewMode(mode) },
                            label = { Text(mode.label()) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.typeFilter == null,
                        onClick = { onTypeFilter(null) },
                        label = { Text("All types") },
                    )
                    EventUiTypes.forEach { type ->
                        FilterChip(
                            selected = state.typeFilter == type,
                            onClick = { onTypeFilter(if (state.typeFilter == type) null else type) },
                            label = { Text(type.label()) },
                        )
                    }
                }
                Text(
                    text = state.selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                        " ${state.selectedDate.year}",
                    style = MaterialTheme.typography.titleMedium,
                )
                when (state.viewMode) {
                    CalendarViewMode.MONTH -> MonthGrid(
                        month = YearMonth.from(state.visibleMonth),
                        selected = state.selectedDate,
                        eventsByDay = state.eventsByDay,
                        onSelect = onSelectDate,
                    )
                    CalendarViewMode.WEEK,
                    CalendarViewMode.DAY,
                    CalendarViewMode.AGENDA,
                    -> Unit
                }
                val visibleEvents = when (state.viewMode) {
                    CalendarViewMode.DAY -> state.eventsByDay[state.selectedDate].orEmpty()
                    CalendarViewMode.WEEK -> state.events
                    CalendarViewMode.MONTH -> state.eventsByDay[state.selectedDate].orEmpty()
                    CalendarViewMode.AGENDA -> state.events
                }
                if (visibleEvents.isEmpty()) {
                    FamilyEmptyState(message = "No events in this range.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(visibleEvents, key = { it.id + it.startAt }) { event ->
                            EventCard(event = event, onClick = { onEventClick(event.id) }, onDelete = { onDelete(event.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    eventsByDay: Map<LocalDate, List<CalendarEvent>>,
    onSelect: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val startOffset = (first.dayOfWeek.value + 6) % 7
    val days = month.lengthOfMonth()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        var day = 1 - startOffset
        repeat(6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) {
                    val date = if (day in 1..days) month.atDay(day) else null
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    date == selected -> MaterialTheme.colorScheme.primary
                                    date != null && eventsByDay[date].orEmpty().isNotEmpty() ->
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.surface
                                },
                            )
                            .clickable(enabled = date != null) { date?.let(onSelect) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                color = if (date == selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                    day++
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: CalendarEvent,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Text(
                "${event.type.label()} · ${formatEventDay(event.startAt)} ${
                    if (event.allDay) "All day" else formatEventTime(event.startAt) + "–" + formatEventTime(event.endAt)
                }",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            if (!event.location.isNullOrBlank()) {
                Text(event.location.orEmpty(), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "Delete",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable(onClick = onDelete),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarPreview() {
    FamilyOsTheme {
        CalendarContent(
            state = CalendarUiState(isLoading = false, familyId = "f1"),
            snackbar = remember { SnackbarHostState() },
            onViewMode = {},
            onSelectDate = {},
            onShift = {},
            onTypeFilter = {},
            onAddClick = {},
            onEventClick = {},
            onDelete = {},
            onBack = null,
            onClearError = {},
        )
    }
}
