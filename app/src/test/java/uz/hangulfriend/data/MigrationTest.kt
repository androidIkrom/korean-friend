package uz.hangulfriend.data

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {
    private val file = ApplicationProvider.getApplicationContext<Context>().getDatabasePath("mig.db")

    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        file,
        AndroidSQLiteDriver(),
        AppDatabase::class,
    )

    private fun SQLiteConnection.count(sql: String): Long = prepare(sql).use { it.step(); it.getLong(0) }

    @Test fun migrate1to2_keepsCards() {
        helper.createDatabase(1).apply {
            execSQL(
                "INSERT INTO cards (id, itemId, kind, lessonId, origin, state, step, stability, difficulty, dueMs, " +
                    "lastReviewMs, firstReviewedMs, reps, lapses, lessonOrder) VALUES " +
                    "('w#R', 'w', 'RECOGNIZE', 'u02_l1', 'LESSON', 'REVIEW', NULL, 3.0, 5.0, 1000, 900, 800, 2, 0, 2)",
            )
            execSQL("INSERT INTO lesson_progress (lessonId, status, stage, bestTestScore) VALUES ('u02_l1', 'COMPLETED', 4, 90)")
            close()
        }
        val db = helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2))
        assertEquals(1L, db.count("SELECT COUNT(*) FROM cards"))
        assertEquals(90L, db.count("SELECT bestTestScore FROM lesson_progress"))
        assertEquals(0L, db.count("SELECT COUNT(*) FROM xp_events"))
        db.close()
    }
}
