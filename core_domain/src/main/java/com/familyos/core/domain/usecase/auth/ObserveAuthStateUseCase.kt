package com.familyos.core.domain.usecase.auth

import com.familyos.core.domain.model.User
import com.familyos.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Observes authentication state changes. */
class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<User?> = authRepository.observeAuthState()
}
