package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.hangulfriend.Fixtures
import uz.hangulfriend.data.CardEntity
import uz.hangulfriend.data.CardKind
import uz.hangulfriend.data.CardOrigin
import uz.hangulfriend.srs.CardState
import uz.hangulfriend.ui.vocab.VocabEntry
import uz.hangulfriend.ui.vocab.VocabFilter
import uz.hangulfriend.ui.vocab.WordStatus
import uz.hangulfriend.ui.vocab.filterVocab
import uz.hangulfriend.ui.vocab.quizSize
import uz.hangulfriend.ui.vocab.quizSource
import uz.hangulfriend.ui.vocab.statusOf

class VocabModelTest {
    private fun book(id: String, ko: String, uz: String, lessonId: String = "u02_l1") =
        VocabEntry(id, ko, uz, lessonId, "2-1", own = false, ownId = null, word = Fixtures.word(id, ko, uz), note = null)

    private fun own(n: Long, ko: String, uz: String) =
        VocabEntry("user_w$n", ko, uz, "user", "★", own = true, ownId = n, word = Fixtures.word("user_w$n", ko, uz), note = null)

    private val entries = listOf(
        book("w1", "사과", "olma"),
        book("w2", "한국어", "koreys tili"),
        book("w3", "가방", "sumka", lessonId = "u03_l1"),
        book("w4", "옷", "o'zbekcha kiyim"),
        own(1, "커피", "Qahva"),
    )

    private fun ids(q: String, f: VocabFilter = VocabFilter.All) = filterVocab(entries, q, f).map { it.id }

    @Test fun koreanSearch() = assertEquals(listOf("w2"), ids("국어"))

    @Test fun uzbekSearchIgnoresCase() = assertEquals(listOf("user_w1"), ids("QAH"))

    @Test fun apostropheVariantsMatch() {
        assertEquals(listOf("w4"), ids("oʻzbek"))
        assertEquals(listOf("w4"), ids("o’zbek"))
        assertEquals(listOf("w4"), ids("o'zbek"))
    }

    @Test fun spacesIgnoredInKorean() = assertEquals(listOf("w2"), ids("한 국어"))

    @Test fun ownFilter() = assertEquals(listOf("user_w1"), ids("", VocabFilter.Own))

    @Test fun lessonFilter() = assertEquals(listOf("w3"), ids("", VocabFilter.Lesson("u03_l1")))

    @Test fun ownWordsFirst() = assertEquals(listOf("user_w1", "w1", "w2", "w3", "w4"), ids(" "))

    private fun card(state: CardState, firstReviewedMs: Long?) = CardEntity(
        id = "w1#R", itemId = "w1", kind = CardKind.RECOGNIZE, lessonId = "u02_l1", origin = CardOrigin.LESSON, state = state,
        step = null, stability = null, difficulty = null, dueMs = 0, lastReviewMs = null, firstReviewedMs = firstReviewedMs,
        reps = 0, lapses = 0, lessonOrder = 0,
    )

    @Test fun statusMapping() {
        assertEquals(WordStatus.NEW, statusOf(null))
        assertEquals(WordStatus.NEW, statusOf(card(CardState.LEARNING, null)))
        assertEquals(WordStatus.LEARNING, statusOf(card(CardState.RELEARNING, 5)))
        assertEquals(WordStatus.LEARNED, statusOf(card(CardState.REVIEW, 5)))
    }

    @Test fun quizSizeFollowsFilterNotQuery() {
        assertEquals(5, quizSize(entries, VocabFilter.All))
        assertEquals(1, quizSize(entries, VocabFilter.Own))
        assertEquals(3, quizSize(entries, VocabFilter.Lesson("u02_l1")))
        assertEquals(0, quizSize(entries, VocabFilter.Lesson("u09_l1")))
    }

    @Test fun quizSizeCapsAtTen() {
        val many = (1..14).map { book("m$it", "말$it", "so'z $it") }
        assertEquals(10, quizSize(many, VocabFilter.All))
    }

    @Test fun quizSourcePerFilter() {
        assertEquals("all", VocabFilter.All.quizSource())
        assertEquals("user", VocabFilter.Own.quizSource())
        assertEquals("u03_l1", VocabFilter.Lesson("u03_l1").quizSource())
    }
}
