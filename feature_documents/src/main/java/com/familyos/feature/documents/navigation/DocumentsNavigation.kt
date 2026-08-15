package com.familyos.feature.documents.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.familyos.feature.documents.ui.DocumentDetailScreen
import com.familyos.feature.documents.ui.DocumentListScreen
import com.familyos.feature.documents.ui.DocumentLockScreen
import com.familyos.feature.documents.ui.ImportDocumentScreen
import com.familyos.feature.documents.viewmodel.DocumentsViewModel

/** Documents feature route constants. */
object DocumentsRoutes {
    const val ROOT = "documents"
    const val LOCK = "documents/lock"
    const val LIST = "documents/list"
    const val IMPORT = "documents/import"
    const val DETAIL = "documents/detail/{documentId}"

    fun detail(documentId: String) = "documents/detail/$documentId"
}

/**
 * Registers documents navigation destinations.
 */
fun NavGraphBuilder.documentsGraph(navController: NavHostController) {
    composable(DocumentsRoutes.ROOT) {
        DocumentsEntry(navController)
    }
    composable(DocumentsRoutes.LOCK) {
        DocumentsLockRoute(navController)
    }
    composable(DocumentsRoutes.LIST) {
        DocumentsListRoute(navController)
    }
    composable(DocumentsRoutes.IMPORT) {
        DocumentsImportRoute(navController)
    }
    composable(
        route = DocumentsRoutes.DETAIL,
        arguments = listOf(navArgument("documentId") { type = NavType.StringType }),
    ) { entry ->
        val id = entry.arguments?.getString("documentId").orEmpty()
        DocumentsDetailRoute(navController, id)
    }
}

@Composable
private fun DocumentsEntry(navController: NavHostController) {
    val vm: DocumentsViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isUnlocked, state.pinConfigured) {
        if (state.isUnlocked) {
            navController.navigate(DocumentsRoutes.LIST) {
                popUpTo(DocumentsRoutes.ROOT) { inclusive = true }
            }
        } else {
            navController.navigate(DocumentsRoutes.LOCK) {
                popUpTo(DocumentsRoutes.ROOT) { inclusive = true }
            }
        }
    }
}

@Composable
private fun DocumentsLockRoute(navController: NavHostController) {
    val vm: DocumentsViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isUnlocked) {
        if (state.isUnlocked) {
            navController.navigate(DocumentsRoutes.LIST) {
                popUpTo(DocumentsRoutes.LOCK) { inclusive = true }
            }
        }
    }
    DocumentLockScreen(
        state = state,
        onSetupPin = vm::setupPin,
        onUnlockPin = vm::unlockWithPin,
        onBiometricSuccess = vm::unlockWithBiometric,
        onBiometricToggle = vm::setBiometricEnabled,
    )
}

@Composable
private fun DocumentsListRoute(navController: NavHostController) {
    val vm: DocumentsViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isUnlocked) {
        if (!state.isUnlocked) {
            navController.navigate(DocumentsRoutes.LOCK) {
                popUpTo(DocumentsRoutes.LIST) { inclusive = true }
            }
        }
    }
    DocumentListScreen(
        state = state,
        onOpenDocument = { navController.navigate(DocumentsRoutes.detail(it)) },
        onImport = { navController.navigate(DocumentsRoutes.IMPORT) },
        onLock = {
            vm.lock()
            navController.navigate(DocumentsRoutes.LOCK) {
                popUpTo(DocumentsRoutes.LIST) { inclusive = true }
            }
        },
        onQueryChange = vm::setQuery,
        onFilterChange = vm::setFilter,
    )
}

@Composable
private fun DocumentsImportRoute(navController: NavHostController) {
    val vm: DocumentsViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    ImportDocumentScreen(
        state = state,
        onBack = { navController.popBackStack() },
        onImport = { title, type, mime, bytes, tags -> vm.import(title, type, mime, bytes, tags) },
        onConsumedSuccess = vm::clearMessages,
    )
}

@Composable
private fun DocumentsDetailRoute(navController: NavHostController, documentId: String) {
    val vm: DocumentsViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(documentId) { vm.loadDocument(documentId) }
    DocumentDetailScreen(
        document = state.selected,
        isLoading = state.isLoading,
        errorMessage = state.errorMessage,
        onBack = { navController.popBackStack() },
        onDelete = {
            vm.delete(it)
            navController.popBackStack()
        },
        onOpenDecrypted = { id ->
            vm.openDecrypted(id) { bytes ->
                Toast.makeText(context, "Decrypted ${bytes.size} bytes", Toast.LENGTH_SHORT).show()
            }
        },
    )
}
