package com.familyos.feature.auth.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.familyos.feature.auth.AuthViewModel
import com.familyos.feature.auth.google.GoogleSignInHelper
import com.familyos.feature.auth.ui.ForgotPasswordScreen
import com.familyos.feature.auth.ui.LoginScreen
import com.familyos.feature.auth.ui.SignUpScreen

/**
 * Nested authentication navigation routes.
 */
object AuthRoutes {
    const val GRAPH = "auth_graph"
    const val LOGIN = "auth/login"
    const val SIGN_UP = "auth/sign_up"
    const val FORGOT_PASSWORD = "auth/forgot_password"
}

/**
 * Registers the authentication nested navigation graph.
 *
 * @param navController host controller used for in-graph navigation
 * @param googleSignInHelper Google Sign-In helper for login
 * @param onAuthenticated callback when the user completes sign-in
 */
fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    googleSignInHelper: GoogleSignInHelper,
    onAuthenticated: () -> Unit,
) {
    navigation(
        startDestination = AuthRoutes.LOGIN,
        route = AuthRoutes.GRAPH,
    ) {
        composable(AuthRoutes.LOGIN) {
            val viewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                onNavigateToSignUp = { navController.navigate(AuthRoutes.SIGN_UP) },
                onNavigateToForgotPassword = { navController.navigate(AuthRoutes.FORGOT_PASSWORD) },
                onSignedIn = onAuthenticated,
                googleSignInHelper = googleSignInHelper,
                viewModel = viewModel,
            )
        }
        composable(AuthRoutes.SIGN_UP) {
            val viewModel: AuthViewModel = hiltViewModel()
            SignUpScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignedIn = onAuthenticated,
                viewModel = viewModel,
            )
        }
        composable(AuthRoutes.FORGOT_PASSWORD) {
            val viewModel: AuthViewModel = hiltViewModel()
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = viewModel,
            )
        }
    }
}
