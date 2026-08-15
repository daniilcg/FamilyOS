package com.familyos.core.data.auth

import android.util.Base64
import com.familyos.core.data.local.dao.AuthCredentialDao
import com.familyos.core.data.local.dao.UserDao
import com.familyos.core.data.local.entity.AuthCredentialEntity
import com.familyos.core.data.mapper.EntityMappers.toDomain
import com.familyos.core.data.mapper.EntityMappers.toEntity
import com.familyos.core.data.preferences.UserPreferencesDataStore
import com.familyos.core.domain.model.User
import com.familyos.core.domain.repository.FamilyRepository
import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.AppException
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline Room-backed authentication engine (email/password + local Google JWT decode).
 */
@Singleton
class LocalAuthEngine @Inject constructor(
    private val authCredentialDao: AuthCredentialDao,
    private val userDao: UserDao,
    private val sessionStore: AuthSessionStore,
    private val preferences: UserPreferencesDataStore,
    private val familyRepository: FamilyRepository,
) {

    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAuthState(): Flow<User?> =
        sessionStore.observeUserId().flatMapLatest { userId ->
            if (userId == null) flowOf(null)
            else userDao.observeById(userId).map { it?.toDomain() }
        }

    suspend fun getCurrentUser(): User? {
        val userId = sessionStore.getUserId() ?: return null
        return userDao.getById(userId)?.toDomain()
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> =
        Result.runCatching {
            val normalized = email.trim().lowercase()
            validateEmail(normalized)
            validatePassword(password)
            if (authCredentialDao.getByEmail(normalized) != null) {
                throw AppException(AppError.Validation("Аккаунт с таким email уже существует", "email"))
            }
            val now = System.currentTimeMillis()
            val userId = UUID.randomUUID().toString()
            val salt = PasswordHasher.newSalt()
            val hash = PasswordHasher.hash(password, salt)
            val user = User(
                id = userId,
                email = normalized,
                displayName = displayName.trim(),
                photoUrl = null,
                phoneNumber = null,
                familyId = null,
                preferredLanguage = "ru",
                createdAt = now,
                updatedAt = now,
                isEmailVerified = true,
            )
            userDao.upsert(user.toEntity())
            authCredentialDao.upsert(
                AuthCredentialEntity(
                    userId = userId,
                    email = normalized,
                    passwordSalt = salt,
                    passwordHash = hash,
                    provider = PROVIDER_EMAIL,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            val familyName = "${user.displayName}'s Family"
            when (val familyResult = familyRepository.createFamily(familyName, userId)) {
                is Result.Success -> {
                    preferences.setActiveFamilyId(familyResult.data.id)
                }
                is Result.Error -> {
                    Timber.w("Family auto-create failed: %s", familyResult.error.message)
                }
            }
            val withFamily = userDao.getById(userId)?.toDomain() ?: user
            persistSession(userId)
            withFamily
        }

    suspend fun signInWithEmail(email: String, password: String): Result<User> =
        Result.runCatching {
            val normalized = email.trim().lowercase()
            validateEmail(normalized)
            val credential = authCredentialDao.getByEmail(normalized)
                ?: throw AppException(AppError.Unauthorized("Неверный email или пароль"))
            if (!PasswordHasher.verify(password, credential.passwordSalt, credential.passwordHash)) {
                throw AppException(AppError.Unauthorized("Неверный email или пароль"))
            }
            val user = userDao.getById(credential.userId)?.toDomain()
                ?: throw AppException(AppError.NotFound("User", credential.userId))
            persistSession(user.id)
            user
        }

    suspend fun signInWithGoogle(idToken: String): Result<User> =
        Result.runCatching {
            if (idToken.isBlank()) {
                throw AppException(
                    AppError.Validation(
                        "Google Sign-In недоступен в локальном режиме без ID token",
                    ),
                )
            }
            val claims = decodeJwtPayload(idToken)
            val sub = claims["sub"]?.takeIf { it.isNotBlank() }
                ?: throw AppException(AppError.Validation("Не удалось разобрать Google token (нет sub)"))
            val email = claims["email"]?.trim()?.lowercase().orEmpty()
            val name = claims["name"]?.trim().orEmpty()
                .ifBlank { email.substringBefore("@").ifBlank { "Google User" } }
            if (email.isEmpty() || !email.contains("@")) {
                throw AppException(AppError.Validation("Не удалось разобрать Google token (нет email)"))
            }
            val now = System.currentTimeMillis()
            val existingByEmail = authCredentialDao.getByEmail(email)
            val userId = existingByEmail?.userId ?: "google_$sub"
            val cached = userDao.getById(userId)?.toDomain()
            val user = User(
                id = userId,
                email = email,
                displayName = name.ifBlank { cached?.displayName.orEmpty() },
                photoUrl = claims["picture"] ?: cached?.photoUrl,
                phoneNumber = cached?.phoneNumber,
                familyId = cached?.familyId,
                preferredLanguage = cached?.preferredLanguage ?: "ru",
                createdAt = cached?.createdAt?.takeIf { it > 0 } ?: now,
                updatedAt = now,
                isEmailVerified = true,
            )
            userDao.upsert(user.toEntity())
            if (existingByEmail == null) {
                val salt = PasswordHasher.newSalt()
                val randomPassword = UUID.randomUUID().toString()
                authCredentialDao.upsert(
                    AuthCredentialEntity(
                        userId = userId,
                        email = email,
                        passwordSalt = salt,
                        passwordHash = PasswordHasher.hash(randomPassword, salt),
                        provider = PROVIDER_GOOGLE,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                if (user.familyId == null) {
                    when (val familyResult = familyRepository.createFamily("${user.displayName}'s Family", userId)) {
                        is Result.Success -> preferences.setActiveFamilyId(familyResult.data.id)
                        is Result.Error -> Unit
                    }
                }
            } else if (existingByEmail.provider != PROVIDER_GOOGLE) {
                authCredentialDao.upsert(
                    existingByEmail.copy(provider = PROVIDER_GOOGLE, updatedAt = now),
                )
            }
            val finalUser = userDao.getById(userId)?.toDomain() ?: user
            persistSession(finalUser.id)
            finalUser
        }

    suspend fun resetPassword(email: String): Result<Unit> =
        Result.runCatching {
            val normalized = email.trim().lowercase()
            validateEmail(normalized)
            val exists = authCredentialDao.getByEmail(normalized) != null
            if (!exists) {
                throw AppException(AppError.NotFound("User", normalized))
            }
            // UI should collect a new password and call changePassword.
        }

    suspend fun changePassword(email: String, newPassword: String): Result<Unit> =
        Result.runCatching {
            val normalized = email.trim().lowercase()
            validateEmail(normalized)
            validatePassword(newPassword)
            val credential = authCredentialDao.getByEmail(normalized)
                ?: throw AppException(AppError.NotFound("User", normalized))
            val salt = PasswordHasher.newSalt()
            val hash = PasswordHasher.hash(newPassword, salt)
            authCredentialDao.upsert(
                credential.copy(
                    passwordSalt = salt,
                    passwordHash = hash,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }

    suspend fun logout(): Result<Unit> =
        Result.runCatching { sessionStore.clear() }

    suspend fun deleteAccount(): Result<Unit> =
        Result.runCatching {
            val userId = sessionStore.getUserId()
                ?: throw AppException(AppError.Unauthorized())
            authCredentialDao.delete(userId)
            userDao.delete(userId)
            preferences.setActiveFamilyId(null)
            sessionStore.clear()
        }

    suspend fun updateProfile(displayName: String?, photoUrl: String?, phoneNumber: String?): Result<User> =
        Result.runCatching {
            val userId = sessionStore.getUserId()
                ?: throw AppException(AppError.Unauthorized())
            val existing = userDao.getById(userId)?.toDomain()
                ?: throw AppException(AppError.NotFound("User", userId))
            val updated = existing.copy(
                displayName = displayName ?: existing.displayName,
                photoUrl = photoUrl ?: existing.photoUrl,
                phoneNumber = phoneNumber ?: existing.phoneNumber,
                updatedAt = System.currentTimeMillis(),
            )
            userDao.upsert(updated.toEntity())
            updated
        }

    private suspend fun persistSession(userId: String) {
        val rememberMe = preferences.get().rememberMe
        sessionStore.setUserId(userId, persist = rememberMe)
    }

    private fun validateEmail(email: String) {
        if (email.isEmpty() || !email.contains("@") || !email.contains(".")) {
            throw AppException(AppError.Validation("Укажите корректный email", "email"))
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 6) {
            throw AppException(AppError.Validation("Пароль должен быть не короче 6 символов", "password"))
        }
    }

    /**
     * Decodes JWT payload without signature verification (local mode only).
     */
    private fun decodeJwtPayload(token: String): Map<String, String?> {
        val parts = token.split('.')
        if (parts.size < 2) {
            throw AppException(AppError.Validation("Неверный формат Google ID token"))
        }
        return try {
            var payload = parts[1]
            val padding = (4 - payload.length % 4) % 4
            if (padding > 0) payload += "=".repeat(padding)
            val decoded = Base64.decode(payload, Base64.URL_SAFE)
            val text = String(decoded, Charsets.UTF_8)
            val obj = json.parseToJsonElement(text).jsonObject
            mapOf(
                "sub" to obj["sub"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() },
                "email" to obj["email"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() },
                "name" to obj["name"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() },
                "picture" to obj["picture"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() },
            )
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw AppException(
                AppError.Validation("Не удалось разобрать Google token: ${e.message ?: "ошибка"}"),
            )
        }
    }

    companion object {
        const val PROVIDER_EMAIL = "EMAIL"
        const val PROVIDER_GOOGLE = "GOOGLE"
        const val PROVIDER_LOCAL = "LOCAL"
    }
}
