package uz.hangulfriend.ui.kit

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.theme.GameTokens

/** The six corners of a box [w]×[h] with the top-left and bottom-right corners cut by [cut] (clamped to half the short side). */
fun cutPolygon(w: Float, h: Float, cut: Float): List<Offset> {
    val c = cut.coerceIn(0f, min(w, h) / 2)
    return listOf(Offset(c, 0f), Offset(w, 0f), Offset(w, h - c), Offset(w - c, h), Offset(0f, h), Offset(0f, c))
}

/** The Solo Leveling "System" frame: a box with two opposite corners cut off. */
class CutShape(private val cut: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val points = cutPolygon(size.width, size.height, with(density) { cut.toPx() })
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        return Outline.Generic(path)
    }
}

fun cutShape(cut: Dp): Shape = CutShape(cut)

/** The theme's frame: cut corners in System, rounded in Neon. */
fun GameTokens.shape(cut: Dp = 10.dp): Shape =
    if (id == GameThemeId.SYSTEM) CutShape(cut) else RoundedCornerShape(panelCorner)

private fun Outline.toPath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
}

/** Soft light outside [shape]; drawn behind the content so it never covers it. */
fun Modifier.shapeGlow(color: Color, shape: Shape, radius: Dp = 12.dp): Modifier = drawBehind {
    val paint = android.graphics.Paint().apply {
        this.color = color.toArgb()
        maskFilter = BlurMaskFilter(radius.toPx(), BlurMaskFilter.Blur.OUTER)
    }
    val path = shape.createOutline(size, layoutDirection, this).toPath().asAndroidPath()
    drawIntoCanvas { it.nativeCanvas.drawPath(path, paint) }
}

/** Short bright brackets on the two corners a [CutShape] keeps (top-right, bottom-left): the System window look. */
fun Modifier.cornerTicks(color: Color, length: Dp = 12.dp): Modifier = drawBehind {
    val l = length.toPx()
    val s = 2.dp.toPx()
    drawLine(color, Offset(size.width - l, s / 2), Offset(size.width, s / 2), s)
    drawLine(color, Offset(size.width - s / 2, 0f), Offset(size.width - s / 2, l), s)
    drawLine(color, Offset(0f, size.height - s / 2), Offset(l, size.height - s / 2), s)
    drawLine(color, Offset(s / 2, size.height - l), Offset(s / 2, size.height), s)
}

/** Outline of [shape] grown by [grow] px on every side, for rings around a component. */
internal fun Modifier.drawGrownOutline(shape: Shape, grow: () -> Float, color: () -> Color, width: Dp): Modifier = drawBehind {
    val g = grow()
    val outline = shape.createOutline(Size(size.width + 2 * g, size.height + 2 * g), layoutDirection, this)
    val path = outline.toPath()
    path.translate(Offset(-g, -g))
    drawPath(path, color(), style = Stroke(width.toPx()))
}
