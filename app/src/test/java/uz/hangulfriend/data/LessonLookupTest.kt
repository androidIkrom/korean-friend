package uz.hangulfriend.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.srs.FsrsScheduler

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LessonLookupTest {
    @get:Rule val tmp = TemporaryFolder()

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    @After fun close() = db.close()

    private fun lookup(): Pair<LessonLookup, UserWordRepository> {
        fun write(path: String, text: String) = File(tmp.root, "assets/$path").apply { parentFile.mkdirs() }.writeText(text)
        write(
            "book.json",
            """{"lessons":[{"id":"u02_l1","unit":2,"lesson":1,"title_ko":"가","title_uz":"a","topic_uz":"Xarid"}]}""",
        )
        write("lessons/u02_l1.json", ContentJson.encodeToString(Lesson.serializer(), Fixtures.validLesson()))
        val content = ContentRepository(DirAssetSource(File(tmp.root, "assets")), strict = true)
        val clock = MutableClock()
        val words = UserWordRepository(db, StudyRepository(db, FsrsScheduler(), clock), clock)
        return LessonLookup(content, words) to words
    }

    @Test fun lookupResolvesUserLesson() = runTest {
        val (lookup, words) = lookup()
        words.add("사과", "olma", null, emptySet())
        assertEquals(listOf("user_w1"), lookup.lesson(USER_LESSON_ID)?.words?.map { it.id })
    }

    @Test fun lookupFallsBackToBook() = runTest {
        val (lookup, _) = lookup()
        assertEquals("u02_l1", lookup.lesson("u02_l1")?.id)
        assertNull(lookup.lesson("u09_l9"))
    }
}
