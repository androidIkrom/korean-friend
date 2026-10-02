package uz.hangulfriend.ui.avatar

import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.data.HeroGender
import uz.hangulfriend.study.Rank
import uz.hangulfriend.ui.theme.GameTokens
import uz.hangulfriend.ui.theme.LocalGameTokens

/** Fill colours of the vector avatar per theme (mockup palettes). */
internal data class AvatarPalette(
    val coat: Color, val shirt: Color, val skin: Color, val shade: Color, val hair: Color, val hood: Color,
    val armor: Color, val dim: Color, val eye: Color, val eyeGlow: Color, val blade: Color, val glow: Color,
)

internal fun paletteFor(id: GameThemeId): AvatarPalette = when (id) {
    GameThemeId.SYSTEM -> AvatarPalette(
        coat = Color(0xFF070B16), shirt = Color(0xFF0E1628), skin = Color(0xFF1A2440), shade = Color(0xFF10182C),
        hair = Color(0xFF04060E), hood = Color(0xFF111A2E), armor = Color(0xFF0B1222), dim = Color(0xFF2B3A55),
        eye = Color(0xFF7F93B5), eyeGlow = Color(0xFFAEE8FF), blade = Color(0xFF9FB3D1), glow = Color(0xFF2E8BFF),
    )
    GameThemeId.NEON -> AvatarPalette(
        coat = Color(0xFF140A24), shirt = Color(0xFF1D1036), skin = Color(0xFF2A1838), shade = Color(0xFF1F122B),
        hair = Color(0xFF0D0618), hood = Color(0xFF1F122B), armor = Color(0xFF1A0E30), dim = Color(0xFF4A3360),
        eye = Color(0xFF9B8FC0), eyeGlow = Color(0xFF2EE6FF), blade = Color(0xFF2EE6FF), glow = Color(0xFFFF3D9A),
    )
}

private val Sclera = Color(0xFFC9D4E6)
private val Iris = Color(0xFF22304D)
private val HotCore = Color(0xFFE9DDFF)
private val ColdCore = Color(0xFFAEE8FF)
private val MouthColor = Color(0xFF5A6F95)

/** Parsed once; the paths are never mutated. */
internal val avatarPaths: Map<String, Path> by lazy {
    AvatarPaths.all.associate { (name, d) -> name to PathParser().parsePathString(d).toPath() }
}

private val parsedCache = HashMap<String, Path>()

/** Paths that are not named constants (eye styles) are parsed on first use and cached. */
internal fun pathOf(d: String): Path = synchronized(parsedCache) {
    parsedCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }
}

internal const val VIEW_W = 248f
internal const val VIEW_H = 298f

/** Scales the 248×298 view box (origin −24, −14) into the canvas, centred, keeping the aspect ratio. */
internal fun DrawScope.inViewBox(block: DrawScope.() -> Unit) {
    val s = min(size.width / VIEW_W, size.height / VIEW_H)
    withTransform({
        translate((size.width - VIEW_W * s) / 2f, (size.height - VIEW_H * s) / 2f)
        scale(s, s, Offset.Zero)
        translate(24f, 14f)
    }, block)
}

internal fun DrawScope.part(name: String, fill: Color?, line: Color?, width: Float, cap: StrokeCap = StrokeCap.Butt) {
    drawShape(avatarPaths.getValue(name), fill, line, width, cap)
}

private fun DrawScope.drawShape(p: Path, fill: Color?, line: Color?, width: Float, cap: StrokeCap = StrokeCap.Butt) {
    if (fill != null) drawPath(p, fill)
    if (line != null) drawPath(p, line, style = Stroke(width, cap = cap, join = StrokeJoin.Round))
}

/** Aura strength per rank: none below C, full at S. */
private fun auraStrength(rank: Rank): Float = when (rank) {
    Rank.E, Rank.D -> 0f
    Rank.C -> 0.3f
    Rank.B -> 0.55f
    Rank.A -> 0.8f
    Rank.S -> 1f
}

