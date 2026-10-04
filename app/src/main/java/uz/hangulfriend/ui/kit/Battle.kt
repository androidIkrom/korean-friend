package uz.hangulfriend.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.theme.WrongRed

/** Gold of combos, stars and loot. */
val Gold = Color(0xFFFFD66B)

/**
 * Runs [effect] each time [key] changes to a new non-zero value after the first composition, so
 * effects never replay an old event when the screen is recreated (rotation). Zero means "no event".
 */
@Composable
private fun OnNewKey(key: Int, effect: suspend () -> Unit) {
    val first = remember { key }
    LaunchedEffect(key) { if (key != first && key != 0) effect() }
}

enum class OptionState { IDLE, PICKED, RIGHT, WRONG, DIM }

/**
 * An answer tile: a raised 20dp card with an optional round letter badge. States recolour the fill:
 * RIGHT turns green and pops, WRONG turns red and shakes, DIM fades the options that no longer matter.
 */
@Composable
fun OptionTile(
    text: String,
    onClick: () -> Unit,
    state: OptionState,
    modifier: Modifier = Modifier,
    letter: Char? = null,
    enabled: Boolean = true,
) {
    val t = LocalGameTokens.current
    val reduced = LocalReducedMotion.current
    val feedback = LocalGameFeedback.current
    val tint = when (state) {
        OptionState.IDLE, OptionState.DIM -> null
        OptionState.PICKED -> t.accent
        OptionState.RIGHT -> CorrectGreen
        OptionState.WRONG -> WrongRed
    }
    val fill = if (tint != null) lerp(t.raised, tint, if (state == OptionState.PICKED) 0.25f else 0.3f) else t.raised
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(state) {
        if (state == OptionState.RIGHT && !reduced) {
            pop.snapTo(1.05f)
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 500f))
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .alpha(if (state == OptionState.DIM) 0.5f else 1f)
            .shake(if (state == OptionState.WRONG) 1 else 0)
            .scale(pop.value)
            .springPress(interaction, enabled)
            .clip(shape)
            .background(fill)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button) {
                feedback.play(Sfx.TAP)
                onClick()
            }
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (letter != null) {
            Box(
                Modifier
                    .size(30.dp)
                    .background((tint ?: t.accent).copy(alpha = if (tint != null) 0.9f else 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    letter.toString(),
                    color = if (tint != null) t.background else t.accent,
                    fontFamily = t.numbers,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
        Text(text, color = t.text, fontFamily = t.ui, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

/** A horizontal shake when [key] changes to a new non-zero value: the "miss" reaction. */
fun Modifier.shake(key: Int): Modifier = composed {
    val reduced = LocalReducedMotion.current
    val x = remember { Animatable(0f) }
    val first = remember { key }
    val px = with(LocalDensity.current) { 1.dp.toPx() }
    LaunchedEffect(key) {
        if (key != 0 && key != first && !reduced) {
            for (d in listOf(-10f, 10f, -7f, 7f, -3f, 3f, 0f)) x.animateTo(d * px, tween(45))
        }
    }
    graphicsLayer { translationX = x.value }
}

/**
 * The run's floors: one segment per item, lit when done, the current one pulsing. Long runs
 * (over 20 items, e.g. the final test) use one continuous bar.
 */
@Composable
fun FloorBar(done: Int, total: Int, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    if (total > 20 || total == 0) {
        GlowBar(if (total == 0) 0f else done.toFloat() / total, modifier, height = 10)
        return
    }
    val reduced = LocalReducedMotion.current
    val pulse = if (reduced) {
        1f
    } else {
        val p by rememberInfiniteTransition(label = "floor").animateFloat(
            0.35f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "floorPulse",
        )
        p
    }
    val seg: Shape = CircleShape
    Row(modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(total) { i ->
            val color = when {
                i < done -> t.accent
                i == done -> t.accent.copy(alpha = 0.25f + 0.45f * pulse)
                else -> Color.White.copy(alpha = 0.1f)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(if (i == done) 12.dp else 9.dp)
                    .align(Alignment.CenterVertically)
                    .background(color, seg),
            )
        }
    }
}

/** Boss lives. A heart that is lost cracks: it swells, tilts and turns into a broken heart. */
@Composable
fun HeartRow(hearts: Int, max: Int, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    val reduced = LocalReducedMotion.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(max) { i ->
            val lost = i >= hearts
            val p by animateFloatAsState(if (lost) 1f else 0f, if (reduced) snap() else tween(520), label = "heart$i")
            val swell = 1f + 0.45f * sin(PI * p.toDouble()).toFloat()
            Icon(
                if (p > 0.5f) Icons.Filled.HeartBroken else Icons.Filled.Favorite,
                contentDescription = null,
                tint = t.danger.copy(alpha = 1f - 0.6f * p),
                modifier = Modifier
                    .size(22.dp)
                    .scale(swell)
                    .rotate(-16f * p),
            )
        }
    }
}

/** "COMBO ×n" in gold that pops each time the run grows. */
@Composable
fun ComboChip(combo: Int, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    val reduced = LocalReducedMotion.current
    val s = remember { Animatable(1f) }
    OnNewKey(combo) {
        if (!reduced) {
            s.snapTo(1.4f)
            s.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 420f))
        }
    }
    Row(
        modifier
            .scale(s.value)
            .background(Gold.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Gold, modifier = Modifier.size(15.dp))
        Text("Combo ×$combo", color = Gold, fontFamily = t.ui, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    }
}

/** [text] rises 48 dp and fades out each time [key] changes: "+15 XP". */
@Composable
fun FloatingText(text: String, key: Int, color: Color, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    val reduced = LocalReducedMotion.current
    val p = remember { Animatable(1f) }
    OnNewKey(key) {
        if (reduced) {
            p.snapTo(0.3f)
            delay(700)
            p.snapTo(1f)
        } else {
            p.snapTo(0f)
            p.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }
    if (p.value >= 1f) return
    val rise = with(LocalDensity.current) { 48.dp.toPx() }
    Text(
        text,
        color = color,
        fontFamily = t.display,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        modifier = modifier.graphicsLayer {
            translationY = -rise * p.value
            alpha = if (p.value < 0.6f) 1f else (1f - p.value) / 0.4f
            val s = if (p.value < 0.15f) 0.7f + 2f * p.value else 1f
            scaleX = s
            scaleY = s
        },
    )
}

/** The screen edge glows [color] and fades out over 450 ms each time [key] changes: hit or miss. */
@Composable
fun EdgeFlash(color: Color, key: Int, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val a = remember { Animatable(0f) }
    OnNewKey(key) {
        if (reduced) return@OnNewKey
        a.snapTo(0.75f)
        a.animateTo(0f, tween(450))
    }
    if (a.value <= 0f) return
    val edge = with(LocalDensity.current) { 36.dp.toPx() }
    Box(
        modifier.fillMaxSize().drawBehind {
            val c = color.copy(alpha = a.value)
            val clear = Color.Transparent
            drawRect(Brush.verticalGradient(listOf(c, clear), startY = 0f, endY = edge))
            drawRect(Brush.verticalGradient(listOf(clear, c), startY = size.height - edge, endY = size.height))
            drawRect(Brush.horizontalGradient(listOf(c, clear), startX = 0f, endX = edge))
            drawRect(Brush.horizontalGradient(listOf(clear, c), startX = size.width - edge, endX = size.width))
        },
    )
}

/** Slowly turning light rays behind a reward (Gate Cleared, rank up). Still when motion is reduced. */
@Composable
fun GateRays(color: Color, modifier: Modifier = Modifier, rays: Int = 12) {
    val reduced = LocalReducedMotion.current
    val turn = if (reduced) {
        0f
    } else {
        val a by rememberInfiniteTransition(label = "rays").animateFloat(
            0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "raysTurn",
        )
        a
    }
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2
        val half = (PI / rays * 0.45).toFloat()
        val brush = Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center = c, radius = r)
        repeat(rays) { i ->
            val mid = (2 * PI * i / rays).toFloat() + Math.toRadians(turn.toDouble()).toFloat()
            val path = Path().apply {
                moveTo(c.x, c.y)
                lineTo(c.x + r * cos(mid - half), c.y + r * sin(mid - half))
                lineTo(c.x + r * cos(mid + half), c.y + r * sin(mid + half))
                close()
            }
            drawPath(path, brush)
        }
    }
}
