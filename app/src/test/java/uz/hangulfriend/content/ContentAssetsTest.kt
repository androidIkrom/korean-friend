package uz.hangulfriend.content

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Validates the real assets shipped in the app. Run via `python tools/validate_content.py`. */
class ContentAssetsTest {
    private val repo = ContentRepository(DirAssetSource(File("src/main/assets")), strict = true)
    private val catalog = repo.catalog()
    private val lessons = catalog.filter { repo.isAvailable(it.id) }.map { repo.lesson(it.id)!! }

    @Test fun catalogHas18Lessons() = assertEquals(18, catalog.size)

    @Test fun unit2IsWritten() =
        assertTrue(listOf("u02_l1", "u02_l2").all { id -> lessons.any { it.id == id } })

    @Test fun validatorPasses() = assertEquals(emptyList<String>(), LessonValidator.validate(lessons))

    @Test fun dialogueSpeakersExist() {
        val ids = repo.characters().map { it.id }.toSet()
        lessons.flatMap { l -> l.dialogue.lines.map { l.id to it.speaker } }
            .forEach { (lesson, speaker) -> assertTrue("$lesson: unknown speaker $speaker", speaker in ids) }
    }

    @Test fun lessonsMeetSpecSizes() {
        for (l in lessons) {
            val practice = l.exercises.filter { it.id !in l.test }
            assertTrue("${l.id}: 25–35 words", l.words.size in 25..35)
            assertTrue("${l.id}: 8–14 dialogue lines", l.dialogue.lines.size in 8..14)
            assertTrue("${l.id}: ≥20 practice exercises", practice.size >= 20)
            assertEquals("${l.id}: 15 test exercises", 15, l.test.size)
            for (g in l.grammar) {
                assertTrue("${g.id}: ≥4 practice exercises", practice.count { g.id in it.targets } >= 4)
                assertTrue("${g.id}: 4–6 examples", g.examples.size in 4..6)
                assertTrue("${g.id}: 2–4 mistakes", g.mistakes.size in 2..4)
            }
            for (type in ExerciseType.entries) {
                assertTrue("${l.id}: ≥2 $type", practice.count { it.type == type } >= 2)
            }
        }
    }

    @Test fun lessonIdsMatchCatalog() {
        for (l in lessons) {
            val entry = catalog.first { it.id == l.id }
            assertEquals(entry.unit, l.unit)
            assertEquals(entry.lesson, l.lesson)
            assertEquals(entry.titleKo, l.titleKo)
        }
    }
}
