package com.familyos.feature.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.AiConversation
import com.familyos.core.domain.model.AiMessage
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.ai.ApplyAiShoppingListUseCase
import com.familyos.core.domain.usecase.ai.ApplyAiTaskSetUseCase
import com.familyos.core.domain.usecase.ai.CreateAiConversationUseCase
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.billing.PremiumAccessControl
import com.familyos.core.domain.util.Result
import com.familyos.feature.ai.keys.AiKeyStore
import com.familyos.feature.ai.parser.AiDomainAction
import com.familyos.feature.ai.parser.AiResponseParser
import com.familyos.feature.ai.prompt.FamilyAiPrompts
import com.familyos.feature.ai.provider.AiChatMessage
import com.familyos.feature.ai.provider.AiCompletionRequest
import com.familyos.feature.ai.provider.AiProviderFactory
import com.familyos.feature.ai.provider.AiProviderId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** A rendered chat bubble in the Family AI UI. */
data class AiUiMessage(
    val id: String,
    val role: AiMessage.Role,
    val content: String,
    val actionSummary: String? = null,
)

/** Family AI screen state. */
data class AiUiState(
    val messages: List<AiUiMessage> = emptyList(),
    val draft: String = "",
    val providerId: AiProviderId = AiProviderId.OPENAI,
    val apiKeyDraft: String = "",
    val hasApiKey: Boolean = false,
    val isSending: Boolean = false,
    val isPremium: Boolean = false,
    val pendingAction: AiDomainAction? = null,
    val errorMessage: String? = null,
    val appliedMessage: String? = null,
    val keySavedMessage: String? = null,
    val familyId: String? = null,
    val userId: String? = null,
    val conversationId: String? = null,
)

/**
 * Family AI ViewModel: provider switching, real HTTP completions, JSON → domain actions.
 */
