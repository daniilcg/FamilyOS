package com.familyos.feature.ai.provider

import com.familyos.core.domain.util.AppError
import com.familyos.core.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenAI Chat Completions provider (gpt-4o-mini by default).
 */
@Singleton
class OpenAiProvider @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) : AiProvider {

    override val id: AiProviderId = AiProviderId.OPENAI

    override suspend fun complete(apiKey: String, request: AiCompletionRequest): Result<AiCompletionResponse> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(AppError.Validation("OpenAI API key is missing"))
            }
            val body = buildJsonObject {
                put("model", request.model ?: DEFAULT_MODEL)
                put("temperature", request.temperature)
                if (request.jsonMode) {
                    put("response_format", buildJsonObject { put("type", "json_object") })
                }
                put(
                    "messages",
                    buildJsonArray {
                        request.messages.forEach { msg ->
                            add(
                                buildJsonObject {
                                    put("role", msg.role)
                                    put("content", msg.content)
                                },
                            )
                        }
                    },
                )
            }
            val httpRequest = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()
            runCatching {
                client.newCall(httpRequest).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(httpFailure(response.code, raw))
                    }
                    val root = json.parseToJsonElement(raw).jsonObject
                    val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                    val content = choice?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
                        ?: return@withContext Result.failure(AppError.Remote("Empty OpenAI response"))
                    val usage = root["usage"]?.jsonObject
                    Result.success(
                        AiCompletionResponse(
                            content = content,
                            model = root["model"]?.jsonPrimitive?.contentOrNull ?: DEFAULT_MODEL,
                            providerId = id.id,
                            promptTokens = usage?.get("prompt_tokens")?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                            completionTokens = usage?.get("completion_tokens")?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                        ),
                    )
                }
            }.getOrElse {
                Result.failure(AppError.Network(it.message ?: "OpenAI network error", it))
            }
        }

    companion object {
        private const val DEFAULT_MODEL = "gpt-4o-mini"
        private val JSON_MEDIA = "application/json".toMediaType()
    }
}

/**
 * Google Gemini generateContent provider.
 */
@Singleton
class GeminiProvider @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) : AiProvider {

    override val id: AiProviderId = AiProviderId.GEMINI

    override suspend fun complete(apiKey: String, request: AiCompletionRequest): Result<AiCompletionResponse> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(AppError.Validation("Gemini API key is missing"))
            }
            val model = request.model ?: DEFAULT_MODEL
            val system = request.messages.filter { it.role == "system" }.joinToString("\n") { it.content }
            val contents = buildJsonArray {
                request.messages.filter { it.role != "system" }.forEach { msg ->
                    add(
                        buildJsonObject {
                            put("role", if (msg.role == "assistant") "model" else "user")
                            put(
                                "parts",
                                buildJsonArray {
                                    add(buildJsonObject { put("text", msg.content) })
                                },
                            )
                        },
                    )
                }
            }
            val body = buildJsonObject {
                if (system.isNotBlank()) {
                    put("systemInstruction", buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", system) })
                        })
                    })
                }
                put("contents", contents)
                put(
                    "generationConfig",
                    buildJsonObject {
                        put("temperature", request.temperature)
                        if (request.jsonMode) put("responseMimeType", "application/json")
                    },
                )
            }
            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val httpRequest = Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()
            runCatching {
                client.newCall(httpRequest).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(httpFailure(response.code, raw))
                    }
                    val root = json.parseToJsonElement(raw).jsonObject
                    val text = root["candidates"]?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.contentOrNull
                        ?: return@withContext Result.failure(AppError.Remote("Empty Gemini response"))
                    Result.success(
                        AiCompletionResponse(
                            content = text,
                            model = model,
                            providerId = id.id,
                        ),
                    )
                }
            }.getOrElse {
                Result.failure(AppError.Network(it.message ?: "Gemini network error", it))
            }
        }

    companion object {
        private const val DEFAULT_MODEL = "gemini-2.0-flash"
        private val JSON_MEDIA = "application/json".toMediaType()
    }
}

/**
 * OpenRouter chat completions provider (OpenAI-compatible).
 */
@Singleton
class OpenRouterProvider @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) : AiProvider {

    override val id: AiProviderId = AiProviderId.OPENROUTER

    override suspend fun complete(apiKey: String, request: AiCompletionRequest): Result<AiCompletionResponse> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(AppError.Validation("OpenRouter API key is missing"))
            }
            val body = buildJsonObject {
                put("model", request.model ?: DEFAULT_MODEL)
                put("temperature", request.temperature)
                if (request.jsonMode) {
                    put("response_format", buildJsonObject { put("type", "json_object") })
                }
                put(
                    "messages",
                    buildJsonArray {
                        request.messages.forEach { msg ->
                            add(
                                buildJsonObject {
                                    put("role", msg.role)
                                    put("content", msg.content)
                                },
                            )
                        }
                    },
                )
            }
            val httpRequest = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "https://familyos.app")
                .header("X-Title", "FamilyOS")
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()
            runCatching {
                client.newCall(httpRequest).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(httpFailure(response.code, raw))
                    }
                    val root = json.parseToJsonElement(raw).jsonObject
                    val content = root["choices"]?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("message")?.jsonObject
                        ?.get("content")?.jsonPrimitive?.contentOrNull
                        ?: return@withContext Result.failure(AppError.Remote("Empty OpenRouter response"))
                    Result.success(
                        AiCompletionResponse(
                            content = content,
                            model = root["model"]?.jsonPrimitive?.contentOrNull ?: DEFAULT_MODEL,
                            providerId = id.id,
                        ),
                    )
                }
            }.getOrElse {
                Result.failure(AppError.Network(it.message ?: "OpenRouter network error", it))
            }
        }

    companion object {
        private const val DEFAULT_MODEL = "openai/gpt-4o-mini"
        private val JSON_MEDIA = "application/json".toMediaType()
    }
}

/**
 * Default [AiProviderFactory] wiring all three providers.
 */
@Singleton
class DefaultAiProviderFactory @Inject constructor(
    private val openAi: OpenAiProvider,
    private val gemini: GeminiProvider,
    private val openRouter: OpenRouterProvider,
) : AiProviderFactory {
    override fun get(providerId: AiProviderId): AiProvider = when (providerId) {
        AiProviderId.OPENAI -> openAi
        AiProviderId.GEMINI -> gemini
        AiProviderId.OPENROUTER -> openRouter
    }
}
