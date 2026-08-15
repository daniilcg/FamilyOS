package com.familyos.core.data.repository

import com.familyos.core.data.preferences.UserPreferencesDataStore
import com.familyos.core.domain.model.ThemeMode
import com.familyos.core.domain.model.UserPreferences
import com.familyos.core.domain.repository.UserPreferencesRepository
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore-backed user preferences repository. */
@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore,
) : UserPreferencesRepository {

    override fun observe(): Flow<UserPreferences> = dataStore.preferencesFlow

    override suspend fun get(): UserPreferences = dataStore.preferencesFlow.first()

    override suspend fun setThemeMode(mode: ThemeMode): Result<Unit> =
        Result.runCatching { dataStore.setThemeMode(mode) }

    override suspend fun setRememberMe(enabled: Boolean): Result<Unit> =
        Result.runCatching { dataStore.setRememberMe(enabled) }

    override suspend fun setActiveFamilyId(familyId: String?): Result<Unit> =
        Result.runCatching { dataStore.setActiveFamilyId(familyId) }

    override suspend fun setActiveSessionUserId(userId: String?): Result<Unit> =
        Result.runCatching { dataStore.setActiveSessionUserId(userId) }

    override suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit> =
        Result.runCatching { dataStore.setBiometricEnabled(enabled) }

    override suspend fun setLanguage(tag: String): Result<Unit> =
        Result.runCatching { dataStore.setLanguage(tag) }

    override suspend fun setCurrency(code: String): Result<Unit> =
        Result.runCatching { dataStore.setCurrency(code) }

    override suspend fun setAiProvider(provider: String): Result<Unit> =
        Result.runCatching { dataStore.setAiProvider(provider) }

    override suspend fun setAiApiKeyAlias(alias: String?): Result<Unit> =
        Result.runCatching { dataStore.setAiApiKeyAlias(alias) }

    override suspend fun setNotificationsEnabled(enabled: Boolean): Result<Unit> =
        Result.runCatching { dataStore.setNotificationsEnabled(enabled) }

    override suspend fun update(preferences: UserPreferences): Result<Unit> =
        Result.runCatching { dataStore.update(preferences) }
}
