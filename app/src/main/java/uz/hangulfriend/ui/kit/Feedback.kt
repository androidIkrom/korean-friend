package uz.hangulfriend.ui.kit

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import uz.hangulfriend.R
import uz.hangulfriend.data.Settings

/** Short game sounds; each also has a haptic (see [FeedbackPolicy]). */
enum class Sfx { TAP, CORRECT, WRONG, COMBO, XP, CLEAR, RANK_UP, OPEN }

/** Plays a sound and a haptic for a game event, each when its setting is on. */
fun interface GameFeedback {
    fun play(sfx: Sfx)
}

/** Silent outside the activity (tests, previews). */
val LocalGameFeedback = staticCompositionLocalOf { GameFeedback { } }

enum class Haptic { TICK, CLICK, DOUBLE, HEAVY }

/** Which haptic each event gets, how loud its sound is, and what the settings allow. Pure, so it is unit-tested. */
object FeedbackPolicy {
    fun haptic(sfx: Sfx): Haptic = when (sfx) {
        Sfx.TAP, Sfx.OPEN -> Haptic.TICK
        Sfx.CORRECT, Sfx.XP, Sfx.COMBO -> Haptic.CLICK
        Sfx.WRONG -> Haptic.DOUBLE
        Sfx.CLEAR, Sfx.RANK_UP -> Haptic.HEAVY
    }

    /** Taps stay quiet; results and rewards are louder. */
    fun volume(sfx: Sfx): Float = if (sfx == Sfx.TAP || sfx == Sfx.OPEN) 0.45f else 0.85f

    fun sound(s: Settings): Boolean = s.soundOn

    fun vibrate(s: Settings): Boolean = s.hapticsOn

    /** Predefined effects exist from API 29; older phones get a short one-shot pulse. */
    fun usesPredefined(sdk: Int): Boolean = sdk >= Build.VERSION_CODES.Q

    /** Length of the fallback one-shot pulse per haptic. */
    fun fallbackMs(h: Haptic): Long = when (h) {
        Haptic.TICK -> 12
        Haptic.CLICK -> 20
        Haptic.DOUBLE -> 45
        Haptic.HEAVY -> 60
    }
}

private fun Sfx.raw(): Int = when (this) {
    Sfx.TAP -> R.raw.sfx_tap
    Sfx.CORRECT -> R.raw.sfx_correct
    Sfx.WRONG -> R.raw.sfx_wrong
    Sfx.COMBO -> R.raw.sfx_combo
    Sfx.XP -> R.raw.sfx_xp
    Sfx.CLEAR -> R.raw.sfx_clear
    Sfx.RANK_UP -> R.raw.sfx_rank_up
    Sfx.OPEN -> R.raw.sfx_open
}

/** SoundPool for the clips (loaded once) and the system vibrator, both gated by the live settings. */
class AndroidGameFeedback(context: Context, scope: CoroutineScope, settings: Flow<Settings>) : GameFeedback {
    private val current = settings.stateIn(scope, SharingStarted.Eagerly, Settings())
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
        )
        .build()
    private val ids: Map<Sfx, Int> = Sfx.entries.associateWith { pool.load(context, it.raw(), 1) }
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    override fun play(sfx: Sfx) {
        val s = current.value
        if (FeedbackPolicy.sound(s)) {
            val v = FeedbackPolicy.volume(sfx)
            // A clip still loading returns 0 and is simply skipped.
            ids[sfx]?.let { pool.play(it, v, v, 1, 0, 1f) }
        }
        if (FeedbackPolicy.vibrate(s)) vibrate(FeedbackPolicy.haptic(sfx))
    }

    private fun vibrate(h: Haptic) {
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = if (FeedbackPolicy.usesPredefined(Build.VERSION.SDK_INT)) {
            VibrationEffect.createPredefined(
                when (h) {
                    Haptic.TICK -> VibrationEffect.EFFECT_TICK
                    Haptic.CLICK -> VibrationEffect.EFFECT_CLICK
                    Haptic.DOUBLE -> VibrationEffect.EFFECT_DOUBLE_CLICK
                    Haptic.HEAVY -> VibrationEffect.EFFECT_HEAVY_CLICK
                },
            )
        } else {
            VibrationEffect.createOneShot(FeedbackPolicy.fallbackMs(h), VibrationEffect.DEFAULT_AMPLITUDE)
        }
        runCatching { v.vibrate(effect) }
    }

    fun release() = pool.release()
}
