package com.familyos.feature.ai.keys

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.familyos.feature.ai.BuildConfig
import com.familyos.feature.ai.provider.AiProviderId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiKeysDataStore by preferencesDataStore("familyos_ai_keys")

/**
 * Resolves AI API keys from DataStore overrides, falling back to BuildConfig /
 * local.properties values (`AI_OPENAI_KEY`, `AI_GEMINI_KEY`, `AI_OPENROUTER_KEY`).
 */
@Singleton
class AiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val openAiKey = stringPreferencesKey("openai_key")
    private val geminiKey = stringPreferencesKey("gemini_key")
    private val openRouterKey = stringPreferencesKey("openrouter_key")

    fun observeKey(providerId: AiProviderId): Flow<String> =
        context.aiKeysDataStore.data.map { prefs ->
            when (providerId) {
                AiProviderId.OPENAI -> prefs[openAiKey]?.takeIf { it.isNotBlank() } ?: BuildConfig.AI_OPENAI_KEY
                AiProviderId.GEMINI -> prefs[geminiKey]?.takeIf { it.isNotBlank() } ?: BuildConfig.AI_GEMINI_KEY
                AiProviderId.OPENROUTER -> prefs[openRouterKey]?.takeIf { it.isNotBlank() }
                    ?: BuildConfig.AI_OPENROUTER_KEY
            }
        }

    suspend fun getKey(providerId: AiProviderId): String = observeKey(providerId).first()

    suspend fun setKey(providerId: AiProviderId, value: String) {
        context.aiKeysDataStore.edit { prefs ->
            when (providerId) {
                AiProviderId.OPENAI -> prefs[openAiKey] = value
                AiProviderId.GEMINI -> prefs[geminiKey] = value
                AiProviderId.OPENROUTER -> prefs[openRouterKey] = value
            }
        }
    }
}
