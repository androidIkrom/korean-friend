package uz.hangulfriend.ui.kit

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.hangulfriend.ui.theme.GameTokens

/** Stage 10 rounded corners: small "cuts" become control corners (16dp), larger ones card corners (24dp). */
fun cutShape(cut: Dp): Shape = RoundedCornerShape(if (cut >= 10.dp) 24.dp else 16.dp)

/** The theme's frame: a soft rounded rectangle (24dp cards, 16dp controls for small [cut] values). */
@Suppress("UnusedReceiverParameter")
fun GameTokens.shape(cut: Dp = 10.dp): Shape = cutShape(cut)

private fun Outline.toPath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
}

/** Outline of [shape] grown by [grow] px on every side, for rings around a component. */
internal fun Modifier.drawGrownOutline(shape: Shape, grow: () -> Float, color: () -> Color, width: Dp): Modifier = drawBehind {
    val g = grow()
    val outline = shape.createOutline(Size(size.width + 2 * g, size.height + 2 * g), layoutDirection, this)
    val path = outline.toPath()
    path.translate(Offset(-g, -g))
    drawPath(path, color(), style = Stroke(width.toPx()))
}
