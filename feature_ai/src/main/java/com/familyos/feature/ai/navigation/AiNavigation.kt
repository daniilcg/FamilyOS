package com.familyos.feature.ai.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.familyos.feature.ai.ui.AiScreen
import com.familyos.feature.ai.viewmodel.AiViewModel

/** Family AI route. */
object AiRoutes {
    const val ROOT = "ai"
}

/** Registers Family AI destination. */
fun NavGraphBuilder.aiGraph() {
    composable(AiRoutes.ROOT) { AiRoute() }
}

@Composable
private fun AiRoute() {
    val vm: AiViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    AiScreen(
        state = state,
        onDraftChange = vm::setDraft,
        onSend = vm::send,
        onProviderChange = vm::setProvider,
        onApiKeyChange = vm::setApiKeyDraft,
        onSaveApiKey = vm::saveApiKey,
        onApplyAction = vm::applyPendingAction,
    )
}
