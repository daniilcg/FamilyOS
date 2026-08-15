package com.familyos.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.familyos.feature.settings.ui.SettingsScreen

/**
 * Settings feature routes.
 */
object SettingsRoutes {
    const val SETTINGS = "settings"
}

/**
 * Registers the settings destination.
 */
fun NavGraphBuilder.settingsNavGraph(
    onNavigateBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    composable(SettingsRoutes.SETTINGS) {
        SettingsScreen(
            onNavigateBack = onNavigateBack,
            onLoggedOut = onLoggedOut,
            onOpenProfile = onOpenProfile,
        )
    }
}
