package com.familyos.core.domain.usecase.auth

import com.familyos.core.domain.model.User
import com.familyos.core.domain.repository.AuthRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Constants
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Registers a new email/password account. */
class SignUpEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String, displayName: String): Result<User> {
        val normalizedEmail = email.trim()
        val name = displayName.trim()
        if (normalizedEmail.isEmpty() || !normalizedEmail.contains("@")) {
            return Result.failure(AppError.Validation("Valid email is required", "email"))
        }
        if (password.length < 6) {
            return Result.failure(AppError.Validation("Password must be at least 6 characters", "password"))
        }
        if (name.isEmpty() || name.length > Constants.MAX_DISPLAY_NAME_LENGTH) {
            return Result.failure(AppError.Validation("Display name is required", "displayName"))
        }
        return authRepository.signUpWithEmail(normalizedEmail, password, name)
    }
}
