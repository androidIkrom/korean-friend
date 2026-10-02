package uz.hangulfriend.ui.avatar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

/** One cubic Bézier piece of a closed outline, starting where the previous one ended. */
data class CubicSegment(val c1: Offset, val c2: Offset, val end: Offset)

/**
 * Control points of the flame aura around the body (view-box units): rounded lobes of shoulder–crest–shoulder,
 * so the smoothed outline reads as fire, not spikes (stage 6b spec §3.3). [k] is the strength (C .3 … S 1).
 */
fun flameOutline(k: Float, scale: Float, seed: Int): List<Offset> {
    val lobes = (6 + 4 * k).toInt()
    val cx = 100f
    val cy = 150f
    val rx = 88f * scale
    val ry = 118f * scale
    val span = 230f / lobes

    fun at(deg: Float, out: Float, lift: Float): Offset {
        val th = Math.toRadians(deg.toDouble())
        val c = cos(th).toFloat()
        val s = sin(th).toFloat()
        return Offset(cx + rx * c + c * out, cy - ry * s - s * out - lift)
    }

    val points = mutableListOf(Offset(cx - rx * 1.02f, 282f))
    repeat(lobes) { j ->
        val a0 = 205f - span * j
        val variation = 0.75f + 0.5f * (((j + seed) * 37) % 11) / 10f
        val h = (10f + 24f * k) * variation * scale
        val sway = (if ((j + seed) % 2 == 1) 3f else -3f) * k
        points += at(a0, 3f, 0f)
        points += at(a0 - span * 0.22f, h * 0.35f, h * 0.45f)
        points += at(a0 - span * 0.5f, h * 0.5f, h * 0.8f) + Offset(sway, 0f)
        points += at(a0 - span * 0.78f, h * 0.35f, h * 0.45f)
    }
    points += at(205f - span * lobes, 3f, 0f)
    points += Offset(cx + rx * 1.02f, 282f)
    return points
}

/** Closed Catmull-Rom spline through [points] as cubic segments; segment i runs from points[i] to points[i + 1]. */
fun catmullRom(points: List<Offset>): List<CubicSegment> {
    val n = points.size
    return List(n) { i ->
        val p0 = points[(i - 1 + n) % n]
        val p1 = points[i]
        val p2 = points[(i + 1) % n]
        val p3 = points[(i + 2) % n]
        CubicSegment(p1 + (p2 - p0) / 6f, p2 - (p3 - p1) / 6f, p2)
    }
}

fun flamePath(k: Float, scale: Float, seed: Int): Path {
    val points = flameOutline(k, scale, seed)
    return Path().apply {
        moveTo(points[0].x, points[0].y)
        catmullRom(points).forEach { cubicTo(it.c1.x, it.c1.y, it.c2.x, it.c2.y, it.end.x, it.end.y) }
        close()
    }
}

/** Tips of the small teardrop flames drifting above the head (B and up). */
fun emberCenters(k: Float): List<Offset> = List((2 + 4 * k).toInt()) { j ->
    Offset(40f + (j * 53) % 120, -4f + (j * 29) % 40 - 30f * k)
}
