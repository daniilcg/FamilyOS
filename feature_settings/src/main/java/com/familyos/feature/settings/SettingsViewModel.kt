package com.familyos.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.ThemeMode
import com.familyos.core.domain.model.UserPreferences
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.LogoutUseCase
import com.familyos.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Settings UI state.
 */
data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

/**
 * Settings one-shot events.
 */
sealed interface SettingsEvent {
    data object LoggedOut : SettingsEvent
}

/**
 * Supported AI providers selectable in settings.
 */
enum class AiProviderOption(val id: String, val label: String) {
    OPENAI("openai", "OpenAI"),
    ANTHROPIC("anthropic", "Anthropic"),
    GOOGLE("google", "Google Gemini"),
    LOCAL("local", "On-device"),
}

/**
 * ViewModel for theme, notifications, biometric lock, language, AI provider, and logout.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.observe().collectLatest { prefs ->
                _uiState.update { it.copy(preferences = prefs, isLoading = false) }
            }
        }
    }

    /** Clears banners. */
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    /** Updates theme preference. */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            when (val result = userPreferencesRepository.setThemeMode(mode)) {
                is Result.Success -> Unit
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    /** Toggles push / in-app notification preference. */
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            when (val result = userPreferencesRepository.setNotificationsEnabled(enabled)) {
                is Result.Success -> Unit
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    /** Enables or disables biometric app lock. */
    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            when (val result = userPreferencesRepository.setBiometricEnabled(enabled)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            infoMessage = if (enabled) {
                                "Biometric lock enabled"
                            } else {
                                "Biometric lock disabled"
                            },
                        )
                    }
                }
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    /** Updates BCP-47 language tag. */
    fun setLanguage(tag: String) {
        viewModelScope.launch {
            when (val result = userPreferencesRepository.setLanguage(tag)) {
                is Result.Success -> _uiState.update { it.copy(infoMessage = "Language updated") }
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    /** Selects the AI provider id. */
    fun setAiProvider(providerId: String) {
        viewModelScope.launch {
            when (val result = userPreferencesRepository.setAiProvider(providerId)) {
                is Result.Success -> _uiState.update { it.copy(infoMessage = "AI provider updated") }
                is Result.Error -> _uiState.update { it.copy(errorMessage = result.error.message) }
            }
        }
    }

    /** Signs the user out. */
    fun logout() {
        viewModelScope.launch {
            when (val result = logoutUseCase()) {
                is Result.Success -> _events.emit(SettingsEvent.LoggedOut)
                is Result.Error -> {
                    Timber.w("Logout failed: %s", result.error.message)
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }
}
