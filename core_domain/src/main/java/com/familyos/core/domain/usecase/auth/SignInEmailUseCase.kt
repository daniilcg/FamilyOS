package com.familyos.core.domain.usecase.auth

import com.familyos.core.domain.model.User
import com.familyos.core.domain.repository.AuthRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Signs in a user with email and password. */
class SignInEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        val normalized = email.trim()
        if (normalized.isEmpty() || !normalized.contains("@")) {
            return Result.failure(AppError.Validation("Valid email is required", "email"))
        }
        if (password.length < 6) {
            return Result.failure(AppError.Validation("Password must be at least 6 characters", "password"))
        }
        return authRepository.signInWithEmail(normalized, password)
    }
}
