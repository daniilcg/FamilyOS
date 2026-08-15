package com.familyos.feature.calendar.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.familyos.feature.calendar.ui.CalendarScreen
import com.familyos.feature.calendar.ui.editor.EventEditorScreen

/**
 * Registers the calendar nested navigation graph.
 */
fun NavGraphBuilder.calendarNavGraph(
    navController: NavHostController,
    onExit: (() -> Unit)? = null,
) {
    navigation(startDestination = CalendarRoutes.HOME, route = CalendarRoutes.GRAPH) {
        composable(CalendarRoutes.HOME) {
            CalendarScreen(
                onAddClick = { navController.navigate(CalendarRoutes.ADD) },
                onEventClick = { id -> navController.navigate(CalendarRoutes.edit(id)) },
                onBack = onExit,
            )
        }
        composable(CalendarRoutes.ADD) {
            EventEditorScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = CalendarRoutes.EDIT,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
        ) {
            EventEditorScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
