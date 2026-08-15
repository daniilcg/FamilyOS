package com.familyos.feature.home.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyos.core.domain.model.BudgetSummary
import com.familyos.core.domain.model.CalendarEvent
import com.familyos.core.domain.model.FamilyActivity
import com.familyos.core.domain.model.ShoppingItem
import com.familyos.core.domain.model.TaskItem
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.core.ui.theme.FamilyDanger
import com.familyos.core.ui.theme.FamilyWarning
import com.familyos.feature.home.HomeViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/**
 * Home dashboard aggregating tasks, shopping, events, budget, and activity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTasks: () -> Unit,
    onOpenShopping: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenBudget: () -> Unit,
    onCreateOrJoinFamily: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Home", style = MaterialTheme.typography.titleLarge)
                        state.familyName?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> FamilyLoading()
            state.needsFamily -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Set up your family", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Create a family or join with an invite code to see your dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onCreateOrJoinFamily) {
                        Text("Create or join family")
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    item {
                        DashboardSection(
                            title = "Overdue",
                            actionLabel = "Tasks",
                            onAction = onOpenTasks,
                            empty = state.overdueTasks.isEmpty(),
                            emptyMessage = "Nothing overdue",
                        ) {
                            state.overdueTasks.take(5).forEach { task ->
                                TaskLine(task = task, highlight = true, onClick = onOpenTasks)
                            }
                        }
                    }
                    item {
                        DashboardSection(
                            title = "Open tasks",
                            actionLabel = "See all",
                            onAction = onOpenTasks,
                            empty = state.openTasks.isEmpty(),
                            emptyMessage = "No open tasks",
                        ) {
                            state.openTasks.take(5).forEach { task ->
                                TaskLine(task = task, onClick = onOpenTasks)
                            }
                        }
                    }
                    item {
                        DashboardSection(
                            title = "Shopping",
                            actionLabel = "List",
                            onAction = onOpenShopping,
                            empty = state.shoppingItems.isEmpty(),
                            emptyMessage = "Shopping list is clear",
                        ) {
                            state.shoppingItems.take(6).forEach { item ->
                                ShoppingLine(item = item, onClick = onOpenShopping)
                            }
                        }
                    }
                    item {
                        DashboardSection(
                            title = "Today's events",
                            actionLabel = "Calendar",
                            onAction = onOpenCalendar,
                            empty = state.todayEvents.isEmpty(),
                            emptyMessage = "No events today",
                        ) {
                            state.todayEvents.forEach { event ->
                                EventLine(event = event, onClick = onOpenCalendar)
                            }
                        }
                    }
                    item {
                        DashboardSection(
                            title = "Upcoming deadlines",
                            actionLabel = "Tasks",
                            onAction = onOpenTasks,
                            empty = state.upcomingDeadlines.isEmpty(),
                            emptyMessage = "No upcoming deadlines",
                        ) {
                            state.upcomingDeadlines.take(5).forEach { task ->
                                TaskLine(task = task, onClick = onOpenTasks)
                            }
                        }
                    }
                    item {
                        BudgetCard(
                            summary = state.monthBudget,
                            onClick = onOpenBudget,
                        )
                    }
                    item {
                        DashboardSection(
                            title = "Recent family activity",
                            actionLabel = null,
                            onAction = null,
                            empty = state.recentActivity.isEmpty(),
                            emptyMessage = "Activity will appear as your family collaborates",
                        ) {
                            state.recentActivity.take(8).forEach { activity ->
                                ActivityLine(activity)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSection(
    title: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    empty: Boolean,
    emptyMessage: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (empty) {
            Text(
                emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        } else {
            content()
        }
    }
}

@Composable
private fun TaskLine(task: TaskItem, highlight: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (highlight) Icons.Outlined.WarningAmber else Icons.Outlined.TaskAlt,
            contentDescription = null,
            tint = if (highlight) FamilyDanger else MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.bodyLarge)
            task.dueAt?.let {
                Text(
                    formatDateTime(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (highlight) FamilyDanger else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        Text(
            task.priority.name,
            style = MaterialTheme.typography.labelSmall,
            color = FamilyWarning,
        )
    }
    HorizontalDivider()
}

@Composable
private fun ShoppingLine(item: ShoppingItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ShoppingCart, contentDescription = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${item.quantity}${item.unit?.let { " $it" } ?: ""} · ${item.category.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun EventLine(event: CalendarEvent, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Event, contentDescription = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                formatDateTime(event.startAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun ActivityLine(activity: FamilyActivity) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(activity.summary, style = MaterialTheme.typography.bodyLarge)
        Text(
            "${activity.actorName} · ${formatDateTime(activity.createdAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun BudgetCard(summary: BudgetSummary?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Text(
            text = "This month's budget",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (summary == null) {
            Text(
                "No budget data yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        } else {
            val currency = runCatching {
                NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
                    this.currency = Currency.getInstance(summary.currency)
                }
            }.getOrElse { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
            Text("Income  ${currency.format(summary.totalIncome)}")
            Text("Expense ${currency.format(summary.totalExpense)}")
            Text(
                "Balance ${currency.format(summary.balance)}",
                fontWeight = FontWeight.SemiBold,
                color = if (summary.balance >= 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    FamilyDanger
                },
            )
        }
    }
}

private fun formatDateTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(Instant.ofEpochMilli(epochMillis))
}
