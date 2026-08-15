package com.familyos.feature.chat.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.familyos.feature.chat.ui.ChatScreen
import com.familyos.feature.chat.viewmodel.ChatViewModel

/** Chat route. */
object ChatRoutes {
    const val ROOT = "chat"
}

/** Registers the family chat destination. */
fun NavGraphBuilder.chatGraph() {
    composable(ChatRoutes.ROOT) { ChatRoute() }
}

@Composable
private fun ChatRoute() {
    val vm: ChatViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onDraftChange = vm::setDraft,
        onSendText = vm::sendText,
        onSendEmoji = vm::sendEmoji,
        onSendPhoto = vm::sendPhoto,
        onSendVoice = vm::sendVoice,
        onRecordingChanged = vm::setRecording,
    )
}
