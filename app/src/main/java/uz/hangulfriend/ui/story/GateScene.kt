package uz.hangulfriend.ui.story

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import uz.hangulfriend.R
import uz.hangulfriend.ui.kit.LocalReducedMotion
import uz.hangulfriend.ui.theme.GameTokens
import uz.hangulfriend.ui.theme.LocalGameTokens

private val Cinzel = FontFamily(Font(R.font.cinzel_black, FontWeight.Black))

/** Seconds since the scene appeared, advanced every frame; stays 0 with reduced motion. */
@Composable
private fun sceneClock(): MutableFloatState {
    val time = remember { mutableFloatStateOf(STILL_TIME) }
    val reduced = LocalReducedMotion.current
    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) withFrameNanos { time.floatValue = STILL_TIME + (it - start) / 1e9f }
    }
    return time
}

/**
 * The dungeon gate behind an episode's speaker: nebula sky, skyline towers, a glowing gate with runes and a
 * light beam, energy bands, floating rocks and an aura-like magic circle. [seed] picks the scene's layout.
 * Only the draw pass reads the clock, so animation never recomposes.
 */
@Composable
fun GateScene(seed: Int, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    val layout = remember(seed) { gateLayout(seed) }
    val clock = sceneClock()
    // Offscreen so the edge mask at the end of the draw fades the whole scene, not what lies behind it.
    Canvas(modifier.clipToBounds().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val time = clock.floatValue
        drawGate(t, layout, time)
    }
}

