package com.familyos.core.domain.usecase.auth

import com.familyos.core.domain.model.User
import com.familyos.core.domain.repository.AuthRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import javax.inject.Inject

/** Signs in a user with a Google ID token from Credential Manager / Play Services. */
class SignInGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(idToken: String): Result<User> {
        if (idToken.isBlank()) {
            return Result.failure(AppError.Validation("Google ID token is required", "idToken"))
        }
        return authRepository.signInWithGoogle(idToken)
    }
}
