package uz.hangulfriend.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntPanel
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.LocalGameFeedback
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.kit.shape

/** Soft outer light around a rounded rectangle (System theme panels and buttons). */
fun Modifier.glow(color: Color, radius: Dp = 12.dp, corner: Dp = 0.dp): Modifier = drawBehind {
    val r = radius.toPx()
    val paint = android.graphics.Paint().apply {
        this.color = color.toArgb()
        maskFilter = BlurMaskFilter(r, BlurMaskFilter.Blur.OUTER)
    }
    drawIntoCanvas { it.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, corner.toPx(), corner.toPx(), paint) }
}

/** A titled game window: `[ STATUS ]` frame in System, glass card in Neon (drawn by the kit). */
@Composable
fun GamePanel(
    title: String?,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) = HuntPanel(modifier, title = title, accent = borderColor, content = content)

/** The main call to action: the kit's 3D primary button, full width. */
@Composable
fun GameButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) =
    HuntButton(text, onClick, modifier.fillMaxWidth(), HuntStyle.PRIMARY, enabled)

@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color? = null) {
    val t = LocalGameTokens.current
    val c = color ?: t.accent
    val shape = if (t.id == GameThemeId.SYSTEM) RectangleShape else RoundedCornerShape(3.dp)
    Box(modifier.fillMaxWidth().height(6.dp).background(c.copy(alpha = 0.15f), shape)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).background(c, shape))
    }
}

enum class BadgeState { DONE, ACTIVE, LOCKED }

/** A rank letter: a 45° diamond in System, a ring in Neon. */
@Composable
fun RankBadge(letter: String, state: BadgeState, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val t = LocalGameTokens.current
    val color = when (state) {
        BadgeState.DONE -> t.muted
        BadgeState.ACTIVE -> t.accent
        BadgeState.LOCKED -> t.muted.copy(alpha = 0.5f)
    }
    val glow = if (state == BadgeState.ACTIVE) Modifier.glow(color.copy(alpha = 0.6f), 10.dp) else Modifier
    if (t.id == GameThemeId.SYSTEM) {
        Box(modifier.size(size * 1.42f), contentAlignment = Alignment.Center) {
            Box(Modifier.size(size).rotate(45f).then(glow).background(t.background).border(1.5.dp, color))
            Text(letter, color = color, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.45f).sp)
        }
    } else {
        Box(
            modifier.size(size).then(glow).background(t.background, CircleShape).border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(letter, color = color, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.45f).sp)
        }
    }
}

/** True inside a [GameBackground]: a nested one only lays out its content instead of drawing a second backdrop. */
val LocalInGameBackground = staticCompositionLocalOf { false }

/** Full-screen backdrop: a faint grid with a blue glow (System) or a night-sky gradient (Neon). */
@Composable
fun GameBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val t = LocalGameTokens.current
    if (LocalInGameBackground.current) {
        Box(modifier.fillMaxSize(), content = content)
        return
    }
    CompositionLocalProvider(LocalInGameBackground provides true) {
        Backdrop(t, modifier, content)
    }
}

@Composable
private fun Backdrop(t: GameTokens, modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(t.background)
            .drawBehind {
                if (t.id == GameThemeId.SYSTEM) {
                    val step = 32.dp.toPx()
                    val line = t.accent.copy(alpha = 0.05f)
                    var x = 0f
                    while (x < size.width) {
                        drawLine(line, Offset(x, 0f), Offset(x, size.height))
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(line, Offset(0f, y), Offset(size.width, y))
                        y += step
                    }
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color(0x592870FF), Color.Transparent),
                            center = Offset(size.width / 2, size.height * 0.2f),
                            radius = size.width * 0.8f,
                        ),
                    )
                } else {
                    drawRect(Brush.verticalGradient(listOf(Color(0xFF26104A), t.background), endY = size.height * 0.6f))
                }
            },
        content = content,
    )
}

/** A game-styled card: panel fill, thin theme border, theme shape; clickable (with a tap sound) when [onClick] is given. */
@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    containerColor: Color? = null,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = LocalGameTokens.current
    val shape = t.shape(10.dp)
    val feedback = LocalGameFeedback.current
    Column(
        modifier
            .clip(shape)
            .background(containerColor ?: t.panel, shape)
            .border(1.dp, borderColor ?: t.panelBorder, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, role = Role.Button) {
                        feedback.play(Sfx.TAP)
                        onClick()
                    }
                } else {
                    Modifier
                },
            ),
        content = content,
    )
}
