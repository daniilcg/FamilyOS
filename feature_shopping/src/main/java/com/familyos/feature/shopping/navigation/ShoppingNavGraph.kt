package com.familyos.feature.shopping.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.familyos.feature.shopping.ui.archive.ShoppingArchiveScreen
import com.familyos.feature.shopping.ui.edit.AddEditShoppingScreen
import com.familyos.feature.shopping.ui.history.ShoppingHistoryScreen
import com.familyos.feature.shopping.ui.list.ShoppingListScreen

/**
 * Registers the shopping nested navigation graph.
 *
 * @param onExit Optional callback when leaving the shopping graph root
 */
fun NavGraphBuilder.shoppingNavGraph(
    navController: NavHostController,
    onExit: (() -> Unit)? = null,
) {
    navigation(
        startDestination = ShoppingRoutes.LIST,
        route = ShoppingRoutes.GRAPH,
    ) {
        composable(ShoppingRoutes.LIST) {
            ShoppingListScreen(
                onAddClick = { navController.navigate(ShoppingRoutes.ADD) },
                onEditClick = { id -> navController.navigate(ShoppingRoutes.edit(id)) },
                onHistoryClick = { navController.navigate(ShoppingRoutes.HISTORY) },
                onArchiveClick = { navController.navigate(ShoppingRoutes.ARCHIVE) },
                onBack = onExit,
            )
        }
        composable(ShoppingRoutes.HISTORY) {
            ShoppingHistoryScreen(
                onBack = { navController.popBackStack() },
                onEditClick = { id -> navController.navigate(ShoppingRoutes.edit(id)) },
            )
        }
        composable(ShoppingRoutes.ARCHIVE) {
            ShoppingArchiveScreen(
                onBack = { navController.popBackStack() },
                onEditClick = { id -> navController.navigate(ShoppingRoutes.edit(id)) },
            )
        }
        composable(ShoppingRoutes.ADD) {
            AddEditShoppingScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = ShoppingRoutes.EDIT,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType }),
        ) {
            AddEditShoppingScreen(
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
