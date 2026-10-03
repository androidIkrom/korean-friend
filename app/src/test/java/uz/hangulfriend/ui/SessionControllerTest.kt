package uz.hangulfriend.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.random.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.hangulfriend.Fixtures
import uz.hangulfriend.MutableClock
import uz.hangulfriend.content.ContentJson
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.content.DirAssetSource
import uz.hangulfriend.content.ExerciseType
import uz.hangulfriend.content.FinalTest
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.data.AppDatabase
import uz.hangulfriend.data.CardOrigin
import uz.hangulfriend.data.GameRepository
import uz.hangulfriend.data.LessonStatus
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.data.StudyRepository
import uz.hangulfriend.data.flagRef
import uz.hangulfriend.srs.FsrsScheduler
import uz.hangulfriend.srs.Rating
import uz.hangulfriend.study.FINAL_TEST_ID
import uz.hangulfriend.study.ExerciseItem
import uz.hangulfriend.study.GameRules
import uz.hangulfriend.study.Grader
import uz.hangulfriend.study.SessionBuilder
import uz.hangulfriend.ui.session.ExerciseOutcome
import uz.hangulfriend.ui.session.FinalScore
import uz.hangulfriend.ui.session.SessionController
import uz.hangulfriend.ui.session.SessionMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionControllerTest {
    @get:Rule val tmp = TemporaryFolder()

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val clock = MutableClock()
    private val study = StudyRepository(db, FsrsScheduler(), clock)
    private val progress = ProgressRepository(db)
    private val game = GameRepository(db, clock)
    private val settings by lazy {
        SettingsRepository(
            PreferenceDataStoreFactory.create(scope = TestScope(UnconfinedTestDispatcher())) {
                File(tmp.root, "s.preferences_pb")
            },
        )
    }

    @After fun close() = db.close()

    /** Fixture lesson whose test holds 15 conjugate exercises. */
    private val lesson: Lesson = Fixtures.validLesson().let { base ->
        val tests = (1..15).map { i ->
            Fixtures.exercise("u02_l1_t%03d".format(i), ExerciseType.CONJUGATE, listOf("u02_l1_g1"), listOf("가 보다")) {
                copy(base = "가다", form = "-아/어 보다")
            }
        }
        base.copy(
            exercises = base.exercises + tests,
            test = tests.map { it.id },
            grammar = base.grammar.map { g -> g.copy(examples = g.examples.map { it.copy(audio = "ex.ogg") }) },
        )
    }

    private fun controller(mode: SessionMode, lessonId: String? = "u02_l1", speech: Boolean = false): SessionController {
        val assets = File(tmp.root, "assets")
        File(assets, "lessons").mkdirs()
        File(assets, "lessons/u02_l1.json").writeText(ContentJson.encodeToString(Lesson.serializer(), lesson))
        File(assets, "lessons/u02_l2.json").writeText(
            ContentJson.encodeToString(Lesson.serializer(), Fixtures.validLesson("u02_l2", 2, 2)),
        )
        File(assets, "final_test.json").writeText(ContentJson.encodeToString(FinalTest.serializer(), finalTest))
        File(assets, "book.json").writeText(
            """{"lessons":[{"id":"u02_l1","unit":2,"lesson":1,"title_ko":"다","title_uz":"c","topic_uz":"Xarid"},
              {"id":"u02_l2","unit":2,"lesson":2,"title_ko":"라","title_uz":"d","topic_uz":"Xarid"}]}""",
        )
        return SessionController(
            mode = mode,
            lessonId = lessonId,
            content = ContentRepository(DirAssetSource(assets), strict = true),
            study = study,
            progress = progress,
            settings = settings,
            builder = SessionBuilder(Random(7)),
            grader = Grader(study),
            speechAvailable = speech,
            game = game,
        )
    }

    private val finalTest = FinalTest(
        listening = (1..2).map { i ->
            Fixtures.exercise("final_l0$i", ExerciseType.LISTEN_QUESTION, emptyList(), listOf("가")) {
                copy(audioText = "가", options = listOf("가", "나", "다", "라"))
            }
        },
        reading = (1..2).map { i ->
            Fixtures.exercise("final_r0$i", ExerciseType.READ_CHOICE, emptyList(), listOf("가")) {
                copy(sentence = "글", options = listOf("가", "나", "다", "라"))
            }
        },
    )

    private suspend fun runFinal(vararg correct: Boolean): SessionController {
        val c = controller(SessionMode.FINAL, lessonId = null)
        c.load()
        correct.forEach {
            c.submit(checked(it))
            c.next()
        }
        return c
    }

    @Test fun finalGivesNoAnswerXp() = runTest {
        val c = controller(SessionMode.FINAL, lessonId = null)
        c.load()
        assertEquals(listOf("final_l01", "final_l02", "final_r01", "final_r02"), c.state.value.items.map { flagRef(it) })
        c.submit(checked(true))
        assertEquals(0, c.state.value.xpEarned)
        assertEquals(0, game.observeTotalXp().first())
    }

    @Test fun finalScoresSections() = runTest {
        val c = runFinal(true, false, true, true)
        assertTrue(c.state.value.finished)
        assertEquals(FinalScore(1, 2, 2, 2), c.state.value.final)
        assertEquals(75, c.state.value.final!!.percent)
        assertEquals(2, c.state.value.final!!.level)
    }

    @Test fun timeUpEndsTest() = runTest {
        val c = runFinal(true)
        c.timeUp()
        assertTrue(c.state.value.finished)
        assertEquals(FinalScore(1, 2, 0, 2), c.state.value.final)
    }

    @Test fun bonusOnlyOnce() = runTest {
        // The bonus may also cross the daily goal, which adds the goal XP on top.
        assertTrue(runFinal(true, true, true, true).state.value.xpEarned >= GameRules.XP_FINAL)
        assertEquals(0, runFinal(true, true, true, true).state.value.xpEarned)
        assertEquals(100, game.best(FINAL_TEST_ID))
    }

    private fun checked(correct: Boolean) = ExerciseOutcome.Checked(correct, usedHint = false, elapsedMs = 1000, check = null)

    @Test fun session_testModeRecordsScore() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        val c = controller(SessionMode.TEST)
        c.load()
        assertEquals(15, c.state.value.items.size)
        repeat(15) { i ->
            c.submit(checked(correct = i < 12))
            c.next()
        }
        assertTrue(c.state.value.finished)
        assertEquals(80, c.state.value.scorePercent)
        val p = progress.observeAll().first().getValue("u02_l1")
        assertEquals(LessonStatus.COMPLETED, p.status)
        assertEquals(80, p.bestTestScore)
    }

    @Test fun session_wrongAnswerGradesAgain() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        val c = controller(SessionMode.TEST)
        c.load()
        c.submit(checked(correct = false))
        assertEquals(Rating.AGAIN.value, db.logs().forCard("u02_l1_g1#G").last().rating)
        assertEquals(0, c.state.value.correctCount)
    }

    @Test fun session_submitTwiceCountsOnce() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        val c = controller(SessionMode.TEST)
        c.load()
        c.submit(checked(correct = true))
        c.submit(checked(correct = true))
        assertEquals(1, c.state.value.correctCount)
        assertEquals(1, db.logs().forCard("u02_l1_g1#G").size)
    }

    @Test fun session_reviewModeBuildsFromDueQueue() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        val c = controller(SessionMode.REVIEW, lessonId = null)
        c.load()
        assertEquals(study.dueQueue(20).size, c.state.value.items.size)
        assertEquals(5, c.state.value.items.size)
    }

    @Test fun session_emptyReviewFinishesImmediately() = runTest {
        val c = controller(SessionMode.REVIEW, lessonId = null)
        c.load()
        assertTrue(c.state.value.finished)
    }

    @Test fun session_flashcardAgainIsNotCorrect() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        val c = controller(SessionMode.LESSON_REVIEW)
        c.load()
        val flashIndex = c.state.value.items.indexOfFirst { it.typeKey == "flashcard" }
        repeat(flashIndex) { c.next() }
        c.submit(ExerciseOutcome.Rated(Rating.AGAIN))
        assertEquals(0, c.state.value.correctCount)
    }

    @Test fun session_skippedItemNotGradedNorScored() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        val c = controller(SessionMode.TEST)
        c.load()
        c.submit(ExerciseOutcome.Skipped)
        c.next()
        repeat(14) {
            c.submit(checked(correct = true))
            c.next()
        }
        assertEquals(100, c.state.value.scorePercent)
        assertEquals(14, db.logs().forCard("u02_l1_g1#G").size)
    }

    @Test fun session_speakDoesNotGrade() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        val c = controller(SessionMode.PRACTICE, speech = true)
        c.load()
        val speakIndex = c.state.value.items.indexOfFirst { it.typeKey == "speak" }
        assertTrue("practice should contain a speak item", speakIndex >= 0)
        repeat(speakIndex) { c.next() }
        val before = db.logs().forCard("u02_l1_g1#G").size
        c.submit(checked(correct = true))
        assertEquals(before, db.logs().forCard("u02_l1_g1#G").size)
        assertEquals(1, c.state.value.correctCount)
    }

    private suspend fun answerAll(c: SessionController, vararg correct: Boolean) = correct.forEach {
        c.submit(checked(it))
        c.next()
    }

    @Test fun session_awardsXpAndCombo() = runTest {
        settings.setDailyGoalXp(1000)
        val c = controller(SessionMode.TEST)
        c.load()
        answerAll(c, true, true, true, true)
        assertEquals(50, c.state.value.xpEarned)
        assertEquals(4, c.state.value.combo)
    }

    @Test fun session_wrongResetsCombo() = runTest {
        settings.setDailyGoalXp(1000)
        val c = controller(SessionMode.TEST)
        c.load()
        answerAll(c, true, true, false, true)
        assertEquals(30, c.state.value.xpEarned)
        assertEquals(1, c.state.value.combo)
    }

    @Test fun session_bossLosesAfterThreeWrong() = runTest {
        val c = controller(SessionMode.BOSS, lessonId = "2")
        c.load()
        assertEquals(15, c.state.value.items.size)
        assertEquals(3, c.state.value.hearts)
        answerAll(c, false, true, false, false)
        assertTrue(c.state.value.failed)
        assertTrue(c.state.value.finished)
        assertEquals(0, db.game().countReason(GameRepository.REASON_BOSS))
    }

    @Test fun session_bossWinAwards200AndAchievement() = runTest {
        val c = controller(SessionMode.BOSS, lessonId = "2")
        c.load()
        answerAll(c, *BooleanArray(15) { true })
        assertTrue(c.state.value.finished)
        assertEquals(1, db.game().countReason(GameRepository.REASON_BOSS))
        assertTrue("first_boss" in c.state.value.newAchievements)
    }

    @Test fun session_testCompletionAwardsLessonXpOnce() = runTest {
        repeat(2) {
            val c = controller(SessionMode.TEST)
            c.load()
            answerAll(c, *BooleanArray(15) { true })
        }
        assertEquals(1, db.game().countReason(GameRepository.REASON_LESSON))
    }

    @Test fun session_quickCheckVerifies() = runTest {
        progress.markPassed(listOf("u02_l1"))
        val c = controller(SessionMode.QUICK_CHECK)
        c.load()
        assertEquals(10, c.state.value.items.size)
        answerAll(c, *BooleanArray(10) { true })
        assertEquals(LessonStatus.VERIFIED, progress.observeAll().first().getValue("u02_l1").status)
    }

    @Test fun session_mistakesFromAgainCards() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        study.grade("u02_l1_w001#R", Rating.AGAIN, 1000)
        val c = controller(SessionMode.MISTAKES, lessonId = null)
        c.load()
        assertEquals(listOf("u02_l1_w001#R"), c.state.value.items.flatMap { it.cardIds })
    }

    @Test fun lastCorrectFollowsAnswer() = runTest {
        val c = controller(SessionMode.TEST)
        c.load()
        assertEquals(null, c.state.value.lastCorrect)
        c.submit(checked(true))
        assertEquals(true, c.state.value.lastCorrect)
        c.next()
        assertEquals(null, c.state.value.lastCorrect)
        c.submit(checked(false))
        assertEquals(false, c.state.value.lastCorrect)
    }

    @Test fun skippedHasNoVerdict() = runTest {
        val c = controller(SessionMode.TEST)
        c.load()
        c.submit(ExerciseOutcome.Skipped)
        assertTrue(c.state.value.answered)
        assertEquals(null, c.state.value.lastCorrect)
    }

    @Test fun totalXpAfterSetOnFinish() = runTest {
        val c = controller(SessionMode.TEST)
        c.load()
        assertEquals(null, c.state.value.totalXpAfter)
        answerAll(c, *BooleanArray(15) { true })
        assertEquals(game.observeTotalXp().first(), c.state.value.totalXpAfter)
    }

    @Test fun lootHoldsWordsAnsweredRight() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        val c = controller(SessionMode.LESSON_REVIEW)
        c.load()
        val items = c.state.value.items
        val flash = items.indexOfFirst { it is ExerciseItem.Flashcard }
        val word = (items[flash] as ExerciseItem.Flashcard).word
        repeat(flash) { c.next() }
        c.submit(ExerciseOutcome.Rated(Rating.GOOD))
        assertEquals(listOf(word), c.state.value.loot)
        // A second hit on the same word does not add it twice; AGAIN adds nothing.
        c.next()
        val again = c.state.value.items.drop(flash + 1).indexOfFirst { it is ExerciseItem.Flashcard }
        if (again >= 0) {
            repeat(again) { c.next() }
            c.submit(ExerciseOutcome.Rated(Rating.AGAIN))
            assertEquals(listOf(word), c.state.value.loot)
        }
    }

    @Test fun session_practiceDoesNotRecordTest() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        val c = controller(SessionMode.PRACTICE)
        c.load()
        repeat(c.state.value.items.size) {
            c.submit(checked(correct = true))
            c.next()
        }
        assertTrue(c.state.value.finished)
        assertEquals(null, progress.observeAll().first()["u02_l1"]?.bestTestScore)
    }
}
