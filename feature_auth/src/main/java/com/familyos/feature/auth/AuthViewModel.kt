package com.familyos.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.User
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.ObserveAuthStateUseCase
import com.familyos.core.domain.usecase.auth.ResetPasswordUseCase
import com.familyos.core.domain.usecase.auth.SignInEmailUseCase
import com.familyos.core.domain.usecase.auth.SignInGoogleUseCase
import com.familyos.core.domain.usecase.auth.SignUpEmailUseCase
import com.familyos.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for authentication screens.
 */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val confirmPassword: String = "",
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isSignedIn: Boolean = false,
    val currentUser: User? = null,
    val autoLoginChecked: Boolean = false,
)

/**
 * One-shot navigation / side-effect events from [AuthViewModel].
 */
sealed interface AuthEvent {
    data object NavigateToHome : AuthEvent
    data object PasswordResetSent : AuthEvent
}

/**
 * ViewModel that drives login, sign-up, password reset, Google sign-in,
 * remember-me persistence, and auto-login observation.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInEmail: SignInEmailUseCase,
    private val signInGoogle: SignInGoogleUseCase,
    private val signUpEmail: SignUpEmailUseCase,
    private val resetPassword: ResetPasswordUseCase,
    private val observeAuthState: ObserveAuthStateUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val prefs = userPreferencesRepository.observe().first()
            _uiState.update { it.copy(rememberMe = prefs.rememberMe) }
        }
        observeSession()
    }

    /** Updates the email field. */
    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, errorMessage = null) }
    }

    /** Updates the password field. */
    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    /** Updates the confirm-password field. */
    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    /** Updates the display name field. */
    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value, errorMessage = null) }
    }

    /** Toggles remember-me and persists the preference. */
    fun onRememberMeChange(enabled: Boolean) {
        _uiState.update { it.copy(rememberMe = enabled) }
        viewModelScope.launch {
            userPreferencesRepository.setRememberMe(enabled)
        }
    }

    /** Clears transient error / info banners. */
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    /** Signs in with email and password. */
    fun signIn() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            userPreferencesRepository.setRememberMe(state.rememberMe)
            when (val result = signInEmail(state.email, state.password)) {
                is Result.Success -> {
                    Timber.d("Email sign-in succeeded for %s", result.data.id)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSignedIn = true,
                            currentUser = result.data,
                        )
                    }
                    _events.emit(AuthEvent.NavigateToHome)
                }
                is Result.Error -> {
                    Timber.w("Email sign-in failed: %s", result.error.message)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    /** Completes Google sign-in using a Firebase-compatible ID token. */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            userPreferencesRepository.setRememberMe(_uiState.value.rememberMe)
            when (val result = signInGoogle(idToken)) {
                is Result.Success -> {
                    Timber.d("Google sign-in succeeded for %s", result.data.id)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSignedIn = true,
                            currentUser = result.data,
                        )
                    }
                    _events.emit(AuthEvent.NavigateToHome)
                }
                is Result.Error -> {
                    Timber.w("Google sign-in failed: %s", result.error.message)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    /** Registers a new email account. */
    fun signUp() {
        val state = _uiState.value
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            userPreferencesRepository.setRememberMe(state.rememberMe)
            when (val result = signUpEmail(state.email, state.password, state.displayName)) {
                is Result.Success -> {
                    Timber.d("Sign-up succeeded for %s", result.data.id)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSignedIn = true,
                            currentUser = result.data,
                        )
                    }
                    _events.emit(AuthEvent.NavigateToHome)
                }
                is Result.Error -> {
                    Timber.w("Sign-up failed: %s", result.error.message)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    /** Sends a password-reset email. */
    fun sendPasswordReset() {
        val email = _uiState.value.email
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            when (val result = resetPassword(email)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Password reset email sent. Check your inbox.",
                        )
                    }
                    _events.emit(AuthEvent.PasswordResetSent)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    /**
     * Observes Firebase auth state for auto-login when remember-me is enabled.
     */
    private fun observeSession() {
        viewModelScope.launch {
            val rememberMe = userPreferencesRepository.observe().first().rememberMe
            observeAuthState().collectLatest { user ->
                if (user != null && rememberMe) {
                    _uiState.update {
                        it.copy(
                            isSignedIn = true,
                            currentUser = user,
                            autoLoginChecked = true,
                        )
                    }
                    _events.emit(AuthEvent.NavigateToHome)
                } else {
                    _uiState.update {
                        it.copy(
                            isSignedIn = user != null && rememberMe,
                            currentUser = if (rememberMe) user else null,
                            autoLoginChecked = true,
                        )
                    }
                }
            }
        }
    }
}