@HiltViewModel
class AiViewModel @Inject constructor(
    private val providerFactory: AiProviderFactory,
    private val keyStore: AiKeyStore,
    private val parser: AiResponseParser,
    private val applyShopping: ApplyAiShoppingListUseCase,
    private val applyTasks: ApplyAiTaskSetUseCase,
    private val createConversation: CreateAiConversationUseCase,
    private val preferencesRepository: UserPreferencesRepository,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val premiumAccess: PremiumAccessControl,
) : ViewModel() {

    private val _state = MutableStateFlow(AiUiState())
    val state: StateFlow<AiUiState> = _state.asStateFlow()

    private val history = mutableListOf<AiChatMessage>()

    init {
        viewModelScope.launch {
            val user = getCurrentUser()
            val prefs = preferencesRepository.get()
            val familyId = prefs.activeFamilyId ?: user?.familyId
            val provider = AiProviderId.fromId(prefs.aiProvider)
            _state.update {
                it.copy(
                    familyId = familyId,
                    userId = user?.id,
                    providerId = provider,
                )
            }
            if (!familyId.isNullOrBlank()) {
                launch {
                    premiumAccess.observeEntitlements(familyId).collect { ents ->
                        _state.update { it.copy(isPremium = ents.aiEnabled) }
                    }
                }
            }
            if (!familyId.isNullOrBlank() && user != null) {
                val now = System.currentTimeMillis()
                when (
                    val created = createConversation(
                        AiConversation(
                            id = UUID.randomUUID().toString(),
                            familyId = familyId,
                            userId = user.id,
                            title = "Family AI",
                            provider = provider.id,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                ) {
                    is Result.Success -> _state.update { it.copy(conversationId = created.data.id) }
                    is Result.Error -> Unit
                }
            }
            history += AiChatMessage("system", FamilyAiPrompts.SYSTEM_CORE)
            loadApiKey(provider)
        }
    }

    private fun loadApiKey(providerId: AiProviderId) {
        viewModelScope.launch {
            val key = keyStore.getKey(providerId)
            _state.update {
                it.copy(
                    apiKeyDraft = key,
                    hasApiKey = key.isNotBlank(),
                )
            }
        }
    }

    fun setDraft(value: String) {
        _state.update { it.copy(draft = value) }
    }

    fun setProvider(providerId: AiProviderId) {
        viewModelScope.launch {
            preferencesRepository.setAiProvider(providerId.id)
            _state.update { it.copy(providerId = providerId) }
            loadApiKey(providerId)
        }
    }

    fun setApiKeyDraft(value: String) {
        _state.update { it.copy(apiKeyDraft = value) }
    }

    fun saveApiKey() {
        val providerId = _state.value.providerId
        val value = _state.value.apiKeyDraft.trim()
        viewModelScope.launch {
            keyStore.setKey(providerId, value)
            _state.update {
                it.copy(
                    hasApiKey = value.isNotBlank(),
                    keySavedMessage = if (value.isBlank()) "API key cleared" else "API key saved",
                    errorMessage = null,
                )
            }
        }
    }

    fun send() {
        val text = _state.value.draft.trim()
        if (text.isEmpty() || _state.value.isSending) return
        if (!_state.value.isPremium) {
            _state.update { it.copy(errorMessage = "Family AI requires Premium") }
            return
        }
        _state.update {
            it.copy(
                draft = "",
                isSending = true,
                errorMessage = null,
                messages = it.messages + AiUiMessage(UUID.randomUUID().toString(), AiMessage.Role.USER, text),
            )
        }
        viewModelScope.launch {
            val featurePrompt = FamilyAiPrompts.detectFeaturePrompt(text)
            val providerId = _state.value.providerId
            val apiKey = keyStore.getKey(providerId)
            val requestMessages = history + listOf(
                AiChatMessage("system", featurePrompt),
                AiChatMessage("user", text),
            )
            when (
                val result = providerFactory.get(providerId).complete(
                    apiKey = apiKey,
                    request = AiCompletionRequest(messages = requestMessages, jsonMode = true),
                )
            ) {
                is Result.Success -> {
                    val action = parser.parse(result.data.content)
                    val display = renderAction(action)
                    history += AiChatMessage("user", text)
                    history += AiChatMessage("assistant", result.data.content)
                    _state.update {
                        it.copy(
                            isSending = false,
                            pendingAction = action,
                            messages = it.messages + AiUiMessage(
                                id = UUID.randomUUID().toString(),
                                role = AiMessage.Role.ASSISTANT,
                                content = display,
                                actionSummary = action::class.simpleName,
                            ),
                        )
                    }
                }
                is Result.Error -> {
                    _state.update {
                        it.copy(isSending = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    fun applyPendingAction() {
        val action = _state.value.pendingAction ?: return
        val familyId = _state.value.familyId ?: return
        val userId = _state.value.userId ?: return
        viewModelScope.launch {
            val message = when (action) {
                is AiDomainAction.ShoppingList -> {
                    when (val r = applyShopping(familyId, userId, action.items)) {
                        is Result.Success -> "Created ${r.data.size} shopping items"
                        is Result.Error -> r.error.message
                    }
                }
                is AiDomainAction.TaskSet -> {
                    when (val r = applyTasks(familyId, userId, action.tasks)) {
                        is Result.Success -> "Created ${r.data.size} tasks"
                        is Result.Error -> r.error.message
                    }
                }
                is AiDomainAction.TripChecklist -> {
                    val shop = applyShopping(familyId, userId, action.packing)
                    val tasks = applyTasks(familyId, userId, action.tasks)
                    when {
                        shop is Result.Error -> shop.error.message
                        tasks is Result.Error -> tasks.error.message
                        else -> "Applied trip packing + tasks for ${action.destination}"
                    }
                }
                is AiDomainAction.BudgetPlan -> action.summary.ifBlank {
                    "Budget plan ready (${action.currency} ${action.total})"
                }
                is AiDomainAction.ChatReply -> action.reply
                is AiDomainAction.Unknown -> "No structured action to apply"
            }
            _state.update { it.copy(appliedMessage = message, pendingAction = null) }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(errorMessage = null, appliedMessage = null, keySavedMessage = null) }
    }

    private fun renderAction(action: AiDomainAction): String = when (action) {
        is AiDomainAction.ShoppingList ->
            buildString {
                appendLine("Shopping list: ${action.title}")
                action.items.forEach { appendLine("• ${it.quantity} ${it.unit.orEmpty()} ${it.title}".trim()) }
            }
        is AiDomainAction.TaskSet ->
            buildString {
                appendLine("Tasks for: ${action.goal}")
                action.tasks.forEach { appendLine("• [${it.priority}] ${it.title}") }
            }
        is AiDomainAction.BudgetPlan ->
            buildString {
                appendLine(action.summary)
                action.allocations.forEach {
                    appendLine("• ${it.category}: ${it.amount} (${it.percent}%)")
                }
            }
        is AiDomainAction.TripChecklist ->
            buildString {
                appendLine("Trip: ${action.destination}")
                appendLine("Packing:")
                action.packing.forEach { appendLine("• ${it.title}") }
                appendLine("Tasks:")
                action.tasks.forEach { appendLine("• ${it.title}") }
            }
        is AiDomainAction.ChatReply -> action.reply
        is AiDomainAction.Unknown -> action.raw
    }
}
