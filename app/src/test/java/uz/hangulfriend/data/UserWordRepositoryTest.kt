package uz.hangulfriend.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.hangulfriend.MutableClock
import uz.hangulfriend.srs.FsrsScheduler

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UserWordRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val clock = MutableClock()
    private val repo = UserWordRepository(db, StudyRepository(db, FsrsScheduler(), clock), clock)

    @After fun close() = db.close()

    @Test fun addCreatesCards() = runTest {
        val r = repo.add("사과", "olma", null, emptySet())
        assertEquals(UserWordRepository.AddResult.Added(1), r)
        val cards = db.cards().all().sortedBy { it.id }
        assertEquals(listOf("user_w1#P", "user_w1#R"), cards.map { it.id })
        assertTrue(cards.all { it.lessonId == USER_LESSON_ID && it.lessonOrder == -1 && it.origin == CardOrigin.LESSON })
    }

    @Test fun duplicateOwnWordRejected() = runTest {
        repo.add("사과", "olma", null, emptySet())
        assertEquals(UserWordRepository.AddResult.Duplicate, repo.add(" 사 과 ", "olma 2", null, emptySet()))
        assertEquals(1, db.userWords().all().size)
    }

    @Test fun bookWordRejected() = runTest {
        assertEquals(UserWordRepository.AddResult.Duplicate, repo.add("가방", "sumka", null, setOf("가방")))
    }

    @Test fun blankRejected() = runTest {
        assertEquals(UserWordRepository.AddResult.Empty, repo.add("  ", "olma", null, emptySet()))
        assertEquals(UserWordRepository.AddResult.Empty, repo.add("사과", " ", null, emptySet()))
    }

    @Test fun tooLongRejected() = runTest {
        assertEquals(UserWordRepository.AddResult.TooLong, repo.add("가".repeat(41), "olma", null, emptySet()))
        assertEquals(UserWordRepository.AddResult.TooLong, repo.add("사과", "o".repeat(81), null, emptySet()))
        assertEquals(UserWordRepository.AddResult.TooLong, repo.add("사과", "olma", "n".repeat(121), emptySet()))
    }

    @Test fun deleteRemovesCardsAndLogs() = runTest {
        repo.add("사과", "olma", null, emptySet())
        db.logs().insert(ReviewLogEntity(cardId = "user_w1#R", rating = 3, reviewedMs = 1, elapsedMs = 1))
        repo.delete(1)
        assertTrue(db.cards().all().isEmpty())
        assertTrue(db.logs().all().isEmpty())
        assertTrue(db.userWords().all().isEmpty())
    }

    @Test fun asLessonListsWords() = runTest {
        repo.add("사과", "olma", "qizil meva", emptySet())
        val lesson = repo.asLesson()
        assertEquals(USER_LESSON_ID, lesson.id)
        val w = lesson.words.single()
        assertEquals("user_w1", w.id)
        assertEquals("사과", w.ko)
        assertEquals("olma", w.uz)
        assertEquals("qizil meva", w.exampleKo)
    }
}
