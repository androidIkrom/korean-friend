package uz.hangulfriend.ui.avatar

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.study.Rank
import uz.hangulfriend.ui.theme.GameTokens
import uz.hangulfriend.ui.theme.LocalGameTokens

/** Fill colours of the vector avatar per theme (mockup palettes EVO_A / EVO_C). */
internal data class AvatarPalette(
    val coat: Color, val shirt: Color, val skin: Color, val shade: Color, val hair: Color, val hood: Color,
    val armor: Color, val dim: Color, val eye: Color, val eyeGlow: Color, val blade: Color, val glow: Color,
)

internal fun paletteFor(id: GameThemeId): AvatarPalette = when (id) {
    GameThemeId.SYSTEM -> AvatarPalette(
        coat = Color(0xFF070B16), shirt = Color(0xFF0E1628), skin = Color(0xFF18213A), shade = Color(0xFF10182C),
        hair = Color(0xFF04060E), hood = Color(0xFF111A2E), armor = Color(0xFF0B1222), dim = Color(0xFF2B3A55),
        eye = Color(0xFF7F93B5), eyeGlow = Color(0xFFAEE8FF), blade = Color(0xFF9FB3D1), glow = Color(0xFF2E8BFF),
    )
    GameThemeId.NEON -> AvatarPalette(
        coat = Color(0xFF140A24), shirt = Color(0xFF1D1036), skin = Color(0xFF2A1838), shade = Color(0xFF1F122B),
        hair = Color(0xFF0D0618), hood = Color(0xFF1F122B), armor = Color(0xFF1A0E30), dim = Color(0xFF4A3360),
        eye = Color(0xFF9B8FC0), eyeGlow = Color(0xFF2EE6FF), blade = Color(0xFF2EE6FF), glow = Color(0xFFFF3D9A),
    )
}

/** Parsed once; the paths are never mutated. */
internal val avatarPaths: Map<String, Path> by lazy {
    AvatarPaths.all.associate { (name, d) -> name to PathParser().parsePathString(d).toPath() }
}

internal const val VIEW_W = 240f
internal const val VIEW_H = 290f

/** Scales the 240×290 view box (origin −20, −10) into the canvas, centred, keeping the aspect ratio. */
internal fun DrawScope.inViewBox(block: DrawScope.() -> Unit) {
    val s = min(size.width / VIEW_W, size.height / VIEW_H)
    withTransform({
        translate((size.width - VIEW_W * s) / 2f, (size.height - VIEW_H * s) / 2f)
        scale(s, s, Offset.Zero)
        translate(20f, 10f)
    }, block)
}

internal fun DrawScope.part(name: String, fill: Color?, line: Color?, width: Float, cap: StrokeCap = StrokeCap.Butt) {
    val p = avatarPaths.getValue(name)
    if (fill != null) drawPath(p, fill)
    if (line != null) drawPath(p, line, style = Stroke(width, cap = cap, join = StrokeJoin.Round))
}

/** Aziz at [rank]; a drawn image from `assets/avatar` replaces the vector when present. */
@Composable
fun Avatar(rank: Rank, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    val slot = LocalAvatarAssets.current.slotFor(t.id, rank)
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, slot) {
        value = slot?.let {
            withContext(Dispatchers.IO) {
                runCatching { context.assets.open(it).use { s -> BitmapFactory.decodeStream(s)?.asImageBitmap() } }.getOrNull()
            }
        }
    }
    val bitmap = image
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, modifier = modifier, contentScale = ContentScale.Fit)
    } else {
        val layers = layersFor(rank)
        Canvas(modifier) { inViewBox { drawAvatar(rank, layers, t, paletteFor(t.id)) } }
    }
}

