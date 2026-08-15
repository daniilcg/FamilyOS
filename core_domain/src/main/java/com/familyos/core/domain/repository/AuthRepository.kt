package com.familyos.core.domain.repository

import com.familyos.core.domain.model.User
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Authentication and current-user access.
 */
interface AuthRepository {
    /** Observes the signed-in user; emits null when signed out. */
    fun observeAuthState(): Flow<User?>

    /** Returns the cached current user or null. */
    suspend fun getCurrentUser(): User?

    /** Signs in with email/password. */
    suspend fun signInWithEmail(email: String, password: String): Result<User>

    /** Signs in with a Google ID token. */
    suspend fun signInWithGoogle(idToken: String): Result<User>

    /** Creates an account with email/password and profile name. */
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User>

    /** Sends a password reset email. */
    suspend fun resetPassword(email: String): Result<Unit>

    /** Signs the current user out. */
    suspend fun logout(): Result<Unit>

    /** Permanently deletes the authenticated account and local user data. */
    suspend fun deleteAccount(): Result<Unit>

    /** Updates mutable profile fields for the current user. */
    suspend fun updateProfile(displayName: String?, photoUrl: String?, phoneNumber: String?): Result<User>
}
