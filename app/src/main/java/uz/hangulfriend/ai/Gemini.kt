package uz.hangulfriend.ai

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class HttpResult(val code: Int, val body: String)

/** One HTTP call to Gemini. May throw [IOException] when there is no network. */
interface GeminiTransport {
    suspend fun post(model: String, key: String, body: String): HttpResult
}

class HttpGeminiTransport(private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta") : GeminiTransport {
    override suspend fun post(model: String, key: String, body: String): HttpResult = withContext(Dispatchers.IO) {
        val conn = URL("$baseUrl/models/$model:generateContent").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("x-goog-api-key", key)
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            HttpResult(code, stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty())
        } finally {
            conn.disconnect()
        }
    }
}

data class ChatMessage(val fromUser: Boolean, val text: String)

sealed interface AiResult {
    data class Success(val text: String) : AiResult

    /** Every key hit its limit (or was rejected). */
    data object QuotaExhausted : AiResult

    data object Offline : AiResult

    /** No key in the build (.env missing). */
    data object NotConfigured : AiResult

    data class Failed(val message: String) : AiResult
}

@Serializable
internal data class Part(val text: String)

@Serializable
internal data class Content(val role: String? = null, val parts: List<Part> = emptyList())

@Serializable
internal data class GenerationConfig(val responseMimeType: String? = null)

@Serializable
internal data class GenerateRequest(
    val systemInstruction: Content,
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
)

@Serializable
internal data class Candidate(val content: Content? = null, val finishReason: String? = null)

@Serializable
internal data class GenerateResponse(val candidates: List<Candidate> = emptyList())

internal val GeminiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * Calls Gemini with the keys in order: a key that hits its limit (429) or is rejected (401/403)
 * hands over to the next one; a busy server (5xx) tries the next model with the same key.
 * The key that worked last is where the next call starts.
 */
class GeminiClient(
    private val keys: List<String>,
    private val transport: GeminiTransport,
    private val models: List<String> = DEFAULT_MODELS,
    private val retryDelayMs: Long = 1_500,
) {
    @Volatile private var current = 0

    suspend fun generate(system: String, messages: List<ChatMessage>, json: Boolean = false): AiResult {
        if (keys.isEmpty()) return AiResult.NotConfigured
        val body = GeminiJson.encodeToString(
            GenerateRequest.serializer(),
            GenerateRequest(
                systemInstruction = Content(parts = listOf(Part(system))),
                contents = messages.map { Content(role = if (it.fromUser) "user" else "model", parts = listOf(Part(it.text))) },
                generationConfig = if (json) GenerationConfig(responseMimeType = "application/json") else null,
            ),
        )
        // A busy model (5xx) is usually free again a moment later, so the whole list gets one more pass.
        for (pass in 0 until BUSY_PASSES) {
            var busy = false
            for (offset in keys.indices) {
                val index = (current + offset) % keys.size
                var nextKey = false
                for (model in models) {
                    val result = try {
                        transport.post(model, keys[index], body)
                    } catch (_: IOException) {
                        return AiResult.Offline
                    } catch (_: SecurityException) {
                        // No network access for the app: report it like being offline instead of crashing.
                        return AiResult.Offline
                    }
                    when (result.code) {
                        200 -> {
                            current = index
                            return parse(result.body)
                        }
                        429, 401, 403 -> {
                            nextKey = true
                            break
                        }
                        in 500..599 -> continue
                        else -> return AiResult.Failed("HTTP ${result.code}")
                    }
                }
                if (!nextKey) {
                    busy = true
                    break
                }
            }
            if (!busy) return AiResult.QuotaExhausted
            if (pass + 1 < BUSY_PASSES) delay(retryDelayMs)
        }
        return AiResult.Failed("Gemini hozir band")
    }

    private fun parse(body: String): AiResult {
        val response = try {
            GeminiJson.decodeFromString(GenerateResponse.serializer(), body)
        } catch (_: SerializationException) {
            return AiResult.Failed("Noto'g'ri javob")
        }
        val text = response.candidates.firstOrNull()?.content?.parts?.joinToString("") { it.text }.orEmpty()
        return if (text.isBlank()) AiResult.Failed("Bo'sh javob") else AiResult.Success(text)
    }

    companion object {
        val DEFAULT_MODELS = listOf("gemini-3.8-flash", "gemini-3.5-flash", "gemini-3.5-flash-lite")
        const val BUSY_PASSES = 2
    }
}
