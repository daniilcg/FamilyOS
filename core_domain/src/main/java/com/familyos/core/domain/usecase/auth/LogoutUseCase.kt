package com.familyos.core.domain.usecase.auth

import com.familyos.core.domain.repository.AuthRepository
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Signs the current user out of Firebase Auth and clears session caches. */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.logout()
}
