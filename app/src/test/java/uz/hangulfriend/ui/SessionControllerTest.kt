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
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.data.AppDatabase
import uz.hangulfriend.data.CardOrigin
import uz.hangulfriend.data.LessonStatus
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.data.StudyRepository
import uz.hangulfriend.srs.FsrsScheduler
import uz.hangulfriend.srs.Rating
import uz.hangulfriend.study.Grader
import uz.hangulfriend.study.SessionBuilder
import uz.hangulfriend.ui.session.ExerciseOutcome
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
        File(assets, "book.json").writeText(
            """{"lessons":[{"id":"u02_l1","unit":2,"lesson":1,"title_ko":"다","title_uz":"c","topic_uz":"Xarid"}]}""",
        )
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = TestScope(UnconfinedTestDispatcher())) {
                File(tmp.root, "s.preferences_pb")
            },
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
        )
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
