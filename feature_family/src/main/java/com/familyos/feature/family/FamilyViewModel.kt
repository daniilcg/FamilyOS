package com.familyos.feature.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyos.core.domain.model.Family
import com.familyos.core.domain.model.FamilyMember
import com.familyos.core.domain.model.FamilyRole
import com.familyos.core.domain.model.User
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.usecase.auth.GetCurrentUserUseCase
import com.familyos.core.domain.usecase.auth.ObserveAuthStateUseCase
import com.familyos.core.domain.usecase.family.CreateFamilyUseCase
import com.familyos.core.domain.usecase.family.GenerateInviteUseCase
import com.familyos.core.domain.usecase.family.JoinFamilyByCodeUseCase
import com.familyos.core.domain.usecase.family.LeaveFamilyUseCase
import com.familyos.core.domain.usecase.family.ObserveFamilyMembersUseCase
import com.familyos.core.domain.usecase.family.ObserveFamilyUseCase
import com.familyos.core.domain.usecase.family.RemoveMemberUseCase
import com.familyos.core.domain.usecase.family.UpdateMemberRoleUseCase
import com.familyos.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * UI state for family management screens.
 */
data class FamilyUiState(
    val currentUser: User? = null,
    val family: Family? = null,
    val members: List<FamilyMember> = emptyList(),
    val familyNameInput: String = "",
    val inviteCodeInput: String = "",
    val inviteCode: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val currentUserRole: FamilyRole? = null,
    val canManageRoles: Boolean = false,
)

/**
 * One-shot family events.
 */
sealed interface FamilyEvent {
    data object FamilyReady : FamilyEvent
    data object LeftFamily : FamilyEvent
}

