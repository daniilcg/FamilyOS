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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.PREFS_NAME,
)

/**
 * DataStore-backed user preferences with encrypted storage for AI key aliases.
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.userPrefsDataStore

    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            Constants.ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Observes all preference values as a [UserPreferences] model. */
    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[KEY_THEME]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
            } ?: ThemeMode.SYSTEM,
            rememberMe = prefs[KEY_REMEMBER_ME] ?: true,
            activeFamilyId = prefs[KEY_FAMILY_ID],
            biometricEnabled = prefs[KEY_BIOMETRIC] ?: false,
            languageTag = prefs[KEY_LANGUAGE] ?: Constants.DEFAULT_LANGUAGE,
            currencyCode = prefs[KEY_CURRENCY] ?: Constants.DEFAULT_CURRENCY,
            aiProvider = prefs[KEY_AI_PROVIDER] ?: "openai",
            aiApiKeyAlias = securePrefs.getString(SECURE_AI_ALIAS, null),
            notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
        )
    }

    /** Returns a snapshot of current preferences. */
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

    /**
     * Stores an encrypted reference/alias for the AI API key (not the raw key in cleartext DataStore).
     */
    suspend fun setAiApiKeyAlias(alias: String?) {
        securePrefs.edit().apply {
            if (alias.isNullOrBlank()) remove(SECURE_AI_ALIAS) else putString(SECURE_AI_ALIAS, alias)
        }.apply()
    }

    /** Stores the raw AI API key in encrypted shared preferences under [alias]. */
    fun storeEncryptedApiKey(alias: String, apiKey: String) {
        securePrefs.edit().putString("$SECURE_AI_KEY_PREFIX$alias", apiKey).apply()
    }

    /** Reads the raw AI API key for [alias] from encrypted storage. */
    fun readEncryptedApiKey(alias: String): String? =
        securePrefs.getString("$SECURE_AI_KEY_PREFIX$alias", null)

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    suspend fun update(preferences: UserPreferences) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME] = preferences.themeMode.name
            prefs[KEY_REMEMBER_ME] = preferences.rememberMe
            if (preferences.activeFamilyId == null) prefs.remove(KEY_FAMILY_ID)
            else prefs[KEY_FAMILY_ID] = preferences.activeFamilyId!!
            prefs[KEY_BIOMETRIC] = preferences.biometricEnabled
            prefs[KEY_LANGUAGE] = preferences.languageTag
            prefs[KEY_CURRENCY] = preferences.currencyCode
            prefs[KEY_AI_PROVIDER] = preferences.aiProvider
            prefs[KEY_NOTIFICATIONS] = preferences.notificationsEnabled
        }
        setAiApiKeyAlias(preferences.aiApiKeyAlias)
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
        private val KEY_FAMILY_ID = stringPreferencesKey("active_family_id")
        private val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        private val KEY_LANGUAGE = stringPreferencesKey("language_tag")
        private val KEY_CURRENCY = stringPreferencesKey("currency_code")
        private val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        private const val SECURE_AI_ALIAS = "ai_api_key_alias"
        private const val SECURE_AI_KEY_PREFIX = "ai_api_key_"
    }
}
