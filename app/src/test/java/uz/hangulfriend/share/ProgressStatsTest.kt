package uz.hangulfriend.share

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.hangulfriend.MutableClock
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.content.DirAssetSource
import uz.hangulfriend.data.AppDatabase
import uz.hangulfriend.data.GameRepository
import uz.hangulfriend.data.LessonProgressEntity
import uz.hangulfriend.data.LessonStatus
import uz.hangulfriend.data.SettingsRepository

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressStatsTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val storeFile = File.createTempFile("settings", ".preferences_pb").also { it.delete() }
    private val settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = storeScope) { storeFile })
    private val clock = MutableClock()
    private val content = ContentRepository(DirAssetSource(File("src/main/assets")), strict = true)
    private val stats = ProgressStats(content, db, GameRepository(db, clock), settings, clock)

    @After fun close() {
        db.close()
        storeScope.cancel()
        storeFile.delete()
    }

    @Test fun emptyDatabaseGivesZeros() = runTest {
        val s = stats.snapshot()
        assertEquals(1, s.level)
        assertEquals(0, s.totalXp)
        assertEquals(0, s.streak)
        assertEquals(0, s.learnedWords)
        assertEquals(0, s.completedLessons)
        assertEquals(List(18) { LessonStatus.NOT_STARTED }, s.statuses)
        assertNull(s.currentLessonTitle)
    }

    @Test fun countsCompletedAndVerifiedOnly() = runTest {
        db.progress().upsert(LessonProgressEntity("u01_l1", LessonStatus.COMPLETED, 4, 90))
        db.progress().upsert(LessonProgressEntity("u01_l2", LessonStatus.VERIFIED, 0, null))
        db.progress().upsert(LessonProgressEntity("u02_l1", LessonStatus.PASSED, 0, null))
        val s = stats.snapshot()
        assertEquals(2, s.completedLessons)
        assertEquals(LessonStatus.PASSED, s.statuses[2])
    }

    @Test fun currentLessonTitleFormatted() = runTest {
        settings.setCurrentLesson("u02_l1")
        assertEquals("2-1 한번 입어 보세요", stats.snapshot().currentLessonTitle)
    }

    @Test fun gridShowsOnlyTheCurrentBook() = runTest {
        assertEquals(18, stats.snapshot().statuses.size)
        settings.setCurrentLesson("b1_u03_l1")
        assertEquals(emptyList<LessonStatus>(), stats.snapshot().statuses)
    }
}
