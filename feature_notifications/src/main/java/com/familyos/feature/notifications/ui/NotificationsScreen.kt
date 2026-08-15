package com.familyos.feature.notifications.ui

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familyos.core.domain.model.AppNotification
import com.familyos.core.domain.model.NotificationType
import com.familyos.core.ui.components.FamilyEmptyState
import com.familyos.core.ui.components.FamilyLoading
import com.familyos.feature.notifications.viewmodel.NotificationsUiState
import java.text.DateFormat
import java.util.Date

private val PRIMARY_FILTERS = listOf(
    NotificationType.NEW_TASK,
    NotificationType.SHOPPING,
    NotificationType.EVENT,
    NotificationType.BUDGET,
    NotificationType.DOCUMENT,
    NotificationType.MEMBER_JOINED,
)

/**
 * In-app notification center list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onFilterChange: (NotificationType?) -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    BadgedBox(
                        badge = {
                            if (state.unreadCount > 0) {
                                Badge { Text("${state.unreadCount}") }
                            }
                        },
                    ) {
                        Text("Notifications")
                    }
                },
                actions = {
                    IconButton(onClick = onMarkAllRead) {
                        Icon(Icons.Default.DoneAll, contentDescription = "Mark all read")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.filter == null,
                    onClick = { onFilterChange(null) },
                    label = { Text("All") },
                )
                PRIMARY_FILTERS.forEach { type ->
                    FilterChip(
                        selected = state.filter == type,
                        onClick = { onFilterChange(type) },
                        label = { Text(type.label()) },
                    )
                }
            }
            when {
                state.isLoading -> FamilyLoading()
                state.notifications.isEmpty() -> FamilyEmptyState("You're all caught up.")
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.notifications, key = { it.id }) { notification ->
                        NotificationRow(
                            notification = notification,
                            onClick = { onMarkRead(notification.id) },
                            onDelete = { onDelete(notification.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                notification.title,
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold,
            )
        },
        supportingContent = {
            Column {
                Text(notification.body)
                Text(
                    "${notification.type.label()} · " +
                        DateFormat.getDateTimeInstance().format(Date(notification.createdAt)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        },
        leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

private fun NotificationType.label(): String = when (this) {
    NotificationType.NEW_TASK, NotificationType.TASK_ASSIGNED, NotificationType.TASK_DUE -> "Task"
    NotificationType.SHOPPING, NotificationType.SHOPPING_UPDATE -> "Shopping"
    NotificationType.EVENT, NotificationType.EVENT_REMINDER -> "Event"
    NotificationType.BUDGET, NotificationType.BUDGET_ALERT -> "Budget"
    NotificationType.DOCUMENT, NotificationType.DOCUMENT_SHARED -> "Document"
    NotificationType.MEMBER_JOINED, NotificationType.FAMILY_INVITE -> "Member"
    NotificationType.CHAT_MESSAGE -> "Chat"
    NotificationType.SYSTEM -> "System"
}
