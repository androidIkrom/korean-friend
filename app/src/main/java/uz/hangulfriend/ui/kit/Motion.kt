package uz.hangulfriend.ui.kit

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Durations (ms) shared by every animation, so the app moves with one rhythm. */
object GameMotion {
    const val TAP = 90
    const val FAST = 220
    const val PORTAL = 320
    const val BREATH = 3200
    const val PULSE = 2000
    const val SCAN = 3600
}

/** True when the phone's "Remove animations" is on (animator scale 0): loops stop and transitions are instant. */
fun isReducedMotion(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/** Provided once by the activity from [isReducedMotion]. */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Seconds since the caller appeared, advanced every frame and starting at [still]; stays at [still] with
 * reduced motion. Read it only in a draw pass, so the animation never recomposes.
 */
@Composable
fun frameClock(still: Float = 0f): MutableFloatState {
    val time = remember { mutableFloatStateOf(still) }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) withFrameNanos { time.floatValue = still + (it - start) / 1e9f }
    }
    return time
}

/** Slow grow-and-shrink, like breathing: for auras and idle heroes. */
fun Modifier.breathing(amount: Float = 0.04f): Modifier = composed {
    if (LocalReducedMotion.current) return@composed this
    val scale by rememberInfiniteTransition(label = "breath").animateFloat(
        1f, 1f + amount, infiniteRepeatable(tween(GameMotion.BREATH / 2), RepeatMode.Reverse), label = "breathScale",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** A ring that leaves [shape] and fades out every two seconds: "press me". */
fun Modifier.pulseRing(color: Color, shape: Shape): Modifier = composed {
    if (LocalReducedMotion.current) return@composed this
    val p by rememberInfiniteTransition(label = "pulse").animateFloat(
        0f, 1f, infiniteRepeatable(tween(GameMotion.PULSE, easing = LinearEasing)), label = "pulseP",
    )
    val maxGrow = with(LocalDensity.current) { 12.dp.toPx() }
    drawGrownOutline(shape, { p * maxGrow }, { color.copy(alpha = (1f - p) * 0.6f) }, 2.dp)
}

/** A soft band of light that sweeps down the component: the System "scanning" look. Put it after a `clip`. */
@Suppress("UNUSED_PARAMETER")
fun Modifier.scanLine(color: Color): Modifier = this
