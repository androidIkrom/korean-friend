package uz.hangulfriend.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
import uz.hangulfriend.study.Rank
import uz.hangulfriend.srs.CardState

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupServiceTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val storeFile = File.createTempFile("settings", ".preferences_pb").also { it.delete() }
    private val settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = storeScope) { storeFile })
    private val backup = BackupService(db, settings, MutableClock())

    @After fun close() {
        db.close()
        storeScope.cancel()
        storeFile.delete()
    }

    private fun card(id: String) = CardEntity(
        id = id, itemId = id.substringBefore('#'), kind = CardKind.RECOGNIZE, lessonId = "u02_l1",
        origin = CardOrigin.LESSON, state = CardState.REVIEW, step = null, stability = 3.0, difficulty = 5.0,
        dueMs = 1000, lastReviewMs = 900, firstReviewedMs = 800, reps = 2, lapses = 0, lessonOrder = 2,
    )

    private suspend fun seed() {
        db.cards().insertIgnore(listOf(card("w1#R")))
        db.logs().insert(ReviewLogEntity(cardId = "w1#R", rating = 3, reviewedMs = 900, elapsedMs = 3000))
        db.progress().upsert(LessonProgressEntity("u02_l1", LessonStatus.COMPLETED, 4, 85))
        db.game().insertXp(XpEventEntity(amount = 10, reason = "answer", atMs = 900))
        db.game().insertAchievement(AchievementEntity("first_lesson", 950))
        db.game().upsertBest(BestScoreEntity("speed", 9))
        db.story().insertIgnore(StoryProgressEntity("u02_l1", 990))
        settings.setOnboarded()
        settings.setCurrentLesson("u02_l1")
        settings.setDailyGoalXp(80)
        settings.setReminder(true, 1230)
    }

    private fun emptyFile(): BackupFile = (BackupCodec.decode(
        """{"format": "${BackupCodec.FORMAT}", "version": ${BackupCodec.VERSION}, "exported_at_ms": 0, "db_version": 3,
           "settings": {"onboarded": false, "current_lesson_id": null, "daily_new_limit": 20, "daily_goal_xp": 50,
                        "reminder_enabled": false, "reminder_minutes": 1200},
           "cards": [], "review_logs": [], "lesson_progress": [], "xp_events": [], "achievements": [],
           "best_scores": [], "story_progress": []}""",
    ) as BackupResult.Ok).file

    @Test fun exportThenImportRestoresEverything() = runTest {
        seed()
        val exported = backup.export()
        val cardsBefore = db.cards().all()
        val settingsBefore = settings.settings.first()

        backup.import(emptyFile())
        assertTrue(db.cards().all().isEmpty())
        assertEquals(false, settings.settings.first().onboarded)

        backup.import((BackupCodec.decode(exported) as BackupResult.Ok).file)
        assertEquals(cardsBefore, db.cards().all())
        assertEquals(1, db.logs().forCard("w1#R").size)
        assertEquals(LessonStatus.COMPLETED, db.progress().get("u02_l1")?.status)
        assertEquals(1, db.game().countReason("answer"))
        assertEquals(listOf("first_lesson"), db.game().observeAchievements().first().map { it.id })
        assertEquals(9, db.game().best("speed"))
        assertEquals(listOf("u02_l1"), db.story().all().map { it.lessonId })
        assertEquals(settingsBefore, settings.settings.first())
    }

    @Test fun importSetsLastSeenRank() = runTest {
        // 4 500 XP is level 10, rank C.
        val file = emptyFile().copy(xpEvents = listOf(XpEventDto(1, 4500, "answer", 900)))
        backup.import(file)
        assertEquals(Rank.C, settings.lastSeenRank.first())
    }

    @Test fun exportCarriesTheme() = runTest {
        settings.setTheme(GameThemeId.NEON)
        assertEquals("neon", (BackupCodec.decode(backup.export()) as BackupResult.Ok).file.settings.theme)
    }

    @Test fun exportCarriesHero() = runTest {
        settings.setHero(HeroGender.GIRL)
        assertEquals("girl", (BackupCodec.decode(backup.export()) as BackupResult.Ok).file.settings.hero)
    }

    @Test fun exportThenImportRestoresUserWords() = runTest {
        db.userWords().insert(UserWordEntity(ko = "사과", uz = "olma", note = null, createdMs = 5))
        val exported = (BackupCodec.decode(backup.export()) as BackupResult.Ok).file
        backup.import(emptyFile())
        assertTrue(db.userWords().all().isEmpty())
        backup.import(exported)
        assertEquals(listOf("사과"), db.userWords().all().map { it.ko })
    }

    @Test fun importReplacesExistingRows() = runTest {
        seed()
        val exported = (BackupCodec.decode(backup.export()) as BackupResult.Ok).file
        db.cards().insertIgnore(listOf(card("w2#R")))
        backup.import(exported)
        assertEquals(listOf("w1#R"), db.cards().all().map { it.id })
    }
}