private fun DrawScope.drawAvatar(rank: Rank, layers: Set<AvatarLayer>, t: GameTokens, p: AvatarPalette) {
    val s = rank == Rank.S
    val line = when {
        rank == Rank.E -> p.dim
        s -> t.top
        else -> t.accent
    }
    val sw = 1.2f + 0.15f * rank.ordinal
    val glow = if (s) t.top else p.glow

    val auraAlpha = when {
        AvatarLayer.MONARCH_AURA in layers -> 0.8f
        AvatarLayer.AURA_3 in layers -> 0.58f
        AvatarLayer.AURA_2 in layers -> 0.42f
        AvatarLayer.AURA_1 in layers -> 0.28f
        else -> 0f
    }
    if (auraAlpha > 0f) {
        drawOval(
            Brush.radialGradient(listOf(glow.copy(alpha = auraAlpha), Color.Transparent), center = Offset(100f, 145f), radius = 125f),
            topLeft = Offset(-12f, 8f),
            size = Size(224f, 264f),
        )
    }
    if (AvatarLayer.PARTICLES in layers) {
        listOf(24f to 60f, 176f to 74f, 12f to 150f, 190f to 136f, 40f to 24f, 160f to 30f).forEach { (x, y) ->
            drawCircle(glow.copy(alpha = 0.8f), radius = if (s) 3f else 2f, center = Offset(x, y))
        }
    }
    if (AvatarLayer.COMPANIONS in layers) {
        if (t.id == GameThemeId.SYSTEM) {
            part("CROWN", t.top.copy(alpha = 0.35f), null, 0f)
            listOf(-6f, 168f).forEach { x ->
                translate(x, 0f) {
                    part("SOLDIER", Color(0xFF05070F), t.top, 1f)
                    drawCircle(t.top, 1.6f, Offset(16f, 190f))
                    drawCircle(t.top, 1.6f, Offset(22f, 190f))
                }
            }
        } else {
            drawOval(t.top, topLeft = Offset(46f, 18f), size = Size(108f, 24f), style = Stroke(3f))
            listOf(16f to 120f, 184f to 100f).forEach { (x, y) ->
                translate(x, y) {
                    part("DRONE", Color(0xFF0D0618), p.eyeGlow, 2f)
                    drawCircle(p.eyeGlow, 2.5f, Offset.Zero)
                }
            }
        }
    }
    if (AvatarLayer.CAPE in layers) part("CAPE", Color(0xFF03040A), line, 1f)
    if (AvatarLayer.BLADES in layers) {
        part("BLADE_L", p.blade, null, 0f)
        part("BLADE_R", p.blade, null, 0f)
        part("HILT_L", null, line, 4f, StrokeCap.Round)
        part("HILT_R", null, line, 4f, StrokeCap.Round)
    }
    if (AvatarLayer.HOOD in layers) part("HOOD", p.hood, line, sw)
    part("BODY", p.coat, line, sw)
    if (AvatarLayer.LONG_COAT in layers) {
        part("COLLAR_L", p.coat, line, sw)
        part("COLLAR_R", p.coat, line, sw)
    }
    if (AvatarLayer.PAULDRONS in layers) {
        part("PAULD_L", p.armor, line, sw)
        part("PAULD_R", p.armor, line, sw)
    }
    part("NECK", p.shade, null, 0f)
    if (AvatarLayer.HOOD in layers) {
        part("HOODIE_NECK", null, line, sw)
        part("STRINGS", null, line, 1.6f)
    } else {
        part("SHIRT", p.shirt, line, sw)
    }
    part("FACE", p.skin, line, sw)
    part("HAIR", p.hair, line, sw)
    if (AvatarLayer.HAIR_LIGHT in layers) part("HAIR_LIGHT", null, line, 2f, StrokeCap.Round)
    if (AvatarLayer.EYE_GLOW in layers) {
        drawOval(p.eyeGlow.copy(alpha = 0.35f), topLeft = Offset(73f, 98f), size = Size(26f, 10f))
        drawOval(p.eyeGlow.copy(alpha = 0.35f), topLeft = Offset(101f, 98f), size = Size(26f, 10f))
    }
    val eye = when {
        s -> t.top
        AvatarLayer.EYE_GLOW in layers -> p.eyeGlow
        else -> p.eye
    }
    part("EYE_L", eye, null, 0f)
    part("EYE_R", eye, null, 0f)
    if (AvatarLayer.EYE_TRAIL in layers) part("EYE_TRAIL", null, t.top.copy(alpha = 0.9f), 2.5f, StrokeCap.Round)
    part("MOUTH", null, Color(0xFF34507A), 1.8f, StrokeCap.Round)
}
