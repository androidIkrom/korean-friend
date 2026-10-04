package uz.hangulfriend.study

import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.content.DirAssetSource
import uz.hangulfriend.data.CardIds
import uz.hangulfriend.data.CardKind
import uz.hangulfriend.hangul.Hangul

class HangulPracticeTest {
    private val course = ContentRepository(DirAssetSource(File("src/main/assets")), strict = true).hangul()!!

    private fun practice(lessonId: String, seed: Int = 3) =
        hangulPractice(course.lettersOf(lessonId), course.knownBy(lessonId), Random(seed))

    @Test fun everyLetterIsPractised() {
        val items = practice("b1_u01_l2")
        val basics = items.filter { it is ExerciseItem.LetterSound || it is ExerciseItem.LetterListen }
        val expected = course.lettersOf("b1_u01_l2").map { CardIds.of(it.cardItemId, CardKind.RECOGNIZE) }
        assertEquals(expected.sorted(), basics.map { it.cardIds.single() }.sorted())
    }

    @Test fun consonantLessonHasEveryKind() {
        val kinds = (1..5).flatMap { seed -> practice("b1_u01_l2", seed).map { it.typeKey } }.toSet()
        assertEquals(setOf("letter_sound", "letter_listen", "read_word", "build_syllable"), kinds)
    }

    @Test fun optionsHoldTheAnswerOnce() {
        for (id in course.lessons.map { it.id }) for (item in practice(id)) when (item) {
            is ExerciseItem.LetterSound -> assertOptions(item.options.map { it.roman }, item.letter.roman)
            is ExerciseItem.LetterListen -> assertOptions(item.options.map { it.jamo }, item.letter.jamo)
            is ExerciseItem.ReadWord -> assertOptions(item.options.map { it.example.roman }, item.letter.example.roman)
            is ExerciseItem.BuildSyllable -> {
                val p = Hangul.parts(item.target)!!
                assertTrue(p.initial in item.initials && p.medial in item.medials)
                assertTrue(if (p.final == null) item.finals.isEmpty() else p.final in item.finals)
            }
            else -> error("unexpected $item")
        }
    }

    private fun assertOptions(options: List<String>, answer: String) {
        assertEquals(options.size, options.toSet().size)
        assertTrue(answer in options)
        assertTrue(options.size in 2..HangulCaps.OPTIONS)
    }

    @Test fun vowelLessonCannotBuildSyllables() {
        val items = (1..5).flatMap { practice("b1_u01_l1", it) }
        assertTrue(items.none { it is ExerciseItem.BuildSyllable })
        assertTrue(items.any { it is ExerciseItem.ReadWord })
    }

    @Test fun finalsLessonBuildsWithFinals() {
        val builds = (1..5).flatMap { practice("b1_u01_l5", it) }.filterIsInstance<ExerciseItem.BuildSyllable>()
        assertTrue(builds.isNotEmpty() && builds.all { it.finals.isNotEmpty() && Hangul.parts(it.target)!!.final != null })
    }

    @Test fun readableNeedsKnownLetters() {
        val vowels = course.lettersOf("b1_u01_l1")
        assertTrue(readable("아이", vowels))
        assertFalse(readable("가구", vowels))
        assertTrue(readable("가구", course.knownBy("b1_u01_l2")))
        assertFalse(readable("책", course.knownBy("b1_u01_l4")))
        assertTrue(readable("책", course.knownBy("b1_u01_l5")))
    }
}
