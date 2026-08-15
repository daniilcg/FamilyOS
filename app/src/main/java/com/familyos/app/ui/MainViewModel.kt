package com.familyos.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.ThemeMode
import com.familyos.core.domain.model.User
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * App-level ViewModel for auth session and theme observation used by [com.familyos.app.MainActivity].
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    /** Currently signed-in user, or null. */
    val currentUser: StateFlow<User?> = observeAuthState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Theme preference driving [com.familyos.core.ui.theme.FamilyOsTheme]. */
    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.observe()
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    /** Whether biometric lock is enabled. */
    val biometricEnabled: StateFlow<Boolean> = userPreferencesRepository.observe()
        .map { it.biometricEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