/** Animation phases read in the draw pass only, so the aura flickers without recomposing. */
private class AuraMotion(val outer: State<Float>, val inner: State<Float>, val rise: State<Float>)

/**
 * The player's hero at [rank]. With [animated] the flame aura flickers and embers rise (Home, rank-up dialog);
 * elsewhere it is still. A drawn image from `assets/avatar` replaces the vector when present.
 */
@Composable
fun Avatar(rank: Rank, hero: HeroGender, modifier: Modifier = Modifier, animated: Boolean = false) {
    val t = LocalGameTokens.current
    val slot = LocalAvatarAssets.current.slotFor(t.id, hero, rank)
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
        return
    }
    val motion = if (animated) {
        val transition = rememberInfiniteTransition(label = "aura")
        AuraMotion(
            transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "outer"),
            transition.animateFloat(1f, 0f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "inner"),
            transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "rise"),
        )
    } else {
        remember { AuraMotion(mutableFloatStateOf(0.5f), mutableFloatStateOf(0.5f), mutableFloatStateOf(0.4f)) }
    }
    val k = auraStrength(rank)
    val outer = remember(k) { if (k > 0f) flamePath(k, 1f, 0) else null }
    val inner = remember(k) { if (k > 0f) flamePath(k * 0.8f, 0.86f, 3) else null }
    val layers = remember(rank, hero) { layersFor(rank, hero) }
    Canvas(modifier) {
        inViewBox { drawHero(rank, hero, layers, t, paletteFor(t.id), outer, inner, motion) }
    }
}

