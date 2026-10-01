package uz.hangulfriend.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.hangulfriend.Fixtures
import uz.hangulfriend.MutableClock
import uz.hangulfriend.srs.FsrsScheduler
import uz.hangulfriend.srs.Rating

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GameRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val clock = MutableClock()
    private val game = GameRepository(db, clock)
    private val study = StudyRepository(db, FsrsScheduler(), clock)

    @After fun close() = db.close()

    @Test fun award_writesEvent() = runTest {
        assertEquals(10, game.award(10, "answer", goal = 50))
        assertEquals(10, game.observeTotalXp().first())
    }

    @Test fun dailyGoalBonusOncePerDay() = runTest {
        assertEquals(40, game.award(40, "answer", goal = 50))
        assertEquals(70, game.award(20, "answer", goal = 50))
        assertEquals(10, game.award(10, "answer", goal = 50))
        assertEquals(120, game.observeTotalXp().first())
    }

    @Test fun dailyXp_groupsByLocalDate() = runTest {
        clock.setLocal("2026-10-01T23:59:00")
        game.award(10, "answer", goal = 500)
        clock.setLocal("2026-10-02T00:01:00")
        game.award(20, "answer", goal = 500)
        assertEquals(mapOf(LocalDate.of(2026, 10, 1) to 10, LocalDate.of(2026, 10, 2) to 20), game.dailyXp())
        assertEquals(20, game.todayXp())
    }

    @Test fun unlockNew_returnsOnlyNew() = runTest {
        assertEquals(listOf("a", "b"), game.unlockNew(setOf("a", "b")))
        assertEquals(listOf("c"), game.unlockNew(setOf("b", "c")))
        assertEquals(3, game.observeAchievements().first().size)
    }

    @Test fun submitScore_recordOnlyWhenHigher() = runTest {
        assertTrue(game.submitScore("memory", 5))
        assertFalse(game.submitScore("memory", 3))
        assertEquals(5, game.best("memory"))
        assertEquals(0, game.best("speed"))
    }

    @Test fun mistakes_orderedByAgainCount() = runTest {
        val lesson = Fixtures.validLesson()
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        study.grade("u02_l1_w001#R", Rating.AGAIN, 1000)
        study.grade("u02_l1_g1#G", Rating.AGAIN, 1000)
        study.grade("u02_l1_g1#G", Rating.AGAIN, 1000)
        study.grade("u02_l1_w002#R", Rating.GOOD, 1000)
        assertEquals(listOf("u02_l1_g1#G", "u02_l1_w001#R"), game.mistakes().map { it.id })
    }

    @Test fun stats_wordsUnitsAndBossWins() = runTest {
        val lesson = Fixtures.validLesson()
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        study.grade("u02_l1_w001#R", Rating.EASY, 1000)
        val progress = ProgressRepository(db)
        progress.recordTest("u02_l1", 90)
        progress.recordTest("u02_l2", 85)
        game.award(200, "boss", goal = 50)
        val s = game.stats(goal = 50)
        assertEquals(1, s.wordsLearned)
        assertEquals(setOf("u02_l1", "u02_l2"), s.completedLessons)
        assertEquals(1, s.unitsCompleted)
        assertEquals(1, s.bossWins)
        assertEquals(1, s.streak)
    }
}
