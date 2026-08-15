package com.familyos.feature.budget.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.familyos.feature.budget.ui.AddTransactionScreen
import com.familyos.feature.budget.ui.BudgetScreen
import com.familyos.feature.budget.ui.ReportScreen

/**
 * Registers the budget nested navigation graph.
 */
fun NavGraphBuilder.budgetNavGraph(
    navController: NavHostController,
    onExit: (() -> Unit)? = null,
) {
    navigation(startDestination = BudgetRoutes.HOME, route = BudgetRoutes.GRAPH) {
        composable(BudgetRoutes.HOME) {
            BudgetScreen(
                onAddClick = { navController.navigate(BudgetRoutes.ADD) },
                onReportClick = { navController.navigate(BudgetRoutes.REPORT) },
                onTransactionClick = { id -> navController.navigate(BudgetRoutes.edit(id)) },
                onBack = onExit,
            )
        }
        composable(BudgetRoutes.ADD) {
            AddTransactionScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = BudgetRoutes.EDIT,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
        ) {
            AddTransactionScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(BudgetRoutes.REPORT) {
            ReportScreen(onBack = { navController.popBackStack() })
        }
    }
}
