package uz.hangulfriend

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import uz.hangulfriend.ui.avatar.AvatarAssets
import uz.hangulfriend.ui.avatar.LocalAvatarAssets
import uz.hangulfriend.ui.exercise.FlagReporter
import uz.hangulfriend.ui.exercise.LocalFlagReporter
import uz.hangulfriend.audio.AudioPlayer
import uz.hangulfriend.ui.LocalAudioPlayer
import uz.hangulfriend.ui.exercise.LocalSpeechInput
import uz.hangulfriend.ui.tutor.LocalTutor
import uz.hangulfriend.ui.HangulFriendNav
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.i18n.LanguageStore
import uz.hangulfriend.i18n.Localized
import uz.hangulfriend.ui.theme.GameBackground
import uz.hangulfriend.ui.kit.AndroidGameFeedback
import uz.hangulfriend.ui.kit.LocalGameFeedback
import uz.hangulfriend.ui.kit.LocalReducedMotion
import uz.hangulfriend.ui.kit.isReducedMotion
import androidx.lifecycle.lifecycleScope
import uz.hangulfriend.ui.theme.HangulFriendTheme
import uz.hangulfriend.ui.settings.LocalPlayerName

class MainActivity : ComponentActivity() {
    private val audio by lazy { AudioPlayer(this) }
    private val feedback by lazy {
        val container = (application as HangulFriendApp).container
        AndroidGameFeedback(applicationContext, lifecycleScope, container.settings.settings)
    }

    // The chosen app language, not the phone's, decides every string the activity shows.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Localized.wrap(newBase, LanguageStore(newBase).get()))
    }

    /** Starts over in a new task after a language switch, so no screen keeps text in the old language. */
    fun restartForLanguage() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Both game themes are dark, so system bar icons stay light even when the phone uses light mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val container = (application as HangulFriendApp).container
        // Only a fresh launch from the reminder opens the review, not a rotation of that launch.
        val openReview = savedInstanceState == null && intent.getStringExtra(EXTRA_OPEN) == OPEN_REVIEW
        setContent {
            val theme by remember { container.settings.settings.map { it.theme } }.collectAsState(GameThemeId.SYSTEM)
            val playerName by remember { container.settings.settings.map { it.playerName } }.collectAsState("")
            // Listing the avatar folder touches the disk, so it happens off the main thread.
            val avatarAssets by produceState(AvatarAssets(emptySet())) {
                value = withContext(Dispatchers.IO) { container.avatarAssets }
            }
            HangulFriendTheme(theme) {
                CompositionLocalProvider(
                    LocalAudioPlayer provides audio,
                    LocalAvatarAssets provides avatarAssets,
                    LocalFlagReporter provides FlagReporter { item, reason, comment -> container.flags.flag(item, reason, comment) },
                    LocalSpeechInput provides container.speech,
                    LocalTutor provides container.tutor,
                    LocalGameFeedback provides feedback,
                    LocalReducedMotion provides isReducedMotion(this),
                    LocalPlayerName provides playerName,
                ) {
                    // One shared backdrop behind every screen; screens that draw their own reuse it.
                    GameBackground { HangulFriendNav(container, openReview) }
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_REVIEW = "review"
    }

    override fun onDestroy() {
        audio.release()
        feedback.release()
        super.onDestroy()
    }
}
