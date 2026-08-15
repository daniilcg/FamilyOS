package com.familyos.core.domain.usecase.auth

import com.familyos.core.domain.model.User
import com.familyos.core.domain.repository.AuthRepository
import javax.inject.Inject

/** Returns the currently signed-in user, or null. */
class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): User? = authRepository.getCurrentUser()
}
