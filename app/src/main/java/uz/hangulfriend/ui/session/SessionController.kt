package uz.hangulfriend.ui.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import uz.hangulfriend.R
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.data.StudyRepository
import uz.hangulfriend.hangul.CheckResult
import uz.hangulfriend.hangul.Feedback
import uz.hangulfriend.srs.Rating
import uz.hangulfriend.study.ExerciseItem
import uz.hangulfriend.study.Grader
import uz.hangulfriend.study.SessionBuilder

enum class SessionMode(val route: String) {
    PRACTICE("practice"),
    TEST("test"),
    REVIEW("review"),
    LESSON_REVIEW("lessonReview"),
    ;

    companion object {
        fun fromRoute(route: String): SessionMode = entries.first { it.route == route }
    }
}

sealed interface ExerciseOutcome {
    data class Checked(val correct: Boolean, val usedHint: Boolean, val elapsedMs: Long, val check: CheckResult?) :
        ExerciseOutcome

    data class Rated(val rating: Rating) : ExerciseOutcome

    /** [firstTryCorrect] holds the word ids matched on the first attempt. */
    data class Matched(val firstTryCorrect: Set<String>, val total: Int) : ExerciseOutcome

    /** The learner could not do the item (e.g. no microphone): not graded, not scored. */
    data object Skipped : ExerciseOutcome
}

data class SessionState(
    val loading: Boolean = true,
    val items: List<ExerciseItem> = emptyList(),
    val index: Int = 0,
    val correctCount: Int = 0,
    /** Answered items that count toward the score (everything except [ExerciseOutcome.Skipped]). */
    val scored: Int = 0,
    val answered: Boolean = false,
) {
    val finished: Boolean get() = !loading && index >= items.size
    val current: ExerciseItem? get() = items.getOrNull(index)
    val scorePercent: Int get() = if (scored == 0) 0 else correctCount * 100 / scored
}

@androidx.annotation.StringRes
fun feedbackText(f: Feedback): Int = when (f) {
    Feedback.CORRECT -> R.string.feedback_correct
    Feedback.SPACING -> R.string.feedback_spacing
    Feedback.FINAL_CONSONANT -> R.string.feedback_final_consonant
    Feedback.VOWEL -> R.string.feedback_vowel
    Feedback.ONE_LETTER -> R.string.feedback_one_letter
    Feedback.WRONG -> R.string.feedback_wrong
}

/** Session logic without Android lifecycle; [SessionViewModel] runs it in its scope. */
class SessionController(
    private val mode: SessionMode,
    private val lessonId: String?,
    private val content: ContentRepository,
    private val study: StudyRepository,
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val builder: SessionBuilder,
    private val grader: Grader,
    private val speechAvailable: Boolean,
) {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state

    suspend fun load() {
        val items = when (mode) {
            SessionMode.REVIEW -> {
                val cards = study.dueQueue(settings.settings.first().dailyNewLimit)
                val lessons = cards.map { it.lessonId }.distinct().mapNotNull { content.lesson(it) }.associateBy { it.id }
                builder.review(cards, lessons)
            }
            else -> {
                val lesson = lessonId?.let { content.lesson(it) }
                when {
                    lesson == null -> emptyList()
                    mode == SessionMode.PRACTICE -> builder.lessonPractice(lesson, speechAvailable)
                    mode == SessionMode.TEST -> builder.lessonTest(lesson)
                    else -> builder.lessonReview(lesson)
                }
            }
        }
        _state.value = SessionState(loading = false, items = items)
    }

    /** Grades the current item once; later calls for the same item are ignored. */
    suspend fun submit(outcome: ExerciseOutcome) {
        val s = _state.value
        val item = s.current ?: return
        if (s.answered) return
        if (outcome == ExerciseOutcome.Skipped) {
            _state.update { it.copy(answered = true) }
            return
        }
        val correct = when (outcome) {
            is ExerciseOutcome.Checked -> outcome.correct
            is ExerciseOutcome.Rated -> outcome.rating != Rating.AGAIN
            is ExerciseOutcome.Matched -> outcome.firstTryCorrect.size == outcome.total
            ExerciseOutcome.Skipped -> false
        }
        _state.update {
            it.copy(answered = true, scored = it.scored + 1, correctCount = it.correctCount + if (correct) 1 else 0)
        }
        when {
            outcome is ExerciseOutcome.Rated && item is ExerciseItem.Flashcard -> grader.gradeFlashcard(item, outcome.rating)
            outcome is ExerciseOutcome.Matched && item is ExerciseItem.Match -> grader.gradeMatch(item, outcome.firstTryCorrect)
            outcome is ExerciseOutcome.Checked -> grader.gradeAuto(item, correct, outcome.usedHint, outcome.elapsedMs)
            else -> grader.gradeAuto(item, correct, usedHint = false, elapsedMs = 0)
        }
    }

    suspend fun next() {
        _state.update { it.copy(index = it.index + 1, answered = false) }
        val s = _state.value
        if (s.finished && mode == SessionMode.TEST && lessonId != null && s.items.isNotEmpty()) {
            progress.recordTest(lessonId, s.scorePercent)
        }
    }
}
