package com.familyos.core.domain.usecase.auth

import com.familyos.core.domain.repository.AuthRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Sends a Firebase password-reset email. */
class ResetPasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        val normalized = email.trim()
        if (normalized.isEmpty() || !normalized.contains("@")) {
            return Result.failure(AppError.Validation("Valid email is required", "email"))
        }
        return authRepository.resetPassword(normalized)
    }
}
