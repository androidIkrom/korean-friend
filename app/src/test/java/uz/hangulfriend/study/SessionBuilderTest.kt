package uz.hangulfriend.study

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.Fixtures
import uz.hangulfriend.data.CardEntity
import uz.hangulfriend.data.CardKind
import uz.hangulfriend.data.CardOrigin
import uz.hangulfriend.srs.CardState

class SessionBuilderTest {
    private val lesson = Fixtures.validLesson()
    private val builder = SessionBuilder(Random(42))

    /** "a" → flashcards, "b" → typing, "c" → match: three distinct type keys. */
    private fun items(vararg typeCounts: Pair<String, Int>): List<ExerciseItem> =
        typeCounts.flatMap { (type, n) ->
            (1..n).map { i ->
                val w = Fixtures.word("${type}_$i", "말", "so'z")
                when (type) {
                    "a" -> ExerciseItem.Flashcard(w)
                    "b" -> ExerciseItem.WordTyping(w)
                    else -> ExerciseItem.Match(listOf(w))
                }
            }
        }

    private fun card(itemId: String, kind: CardKind, lessonId: String = "u02_l1") = CardEntity(
        id = "$itemId#${kind.suffix}", itemId = itemId, kind = kind, lessonId = lessonId, origin = CardOrigin.LESSON,
        state = CardState.LEARNING, step = 0, stability = null, difficulty = null, dueMs = 0, lastReviewMs = null,
        firstReviewedMs = null, reps = 0, lapses = 0, lessonOrder = 0,
    )

    @Test fun arrange_noAdjacentSameTypeWhenPossible() {
        repeat(20) { seed ->
            val arranged = arrange(items("a" to 4, "b" to 4, "c" to 4), Random(seed))
            arranged.zipWithNext().forEach { (x, y) -> assertTrue("seed $seed", x.typeKey != y.typeKey) }
        }
    }

    @Test fun arrange_skewedCountsStillAlternate() {
        val arranged = arrange(items("a" to 5, "b" to 4), Random(1))
        arranged.zipWithNext().forEach { (x, y) -> assertTrue(x.typeKey != y.typeKey) }
    }

    @Test fun arrange_singleTypeTerminates() = assertEquals(5, arrange(items("a" to 5), Random(1)).size)

    @Test fun arrange_keepsAllItems() {
        val input = items("a" to 3, "b" to 7, "c" to 1)
        assertEquals(input.toSet(), arrange(input, Random(3)).toSet())
    }

    @Test fun lessonPractice_excludesTestExercises() {
        val ids = builder.lessonPractice(lesson).filterIsInstance<ExerciseItem.Authored>().map { it.exercise.id }
        assertTrue("u02_l1_e007" !in ids)
        assertEquals(6, ids.size)
    }

    @Test fun lessonPractice_includesTypingAndMatch() {
        val practice = builder.lessonPractice(lesson)
        assertEquals(2, practice.count { it is ExerciseItem.WordTyping })
        assertEquals(1, practice.count { it is ExerciseItem.Match })
    }

    @Test fun lessonTest_onlyTestExercises() =
        assertEquals(listOf("u02_l1_e007"), builder.lessonTest(lesson).map { (it as ExerciseItem.Authored).exercise.id })

    @Test fun authored_cardIdsFollowTargets() {
        val byId = builder.lessonPractice(lesson).filterIsInstance<ExerciseItem.Authored>().associateBy { it.exercise.id }
        assertEquals(listOf("u02_l1_w001#R"), byId.getValue("u02_l1_e001").cardIds)
        assertEquals(listOf("u02_l1_g1#G"), byId.getValue("u02_l1_e002").cardIds)
    }

    @Test fun vocabChunks_ofSix() {
        val big = lesson.copy(words = (1..14).map { Fixtures.word("u02_l1_w%03d".format(it), "말", "so'z") })
        assertEquals(listOf(6, 6, 2), builder.vocabChunks(big).map { it.size })
    }

    @Test fun review_mapsCardKinds() {
        val items = builder.review(
            listOf(card("u02_l1_w001", CardKind.RECOGNIZE), card("u02_l1_w002", CardKind.PRODUCE)),
            mapOf("u02_l1" to lesson),
        )
        assertEquals(setOf("flashcard", "reverse_typing"), items.map { it.typeKey }.toSet())
    }

    @Test fun review_grammarCardPicksTargetingPracticeExercise() {
        repeat(10) { seed ->
            val item = SessionBuilder(Random(seed)).review(listOf(card("u02_l1_g1", CardKind.GRAMMAR)), mapOf("u02_l1" to lesson))
                .single() as ExerciseItem.Authored
            assertTrue("u02_l1_g1" in item.exercise.targets)
            assertTrue(item.exercise.id !in lesson.test)
            assertEquals(listOf("u02_l1_g1#G"), item.cardIds)
        }
    }

    @Test fun review_skipsCardsOfUnavailableLessons() =
        assertEquals(0, builder.review(listOf(card("u05_l1_w001", CardKind.RECOGNIZE, "u05_l1")), mapOf("u02_l1" to lesson)).size)

    @Test fun lessonReview_flashcardsAndOnePerGrammar() {
        val items = builder.lessonReview(lesson)
        assertEquals(2, items.count { it is ExerciseItem.Flashcard })
        assertEquals(1, items.count { it is ExerciseItem.Authored })
    }
}
