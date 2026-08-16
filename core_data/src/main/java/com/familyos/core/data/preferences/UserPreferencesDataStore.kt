package com.familyos.core.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.familyos.core.domain.model.ThemeMode
import com.familyos.core.domain.model.UserPreferences
import com.familyos.core.domain.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.PREFS_NAME,
)

/**
 * DataStore-backed user preferences. Encrypted prefs are used only for API keys
 * and are never read on the cold-start theme/auth path.
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.userPrefsDataStore

    @Volatile
    private var securePrefs: SharedPreferences? = null

    /** Observes preference values. Does not touch EncryptedSharedPreferences. */
    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[KEY_THEME]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
            } ?: ThemeMode.SYSTEM,
            rememberMe = prefs[KEY_REMEMBER_ME] ?: true,
            activeFamilyId = prefs[KEY_FAMILY_ID],
            activeSessionUserId = prefs[KEY_SESSION_USER_ID],
            biometricEnabled = prefs[KEY_BIOMETRIC] ?: false,
            languageTag = prefs[KEY_LANGUAGE] ?: Constants.DEFAULT_LANGUAGE,
            currencyCode = prefs[KEY_CURRENCY] ?: Constants.DEFAULT_CURRENCY,
            aiProvider = prefs[KEY_AI_PROVIDER] ?: "openai",
            aiApiKeyAlias = prefs[KEY_AI_ALIAS],
            notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
        )
    }

    suspend fun get(): UserPreferences = preferencesFlow.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setRememberMe(enabled: Boolean) {
        dataStore.edit { it[KEY_REMEMBER_ME] = enabled }
    }

    suspend fun setActiveFamilyId(familyId: String?) {
        dataStore.edit { prefs ->
            if (familyId == null) prefs.remove(KEY_FAMILY_ID) else prefs[KEY_FAMILY_ID] = familyId
        }
    }

    suspend fun setActiveSessionUserId(userId: String?) {
        dataStore.edit { prefs ->
            if (userId == null) prefs.remove(KEY_SESSION_USER_ID)
            else prefs[KEY_SESSION_USER_ID] = userId
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BIOMETRIC] = enabled }
    }

    suspend fun setLanguage(tag: String) {
        dataStore.edit { it[KEY_LANGUAGE] = tag }
    }

    suspend fun setCurrency(code: String) {
        dataStore.edit { it[KEY_CURRENCY] = code }
    }

    suspend fun setAiProvider(provider: String) {
        dataStore.edit { it[KEY_AI_PROVIDER] = provider }
    }

    suspend fun setAiApiKeyAlias(alias: String?) {
        dataStore.edit { prefs ->
            if (alias.isNullOrBlank()) prefs.remove(KEY_AI_ALIAS) else prefs[KEY_AI_ALIAS] = alias
        }
    }

    fun storeEncryptedApiKey(alias: String, apiKey: String) {
        runCatching {
            encryptedPrefs()?.edit()?.putString("$SECURE_AI_KEY_PREFIX$alias", apiKey)?.apply()
        }.onFailure { Timber.w(it, "Failed to store encrypted API key") }
    }

    fun readEncryptedApiKey(alias: String): String? =
        runCatching { encryptedPrefs()?.getString("$SECURE_AI_KEY_PREFIX$alias", null) }
            .onFailure { Timber.w(it, "Failed to read encrypted API key") }
            .getOrNull()

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    suspend fun update(preferences: UserPreferences) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME] = preferences.themeMode.name
            prefs[KEY_REMEMBER_ME] = preferences.rememberMe
            if (preferences.activeFamilyId == null) prefs.remove(KEY_FAMILY_ID)
            else prefs[KEY_FAMILY_ID] = preferences.activeFamilyId!!
            if (preferences.activeSessionUserId == null) prefs.remove(KEY_SESSION_USER_ID)
            else prefs[KEY_SESSION_USER_ID] = preferences.activeSessionUserId!!
            prefs[KEY_BIOMETRIC] = preferences.biometricEnabled
            prefs[KEY_LANGUAGE] = preferences.languageTag
            prefs[KEY_CURRENCY] = preferences.currencyCode
            prefs[KEY_AI_PROVIDER] = preferences.aiProvider
            prefs[KEY_NOTIFICATIONS] = preferences.notificationsEnabled
        }
        setAiApiKeyAlias(preferences.aiApiKeyAlias)
    }

    private fun encryptedPrefs(): SharedPreferences? {
        securePrefs?.let { return it }
        synchronized(this) {
            securePrefs?.let { return it }
            val created = runCatching { createEncryptedPrefs() }
                .recoverCatching {
                    Timber.w(it, "Encrypted prefs corrupt — recreating")
                    context.deleteSharedPreferences(Constants.ENCRYPTED_PREFS_NAME)
                    createEncryptedPrefs()
                }
                .onFailure { Timber.e(it, "Encrypted prefs unavailable") }
                .getOrNull()
            securePrefs = created
            return created
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            Constants.ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
        private val KEY_FAMILY_ID = stringPreferencesKey("active_family_id")
        private val KEY_SESSION_USER_ID = stringPreferencesKey("active_session_user_id")
        private val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        private val KEY_LANGUAGE = stringPreferencesKey("language_tag")
        private val KEY_CURRENCY = stringPreferencesKey("currency_code")
        private val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        private val KEY_AI_ALIAS = stringPreferencesKey("ai_api_key_alias")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        private const val SECURE_AI_KEY_PREFIX = "ai_api_key_"
    }
}
