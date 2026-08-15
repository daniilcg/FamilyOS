package com.familyos.feature.profile.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.familyos.feature.profile.ui.ProfileScreen

/**
 * Profile feature routes.
 */
object ProfileRoutes {
    const val PROFILE = "profile"
}

/**
 * Registers the profile destination.
 */
fun NavGraphBuilder.profileNavGraph(
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit,
) {
    composable(ProfileRoutes.PROFILE) {
        ProfileScreen(
            onNavigateBack = onNavigateBack,
            onAccountDeleted = onAccountDeleted,
        )
    }
}
