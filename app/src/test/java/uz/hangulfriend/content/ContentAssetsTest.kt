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

    @Test fun everyLessonHasStory() {
        val missing = lessons.filter { it.story == null }.map { it.id }
        assertEquals("lessons without a story episode", emptyList<String>(), missing)
    }

    @Test fun validatorPasses() = assertEquals(emptyList<String>(), LessonValidator.validate(lessons))

    @Test fun dialogueSpeakersExist() {
        val ids = repo.characters().map { it.id }.toSet()
        lessons.flatMap { l -> (l.dialogue.lines.map { it.speaker } + storySpeakers(l)).map { l.id to it } }
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
            assertTrue("${l.id}: ≥3 listen_question", practice.count { it.type == ExerciseType.LISTEN_QUESTION } >= 3)
            for (type in ExerciseType.entries) {
                assertTrue("${l.id}: ≥2 $type", practice.count { it.type == type } >= 2)
            }
        }
    }

    /** Valid Korean that a learner may write; each must be accepted (found in the final review). */
    @Test fun acceptsValidAlternatives() {
        val cases = mapOf(
            "u02_l2_e014" to "저 치마보다 이 치마가 더 길어요",
            "u02_l2_e109" to "버스보다 지하철이 더 빨라요",
            "u02_l2_e019" to "저 바지보다 이 바지가 더 길어요",
            "u02_l2_e114" to "저 가방보다 이 가방이 더 비싸요",
            "u02_l1_e015" to "한번 이 바지를 입어 보세요",
            "u02_l1_e011" to "가 보셨어요",
            "u02_l1_e021" to "김치를 먹어 보셨어요",
            "u02_l1_e108" to "들어 보셨어요",
        )
        val byId = lessons.flatMap { it.exercises }.associateBy { it.id }
        for ((id, input) in cases) {
            val result = uz.hangulfriend.hangul.AnswerChecker.check(input, byId.getValue(id).answers)
            assertTrue("$id should accept \"$input\" (got ${result.feedback})", result.correct)
        }
    }

    private fun audioNames(l: Lesson): List<String?> =
        l.words.flatMap { listOf(it.audio, it.exampleAudio) } +
            l.grammar.flatMap { g -> g.examples.map { it.audio } } +
            l.dialogue.lines.map { it.audio } +
            l.exercises.filter { it.type == ExerciseType.LISTEN_QUESTION }.map { it.audio } +
            l.story?.steps.orEmpty().mapNotNull {
                when (it) {
                    is StoryLine -> listOf(it.audio)
                    is ChooseReply -> listOf(it.audio)
                    is StoryQuiz -> null
                }
            }.flatten()

    private fun storySpeakers(l: Lesson): List<String> = l.story?.steps.orEmpty().mapNotNull {
        when (it) {
            is StoryLine -> it.speaker
            is ChooseReply -> it.speaker
            is StoryQuiz -> null
        }
    }

    @Test fun everyAudioFileExists() {
        val dir = File("src/main/assets/audio")
        for (l in lessons) for (name in audioNames(l).filterNotNull()) {
            assertTrue("${l.id}: missing audio/$name", File(dir, name).isFile)
        }
    }

    @Test fun everySpeakableHasAudio() {
        for (l in lessons) assertTrue("${l.id}: text without audio", audioNames(l).none { it == null })
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
