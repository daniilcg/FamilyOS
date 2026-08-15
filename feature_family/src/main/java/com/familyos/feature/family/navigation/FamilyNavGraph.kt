package com.familyos.feature.family.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.familyos.feature.family.ui.CreateFamilyScreen
import com.familyos.feature.family.ui.FamilyMembersScreen
import com.familyos.feature.family.ui.InviteScreen
import com.familyos.feature.family.ui.JoinFamilyScreen

/**
 * Family feature navigation routes.
 */
object FamilyRoutes {
    const val GRAPH = "family_graph"
    const val CREATE = "family/create"
    const val JOIN = "family/join"
    const val MEMBERS = "family/members"
    const val INVITE = "family/invite"
}

/**
 * Registers the family nested navigation graph.
 */
fun NavGraphBuilder.familyNavGraph(
    navController: NavHostController,
    onFamilyReady: () -> Unit = {},
) {
    navigation(
        startDestination = FamilyRoutes.MEMBERS,
        route = FamilyRoutes.GRAPH,
    ) {
        composable(FamilyRoutes.CREATE) {
            CreateFamilyScreen(
                onNavigateBack = { navController.popBackStack() },
                onFamilyCreated = {
                    onFamilyReady()
                    navController.navigate(FamilyRoutes.MEMBERS) {
                        popUpTo(FamilyRoutes.CREATE) { inclusive = true }
                    }
                },
            )
        }
        composable(FamilyRoutes.JOIN) {
            JoinFamilyScreen(
                onNavigateBack = { navController.popBackStack() },
                onFamilyJoined = {
                    onFamilyReady()
                    navController.navigate(FamilyRoutes.MEMBERS) {
                        popUpTo(FamilyRoutes.JOIN) { inclusive = true }
                    }
                },
            )
        }
        composable(FamilyRoutes.MEMBERS) {
            FamilyMembersScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToInvite = { navController.navigate(FamilyRoutes.INVITE) },
            )
        }
        composable(FamilyRoutes.INVITE) {
            InviteScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
