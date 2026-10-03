package uz.hangulfriend.ui.vocab

import uz.hangulfriend.content.Word
import uz.hangulfriend.data.CardEntity
import uz.hangulfriend.data.USER_LESSON_ID
import uz.hangulfriend.data.normalizeKo
import uz.hangulfriend.srs.CardState
import uz.hangulfriend.study.SessionBuilder
import uz.hangulfriend.ui.session.SessionController

enum class WordStatus { NEW, LEARNING, LEARNED }

/** Learning status from a word's RECOGNIZE card: never reviewed is new, REVIEW is learned. */
fun statusOf(recognize: CardEntity?): WordStatus = when {
    recognize == null || recognize.firstReviewedMs == null -> WordStatus.NEW
    recognize.state == CardState.REVIEW -> WordStatus.LEARNED
    else -> WordStatus.LEARNING
}

/** One row of the vocabulary list; [lessonTag] is "2-1" for book words and "★" for own words. */
data class VocabEntry(
    val id: String,
    val ko: String,
    val uz: String,
    val lessonId: String,
    val lessonTag: String,
    val own: Boolean,
    val ownId: Long?,
    val word: Word,
    val note: String?,
)

sealed interface VocabFilter {
    data object All : VocabFilter

    data object Own : VocabFilter

    data class Lesson(val id: String) : VocabFilter
}

/** Uzbek comparison key: lower case, every apostrophe variant as ', single spaces. */
fun normalizeUz(s: String): String =
    s.lowercase().replace(Regex("[ʻʼ’‘`]"), "'").replace(Regex("\\s+"), " ").trim()

/** Own words first, then book order; a blank query matches everything. */
fun filterVocab(entries: List<VocabEntry>, query: String, filter: VocabFilter): List<VocabEntry> {
    val byFilter = entries.filter {
        when (filter) {
            VocabFilter.All -> true
            VocabFilter.Own -> it.own
            is VocabFilter.Lesson -> it.lessonId == filter.id
        }
    }
    val q = query.trim()
    val matched = if (q.isEmpty()) {
        byFilter
    } else {
        val ko = normalizeKo(q)
        val uz = normalizeUz(q)
        byFilter.filter { normalizeKo(it.ko).contains(ko) || normalizeUz(it.uz).contains(uz) }
    }
    return matched.filter { it.own } + matched.filterNot { it.own }
}

/** The vocab quiz session's lessonId for [this] filter: [SessionController.VOCAB_ALL], "user" or a lesson id. */
fun VocabFilter.quizSource(): String = when (this) {
    VocabFilter.All -> SessionController.VOCAB_ALL
    VocabFilter.Own -> USER_LESSON_ID
    is VocabFilter.Lesson -> id
}

/** Questions a random quiz over [filter] will ask: every word of the filter, at most [SessionBuilder.VOCAB_QUIZ_SIZE]. */
fun quizSize(entries: List<VocabEntry>, filter: VocabFilter): Int =
    minOf(filterVocab(entries, "", filter).size, SessionBuilder.VOCAB_QUIZ_SIZE)
