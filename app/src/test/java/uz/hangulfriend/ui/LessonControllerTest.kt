package uz.hangulfriend.ui

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
import uz.hangulfriend.data.LessonStatus
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.StudyRepository
import uz.hangulfriend.srs.FsrsScheduler
import uz.hangulfriend.ui.lesson.FloorState
import uz.hangulfriend.ui.lesson.LessonController
import uz.hangulfriend.ui.lesson.floorStates

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LessonControllerTest {
    @get:Rule val tmp = TemporaryFolder()

    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val study = StudyRepository(db, FsrsScheduler(), MutableClock())
    private val progress = ProgressRepository(db)

    @After fun close() = db.close()

    private fun controller(id: String = "u02_l1"): LessonController {
        val assets = File(tmp.root, "assets").apply { File(this, "lessons").mkdirs() }
        File(assets, "lessons/u02_l1.json").writeText(ContentJson.encodeToString(Lesson.serializer(), Fixtures.validLesson()))
        File(assets, "book.json").writeText(
            """{"lessons":[
              {"id":"u01_l1","unit":1,"lesson":1,"title_ko":"가","title_uz":"a","topic_uz":"Oila"},
              {"id":"u02_l1","unit":2,"lesson":1,"title_ko":"다","title_uz":"c","topic_uz":"Xarid"}]}""",
        )
        File(assets, "characters.json").writeText("""{"characters":[{"id":"aziz","name_uz":"Aziz","name_ko":"아지즈","voice":"male"}]}""")
        return LessonController(id, ContentRepository(DirAssetSource(assets), strict = true), study, progress)
    }

    @Test fun open_createsLessonCardsAndSetsInProgress() = runTest {
        controller().load()
        val cards = db.cards().all()
        assertEquals(5, cards.size)
        assertTrue(cards.all { it.origin == CardOrigin.LESSON && it.lessonOrder == 1 })
        assertEquals(LessonStatus.IN_PROGRESS, progress.observeAll().first().getValue("u02_l1").status)
    }

    @Test fun open_loadsCharacters() = runTest {
        val c = controller()
        c.load()
        assertEquals("Aziz", c.state.value.characters.getValue("aziz").nameUz)
    }

    @Test fun goToStage_persistsAndResumes() = runTest {
        val first = controller()
        first.load()
        first.goToStage(3)
        val second = controller()
        second.load()
        assertEquals(3, second.state.value.stage)
    }

    @Test fun open_keepsCompletedStatus() = runTest {
        progress.recordTest("u02_l1", 95)
        controller().load()
        assertEquals(LessonStatus.COMPLETED, progress.observeAll().first().getValue("u02_l1").status)
    }

    @Test fun floorStatesFollowStage() {
        val c = FloorState.CLEARED
        val o = FloorState.OPEN
        assertEquals(listOf(FloorState.CURRENT, o, o, o, o), floorStates(0, LessonStatus.IN_PROGRESS))
        assertEquals(listOf(c, c, FloorState.CURRENT, o, o), floorStates(2, LessonStatus.IN_PROGRESS))
        // Going back: floors after the current one are not shown as cleared.
        assertEquals(listOf(c, FloorState.CURRENT, o, o, o), floorStates(1, LessonStatus.NOT_STARTED))
    }

    @Test fun completedLessonClearsAll() {
        val c = FloorState.CLEARED
        assertEquals(listOf(FloorState.CURRENT, c, c, c, c), floorStates(0, LessonStatus.COMPLETED))
        assertEquals(listOf(c, c, c, c, FloorState.CURRENT), floorStates(4, LessonStatus.VERIFIED))
    }

    @Test fun loadExposesStatus() = runTest {
        progress.recordTest("u02_l1", 95)
        val c = controller()
        c.load()
        assertEquals(LessonStatus.COMPLETED, c.state.value.status)
    }

    @Test fun refreshStatusAfterTest() = runTest {
        val c = controller()
        c.load()
        assertEquals(LessonStatus.IN_PROGRESS, c.state.value.status)
        progress.recordTest("u02_l1", 90)
        c.refreshStatus()
        assertEquals(LessonStatus.COMPLETED, c.state.value.status)
    }

    @Test fun open_missingLesson() = runTest {
        val c = controller("u05_l1")
        c.load()
        assertNull(c.state.value.lesson)
        assertTrue(!c.state.value.loading)
        assertTrue(db.cards().all().isEmpty())
    }
}
