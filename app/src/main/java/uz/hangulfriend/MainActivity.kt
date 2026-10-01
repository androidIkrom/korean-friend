package uz.hangulfriend

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import uz.hangulfriend.ui.avatar.AvatarAssets
import uz.hangulfriend.ui.avatar.LocalAvatarAssets
import uz.hangulfriend.audio.AudioPlayer
import uz.hangulfriend.ui.LocalAudioPlayer
import uz.hangulfriend.ui.exercise.LocalSpeechInput
import uz.hangulfriend.ui.tutor.LocalTutor
import uz.hangulfriend.ui.HangulFriendNav
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.theme.HangulFriendTheme

class MainActivity : ComponentActivity() {
    private val audio by lazy { AudioPlayer(this) }

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
            val theme by container.settings.settings.map { it.theme }.collectAsState(GameThemeId.SYSTEM)
            // Listing the avatar folder touches the disk, so it happens off the main thread.
            val avatarAssets by produceState(AvatarAssets(emptySet())) {
                value = withContext(Dispatchers.IO) { container.avatarAssets }
            }
            HangulFriendTheme(theme) {
                CompositionLocalProvider(
                    LocalAudioPlayer provides audio,
                    LocalAvatarAssets provides avatarAssets,
                    LocalSpeechInput provides container.speech,
                    LocalTutor provides container.tutor,
                ) {
                    HangulFriendNav(container, openReview)
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
        super.onDestroy()
    }
}
