package uz.hangulfriend.srs

import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Test

class FsrsTest {
    private val t0 = Instant.parse("2026-01-01T00:00:00Z")

    @Test fun matchesPyFsrsVectors() {
        val text = javaClass.getResource("/fsrs_vectors.json")!!.readText()
        val cases = Json.parseToJsonElement(text).jsonObject["cases"]!!.jsonArray
        val scheduler = FsrsScheduler()
        cases.forEachIndexed { ci, case ->
            val start = Instant.ofEpochSecond(case.jsonObject["start"]!!.jsonPrimitive.long)
            var card = scheduler.newCard(start)
            case.jsonObject["steps"]!!.jsonArray.forEachIndexed { si, s ->
                val step = s as JsonObject
                val at = "case $ci step $si"
                val now = Instant.ofEpochSecond(step["reviewed"]!!.jsonPrimitive.long)
                assertEquals(at, now, card.due)
                val rating = Rating.entries.first { it.value == step["rating"]!!.jsonPrimitive.int }
                card = scheduler.review(card, rating, now)
                assertEquals(at, step["state"]!!.jsonPrimitive.int, card.state.value)
                assertEquals(at, step["step"]!!.jsonPrimitive.intOrNull, card.step)
                assertEquals(at, step["stability"]!!.jsonPrimitive.double, card.stability!!, 1e-6)
                assertEquals(at, step["difficulty"]!!.jsonPrimitive.double, card.difficulty!!, 1e-6)
                assertEquals(at, step["due"]!!.jsonPrimitive.long, card.due.epochSecond)
                assertEquals(at, now, card.lastReview)
            }
        }
    }

    @Test fun newCard_isLearningDueNow() {
        val c = FsrsScheduler().newCard(t0)
        assertEquals(CardState.LEARNING, c.state)
        assertEquals(0, c.step)
        assertEquals(t0, c.due)
    }

    @Test fun retrievability_zeroForUnreviewed() =
        assertEquals(0.0, FsrsScheduler().retrievability(FsrsScheduler().newCard(t0), t0), 0.0)
}
