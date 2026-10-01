package uz.hangulfriend.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProgressRepositoryTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val progress = ProgressRepository(db)

    @After fun close() = db.close()

    private suspend fun status(id: String) = progress.observeAll().first()[id]?.status

    @Test fun setStage_movesToInProgress() = runTest {
        progress.setStage("u02_l1", 2)
        val p = progress.observeAll().first().getValue("u02_l1")
        assertEquals(LessonStatus.IN_PROGRESS, p.status)
        assertEquals(2, p.stage)
    }

    @Test fun setStage_keepsCompleted() = runTest {
        progress.recordTest("u02_l1", 90)
        progress.setStage("u02_l1", 1)
        assertEquals(LessonStatus.COMPLETED, status("u02_l1"))
    }

    @Test fun recordTest_80CompletesBelowDoesNot() = runTest {
        progress.recordTest("u02_l1", 79)
        assertEquals(LessonStatus.IN_PROGRESS, status("u02_l1"))
        progress.recordTest("u02_l1", 80)
        assertEquals(LessonStatus.COMPLETED, status("u02_l1"))
    }

    @Test fun recordTest_keepsBestScore() = runTest {
        progress.recordTest("u02_l1", 90)
        progress.recordTest("u02_l1", 60)
        assertEquals(90, progress.observeAll().first().getValue("u02_l1").bestTestScore)
    }

    @Test fun recordTest_neverDowngradesVerified() = runTest {
        db.progress().upsert(LessonProgressEntity("u02_l1", LessonStatus.VERIFIED, stage = 4, bestTestScore = 95))
        progress.recordTest("u02_l1", 85)
        assertEquals(LessonStatus.VERIFIED, status("u02_l1"))
    }

    @Test fun lessonCompleteXpOnlyFirstTime() = runTest {
        assertEquals(false, progress.recordTest("u02_l1", 70))
        assertEquals(true, progress.recordTest("u02_l1", 85))
        assertEquals(false, progress.recordTest("u02_l1", 95))
    }

    @Test fun recordQuickCheck_verifiesPassed() = runTest {
        progress.markPassed(listOf("u01_l1", "u01_l2"))
        assertEquals(false, progress.recordQuickCheck("u01_l1", 70))
        assertEquals(LessonStatus.PASSED, status("u01_l1"))
        assertEquals(true, progress.recordQuickCheck("u01_l2", 80))
        assertEquals(LessonStatus.VERIFIED, status("u01_l2"))
    }

    @Test fun markPassed_onlyNotStarted() = runTest {
        progress.recordTest("u01_l2", 100)
        progress.markPassed(listOf("u01_l1", "u01_l2"))
        assertEquals(LessonStatus.PASSED, status("u01_l1"))
        assertEquals(LessonStatus.COMPLETED, status("u01_l2"))
    }
}
