package com.familyos.core.domain.repository

import com.familyos.core.domain.model.ThemeMode
import com.familyos.core.domain.model.UserPreferences
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Local user preference storage.
 */
interface UserPreferencesRepository {
    fun observe(): Flow<UserPreferences>
    suspend fun get(): UserPreferences
    suspend fun setThemeMode(mode: ThemeMode): Result<Unit>
    suspend fun setRememberMe(enabled: Boolean): Result<Unit>
    suspend fun setActiveFamilyId(familyId: String?): Result<Unit>
    suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit>
    suspend fun setLanguage(tag: String): Result<Unit>
    suspend fun setCurrency(code: String): Result<Unit>
    suspend fun setAiProvider(provider: String): Result<Unit>
    suspend fun setAiApiKeyAlias(alias: String?): Result<Unit>
    suspend fun setNotificationsEnabled(enabled: Boolean): Result<Unit>
    suspend fun update(preferences: UserPreferences): Result<Unit>
}
