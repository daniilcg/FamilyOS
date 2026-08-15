package com.familyos.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.User
import com.familyos.core.domain.repository.AuthRepository
import com.familyos.core.domain.usecase.auth.DeleteAccountUseCase
import com.familyos.core.domain.usecase.auth.ObserveAuthStateUseCase
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
 * Profile screen UI state.
 */
data class ProfileUiState(
    val user: User? = null,
    val displayName: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val showDeleteConfirm: Boolean = false,
)

/**
 * Profile one-shot events.
 */
sealed interface ProfileEvent {
    data object AccountDeleted : ProfileEvent
}

/**
 * ViewModel for viewing and updating the signed-in profile, including account deletion.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val observeAuthState: ObserveAuthStateUseCase,
    private val authRepository: AuthRepository,
    private val deleteAccount: DeleteAccountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeAuthState().collectLatest { user ->
                _uiState.update {
                    it.copy(
                        user = user,
                        displayName = user?.displayName.orEmpty(),
                        isLoading = false,
                    )
                }
            }
        }
    }

    /** Updates the editable display name field. */
    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value, errorMessage = null) }
    }

    /** Shows or hides the delete-account confirmation dialog. */
    fun setDeleteConfirmVisible(visible: Boolean) {
        _uiState.update { it.copy(showDeleteConfirm = visible) }
    }

    /** Clears banners. */
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    /** Persists display name (and keeps existing photo / phone). */
    fun saveProfile() {
        val name = _uiState.value.displayName.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Display name is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (
                val result = authRepository.updateProfile(
                    displayName = name,
                    photoUrl = _uiState.value.user?.photoUrl,
                    phoneNumber = _uiState.value.user?.phoneNumber,
                )
            ) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            user = result.data,
                            displayName = result.data.displayName,
                            infoMessage = "Profile updated",
                        )
                    }
                }
                is Result.Error -> {
                    Timber.w("Profile update failed: %s", result.error.message)
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    /** Permanently deletes the authenticated account. */
    fun confirmDeleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDeleteConfirm = false) }
            when (val result = deleteAccount()) {
                is Result.Success -> {
                    _events.emit(ProfileEvent.AccountDeleted)
                }
                is Result.Error -> {
                    Timber.w("Delete account failed: %s", result.error.message)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }
}
