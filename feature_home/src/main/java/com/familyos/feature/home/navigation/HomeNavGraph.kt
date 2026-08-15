package com.familyos.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.familyos.feature.home.ui.HomeScreen

/**
 * Home feature routes.
 */
object HomeRoutes {
    const val HOME = "home"
}

/**
 * Registers the home destination.
 */
fun NavGraphBuilder.homeNavGraph(
    onOpenTasks: () -> Unit,
    onOpenShopping: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenBudget: () -> Unit,
    onCreateOrJoinFamily: () -> Unit,
) {
    composable(HomeRoutes.HOME) {
        HomeScreen(
            onOpenTasks = onOpenTasks,
            onOpenShopping = onOpenShopping,
            onOpenCalendar = onOpenCalendar,
            onOpenBudget = onOpenBudget,
            onCreateOrJoinFamily = onCreateOrJoinFamily,
        )
    }
}
