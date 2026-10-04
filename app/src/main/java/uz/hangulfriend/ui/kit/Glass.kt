package uz.hangulfriend.ui.kit

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.theme.tokensFor

/** The screen's blur source, provided by the game backdrop; null outside one (previews, tests). */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** Glass tint opacity: light over a real blur (API 31+), heavier without one so text keeps its contrast. */
fun glassTintAlpha(sdk: Int): Float = if (sdk >= 31) 0.55f else 0.78f

/**
 * Frosted glass in [shape]: the backdrop blurred behind it, tinted with the theme surface (or [tint]),
 * and a faint light along the top edge. Without a blur source it falls back to a plain tint.
 */
fun Modifier.glass(shape: Shape, tint: Color? = null): Modifier = composed {
    val t = LocalGameTokens.current
    val haze = LocalHazeState.current
    val base = tint ?: t.surface
    val sdk = Build.VERSION.SDK_INT
    val clipped = clip(shape)
    val fill = if (haze == null) {
        clipped.background(base.copy(alpha = glassTintAlpha(sdk)))
    } else {
        clipped.hazeBlur(
            HazeInput.Sources(haze),
            HazeBlurStyle {
                blurRadius(22.dp)
                noiseFactor(0.05f)
                colorEffects(listOf(HazeColorEffect.tint(base.copy(alpha = glassTintAlpha(31)))))
                fallbackColorEffect(HazeColorEffect.tint(base.copy(alpha = glassTintAlpha(30))))
            },
        )
    }
    fill.drawWithContent {
        drawContent()
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(
            outline,
            Brush.verticalGradient(0f to Color.White.copy(alpha = 0.14f), 0.4f to Color.Transparent),
            style = Stroke(1.dp.toPx()),
        )
    }
}

/** One colour blob of the backdrop mesh; positions and radius are fractions of the screen. */
data class MeshBlob(val x: Float, val y: Float, val radius: Float, val color: Color, val drift: Float)

/** The backdrop's blobs: the theme's two accents, large and soft, placed so the top stays brightest. */
fun meshBlobs(id: GameThemeId): List<MeshBlob> {
    val t = tokensFor(id)
    return listOf(
        MeshBlob(0.2f, 0.18f, 0.55f, t.accent2.copy(alpha = 0.32f), 1f),
        MeshBlob(0.85f, 0.42f, 0.5f, t.accent.copy(alpha = 0.38f), 0.7f),
        MeshBlob(0.35f, 0.92f, 0.45f, t.accent.copy(alpha = 0.18f), 1.3f),
    )
}
