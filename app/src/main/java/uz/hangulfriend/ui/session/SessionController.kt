package uz.hangulfriend.ui.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import uz.hangulfriend.R
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.content.Word
import uz.hangulfriend.data.CardEntity
import uz.hangulfriend.data.GameRepository
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.data.StudyRepository
import uz.hangulfriend.hangul.CheckResult
import uz.hangulfriend.hangul.Feedback
import uz.hangulfriend.srs.Rating
import uz.hangulfriend.study.ExerciseItem
import uz.hangulfriend.study.FINAL_TEST_ID
import uz.hangulfriend.study.GameRules
import uz.hangulfriend.study.Grader
import uz.hangulfriend.study.SessionBuilder
import uz.hangulfriend.ui.kit.Sfx

enum class SessionMode(val route: String) {
    PRACTICE("practice"),
    TEST("test"),
    REVIEW("review"),
    LESSON_REVIEW("lessonReview"),

    /** lessonId carries the unit number, e.g. "2". */
    BOSS("boss"),
    QUICK_CHECK("quickCheck"),
    MISTAKES("mistakes"),

    /** The timed final test (stage 7c). */
    FINAL("final"),
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
    /** Correct answers in a row, ending with the latest answer. */
    val combo: Int = 0,
    val xpEarned: Int = 0,
    /** Boss lives left; null outside a boss battle. */
    val hearts: Int? = null,
    /** Boss lost: the session ends at the next [SessionController.next]. */
    val failed: Boolean = false,
    val newAchievements: List<String> = emptyList(),
    /** Final-test result once finished; null in other modes. */
    val final: FinalScore? = null,
    /** Verdict of the current item once answered; null before that and for skipped items. */
    val lastCorrect: Boolean? = null,
    /** Words answered right in this run, in order, each once: the loot of the Gate Cleared screen. */
    val loot: List<Word> = emptyList(),
    /** The learner's total XP after the end-of-run rewards; null until then. */
    val totalXpAfter: Int? = null,
) {
    val finished: Boolean get() = !loading && index >= items.size
    val current: ExerciseItem? get() = items.getOrNull(index)
    val scorePercent: Int get() = if (scored == 0) 0 else correctCount * 100 / scored
}

/** Correct answers per section of the final test. */
data class FinalScore(val listening: Int, val listeningTotal: Int, val reading: Int, val readingTotal: Int) {
    val total: Int get() = listeningTotal + readingTotal
    val percent: Int get() = if (total == 0) 0 else (listening + reading) * 100 / total
    val level: Int get() = GameRules.topikLevel(percent)
}

/** Whether the run counts as a cleared gate (Gate Cleared screen title and sound). */
fun SessionState.cleared(mode: SessionMode): Boolean = when {
    items.isEmpty() -> false
    mode == SessionMode.BOSS -> !failed
    mode == SessionMode.TEST -> scorePercent >= ProgressRepository.PASS_PERCENT
    mode == SessionMode.FINAL -> (final?.level ?: 0) >= 1
    else -> true
}

/** The hit sound of an answer. Self-rated flashcards and skipped items make none. */
fun ExerciseOutcome.verdictSfx(): Sfx? = when (this) {
    is ExerciseOutcome.Checked -> if (correct) Sfx.CORRECT else Sfx.WRONG
    is ExerciseOutcome.Matched -> Sfx.CORRECT
    is ExerciseOutcome.Rated, ExerciseOutcome.Skipped -> null
}

