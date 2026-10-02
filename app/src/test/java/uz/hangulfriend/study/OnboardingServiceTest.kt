package uz.hangulfriend.study

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
import uz.hangulfriend.data.AppDatabase
import uz.hangulfriend.data.CardOrigin
import uz.hangulfriend.data.HeroGender
import uz.hangulfriend.data.LessonStatus
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.data.StudyRepository
import uz.hangulfriend.srs.FsrsScheduler

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OnboardingServiceTest {
    @get:Rule val tmp = TemporaryFolder()

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val scope = TestScope(UnconfinedTestDispatcher())

    @After fun close() = db.close()

    private fun write(path: String, text: String) =
        File(tmp.root, "assets/$path").apply { parentFile.mkdirs() }.writeText(text)

    /** Catalog u01_l1 (has content), u01_l2 (no content), u02_l1 (has content). */
    private fun service(): Triple<OnboardingService, ProgressRepository, SettingsRepository> {
        write(
            "book.json",
            """{"lessons":[
              {"id":"u01_l1","unit":1,"lesson":1,"title_ko":"가","title_uz":"a","topic_uz":"Oila"},
              {"id":"u01_l2","unit":1,"lesson":2,"title_ko":"나","title_uz":"b","topic_uz":"Oila"},
              {"id":"u02_l1","unit":2,"lesson":1,"title_ko":"다","title_uz":"c","topic_uz":"Xarid"}]}""",
        )
        write("lessons/u01_l1.json", ContentJson.encodeToString(Lesson.serializer(), Fixtures.validLesson("u01_l1", 1, 1)))
        write("lessons/u02_l1.json", ContentJson.encodeToString(Lesson.serializer(), Fixtures.validLesson()))
        val content = ContentRepository(DirAssetSource(File(tmp.root, "assets")), strict = true)
        val progress = ProgressRepository(db)
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "settings.preferences_pb") },
        )
        val study = StudyRepository(db, FsrsScheduler(), MutableClock())
        return Triple(OnboardingService(content, study, progress, settings), progress, settings)
    }

    @Test fun onboarding_marksEarlierPassedAndCreatesBacklog() = runTest {
        val (onboarding, progress, settings) = service()
        onboarding.complete("u02_l1")
        val all = progress.observeAll().first()
        assertEquals(LessonStatus.PASSED, all.getValue("u01_l1").status)
        assertTrue("u02_l1" !in all)
        val cards = db.cards().all()
        assertEquals(5, cards.size)
        assertTrue(cards.all { it.lessonId == "u01_l1" && it.origin == CardOrigin.BACKLOG && it.lessonOrder == 0 })
        val s = settings.settings.first()
        assertEquals("u02_l1", s.currentLessonId)
        assertTrue(s.onboarded)
    }

    @Test fun onboarding_savesHero() = runTest {
        val (onboarding, _, settings) = service()
        onboarding.complete("u02_l1", HeroGender.GIRL)
        assertEquals(HeroGender.GIRL, settings.settings.first().hero)
    }

    @Test fun onboarding_withoutHeroKeepsIt() = runTest {
        val (onboarding, _, settings) = service()
        settings.setHero(HeroGender.GIRL)
        onboarding.complete("u02_l1")
        assertEquals(HeroGender.GIRL, settings.settings.first().hero)
    }

    @Test fun onboarding_skipsLessonsWithoutContent() = runTest {
        val (onboarding, progress, _) = service()
        onboarding.complete("u02_l1")
        assertEquals(LessonStatus.PASSED, progress.observeAll().first().getValue("u01_l2").status)
        assertTrue(db.cards().all().none { it.lessonId == "u01_l2" })
    }

    @Test fun onboarding_rerunDoesNotDuplicateCards() = runTest {
        val (onboarding, _, settings) = service()
        onboarding.complete("u02_l1")
        onboarding.complete("u01_l2")
        onboarding.complete("u02_l1")
        assertEquals(5, db.cards().all().size)
        assertEquals("u02_l1", settings.settings.first().currentLessonId)
    }

    @Test fun onboarding_firstLessonMarksNothing() = runTest {
        val (onboarding, progress, _) = service()
        onboarding.complete("u01_l1")
        assertTrue(progress.observeAll().first().isEmpty())
        assertTrue(db.cards().all().isEmpty())
    }
}
