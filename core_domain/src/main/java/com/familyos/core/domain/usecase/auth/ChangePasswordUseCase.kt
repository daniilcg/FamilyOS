package com.familyos.core.domain.usecase.auth

import com.familyos.core.domain.repository.AuthRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Sets a new password for local offline auth accounts. */
class ChangePasswordUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, newPassword: String): Result<Unit> {
        val normalized = email.trim()
        if (normalized.isEmpty() || !normalized.contains("@")) {
            return Result.failure(AppError.Validation("Укажите корректный email", "email"))
        }
        if (newPassword.length < 6) {
            return Result.failure(AppError.Validation("Пароль должен быть не короче 6 символов", "password"))
        }
        return authRepository.changePassword(normalized, newPassword)
    }
}
