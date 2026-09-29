package uz.hangulfriend.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.Duration
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class StudyRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val clock = MutableClock()
    private val study = StudyRepository(db, FsrsScheduler(), clock)

    @After fun close() = db.close()

    private val lesson = Fixtures.validLesson()

    /** A lesson with [n] words and no grammar: 2n cards. */
    private fun wordLesson(id: String, n: Int) = lesson.copy(
        id = id,
        words = (1..n).map { Fixtures.word("${id}_w%03d".format(it), "말$it", "so'z$it") },
        grammar = emptyList(),
        exercises = emptyList(),
        test = emptyList(),
    )

    @Test fun ensureCards_createsRPGCards() = runTest {
        study.ensureCards(lesson, lessonOrder = 2, origin = CardOrigin.LESSON)
        val ids = db.cards().all().map { it.id }.toSet()
        assertEquals(
            setOf("u02_l1_w001#R", "u02_l1_w001#P", "u02_l1_w002#R", "u02_l1_w002#P", "u02_l1_g1#G"),
            ids,
        )
    }

    @Test fun ensureCards_isIdempotentAndKeepsState() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        study.grade("u02_l1_w001#R", Rating.GOOD, 1000)
        val before = db.cards().get("u02_l1_w001#R")!!
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        study.ensureCards(lesson, 2, CardOrigin.BACKLOG)
        assertEquals(5, db.cards().all().size)
        assertEquals(before, db.cards().get("u02_l1_w001#R"))
    }

    @Test fun ensureCards_upgradesUnseenBacklogToLesson() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.BACKLOG)
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        assertTrue(db.cards().all().all { it.origin == CardOrigin.LESSON })
    }

    @Test fun grade_writesLogAndSetsFirstReviewed() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        assertNull(db.cards().get("u02_l1_g1#G")!!.firstReviewedMs)
        study.grade("u02_l1_g1#G", Rating.HARD, 4200)
        val card = db.cards().get("u02_l1_g1#G")!!
        assertEquals(clock.now.toEpochMilli(), card.firstReviewedMs)
        assertEquals(1, card.reps)
        assertNotNull(card.stability)
        val log = db.logs().forCard("u02_l1_g1#G").single()
        assertEquals(Rating.HARD.value, log.rating)
        assertEquals(4200, log.elapsedMs)
    }

    @Test fun grade_lapseCountsOnlyFromReview() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        study.grade("u02_l1_w001#R", Rating.AGAIN, 1000)
        assertEquals(0, db.cards().get("u02_l1_w001#R")!!.lapses)
        study.grade("u02_l1_w002#R", Rating.EASY, 1000)
        clock.advance(Duration.ofDays(30))
        study.grade("u02_l1_w002#R", Rating.AGAIN, 1000)
        assertEquals(1, db.cards().get("u02_l1_w002#R")!!.lapses)
    }

    @Test fun dueQueue_backlogLimitedByDailyNew() = runTest {
        study.ensureCards(wordLesson("u01_l1", 15), 0, CardOrigin.BACKLOG)
        assertEquals(20, study.dueQueue(dailyNewLimit = 20).size)
    }

    @Test fun dueQueue_newLimitResetsAtLocalMidnight() = runTest {
        study.ensureCards(wordLesson("u01_l1", 30), 0, CardOrigin.BACKLOG)
        clock.setLocal("2026-10-01T23:59:00")
        study.dueQueue(20).forEach { study.grade(it.id, Rating.EASY, 1000) }
        assertEquals(0, study.dueQueue(20).count { it.firstReviewedMs == null })
        clock.setLocal("2026-10-02T00:01:00")
        assertEquals(20, study.dueQueue(20).count { it.firstReviewedMs == null })
    }

    @Test fun dueQueue_orderDueFirstThenBacklog() = runTest {
        study.ensureCards(wordLesson("u01_l1", 3), 0, CardOrigin.BACKLOG)
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        clock.advance(Duration.ofMinutes(1))
        val queue = study.dueQueue(20)
        val firstBacklog = queue.indexOfFirst { it.origin == CardOrigin.BACKLOG }
        assertEquals(5, firstBacklog)
        assertTrue(queue.take(5).all { it.lessonId == "u02_l1" })
        assertEquals(11, queue.size)
    }

    /** Opening a big lesson must not flood reviews: never-reviewed LESSON cards are new cards too. */
    @Test fun dueQueue_unseenLessonCardsCountAgainstNewLimit() = runTest {
        study.ensureCards(wordLesson("u02_l2", 15), 3, CardOrigin.LESSON)
        assertEquals(20, study.dueQueue(dailyNewLimit = 20).size)
    }

    @Test fun dueQueue_reviewedLessonCardsAreNotLimited() = runTest {
        study.ensureCards(wordLesson("u02_l2", 15), 3, CardOrigin.LESSON)
        db.cards().all().forEach { study.grade(it.id, Rating.AGAIN, 1000) }
        clock.advance(Duration.ofMinutes(2))
        assertEquals(30, study.dueQueue(dailyNewLimit = 20).size)
    }

    @Test fun dueQueue_excludesCardsNotYetDue() = runTest {
        study.ensureCards(lesson, 2, CardOrigin.LESSON)
        study.grade("u02_l1_g1#G", Rating.EASY, 1000)
        assertTrue(study.dueQueue(20).none { it.id == "u02_l1_g1#G" })
    }
}
