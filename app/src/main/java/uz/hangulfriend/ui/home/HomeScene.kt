package uz.hangulfriend.ui.home

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.study.Rank
import uz.hangulfriend.ui.avatar.Avatar
import uz.hangulfriend.ui.theme.GameTokens
import uz.hangulfriend.ui.theme.LocalGameTokens

/** The avatar on its stage: a glowing gate (System) or a rooftop over a neon city (Neon). */
@Composable
fun HomeScene(rank: Rank, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) { if (t.id == GameThemeId.SYSTEM) portal(t) else skyline(t) }
        Avatar(rank, Modifier.size(220.dp, 266.dp))
    }
}

private fun DrawScope.portal(t: GameTokens) {
    val w = 220.dp.toPx()
    val h = 276.dp.toPx()
    val topLeft = Offset((size.width - w) / 2, size.height - h - 8.dp.toPx())
    drawCircle(
        Brush.radialGradient(listOf(Color(0x592870FF), Color.Transparent), center = center, radius = size.width * 0.6f),
        radius = size.width * 0.6f,
    )
    drawOval(
        Brush.radialGradient(listOf(Color(0xF20E2860), Color(0x4D04060C)), center = topLeft + Offset(w / 2, h / 2), radius = w * 0.7f),
        topLeft = topLeft,
        size = Size(w, h),
    )
    val glow = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6.dp.toPx()
        color = t.accent.copy(alpha = 0.6f).toArgb()
        maskFilter = BlurMaskFilter(14.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
    }
    drawIntoCanvas { it.nativeCanvas.drawOval(topLeft.x, topLeft.y, topLeft.x + w, topLeft.y + h, glow) }
    drawOval(t.accent.copy(alpha = 0.85f), topLeft = topLeft, size = Size(w, h), style = Stroke(2.dp.toPx()))
}

private fun DrawScope.skyline(t: GameTokens) {
    val base = size.height - 30.dp.toPx()
    val unit = size.width / 390f
    val buildings = listOf(0 to 60, 54 to 50, 100 to 70, 166 to 46, 208 to 64, 268 to 52, 316 to 74)
    val heights = listOf(200, 250, 170, 270, 210, 250, 190)
    buildings.forEachIndexed { i, (x, bw) ->
        val top = base - heights[i] * unit
        drawRect(Color(0xFF140A26), Offset(x * unit, top), Size(bw * unit, base - top))
        var wy = top + 14 * unit
        while (wy < base - 10 * unit) {
            var wx = x + 6
            while (wx < x + bw - 6) {
                if ((wx * 5 + (wy / unit).toInt() * 7) % 9 < 2) {
                    drawRect(
                        (if ((wx + wy.toInt()) % 3 == 0) t.accent2 else Color(0xFFFFE45C)).copy(alpha = 0.55f),
                        Offset(wx * unit, wy),
                        Size(4 * unit, 5 * unit),
                    )
                }
                wx += 9
            }
            wy += 16 * unit
        }
    }
    val sign = Paint().apply {
        color = t.accent.toArgb()
        textSize = 18.dp.toPx()
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        setShadowLayer(8.dp.toPx(), 0f, 0f, t.accent.toArgb())
    }
    drawRect(t.accent, Offset(176 * unit, 60 * unit), Size(26 * unit, 96 * unit), style = Stroke(2.dp.toPx()))
    drawIntoCanvas { c ->
        listOf("한", "국", "어").forEachIndexed { i, ch -> c.nativeCanvas.drawText(ch, 189 * unit, (88 + i * 26) * unit, sign) }
    }
    listOf(30 to 20, 90 to 100, 150 to 0, 210 to 70, 270 to 30, 330 to 120, 370 to 10, 120 to 200, 250 to 180).forEach { (x, y) ->
        drawLine(t.accent2.copy(alpha = 0.22f), Offset(x * unit, y * unit), Offset((x - 6) * unit, (y + 22) * unit))
    }
    drawRect(Color(0xFF07040E), Offset(0f, base), Size(size.width, size.height - base))
    drawLine(t.accent, Offset(0f, base), Offset(size.width, base), strokeWidth = 2.dp.toPx())
}
