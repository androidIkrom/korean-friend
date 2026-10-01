package uz.hangulfriend.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.hangulfriend.MutableClock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StoryRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val clock = MutableClock()
    private val game = GameRepository(db, clock)
    private val story = StoryRepository(db, game, clock)

    @After fun close() = db.close()

    @Test fun firstCompletionAwards30Xp() = runTest {
        assertEquals(30, story.complete("u02_l1", goal = 1000))
        assertTrue("u02_l1" in story.observeDone().first())
    }

    @Test fun secondCompletionAwardsNothing() = runTest {
        story.complete("u02_l1", goal = 1000)
        assertEquals(0, story.complete("u02_l1", goal = 1000))
        assertEquals(30, game.observeTotalXp().first())
    }
}
