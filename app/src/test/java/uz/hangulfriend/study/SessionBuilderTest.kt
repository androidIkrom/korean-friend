package uz.hangulfriend.study

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.Fixtures
import uz.hangulfriend.content.ExerciseType
import uz.hangulfriend.content.FinalTest
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
        val ids = builder.lessonPractice(lesson, speechAvailable = false).filterIsInstance<ExerciseItem.Authored>().map { it.exercise.id }
        assertTrue("u02_l1_e007" !in ids)
        assertEquals(7, ids.size)
    }

    @Test fun lessonPractice_includesTypingAndMatch() {
        val practice = builder.lessonPractice(lesson, speechAvailable = false)
        assertEquals(2, practice.count { it is ExerciseItem.WordTyping })
        assertEquals(1, practice.count { it is ExerciseItem.Match })
    }

    /** Spec 3.3: a practice round is 15–25 mixed exercises and still covers every grammar point. */
    @Test fun lessonPractice_bigLessonIsCappedAndCoversGrammar() {
        val big = lesson.copy(
            words = (1..33).map { Fixtures.word("u02_l1_w%03d".format(it), "말$it", "so'z$it") },
            grammar = listOf(lesson.grammar[0], lesson.grammar[0].copy(id = "u02_l1_g2")),
            exercises = (1..22).map { i ->
                val g = if (i <= 11) "u02_l1_g1" else "u02_l1_g2"
                Fixtures.exercise("u02_l1_e%03d".format(i), uz.hangulfriend.content.ExerciseType.CONJUGATE, listOf(g), listOf("가 보다")) {
                    copy(base = "가다", form = "-아/어 보다")
                }
            },
            test = emptyList(),
        )
        repeat(10) { seed ->
            val practice = SessionBuilder(Random(seed)).lessonPractice(withAudio(big), speechAvailable = true)
            assertTrue("size ${practice.size}", practice.size in 15..25)
            val targets = practice.filterIsInstance<ExerciseItem.Authored>().flatMap { it.exercise.targets }.toSet()
            assertTrue(targets.containsAll(listOf("u02_l1_g1", "u02_l1_g2")))
        }
    }

    /** Every word and grammar example gets an audio file name. */
    private fun withAudio(l: uz.hangulfriend.content.Lesson) = l.copy(
        words = l.words.map { it.copy(audio = "${it.id}.ogg", exampleAudio = "${it.id}_ex.ogg") },
        grammar = l.grammar.map { g -> g.copy(examples = g.examples.map { it.copy(audio = "${g.id}.ogg") }) },
    )

    private fun sixWords() = lesson.copy(words = (1..6).map { Fixtures.word("u02_l1_w%03d".format(it), "말$it", "so'z$it") })

    @Test fun lessonPractice_noAudioNoListeningItems() {
        val keys = builder.lessonPractice(sixWords(), speechAvailable = true).map { it.typeKey }
        assertTrue(keys.none { it == "listen_choose" || it == "dictation" || it == "speak" })
    }

    @Test fun lessonPractice_withAudioAddsListening() {
        val keys = builder.lessonPractice(withAudio(sixWords()), speechAvailable = false).map { it.typeKey }
        assertTrue("listen_choose" in keys)
        assertTrue("dictation" in keys)
    }

    @Test fun lessonPractice_noSpeechNoSpeakItems() =
        assertTrue(builder.lessonPractice(withAudio(sixWords()), speechAvailable = false).none { it.typeKey == "speak" })

    @Test fun lessonPractice_speakOnlyWithAudio() {
        assertTrue(builder.lessonPractice(withAudio(sixWords()), speechAvailable = true).any { it.typeKey == "speak" })
        assertTrue(builder.lessonPractice(sixWords(), speechAvailable = true).none { it.typeKey == "speak" })
    }

    @Test fun listenChoose_hasFourDistinctOptionsIncludingAnswer() {
        repeat(10) { seed ->
            val items = SessionBuilder(Random(seed)).lessonPractice(withAudio(sixWords()), speechAvailable = false)
            items.filterIsInstance<ExerciseItem.ListenChoose>().forEach { item ->
                assertEquals(4, item.options.map { it.id }.toSet().size)
                assertTrue(item.word in item.options)
                assertEquals(listOf("${item.word.id}#R"), item.cardIds)
            }
        }
    }

    @Test fun review_productionCardMayBeDictationOnlyWithAudio() {
        val card = card("u02_l1_w001", CardKind.PRODUCE)
        val noAudio = (0 until 20).map { SessionBuilder(Random(it)).review(listOf(card), mapOf("u02_l1" to lesson)).single().typeKey }
        assertTrue(noAudio.all { it == "reverse_typing" })
        val audio = (0 until 20).map { SessionBuilder(Random(it)).review(listOf(card), mapOf("u02_l1" to withAudio(lesson))).single().typeKey }
        assertTrue("dictation" in audio)
    }

    private fun withTests(id: String, lesson: Int, n: Int) = Fixtures.validLesson(id, 2, lesson).let { base ->
        val tests = (1..n).map { i ->
            Fixtures.exercise("${id}_t%03d".format(i), uz.hangulfriend.content.ExerciseType.CONJUGATE, listOf("${id}_g1"), listOf("가 보다")) {
                copy(base = "가다", form = "-아/어 보다")
            }
        }
        base.copy(exercises = base.exercises + tests, test = tests.map { it.id })
    }

    @Test fun boss_fifteenFromBothLessons() {
        val items = builder.boss(listOf(withTests("u02_l1", 1, 10), withTests("u02_l2", 2, 10)))
        assertEquals(15, items.size)
        val ids = items.map { (it as ExerciseItem.Authored).exercise.id }
        assertTrue(ids.any { it.startsWith("u02_l1") } && ids.any { it.startsWith("u02_l2") })
        assertTrue(ids.all { "_t" in it })
    }

    @Test fun boss_missingLessonGivesEmpty() =
        assertTrue(builder.boss(listOf(withTests("u02_l1", 1, 10))).isEmpty())

    @Test fun quickCheck_tenItems() {
        assertEquals(10, builder.quickCheck(withTests("u02_l1", 1, 15)).size)
        // Few test items: filled up from practice exercises.
        assertEquals(9, builder.quickCheck(withTests("u02_l1", 1, 1)).size)
    }

    @Test fun lessonTest_onlyTestExercises() =
        assertEquals(listOf("u02_l1_e007"), builder.lessonTest(lesson).map { (it as ExerciseItem.Authored).exercise.id })

    @Test fun authored_cardIdsFollowTargets() {
        val byId = builder.lessonPractice(lesson, speechAvailable = false).filterIsInstance<ExerciseItem.Authored>().associateBy { it.exercise.id }
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

    @Test fun finalKeepsOrder() {
        val ex = { id: String -> Fixtures.exercise(id, ExerciseType.LISTEN_QUESTION, emptyList(), listOf("가")) }
        val items = builder.finalTest(FinalTest(listOf(ex("final_l01"), ex("final_l02")), listOf(ex("final_r01"))))
        assertEquals(listOf("final_l01", "final_l02", "final_r01"), items.map { (it as ExerciseItem.Authored).exercise.id })
        assertTrue(items.all { it.cardIds.isEmpty() })
    }

    @Test fun reviewBuildsUserWordExercises() {
        val own = lesson.copy(id = "user", words = listOf(Fixtures.word("user_w1", "사과", "olma")), grammar = emptyList())
        val items = builder.review(
            listOf(card("user_w1", CardKind.RECOGNIZE, "user"), card("user_w1", CardKind.PRODUCE, "user")),
            mapOf("user" to own),
        )
        assertEquals(2, items.size)
        assertTrue(items.any { it is ExerciseItem.Flashcard && it.word.id == "user_w1" })
        assertTrue(items.any { it is ExerciseItem.WordTyping && it.word.id == "user_w1" })
    }

    private fun quizWords(n: Int, prefix: String = "q", audio: Boolean = false) =
        (1..n).map { Fixtures.word("${prefix}_$it", "말$it", "$prefix so'z $it").copy(audio = if (audio) "a$it.mp3" else null) }

    private fun quizWord(item: ExerciseItem) = when (item) {
        is ExerciseItem.WordChoose -> item.word
        is ExerciseItem.WordTyping -> item.word
        is ExerciseItem.ListenChoose -> item.word
        else -> error("unexpected $item")
    }

    @Test fun vocabQuiz_takesTenDistinctWords() {
        val pool = quizWords(25)
        val items = builder.vocabQuiz(pool, pool)
        assertEquals(SessionBuilder.VOCAB_QUIZ_SIZE, items.size)
        assertEquals(10, items.map { quizWord(it).id }.toSet().size)
        assertTrue(items.all { quizWord(it) in pool })
    }

    @Test fun vocabQuiz_smallPoolUsesEveryWordOnce() {
        val pool = quizWords(4)
        val items = builder.vocabQuiz(pool, quizWords(30, "x"))
        assertEquals(pool.map { it.id }.toSet(), items.map { quizWord(it).id }.toSet())
        assertEquals(4, items.size)
    }

    @Test fun vocabQuiz_emptyPoolIsEmpty() = assertTrue(builder.vocabQuiz(emptyList(), quizWords(5)).isEmpty())

    @Test fun vocabQuiz_loneWordWithoutOtherMeaningsIsTyped() {
        val one = quizWords(1)
        repeat(10) { seed ->
            val items = SessionBuilder(Random(seed)).vocabQuiz(one, one)
            assertEquals(listOf(ExerciseItem.WordTyping(one[0])), items)
        }
    }

    @Test fun vocabQuiz_choiceOptionsHoldAnswerAndDistinctMeanings() {
        val pool = quizWords(2, "own")
        val extra = quizWords(20, "book") + Fixtures.word("dup", "또", "own so'z 1")
        repeat(30) { seed ->
            SessionBuilder(Random(seed)).vocabQuiz(pool, extra).filterIsInstance<ExerciseItem.WordChoose>().forEach { item ->
                assertEquals(SessionBuilder.LISTEN_OPTIONS, item.options.size)
                assertTrue(item.word in item.options)
                assertEquals(item.options.size, item.options.map { it.uz }.toSet().size)
            }
        }
    }

    @Test fun vocabQuiz_listenOnlyForWordsWithAudio() {
        repeat(20) { seed ->
            val silent = SessionBuilder(Random(seed)).vocabQuiz(quizWords(10), quizWords(10))
            assertTrue(silent.none { it is ExerciseItem.ListenChoose })
        }
        val voiced = quizWords(10, audio = true)
        val kinds = (0 until 20).flatMap { seed -> SessionBuilder(Random(seed)).vocabQuiz(voiced, voiced).map { it.typeKey } }.toSet()
        assertEquals(setOf("word_choose", "reverse_typing", "listen_choose"), kinds)
    }
}
