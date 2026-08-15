package com.familyos.feature.notifications.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.familyos.feature.notifications.ui.NotificationsScreen
import com.familyos.feature.notifications.viewmodel.NotificationsViewModel

/** Notifications route. */
object NotificationsRoutes {
    const val ROOT = "notifications"
}

/** Registers notification center destination. */
fun NavGraphBuilder.notificationsGraph() {
    composable(NotificationsRoutes.ROOT) { NotificationsRoute() }
}

@Composable
private fun NotificationsRoute() {
    val vm: NotificationsViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    NotificationsScreen(
        state = state,
        onFilterChange = vm::setFilter,
        onMarkRead = vm::markAsRead,
        onMarkAllRead = vm::markAllAsRead,
        onDelete = vm::delete,
    )
}
