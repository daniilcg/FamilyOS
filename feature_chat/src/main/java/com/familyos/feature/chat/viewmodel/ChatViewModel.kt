package com.familyos.feature.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.ChatMessage
import com.familyos.core.domain.model.ChatThread
import com.familyos.core.domain.model.MemberPresence
import com.familyos.core.domain.model.MessageType
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.chat.EnsureFamilyChatThreadUseCase
import com.familyos.core.domain.usecase.chat.MarkMessageReadUseCase
import com.familyos.core.domain.usecase.chat.ObserveChatMessagesUseCase
import com.familyos.core.domain.usecase.chat.ObserveChatThreadsUseCase
import com.familyos.core.domain.usecase.chat.ObservePresenceUseCase
import com.familyos.core.domain.usecase.chat.SendChatMessageUseCase
import com.familyos.core.domain.usecase.family.ObserveFamilyMembersUseCase
import com.familyos.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Chat UI state with messages, presence, and composing fields. */
data class ChatUiState(
    val thread: ChatThread? = null,
    val messages: List<ChatMessage> = emptyList(),
    val presence: List<MemberPresence> = emptyList(),
    val draft: String = "",
    val isRecording: Boolean = false,
    val recordingMs: Long = 0L,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val familyId: String? = null,
    val userId: String? = null,
    val userName: String? = null,
)

/**
 * Family chat ViewModel driven by realtime repository Flows.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val observeThreads: ObserveChatThreadsUseCase,
    private val observeMessages: ObserveChatMessagesUseCase,
    private val observePresence: ObservePresenceUseCase,
    private val sendMessage: SendChatMessageUseCase,
    private val markRead: MarkMessageReadUseCase,
    private val ensureThread: EnsureFamilyChatThreadUseCase,
    private val observeMembers: ObserveFamilyMembersUseCase,
    private val preferencesRepository: UserPreferencesRepository,
    private val getCurrentUser: GetCurrentUserUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var messagesJob: Job? = null

    init {
        viewModelScope.launch {
            val user = getCurrentUser()
            val prefs = preferencesRepository.get()
            val familyId = prefs.activeFamilyId ?: user?.familyId
            _state.update {
                it.copy(familyId = familyId, userId = user?.id, userName = user?.displayName)
            }
            if (familyId.isNullOrBlank() || user == null) {
                _state.update { it.copy(isLoading = false, errorMessage = "No active family") }
                return@launch
            }

            launch {
                observePresence(familyId).collect { list ->
                    _state.update { it.copy(presence = list) }
                }
            }

            val existing = observeThreads(familyId).first().firstOrNull()
            val thread = if (existing != null) {
                existing
            } else {
                val memberIds = observeMembers(familyId).first().map { it.userId }.ifEmpty { listOf(user.id) }
                when (
                    val created = ensureThread(
                        familyId = familyId,
                        createdBy = user.id,
                        participantIds = memberIds,
                    )
                ) {
                    is Result.Success -> created.data
                    is Result.Error -> {
                        _state.update { it.copy(isLoading = false, errorMessage = created.error.message) }
                        return@launch
                    }
                }
            }
            bindThread(thread)

            launch {
                observeThreads(familyId).collect { threads ->
                    val latest = threads.firstOrNull() ?: return@collect
                    if (latest.id != _state.value.thread?.id) {
                        bindThread(latest)
                    } else {
                        _state.update { it.copy(thread = latest) }
                    }
                }
            }
        }
    }

    private fun bindThread(thread: ChatThread) {
        _state.update { it.copy(thread = thread, isLoading = false) }
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            observeMessages(thread.id).collect { messages ->
                _state.update { it.copy(messages = messages) }
                val userId = _state.value.userId ?: return@collect
                messages
                    .filter { userId !in it.readBy && it.senderId != userId }
                    .forEach { markRead(it.id, userId) }
            }
        }
    }

    fun setDraft(value: String) {
        _state.update { it.copy(draft = value) }
    }

    fun sendText() {
        val body = _state.value.draft.trim()
        if (body.isEmpty()) return
        enqueue(MessageType.TEXT, body = body)
        _state.update { it.copy(draft = "") }
    }

    fun sendEmoji(emoji: String) {
        enqueue(MessageType.TEXT, body = emoji)
    }

    fun sendPhoto(url: String) {
        enqueue(MessageType.IMAGE, body = "Photo", attachmentUrl = url)
    }

    fun sendVoice(fileUrl: String, durationMs: Long) {
        enqueue(MessageType.VOICE, body = "Voice message", attachmentUrl = fileUrl, durationMs = durationMs)
        _state.update { it.copy(isRecording = false, recordingMs = 0L) }
    }

    fun setRecording(active: Boolean, elapsedMs: Long = 0L) {
        _state.update { it.copy(isRecording = active, recordingMs = elapsedMs) }
    }

    private fun enqueue(
        type: MessageType,
        body: String,
        attachmentUrl: String? = null,
        durationMs: Long? = null,
    ) {
        val s = _state.value
        val thread = s.thread ?: return
        val userId = s.userId ?: return
        val familyId = s.familyId ?: return
        viewModelScope.launch {
            when (
                val result = sendMessage(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        threadId = thread.id,
                        familyId = familyId,
                        senderId = userId,
                        type = type,
                        body = body,
                        attachmentUrl = attachmentUrl,
                        durationMs = durationMs,
                        readBy = listOf(userId),
                    ),
                )
            ) {
                is Result.Success -> Unit
                is Result.Error -> _state.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }
}