/** The place name over the gate, glowing in the theme's second accent. */
@Composable
fun GateTitle(text: String, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    val fontSize = when {
        text.length <= 5 -> 40.sp
        text.length <= 7 -> 34.sp
        else -> 28.sp
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text,
            style = TextStyle(
                fontFamily = Cinzel,
                fontWeight = FontWeight.Black,
                fontSize = fontSize,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                brush = Brush.verticalGradient(listOf(Color.White, t.accent2)),
                shadow = Shadow(t.accent2, blurRadius = 28f),
            ),
        )
        Canvas(Modifier.width(150.dp).height(10.dp)) {
            val y = size.height / 2
            drawLine(t.accent2.copy(alpha = 0.8f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            drawStar(Offset(size.width / 2, y), size.height * 0.9f, t.text.copy(alpha = 0.9f), t.accent2)
        }
    }
}

private fun DrawScope.drawGate(t: GameTokens, l: GateLayout, time: Float) {
    val w = size.width
    val h = size.height
    val stone = lerp(t.background, t.accent2, 0.09f)
    val sky = lerp(t.background, t.accent2, 0.12f)
    val cx = w / 2
    val floor = h * 0.9f

    // Sky: fades in from the top so the scene has no hard upper edge.
    drawRect(Brush.verticalGradient(0f to sky.copy(alpha = 0.6f), 0.5f to sky, 1f to t.background))
    repeat(5) { i ->
        val nx = w * (0.15f + 0.7f * ((i * 0.37f) % 1f)) + sin(time * 0.12f + i) * w * 0.04f
        val ny = h * (0.2f + 0.5f * ((i * 0.61f) % 1f))
        val c = if (i % 2 == 0) t.accent2 else t.accent
        drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.16f), Color.Transparent), Offset(nx, ny), w * 0.38f), w * 0.38f, Offset(nx, ny))
    }

    // Skyline towers.
    for (tw in l.towers) {
        val x = tw.x * w
        val bw = tw.w * w
        val top = floor - tw.h * h
        val path = Path().apply {
            moveTo(x, floor)
            lineTo(x, top)
            lineTo(x + bw / 2, top - tw.spire * h)
            lineTo(x + bw, top)
            lineTo(x + bw, floor)
        }
        drawPath(path, stone.copy(alpha = 0.75f + tw.shade * 0.25f))
        drawPath(path, t.accent2.copy(alpha = 0.3f), style = Stroke(1.dp.toPx()))
    }

    // Gate: a pointed arch with a spire, portal light inside, a beam down the middle.
    val gw = w * 0.36f
    val gh = h * 0.62f
    val gx = cx - gw / 2
    val gTop = floor - gh
    fun arch(inset: Float) = Path().apply {
        val left = gx + inset
        val right = gx + gw - inset
        val top = gTop + inset * 1.4f
        val shoulder = gh * 0.32f
        moveTo(left, floor)
        lineTo(left, top + shoulder)
        quadraticTo(left + (cx - left) * 0.12f, top + shoulder * 0.25f, cx, top)
        quadraticTo(right - (right - cx) * 0.12f, top + shoulder * 0.25f, right, top + shoulder)
        lineTo(right, floor)
    }
    val outer = arch(0f)
    val door = arch(gw * 0.18f)
    drawPath(outer, stone)
    val spire = Path().apply {
        moveTo(cx - gw * 0.06f, gTop + gh * 0.04f)
        lineTo(cx, gTop - gh * 0.14f)
        lineTo(cx + gw * 0.06f, gTop + gh * 0.04f)
        close()
    }
    drawPath(spire, stone)
    glowStroke(spire, t.accent2, 1.2.dp.toPx(), 0.8f)
    clipPath(door) {
        val core = Offset(cx, floor - gh * 0.35f)
        drawRect(Brush.radialGradient(0f to t.accent2.copy(alpha = 0.95f), 0.4f to t.accent.copy(alpha = 0.55f), 1f to t.accent2.copy(alpha = 0.15f), center = core, radius = gh * 0.7f))
        repeat(4) { i ->
            val c = Offset(cx + sin(time * 0.7f + i * 1.7f) * gw * 0.18f, floor - gh * (0.2f + i * 0.15f))
            drawCircle(Brush.radialGradient(listOf(t.text.copy(alpha = 0.25f), Color.Transparent), c, gw * 0.25f), gw * 0.25f, c)
        }
    }
    val beam = 0.8f + 0.2f * sin(time * 3f)
    val beamW = 6.dp.toPx()
    drawRect(
        Brush.horizontalGradient(listOf(Color.Transparent, t.text.copy(alpha = 0.95f * beam), Color.Transparent), cx - beamW, cx + beamW),
        Offset(cx - beamW, gTop + gh * 0.12f),
        Size(beamW * 2, h - gTop - gh * 0.12f),
    )
    glowStroke(outer, t.accent2, 2.dp.toPx(), 0.9f)
    glowStroke(door, t.accent, 1.5.dp.toPx(), 0.9f)

    // Runes on both pillars.
    val runeAlpha = 0.75f + 0.25f * sin(time * 2f)
    l.runes.forEachIndexed { side, column ->
        val px = if (side == 0) gx + gw * 0.09f else gx + gw * 0.91f
        val s = gw * 0.035f
        column.forEachIndexed { i, rune ->
            val ry = gTop + gh * 0.36f + i * gh * 0.085f
            for ((a, b, c, d) in rune.strokes) {
                drawLine(
                    t.accent2.copy(alpha = runeAlpha),
                    Offset(px + (a - 0.5f) * s * 2, ry + b * s * 2),
                    Offset(px + (c - 0.5f) * s * 2, ry + d * s * 2),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }

    // Steps and the floor reflection.
    repeat(3) { i ->
        val sw = gw * (1.2f + i * 0.22f)
        val sy = floor + i * h * 0.022f
        drawRect(stone, Offset(cx - sw / 2, sy), Size(sw, h * 0.022f))
        drawLine(t.accent2.copy(alpha = 0.35f), Offset(cx - sw / 2, sy), Offset(cx + sw / 2, sy), 1.dp.toPx())
    }

    // Energy bands orbiting the gate.
    for (wp in l.wisps) {
        val color = if (wp.second) t.accent else t.accent2
        val path = Path()
        val a0 = wp.start + time * wp.speed
        for (i in 0..40) {
            val a = a0 + i / 40f * wp.length
            val x = cos(a) * wp.rx * w
            val y = sin(a) * wp.ry * h * (1 + 0.15f * sin(a * 3 + time))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        translate(cx, h * wp.cy) {
            rotate(wp.tilt * 23f, Offset.Zero) {
                drawPath(path, color.copy(alpha = 0.1f), style = Stroke(wp.width.dp.toPx() * 3.4f, cap = StrokeCap.Round))
                drawPath(path, color.copy(alpha = 0.22f), style = Stroke(wp.width.dp.toPx() * 1.4f, cap = StrokeCap.Round))
                drawPath(path, color.copy(alpha = 0.8f), style = Stroke(wp.width.dp.toPx() * 0.4f, cap = StrokeCap.Round))
            }
        }
    }

    // Floating rocks with a lit edge.
    for (rock in l.rocks) {
        val x = rock.x * w
        val y = rock.y * h + sin(time * rock.speed + rock.phase) * h * 0.012f
        val path = Path().apply {
            rock.points.forEachIndexed { i, (px, py) -> if (i == 0) moveTo(x + px * w, y + py * w) else lineTo(x + px * w, y + py * w) }
            close()
        }
        drawPath(path, stone)
        drawPath(path, t.accent2.copy(alpha = 0.55f), style = Stroke(1.dp.toPx()))
    }

    // Magic circle as an aura: halo band, plasma tongues along the ring, a breathing core, stars.
    val center = Offset(cx, h * 0.52f)
    val radius = min(w * 0.44f, h * 0.44f)
    val breath = 0.85f + 0.15f * sin(time * 1.6f)
    drawCircle(
        Brush.radialGradient(
            0.64f to Color.Transparent,
            0.82f to t.accent2.copy(alpha = 0.32f * breath),
            1f to Color.Transparent,
            center = center,
            radius = radius * 1.22f,
        ),
        radius * 1.22f,
        center,
    )
    for (p in l.plasma) {
        val color = if (p.second) t.accent else t.accent2
        val a0 = p.start + time * p.speed
        val segments = 16
        val passes = listOf(3.2f to 0.07f, 1.3f to 0.22f, 0.35f to 0.85f)
        for ((width, alpha) in passes) {
            for (i in 0 until segments) {
                val u0 = i.toFloat() / segments
                val u1 = (i + 1f) / segments
                val taper = sin(PI.toFloat() * (u0 + u1) / 2)
                fun point(u: Float): Offset {
                    val a = a0 + u * p.length
                    val rr = radius * (1 + p.amp * sin(a * p.waves + time * p.flow) * taper)
                    return Offset(center.x + cos(a) * rr, center.y + sin(a) * rr)
                }
                drawLine(
                    color.copy(alpha = alpha * taper * breath),
                    point(u0),
                    point(u1),
                    strokeWidth = p.width.dp.toPx() * width,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
    drawCircle(t.accent2.copy(alpha = 0.12f * breath), radius, center, style = Stroke(8.dp.toPx()))
    drawCircle(t.text.copy(alpha = 0.55f * breath), radius, center, style = Stroke(1.2.dp.toPx()))
    drawStar(Offset(center.x, center.y - radius), radius * 0.12f, t.text.copy(alpha = 0.95f), t.accent2)
    drawStar(Offset(center.x, center.y + radius), radius * 0.12f, t.text.copy(alpha = 0.95f), t.accent2)

    // Rising sparks.
    val spark = 2.dp.toPx()
    for (s in l.sparks) {
        val life = (s.delay + time * 0.07f * s.speed) % 1f
        val color = if (s.delay > 0.5f) t.accent else t.accent2
        drawRect(color.copy(alpha = (1 - life) * 0.9f), Offset(s.x * w + sin((life + s.delay) * 7) * 5.dp.toPx(), h * (1 - life)), Size(spark, spark))
    }

    // Soft edges on every side, so the scene melts into the screen instead of ending in a rectangle.
    drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.1f to Color.Black, 0.9f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
    drawRect(Brush.verticalGradient(0f to Color.Transparent, 0.08f to Color.Black, 0.8f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
}

/** A stroke with a soft halo: two wide faint passes under the line itself. */
private fun DrawScope.glowStroke(path: Path, color: Color, width: Float, alpha: Float) {
    drawPath(path, color.copy(alpha = alpha * 0.12f), style = Stroke(width * 6, join = StrokeJoin.Round))
    drawPath(path, color.copy(alpha = alpha * 0.3f), style = Stroke(width * 2.5f, join = StrokeJoin.Round))
    drawPath(path, color.copy(alpha = alpha), style = Stroke(width, join = StrokeJoin.Round))
}

/** A four-pointed sparkle with a soft glow. */
private fun DrawScope.drawStar(c: Offset, s: Float, color: Color, glow: Color) {
    drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.5f), Color.Transparent), c, s * 1.4f), s * 1.4f, c)
    val path = Path().apply {
        moveTo(c.x, c.y - s)
        quadraticTo(c.x, c.y, c.x + s * 0.32f, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + s)
        quadraticTo(c.x, c.y, c.x - s * 0.32f, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - s)
        close()
    }
    drawPath(path, color)
}

/** Time shown with reduced motion: a moment where the bands are spread nicely. */
private const val STILL_TIME = 1.5f
