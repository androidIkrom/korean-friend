package uz.hangulfriend.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.hangulfriend.study.Rank

class SettingsRepositoryTest {
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val storeFile = File.createTempFile("settings", ".preferences_pb").also { it.delete() }
    private val store = PreferenceDataStoreFactory.create(scope = storeScope) { storeFile }
    private val settings = SettingsRepository(store)

    @After fun close() {
        storeScope.cancel()
        storeFile.delete()
    }

    @Test fun themeDefaultsToSystem() = runTest { assertEquals(GameThemeId.SYSTEM, settings.settings.first().theme) }

    @Test fun setThemePersists() = runTest {
        settings.setTheme(GameThemeId.NEON)
        assertEquals(GameThemeId.NEON, settings.settings.first().theme)
    }

    @Test fun unknownThemeFallsBackToSystem() = runTest {
        store.edit { it[stringPreferencesKey("ui_theme")] = "pink" }
        assertEquals(GameThemeId.SYSTEM, settings.settings.first().theme)
    }

    @Test fun lastSeenRankRoundTrip() = runTest {
        assertNull(settings.lastSeenRank.first())
        settings.setLastSeenRank(Rank.B)
        assertEquals(Rank.B, settings.lastSeenRank.first())
    }

    @Test fun unknownRankIsNull() = runTest {
        store.edit { it[stringPreferencesKey("last_seen_rank")] = "Z" }
        assertNull(settings.lastSeenRank.first())
    }

    @Test fun heroDefaultsToBoy() = runTest { assertEquals(HeroGender.BOY, settings.settings.first().hero) }

    @Test fun setHeroPersists() = runTest {
        settings.setHero(HeroGender.GIRL)
        assertEquals(HeroGender.GIRL, settings.settings.first().hero)
    }

    @Test fun unknownHeroIsBoy() = runTest {
        store.edit { it[stringPreferencesKey("hero")] = "robot" }
        assertEquals(HeroGender.BOY, settings.settings.first().hero)
    }

    @Test fun replaceAllWritesHero() = runTest {
        settings.replaceAll(Settings(hero = HeroGender.GIRL))
        assertEquals(HeroGender.GIRL, settings.settings.first().hero)
    }

    @Test fun replaceAllWritesTheme() = runTest {
        settings.replaceAll(Settings(theme = GameThemeId.NEON))
        assertEquals(GameThemeId.NEON, settings.settings.first().theme)
    }
}
