package uz.hangulfriend

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import java.time.Clock
import kotlin.random.Random
import uz.hangulfriend.content.AndroidAssetSource
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.data.AppDatabase
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.data.StudyRepository
import uz.hangulfriend.srs.FsrsScheduler
import uz.hangulfriend.study.Grader
import uz.hangulfriend.study.OnboardingService
import uz.hangulfriend.study.SessionBuilder

private val Context.settingsStore by preferencesDataStore(name = "settings")

/** Hand-wired dependencies; one instance per process, owned by [HangulFriendApp]. */
class AppContainer(context: Context) {
    val clock: Clock = Clock.systemDefaultZone()
    val content = ContentRepository(AndroidAssetSource(context.assets), strict = BuildConfig.DEBUG)
    val db = AppDatabase.open(context)
    val study = StudyRepository(db, FsrsScheduler(), clock)
    val progress = ProgressRepository(db)
    val settings = SettingsRepository(context.settingsStore)
    val onboarding = OnboardingService(content, study, progress, settings)
    val sessionBuilder = SessionBuilder(Random.Default)
    val grader = Grader(study)

    /** Wired to the speech recognizer in stage 2 Task 6. */
    val speechAvailable: Boolean get() = false
}
