package uz.hangulfriend.ui

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.hangulfriend.MutableClock
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.content.DirAssetSource
import uz.hangulfriend.data.AppDatabase
import uz.hangulfriend.data.GameRepository
import uz.hangulfriend.data.HeroGender
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.data.StoryRepository
import uz.hangulfriend.data.StudyRepository
import uz.hangulfriend.share.ProgressStats
import uz.hangulfriend.srs.FsrsScheduler
import uz.hangulfriend.study.Rank
import uz.hangulfriend.ui.home.HomeViewModel

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HomeViewModelTest {
    private val main = UnconfinedTestDispatcher()
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val storeFile = File.createTempFile("settings", ".preferences_pb").also { it.delete() }
    private val settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = storeScope) { storeFile })
    private val clock = MutableClock()
    private val content = ContentRepository(DirAssetSource(File("src/main/assets")), strict = true)
    private val game = GameRepository(db, clock)

    @Before fun useTestMain() = Dispatchers.setMain(main)

    @After fun close() {
        Dispatchers.resetMain()
        db.close()
        storeScope.cancel()
        storeFile.delete()
    }

    private fun vm() = HomeViewModel(
        content,
        StudyRepository(db, FsrsScheduler(), clock),
        settings,
        game,
        ProgressRepository(db),
        StoryRepository(db, game, clock),
        ProgressStats(content, db, game, settings, clock),
        clock,
    )

    /** The screen must never draw the default hero and then swap it: the stats stay unknown until read. */
    @Test fun statsAreUnknownUntilRead_thenShowTheChosenHero() = runTest {
        settings.setHero(HeroGender.GIRL)
        val vm = vm()
        assertNotEquals(HeroGender.BOY, vm.stats.value?.hero)
        val s = vm.stats.filterNotNull().first()
        assertEquals(HeroGender.GIRL, s.hero)
        assertEquals(Rank.E, s.rank)
        assertEquals(1, s.level.level)
    }

    @Test fun statsArriveWithoutAnExplicitRefresh() = runTest {
        val s = vm().stats.filterNotNull().first()
        assertEquals(HeroGender.BOY, s.hero)
        assertEquals(0, s.todayXp)
    }
}