/** Words of [item] that [outcome] won. */
private fun lootOf(item: ExerciseItem, outcome: ExerciseOutcome, correct: Boolean): List<Word> = when (item) {
    is ExerciseItem.Match -> (outcome as? ExerciseOutcome.Matched)?.let { m -> item.words.filter { it.id in m.firstTryCorrect } }.orEmpty()
    is ExerciseItem.Flashcard -> if (correct) listOf(item.word) else emptyList()
    is ExerciseItem.WordTyping -> if (correct) listOf(item.word) else emptyList()
    is ExerciseItem.ListenChoose -> if (correct) listOf(item.word) else emptyList()
    is ExerciseItem.Dictation -> if (correct) listOf(item.word) else emptyList()
    is ExerciseItem.Speak, is ExerciseItem.Authored -> emptyList()
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
    private val game: GameRepository,
    /** Resolves a card's lesson, including the learner's own words ("user"); defaults to book lessons only. */
    private val lessonOf: suspend (String) -> Lesson? = { content.lesson(it) },
) {
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state
    private var wrappedUp = false
    private var finalListening = 0
    private val finalCorrect = mutableMapOf<Int, Boolean>()

    private suspend fun goal() = settings.settings.first().dailyGoalXp

    suspend fun load() {
        val items = when (mode) {
            SessionMode.REVIEW -> {
                val cards = study.dueQueue(settings.settings.first().dailyNewLimit)
                builder.review(cards, lessonsOf(cards))
            }
            SessionMode.MISTAKES -> {
                val cards = game.mistakes()
                builder.mistakes(cards, lessonsOf(cards))
            }
            SessionMode.FINAL -> content.finalTest()?.let { t ->
                finalListening = t.listening.size
                builder.finalTest(t)
            }.orEmpty()
            SessionMode.BOSS -> {
                val unit = lessonId?.toIntOrNull() ?: 0
                builder.boss(listOfNotNull(content.lesson(unitLessonId(unit, 1)), content.lesson(unitLessonId(unit, 2))))
            }
            else -> {
                val lesson = lessonId?.let { content.lesson(it) }
                when {
                    lesson == null -> emptyList()
                    mode == SessionMode.PRACTICE -> builder.lessonPractice(lesson, speechAvailable)
                    mode == SessionMode.TEST -> builder.lessonTest(lesson)
                    mode == SessionMode.QUICK_CHECK -> builder.quickCheck(lesson)
                    else -> builder.lessonReview(lesson)
                }
            }
        }
        _state.value = SessionState(loading = false, items = items, hearts = if (mode == SessionMode.BOSS) BOSS_HEARTS else null)
    }

    private suspend fun lessonsOf(cards: List<CardEntity>) =
        cards.map { it.lessonId }.distinct().mapNotNull { lessonOf(it) }.associateBy { it.id }

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
        val combo = if (correct) s.combo + 1 else 0
        if (mode == SessionMode.FINAL) {
            // An exam: no XP per answer and no FSRS change, only the score.
            finalCorrect[s.index] = correct
            _state.update {
                it.copy(
                    answered = true,
                    scored = it.scored + 1,
                    correctCount = it.correctCount + if (correct) 1 else 0,
                    lastCorrect = correct,
                )
            }
            return
        }
        val gained = game.award(GameRules.xpForAnswer(correct, combo), GameRepository.REASON_ANSWER, goal())
        _state.update {
            val hearts = it.hearts?.let { h -> if (correct) h else h - 1 }
            it.copy(
                answered = true,
                scored = it.scored + 1,
                correctCount = it.correctCount + if (correct) 1 else 0,
                combo = combo,
                xpEarned = it.xpEarned + gained,
                hearts = hearts,
                failed = hearts == 0,
                lastCorrect = correct,
                loot = (it.loot + lootOf(item, outcome, correct)).distinctBy { w -> w.id },
            )
        }
        when {
            outcome is ExerciseOutcome.Rated && item is ExerciseItem.Flashcard -> grader.gradeFlashcard(item, outcome.rating)
            outcome is ExerciseOutcome.Matched && item is ExerciseItem.Match -> grader.gradeMatch(item, outcome.firstTryCorrect)
            outcome is ExerciseOutcome.Checked -> grader.gradeAuto(item, correct, outcome.usedHint, outcome.elapsedMs)
            else -> grader.gradeAuto(item, correct, usedHint = false, elapsedMs = 0)
        }
    }

    suspend fun next() {
        _state.update { it.copy(index = if (it.failed) it.items.size else it.index + 1, answered = false, lastCorrect = null) }
        val s = _state.value
        if (s.finished && !wrappedUp && s.items.isNotEmpty()) {
            wrappedUp = true
            wrapUp(s)
        }
    }

    /** Final test: the time ran out; unanswered questions count as wrong. */
    suspend fun timeUp() {
        if (mode != SessionMode.FINAL) return
        _state.update { it.copy(index = it.items.size, answered = false, lastCorrect = null) }
        if (!wrappedUp && _state.value.items.isNotEmpty()) {
            wrappedUp = true
            wrapUp(_state.value)
        }
    }

    private suspend fun finishFinal(s: SessionState): Int {
        val score = FinalScore(
            listening = (0 until finalListening).count { finalCorrect[it] == true },
            listeningTotal = finalListening,
            reading = (finalListening until s.items.size).count { finalCorrect[it] == true },
            readingTotal = s.items.size - finalListening,
        )
        val before = game.best(FINAL_TEST_ID)
        game.submitScore(FINAL_TEST_ID, score.percent)
        _state.update { it.copy(final = score) }
        val firstPass = before < PASS_PERCENT && score.percent >= PASS_PERCENT
        return if (firstPass) game.award(GameRules.XP_FINAL, GameRepository.REASON_FINAL, goal()) else 0
    }

    /** End-of-session rewards: lesson completion, quick check, boss win, then new achievements. */
    private suspend fun wrapUp(s: SessionState) {
        val goal = goal()
        var bonus = 0
        when (mode) {
            SessionMode.TEST -> if (lessonId != null && progress.recordTest(lessonId, s.scorePercent)) {
                bonus += game.award(GameRules.XP_LESSON, GameRepository.REASON_LESSON, goal)
            }
            SessionMode.QUICK_CHECK -> if (lessonId != null) progress.recordQuickCheck(lessonId, s.scorePercent)
            SessionMode.BOSS -> if (!s.failed) bonus += game.award(GameRules.XP_BOSS, GameRepository.REASON_BOSS, goal)
            SessionMode.FINAL -> bonus += finishFinal(s)
            else -> Unit
        }
        val unlocked = game.unlockNew(GameRules.achievements(game.stats(goal)))
        val total = game.observeTotalXp().first()
        _state.update { it.copy(xpEarned = it.xpEarned + bonus, newAchievements = unlocked, totalXpAfter = total) }
    }

    companion object {
        const val BOSS_HEARTS = 3

        /** Final-test score that earns the one-time bonus (TOPIK I level 2). */
        const val PASS_PERCENT = 70

        fun unitLessonId(unit: Int, lesson: Int) = "u%02d_l%d".format(unit, lesson)
    }
}
