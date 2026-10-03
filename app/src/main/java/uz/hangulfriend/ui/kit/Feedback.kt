package uz.hangulfriend.ui.kit

import androidx.compose.runtime.staticCompositionLocalOf

/** Short game sounds; each also has a haptic (see [FeedbackPolicy]). */
enum class Sfx { TAP, CORRECT, WRONG, COMBO, XP, CLEAR, RANK_UP, OPEN }

/** Plays a sound and a haptic for a game event, each when its setting is on. */
fun interface GameFeedback {
    fun play(sfx: Sfx)
}

/** Silent outside the activity (tests, previews). */
val LocalGameFeedback = staticCompositionLocalOf { GameFeedback { } }
