package uz.hangulfriend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import uz.hangulfriend.audio.AudioPlayer
import uz.hangulfriend.ui.LocalAudioPlayer
import uz.hangulfriend.ui.exercise.LocalSpeechInput
import uz.hangulfriend.ui.tutor.LocalTutor
import uz.hangulfriend.ui.HangulFriendNav
import uz.hangulfriend.ui.theme.HangulFriendTheme

class MainActivity : ComponentActivity() {
    private val audio by lazy { AudioPlayer(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as HangulFriendApp).container
        // Only a fresh launch from the reminder opens the review, not a rotation of that launch.
        val openReview = savedInstanceState == null && intent.getStringExtra(EXTRA_OPEN) == OPEN_REVIEW
        setContent {
            HangulFriendTheme {
                CompositionLocalProvider(
                    LocalAudioPlayer provides audio,
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
