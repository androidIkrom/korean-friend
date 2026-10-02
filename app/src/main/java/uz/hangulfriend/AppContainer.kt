package uz.hangulfriend

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import java.time.Clock
import kotlin.random.Random
import uz.hangulfriend.ai.GeminiClient
import uz.hangulfriend.ai.HttpGeminiTransport
import uz.hangulfriend.ai.TutorService
import uz.hangulfriend.content.AndroidAssetSource
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.data.AppDatabase
import uz.hangulfriend.data.BackupService
import uz.hangulfriend.data.GameRepository
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.data.StoryRepository
import uz.hangulfriend.data.LessonLookup
import uz.hangulfriend.data.StudyRepository
import uz.hangulfriend.data.UserWordRepository
import uz.hangulfriend.reminders.ReminderScheduler
import uz.hangulfriend.share.ProgressStats
import uz.hangulfriend.ui.avatar.AvatarAssets
import uz.hangulfriend.speech.SpeechInput
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
    val game = GameRepository(db, clock)
    val story = StoryRepository(db, game, clock)
    val backup = BackupService(db, settings, clock)
    val userWords = UserWordRepository(db, study, clock)
    val lessons = LessonLookup(content, userWords)
    val shareStats = ProgressStats(content, db, game, settings, clock)
    val avatarAssets by lazy { AvatarAssets.load(context.assets) }

    val reminders = ReminderScheduler(context)

    val speech = SpeechInput(context)
    val tutor = TutorService(
        GeminiClient(BuildConfig.GEMINI_KEYS.split(',').filter { it.isNotBlank() }, HttpGeminiTransport()),
    )
    val speechAvailable: Boolean get() = speech.isAvailable()
}
