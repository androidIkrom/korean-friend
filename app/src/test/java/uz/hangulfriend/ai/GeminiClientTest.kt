package uz.hangulfriend.ai

import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiClientTest {
    private val keys = listOf("k1", "k2", "k3")
    private val models = listOf("m1", "m2")

    /** Records (model, key) calls and answers from [respond]. */
    private class FakeTransport(val respond: (model: String, key: String) -> HttpResult) : GeminiTransport {
        val calls = mutableListOf<Pair<String, String>>()
        val bodies = mutableListOf<String>()

        override suspend fun post(model: String, key: String, body: String): HttpResult {
            calls += model to key
            bodies += body
            return respond(model, key)
        }
    }

    private fun ok(text: String) = HttpResult(200, """{"candidates":[{"content":{"parts":[{"text":"$text"}]}}]}""")

    private val user = listOf(ChatMessage(fromUser = true, text = "Salom"))

    @Test fun generate_successReturnsText() = runTest {
        val t = FakeTransport { _, _ -> ok("Javob") }
        assertEquals(AiResult.Success("Javob"), GeminiClient(keys, t, models).generate("sys", user))
    }

    @Test fun generate_quotaRotatesToNextKey() = runTest {
        val t = FakeTransport { _, k -> if (k == "k1") HttpResult(429, "{}") else ok("ok") }
        assertEquals(AiResult.Success("ok"), GeminiClient(keys, t, models).generate("sys", user))
        assertEquals(listOf("m1" to "k1", "m1" to "k2"), t.calls)
    }

    @Test fun generate_unavailableTriesNextModelSameKey() = runTest {
        val t = FakeTransport { m, _ -> if (m == "m1") HttpResult(503, "{}") else ok("ok") }
        assertEquals(AiResult.Success("ok"), GeminiClient(keys, t, models).generate("sys", user))
        assertEquals(listOf("m1" to "k1", "m2" to "k1"), t.calls)
    }

    /** Every model busy on the first pass: wait briefly and try the whole list once more. */
    @Test fun generate_allBusyRetriesOnce() = runTest {
        var n = 0
        val t = FakeTransport { _, _ -> if (n++ < 2) HttpResult(503, "{}") else ok("ok") }
        assertEquals(AiResult.Success("ok"), GeminiClient(keys, t, models, retryDelayMs = 0).generate("sys", user))
        assertEquals(3, t.calls.size)
    }

    @Test fun defaultModelsHaveLiteFallback() = assertEquals("gemini-3.5-flash-lite", GeminiClient.DEFAULT_MODELS.last())

    @Test fun generate_invalidKeyRotates() = runTest {
        val t = FakeTransport { _, k -> if (k == "k1") HttpResult(403, "{}") else ok("ok") }
        assertEquals(AiResult.Success("ok"), GeminiClient(keys, t, models).generate("sys", user))
    }

    @Test fun generate_allKeysExhausted() = runTest {
        val t = FakeTransport { _, _ -> HttpResult(429, "{}") }
        assertEquals(AiResult.QuotaExhausted, GeminiClient(keys, t, models).generate("sys", user))
        assertEquals(3, t.calls.size)
    }

    @Test fun generate_ioErrorIsOffline() = runTest {
        val t = FakeTransport { _, _ -> throw IOException("no network") }
        assertEquals(AiResult.Offline, GeminiClient(keys, t, models).generate("sys", user))
    }

    /** Without network access (e.g. a missing INTERNET permission) the socket layer throws SecurityException. */
    @Test fun generate_securityErrorIsOffline() = runTest {
        val t = FakeTransport { _, _ -> throw SecurityException("Permission denied (missing INTERNET permission?)") }
        assertEquals(AiResult.Offline, GeminiClient(keys, t, models).generate("sys", user))
    }

    @Test fun generate_noKeys() = runTest {
        val t = FakeTransport { _, _ -> ok("x") }
        assertEquals(AiResult.NotConfigured, GeminiClient(emptyList(), t, models).generate("sys", user))
        assertTrue(t.calls.isEmpty())
    }

    @Test fun generate_stickyKey() = runTest {
        val t = FakeTransport { _, k -> if (k == "k1") HttpResult(429, "{}") else ok("ok") }
        val client = GeminiClient(keys, t, models)
        client.generate("sys", user)
        t.calls.clear()
        client.generate("sys", user)
        assertEquals(listOf("m1" to "k2"), t.calls)
    }

    @Test fun generate_emptyCandidatesFails() = runTest {
        val t = FakeTransport { _, _ -> HttpResult(200, """{"candidates":[{"finishReason":"SAFETY"}]}""") }
        assertTrue(GeminiClient(keys, t, models).generate("sys", user) is AiResult.Failed)
    }

    @Test fun generate_otherClientErrorFails() = runTest {
        val t = FakeTransport { _, _ -> HttpResult(400, """{"error":{"message":"bad"}}""") }
        assertTrue(GeminiClient(keys, t, models).generate("sys", user) is AiResult.Failed)
        assertEquals(1, t.calls.size)
    }

    @Test fun generate_requestBodyShape() = runTest {
        val t = FakeTransport { _, _ -> ok("{}") }
        val history = listOf(ChatMessage(true, "Savol"), ChatMessage(false, "Javob"), ChatMessage(true, "Yana"))
        GeminiClient(keys, t, models).generate("tizim", history, json = true)
        val body = Json.parseToJsonElement(t.bodies.single()).jsonObject
        assertEquals("tizim", body["systemInstruction"]!!.jsonObject["parts"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content)
        assertEquals(listOf("user", "model", "user"), body["contents"]!!.jsonArray.map { it.jsonObject["role"]!!.jsonPrimitive.content })
        assertEquals("application/json", body["generationConfig"]!!.jsonObject["responseMimeType"]!!.jsonPrimitive.content)
    }
}
