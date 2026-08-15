package com.familyos.core.data.repository

import com.familyos.core.data.auth.FirebaseAvailability
import com.familyos.core.data.auth.LocalAuthEngine
import com.familyos.core.domain.model.User
import com.familyos.core.domain.repository.AuthRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes authentication to Firebase when a real project is configured,
 * otherwise to the offline [LocalAuthEngine].
 */
@Singleton
class HybridAuthRepository @Inject constructor(
    private val firebaseAvailability: FirebaseAvailability,
    private val firebaseAuth: AuthRepositoryImpl,
    private val localAuth: LocalAuthEngine,
) : AuthRepository {

    private val useCloud: Boolean
        get() = firebaseAvailability.isCloudAuthAvailable()

    override fun observeAuthState(): Flow<User?> =
        if (useCloud) firebaseAuth.observeAuthState() else localAuth.observeAuthState()

    override suspend fun getCurrentUser(): User? =
        if (useCloud) firebaseAuth.getCurrentUser() else localAuth.getCurrentUser()

    override suspend fun signInWithEmail(email: String, password: String): Result<User> =
        if (useCloud) firebaseAuth.signInWithEmail(email, password)
        else localAuth.signInWithEmail(email, password)

    override suspend fun signInWithGoogle(idToken: String): Result<User> =
        if (useCloud) firebaseAuth.signInWithGoogle(idToken)
        else localAuth.signInWithGoogle(idToken)

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> =
        if (useCloud) firebaseAuth.signUpWithEmail(email, password, displayName)
        else localAuth.signUpWithEmail(email, password, displayName)

    override suspend fun resetPassword(email: String): Result<Unit> =
        if (useCloud) firebaseAuth.resetPassword(email)
        else localAuth.resetPassword(email)

    override suspend fun changePassword(email: String, newPassword: String): Result<Unit> =
        if (useCloud) {
            Result.failure(
                AppError.Validation(
                    "Смена пароля в облачном режиме выполняется через письмо сброса",
                ),
            )
        } else {
            localAuth.changePassword(email, newPassword)
        }

    override suspend fun logout(): Result<Unit> =
        if (useCloud) firebaseAuth.logout() else localAuth.logout()

    override suspend fun deleteAccount(): Result<Unit> =
        if (useCloud) firebaseAuth.deleteAccount() else localAuth.deleteAccount()

    override suspend fun updateProfile(
        displayName: String?,
        photoUrl: String?,
        phoneNumber: String?,
    ): Result<User> =
        if (useCloud) firebaseAuth.updateProfile(displayName, photoUrl, phoneNumber)
        else localAuth.updateProfile(displayName, photoUrl, phoneNumber)
}