/**
 * ViewModel for create/join family, members, invites, and role management.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val createFamilyUseCase: CreateFamilyUseCase,
    private val joinFamilyByCodeUseCase: JoinFamilyByCodeUseCase,
    private val generateInviteUseCase: GenerateInviteUseCase,
    private val observeFamily: ObserveFamilyUseCase,
    private val observeMembers: ObserveFamilyMembersUseCase,
    private val updateMemberRoleUseCase: UpdateMemberRoleUseCase,
    private val removeMemberUseCase: RemoveMemberUseCase,
    private val leaveFamilyUseCase: LeaveFamilyUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val observeAuthState: ObserveAuthStateUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FamilyEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<FamilyEvent> = _events.asSharedFlow()

    private val activeFamilyId = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            observeAuthState().collectLatest { user ->
                _uiState.update { it.copy(currentUser = user) }
                val familyId = user?.familyId
                    ?: userPreferencesRepository.get().activeFamilyId
                activeFamilyId.value = familyId
            }
        }
        viewModelScope.launch {
            activeFamilyId.flatMapLatest { familyId ->
                if (familyId.isNullOrBlank()) {
                    flowOf(Pair<Family?, List<FamilyMember>>(null, emptyList()))
                } else {
                    combine(
                        observeFamily(familyId),
                        observeMembers(familyId),
                    ) { family, members -> family to members }
                }
            }.collectLatest { (family, members) ->
                val userId = _uiState.value.currentUser?.id
                val role = members.firstOrNull { it.userId == userId }?.role
                _uiState.update {
                    it.copy(
                        family = family,
                        members = members,
                        inviteCode = family?.inviteCode ?: it.inviteCode,
                        currentUserRole = role,
                        canManageRoles = role == FamilyRole.OWNER || role == FamilyRole.ADMIN,
                    )
                }
            }
        }
    }

    /** Updates the create-family name field. */
    fun onFamilyNameChange(value: String) {
        _uiState.update { it.copy(familyNameInput = value, errorMessage = null) }
    }

    /** Updates the join-code field. */
    fun onInviteCodeInputChange(value: String) {
        _uiState.update {
            it.copy(inviteCodeInput = value.uppercase().trim(), errorMessage = null)
        }
    }

    /** Clears transient messages. */
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    /** Creates a family owned by the current user. */
    fun createFamily() {
        viewModelScope.launch {
            val user = getCurrentUser()
            if (user == null) {
                _uiState.update { it.copy(errorMessage = "Sign in required") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = createFamilyUseCase(_uiState.value.familyNameInput, user.id)) {
                is Result.Success -> {
                    userPreferencesRepository.setActiveFamilyId(result.data.id)
                    activeFamilyId.value = result.data.id
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            family = result.data,
                            inviteCode = result.data.inviteCode,
                            infoMessage = "Family created",
                        )
                    }
                    _events.emit(FamilyEvent.FamilyReady)
                }
                is Result.Error -> {
                    Timber.w("Create family failed: %s", result.error.message)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    /** Joins a family using the typed or scanned invite code. */
    fun joinFamily(code: String? = null) {
        viewModelScope.launch {
            val user = getCurrentUser()
            if (user == null) {
                _uiState.update { it.copy(errorMessage = "Sign in required") }
                return@launch
            }
            val invite = (code ?: _uiState.value.inviteCodeInput).trim()
            _uiState.update { it.copy(isLoading = true, errorMessage = null, inviteCodeInput = invite) }
            when (
                val result = joinFamilyByCodeUseCase(
                    inviteCode = invite,
                    userId = user.id,
                    displayName = user.displayName,
                    photoUrl = user.photoUrl,
                    email = user.email,
                )
            ) {
                is Result.Success -> {
                    userPreferencesRepository.setActiveFamilyId(result.data.id)
                    activeFamilyId.value = result.data.id
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            family = result.data,
                            inviteCode = result.data.inviteCode,
                            infoMessage = "Joined ${result.data.name}",
                        )
                    }
                    _events.emit(FamilyEvent.FamilyReady)
                }
                is Result.Error -> {
                    Timber.w("Join family failed: %s", result.error.message)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    /** Called when a QR payload containing an invite code is scanned. */
    fun onInviteQrScanned(payload: String) {
        val code = payload.trim()
            .removePrefix("familyos://join/")
            .removePrefix("FAMILYOS:")
            .trim()
        onInviteCodeInputChange(code)
        joinFamily(code)
    }

    /** Generates / rotates the family invite code. */
    fun refreshInviteCode() {
        val familyId = _uiState.value.family?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = generateInviteUseCase(familyId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, inviteCode = result.data)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    /** Updates a member role (Owner/Admin only). */
    fun changeMemberRole(memberId: String, role: FamilyRole) {
        val familyId = _uiState.value.family?.id ?: return
        if (!_uiState.value.canManageRoles) {
            _uiState.update { it.copy(errorMessage = "Only owners and admins can change roles") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = updateMemberRoleUseCase(familyId, memberId, role)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = "Updated ${result.data.displayName} to ${role.name}",
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    /** Removes a member from the family. */
    fun removeFamilyMember(memberId: String) {
        val familyId = _uiState.value.family?.id ?: return
        if (!_uiState.value.canManageRoles) {
            _uiState.update { it.copy(errorMessage = "Only owners and admins can remove members") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = removeMemberUseCase(familyId, memberId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, infoMessage = "Member removed")
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    /** Leaves the current family as the signed-in user. */
    fun leaveCurrentFamily() {
        val familyId = _uiState.value.family?.id ?: return
        val userId = _uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = leaveFamilyUseCase(familyId, userId)) {
                is Result.Success -> {
                    userPreferencesRepository.setActiveFamilyId(null)
                    activeFamilyId.value = null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            family = null,
                            members = emptyList(),
                            inviteCode = null,
                        )
                    }
                    _events.emit(FamilyEvent.LeftFamily)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.message)
                    }
                }
            }
        }
    }

    /** Deep-link helper: bind observation to an explicit family id. */
    fun bindFamily(familyId: String) {
        activeFamilyId.value = familyId
        viewModelScope.launch {
            userPreferencesRepository.setActiveFamilyId(familyId)
        }
    }
}
