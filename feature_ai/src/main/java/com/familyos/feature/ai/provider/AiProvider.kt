package com.familyos.feature.ai.provider

import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result

/**
 * Supported Family AI backend providers.
 */
enum class AiProviderId(val id: String, val displayName: String) {
    OPENAI("openai", "OpenAI"),
    GEMINI("gemini", "Google Gemini"),
    OPENROUTER("openrouter", "OpenRouter");

    companion object {
        fun fromId(value: String): AiProviderId =
            entries.firstOrNull { it.id.equals(value, ignoreCase = true) } ?: OPENAI
    }
}

/**
 * A single chat turn passed to an [AiProvider].
 */
data class AiChatMessage(
    val role: String,
    val content: String,
)

/**
 * Request payload for an AI completion.
 */
data class AiCompletionRequest(
    val messages: List<AiChatMessage>,
    val temperature: Double = 0.3,
    val jsonMode: Boolean = true,
    val model: String? = null,
)

/**
 * Raw completion response from a provider.
 */
data class AiCompletionResponse(
    val content: String,
    val model: String,
    val providerId: String,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
)

/**
 * Contract for Family AI HTTP providers (OpenAI / Gemini / OpenRouter).
 */
interface AiProvider {
    val id: AiProviderId

    /**
     * Executes a chat completion using the provider HTTP API.
     *
     * @param apiKey Provider API key from DataStore / BuildConfig
     */
    suspend fun complete(apiKey: String, request: AiCompletionRequest): Result<AiCompletionResponse>
}

/**
 * Resolves the active [AiProvider] implementation.
 */
interface AiProviderFactory {
    fun get(providerId: AiProviderId): AiProvider
    fun get(providerId: String): AiProvider = get(AiProviderId.fromId(providerId))
}

/**
 * Maps common HTTP failures into [AppError].
 */
internal fun httpFailure(code: Int, body: String): AppError = when (code) {
    401, 403 -> AppError.Unauthorized("AI provider rejected the API key ($code)")
    in 500..599 -> AppError.Remote("AI provider unavailable ($code)", code.toString())
    else -> AppError.Remote(body.ifBlank { "AI request failed ($code)" }, code.toString())
}
