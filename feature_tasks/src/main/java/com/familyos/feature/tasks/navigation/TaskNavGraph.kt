package com.familyos.feature.tasks.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.familyos.feature.tasks.ui.detail.TaskDetailScreen
import com.familyos.feature.tasks.ui.edit.AddEditTaskScreen
import com.familyos.feature.tasks.ui.list.TaskListScreen

/**
 * Registers the tasks nested navigation graph.
 */
fun NavGraphBuilder.tasksNavGraph(
    navController: NavHostController,
    onExit: (() -> Unit)? = null,
) {
    navigation(startDestination = TaskRoutes.LIST, route = TaskRoutes.GRAPH) {
        composable(TaskRoutes.LIST) {
            TaskListScreen(
                onAddClick = { navController.navigate(TaskRoutes.ADD) },
                onTaskClick = { id -> navController.navigate(TaskRoutes.detail(id)) },
                onBack = onExit,
            )
        }
        composable(
            route = TaskRoutes.DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) {
            TaskDetailScreen(
                onEdit = { id -> navController.navigate(TaskRoutes.edit(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(TaskRoutes.ADD) {
            AddEditTaskScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = TaskRoutes.EDIT,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) {
            AddEditTaskScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
