package uz.hangulfriend.ui.story

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** A skyline tower; [x], [w] are fractions of the scene width, [h], [spire] of its height; [shade] 0..1. */
data class GateTower(val x: Float, val w: Float, val h: Float, val spire: Float, val shade: Float)

/** A floating rock centred at ([x], [y]) (fractions); [points] is its outline in fractions of the scene width. */
data class GateRock(val x: Float, val y: Float, val points: List<Pair<Float, Float>>, val phase: Float, val speed: Float)

/** A rune: strokes as (x1, y1, x2, y2) in a unit box. */
data class GateRune(val strokes: List<List<Float>>)

/** An energy band orbiting the gate on an ellipse; radii are fractions of the scene width / height. */
data class GateWisp(
    val rx: Float,
    val ry: Float,
    val tilt: Float,
    val start: Float,
    val length: Float,
    val speed: Float,
    val width: Float,
    val second: Boolean,
    val cy: Float,
)

/** A plasma tongue running along the magic circle; [amp] is the ripple as a fraction of the radius. */
data class GatePlasma(
    val start: Float,
    val length: Float,
    val speed: Float,
    val amp: Float,
    val waves: Int,
    val flow: Float,
    val width: Float,
    val second: Boolean,
)

/** A rising spark: [x] fraction, [delay] 0..1 phase offset, [speed] multiplier. */
data class GateSpark(val x: Float, val delay: Float, val speed: Float)

/** Everything of a gate scene that does not move; the same seed always builds the same scene. */
data class GateLayout(
    val towers: List<GateTower>,
    val rocks: List<GateRock>,
    /** Two columns (left and right pillar) of runes. */
    val runes: List<List<GateRune>>,
    val wisps: List<GateWisp>,
    val plasma: List<GatePlasma>,
    val sparks: List<GateSpark>,
)

/** Rocks stay out of this middle band so they never cover the gate. */
const val GATE_CLEAR_FROM = 0.36f
const val GATE_CLEAR_TO = 0.64f

fun gateLayout(seed: Int): GateLayout {
    val r = Random(seed)
    fun f(from: Float, until: Float) = from + r.nextFloat() * (until - from)

    val towers = buildList {
        for (right in listOf(false, true)) {
            var edge = if (right) 1.02f else -0.02f
            repeat(6) {
                val w = f(0.05f, 0.12f)
                val x = if (right) edge - w else edge
                if (if (right) x < 0.62f else x + w > 0.38f) return@repeat
                add(GateTower(x, w, f(0.25f, 0.7f), f(0.04f, 0.14f), r.nextFloat()))
                edge += (if (right) -1 else 1) * w * f(0.6f, 1.2f)
            }
        }
    }
    val rocks = List(16) {
        val n = 5 + r.nextInt(3)
        val size = f(0.008f, 0.036f)
        val points = List(n) { i ->
            val a = (i.toFloat() / n) * 2 * PI.toFloat() + f(0f, 0.5f)
            Pair(cos(a) * size * f(0.6f, 1.2f), sin(a) * size * f(0.8f, 1.7f))
        }
        var x = r.nextFloat()
        if (x in GATE_CLEAR_FROM..GATE_CLEAR_TO) x = if (x < 0.5f) x - 0.25f else x + 0.25f
        GateRock(x.coerceIn(0.02f, 0.98f), f(0.15f, 0.8f), points, f(0f, 6.28f), f(0.3f, 0.8f))
    }
    val runes = List(2) { List(7) { GateRune(List(2 + r.nextInt(2)) { List(4) { r.nextFloat() } }) } }
    val wisps = List(6) { i ->
        GateWisp(
            rx = f(0.3f, 0.46f),
            ry = f(0.08f, 0.18f),
            tilt = f(-0.2f, 0.2f),
            start = f(0f, 6.28f),
            length = f(1.2f, 2.6f),
            speed = f(0.15f, 0.4f) * if (i % 2 == 0) -1 else 1,
            width = f(1.5f, 5.5f),
            second = i % 2 == 1,
            cy = f(0.45f, 0.7f),
        )
    }
    val plasma = List(7) { i ->
        GatePlasma(
            start = f(0f, 6.28f),
            length = f(0.9f, 2.5f),
            speed = f(0.18f, 0.48f) * if (i % 3 == 0) -1 else 1,
            amp = f(0.03f, 0.08f),
            waves = 4 + r.nextInt(5),
            flow = f(1f, 3f),
            width = f(2f, 6f),
            second = i % 2 == 1,
        )
    }
    val sparks = List(34) { GateSpark(r.nextFloat(), r.nextFloat(), f(0.4f, 1.4f)) }
    return GateLayout(towers, rocks, runes, wisps, plasma, sparks)
}
