package uz.hangulfriend.study

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.hangulfriend.Fixtures
import uz.hangulfriend.MutableClock
import uz.hangulfriend.data.AppDatabase
import uz.hangulfriend.data.CardOrigin
import uz.hangulfriend.data.StudyRepository
import uz.hangulfriend.srs.FsrsScheduler
import uz.hangulfriend.srs.Rating

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GraderTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val study = StudyRepository(db, FsrsScheduler(), MutableClock())
    private val grader = Grader(study)
    private val lesson = Fixtures.validLesson()

    @After fun close() = db.close()

    private suspend fun lastRating(cardId: String) = db.logs().forCard(cardId).last().rating

    @Test fun grader_matchGradesEachWord() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        grader.gradeMatch(ExerciseItem.Match(lesson.words), firstTryCorrect = setOf("u02_l1_w001"))
        assertEquals(Rating.GOOD.value, lastRating("u02_l1_w001#R"))
        assertEquals(Rating.AGAIN.value, lastRating("u02_l1_w002#R"))
    }

    @Test fun grader_autoUsesAutoRating() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        val item = ExerciseItem.Authored.of(lesson, lesson.exercises[1])
        grader.gradeAuto(item, correct = true, usedHint = true, elapsedMs = 2000)
        assertEquals(Rating.HARD.value, lastRating("u02_l1_g1#G"))
        grader.gradeAuto(item, correct = false, usedHint = false, elapsedMs = 2000)
        assertEquals(Rating.AGAIN.value, lastRating("u02_l1_g1#G"))
    }

    @Test fun grader_flashcardUsesGivenRating() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        grader.gradeFlashcard(ExerciseItem.Flashcard(lesson.words[0]), Rating.EASY)
        assertEquals(Rating.EASY.value, lastRating("u02_l1_w001#R"))
    }
}