private fun DrawScope.drawHero(
    rank: Rank,
    hero: HeroGender,
    layers: Set<AvatarLayer>,
    t: GameTokens,
    p: AvatarPalette,
    outerFlame: Path?,
    innerFlame: Path?,
    motion: AuraMotion,
) {
    val girl = hero == HeroGender.GIRL
    val s = rank == Rank.S
    val line = when {
        rank == Rank.E -> p.dim
        s -> t.top
        else -> t.accent
    }
    val sw = 1.2f + 0.15f * rank.ordinal
    val auraColor = if (s) t.top else t.accent
    val k = auraStrength(rank)

    if (outerFlame != null && innerFlame != null) {
        drawOval(
            Brush.radialGradient(listOf(auraColor.copy(alpha = 0.55f), Color.Transparent), center = Offset(100f, 150f), radius = 130f + 30f * k),
            topLeft = Offset(100f - (100f + 30f * k), 150f - (128f + 30f * k)),
            size = Size(2 * (100f + 30f * k), 2 * (128f + 30f * k)),
            alpha = 0.5f + 0.5f * k,
        )
        val pivot = Offset(100f, 282f)
        val o = motion.outer.value
        withTransform({
            scale(1f, 0.95f + 0.1f * o, pivot)
            rotate(-1.5f + 3f * o, pivot)
        }) {
            blurred(outerFlame, auraColor.copy(alpha = 0.6f), 6f)
            drawPath(
                outerFlame,
                Brush.verticalGradient(listOf(Color(0x33050310), Color(0xF2050310)), startY = 0f, endY = 282f),
            )
            drawPath(outerFlame, auraColor, style = Stroke(1.6f, join = StrokeJoin.Round))
        }
        val i = motion.inner.value
        withTransform({
            scale(1f, 0.95f + 0.1f * i, pivot)
            rotate(1.5f - 3f * i, pivot)
        }) {
            drawPath(innerFlame, Color(0xD905030C))
            drawPath(innerFlame, auraColor.copy(alpha = 0.7f), style = Stroke(1f, join = StrokeJoin.Round))
        }
    }
    if (AvatarLayer.EMBERS in layers) {
        val r = motion.rise.value
        emberCenters(k).forEachIndexed { j, c ->
            val phase = (r + j * 0.17f) % 1f
            translate(c.x, c.y + 12f - 40f * phase) {
                drawPath(avatarPaths.getValue("EMBER"), auraColor, alpha = 0.75f * (1f - phase))
            }
        }
    }
    if (AvatarLayer.COMPANIONS in layers) {
        if (t.id == GameThemeId.SYSTEM) {
            listOf(-6f, 168f).forEach { x ->
                translate(x, 0f) {
                    part("SOLDIER", Color(0xFF05070F), t.top, 1f)
                    drawCircle(t.top, 1.8f, Offset(16f, 190f))
                    drawCircle(t.top, 1.8f, Offset(22f, 190f))
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

    // Body, with a soft rim of aura light behind its silhouette from D up.
    val bodyName = if (girl) "MINJI_BODY" else "BODY"
    val faceName = if (girl) "MINJI_FACE" else "FACE"
    if (AvatarLayer.RIM in layers) {
        listOf(bodyName, faceName, if (girl) "MINJI_BANGS" else "HAIR").forEach { blurred(avatarPaths.getValue(it), auraColor.copy(alpha = 0.8f), 3f) }
    }
    if (AvatarLayer.CAPE in layers) part("CAPE", Color(0xFF03040A), line, 1f)
    if (AvatarLayer.TWIN_BLADES in layers) {
        part("BLADE_L", p.blade, null, 0f)
        part("BLADE_R", p.blade, null, 0f)
        part("HILT_L", null, line, 4f, StrokeCap.Round)
        part("HILT_R", null, line, 4f, StrokeCap.Round)
    }
    if (AvatarLayer.SWORD in layers) {
        part("SWORD", p.blade, null, 0f)
        part("SWORD_HILT", null, line, 5f, StrokeCap.Round)
        part("SWORD_GUARD", null, line, 3f, StrokeCap.Round)
    }
    if (AvatarLayer.PONYTAIL in layers) {
        part("PONYTAIL", p.hair, line, sw)
        part("GIRL_TIED", p.hair, line, sw)
    }
    if (AvatarLayer.LONG_HAIR in layers) part("MINJI_BACK", p.hair, line, sw)
    // The hood lies on the shoulders for both heroes: a hood up behind the head read as long hair on the boy.
    if (AvatarLayer.HOOD in layers) part("GIRL_HOOD", p.hood, line, sw)
    part(bodyName, p.coat, line, sw)
    if (AvatarLayer.LONG_COAT in layers) {
        part("COLLAR_L", p.coat, line, sw)
        part("COLLAR_R", p.coat, line, sw)
    }
    if (AvatarLayer.PAULDRONS in layers) {
        part("PAULD_L", p.armor, line, sw)
        part("PAULD_R", p.armor, line, sw)
    }
    part("NECK", p.shade, null, 0f)
    if (AvatarLayer.HOOD in layers && !girl) {
        part("HOODIE_NECK", null, line, sw)
        part("STRINGS", null, line, 1.6f)
    } else {
        part("SHIRT", p.shirt, line, sw)
    }
    part(faceName, p.skin, line, sw)
    if (girl) {
        val tied = AvatarLayer.PONYTAIL in layers
        part(if (tied) "GIRL_LOCK_SHORT_L" else "MINJI_LOCK_L", p.hair, line, sw)
        part(if (tied) "GIRL_LOCK_SHORT_R" else "MINJI_LOCK_R", p.hair, line, sw)
        part("MINJI_BANGS", p.hair, line, sw)
        if (tied) part("GIRL_BAND", t.accent2, null, 0f)
        if (AvatarLayer.TIARA in layers) {
            blurred(avatarPaths.getValue("TIARA"), t.top, 2f, stroke = 3f)
            part("TIARA", null, t.top, 2.2f, StrokeCap.Round)
        }
    } else {
        part("HAIR", p.hair, line, sw)
        if (AvatarLayer.HAIR_LIGHT in layers) part("HAIR_LIGHT", null, line, 2f, StrokeCap.Round)
    }
    drawEyes(eyeStyleFor(rank), girl, t, p, plainBrow = if (rank == Rank.E) p.dim else t.accent)
}

private data class BlurKey(val argb: Int, val radius: Float, val stroke: Float?)

/** Blur paints are reused across frames: the aura animates every frame and would otherwise allocate each time. */
private val blurPaints = HashMap<BlurKey, android.graphics.Paint>()

private fun blurPaint(key: BlurKey): android.graphics.Paint = synchronized(blurPaints) {
    blurPaints.getOrPut(key) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = key.argb
            maskFilter = BlurMaskFilter(key.radius, BlurMaskFilter.Blur.NORMAL)
            if (key.stroke != null) {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = key.stroke
            }
        }
    }
}

/** Draws [path] blurred in [color] behind what follows (unblurred below API 28, which is accepted). */
private fun DrawScope.blurred(path: Path, color: Color, radius: Float, stroke: Float? = null) {
    val paint = blurPaint(BlurKey(color.toArgb(), radius, stroke))
    drawIntoCanvas { it.nativeCanvas.drawPath(path.asAndroidPath(), paint) }
}

private fun DrawScope.drawEyes(e: EyeStyle, girl: Boolean, t: GameTokens, p: AvatarPalette, plainBrow: Color) {
    val glow = if (e.monarch) t.top else p.eyeGlow
    val dx = if (girl) 3f else 0f
    val dy = if (girl) 2f else 0f
    translate(dx, dy) {
        if (e.flameTrail) {
            drawPath(
                pathOf(EYE_TRAIL_FILL),
                Brush.linearGradient(listOf(HotCore, t.top, t.top.copy(alpha = 0f)), start = Offset(73f, 103f), end = Offset(34f, 80f)),
            )
            drawPath(pathOf(EYE_TRAIL_LINE), HotCore.copy(alpha = 0.9f), style = Stroke(1.2f, cap = StrokeCap.Round))
        }
        oneEye(e, girl, glow, plainBrow)
        scale(-1f, 1f, Offset(100f, 0f)) { oneEye(e, girl, glow, plainBrow) }
    }
    translate(if (girl) 1f else 0f, dy) {
        drawPath(pathOf(e.mouth), MouthColor, style = Stroke(1.8f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.oneEye(e: EyeStyle, girl: Boolean, glow: Color, plainBrow: Color) {
    if (!e.glowing) {
        drawPath(pathOf(e.lid), Sclera.copy(alpha = 0.85f))
        drawCircle(Iris, 3.6f, Offset(85f, 103.5f))
        drawCircle(Color.White, 1f, Offset(86.3f, 102.3f))
        drawPath(pathOf(e.brow), plainBrow, style = Stroke(e.browWidth, cap = StrokeCap.Round))
        if (girl) drawPath(pathOf(upperLid(e.lid)), Color.Black, style = Stroke(1.6f, cap = StrokeCap.Round))
        return
    }
    drawOval(
        Brush.radialGradient(listOf(glow.copy(alpha = e.bloomAlpha), Color.Transparent), center = Offset(85f, 103f), radius = e.bloomRadius),
        topLeft = Offset(85f - e.bloomRadius, 103f - e.bloomRadius * 0.55f),
        size = Size(e.bloomRadius * 2, e.bloomRadius * 1.1f),
    )
    drawPath(pathOf(e.lid), glow)
    drawOval(if (e.hotCore) HotCore else ColdCore, topLeft = Offset(82.3f, 100.4f), size = Size(6.4f, 4.4f))
    drawPath(pathOf(e.brow), glow, style = Stroke(e.browWidth, cap = StrokeCap.Round))
    if (girl) drawPath(pathOf(GIRL_LASH_GLOW), Color.Black, style = Stroke(1.8f, cap = StrokeCap.Round))
    if (e.streak) drawPath(pathOf(EYE_STREAK), glow.copy(alpha = 0.85f), style = Stroke(2.4f, cap = StrokeCap.Round))
}
