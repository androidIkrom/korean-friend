package uz.hangulfriend.ui.kit

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.theme.GameTokens
import uz.hangulfriend.ui.theme.LocalGameTokens

enum class HuntStyle { PRIMARY, SECONDARY, GOLD, SUCCESS, DANGER }

/** Colours of one button style: a lit top, a darker bottom, the 3D lip under it, a bright rim and the label. */
data class HuntPalette(val top: Color, val bottom: Color, val lip: Color, val rim: Color, val text: Color, val glow: Color?)

fun huntPalette(style: HuntStyle, t: GameTokens): HuntPalette = when (style) {
    HuntStyle.PRIMARY -> if (t.id == GameThemeId.SYSTEM) {
        HuntPalette(Color(0xFF3A9BFF), Color(0xFF1A5FD0), Color(0xFF0B2E6E), Color(0xFF9FE6FF), Color.White, t.accent)
    } else {
        HuntPalette(Color(0xFFFF4FA6), Color(0xFFB03BE0), Color(0xFF4E1468), Color(0xFFFFA6D6), Color.White, t.accent)
    }
    HuntStyle.SECONDARY -> {
        val base = t.panel.copy(alpha = 1f).compositeOver(t.background)
        HuntPalette(lerp(base, t.accent, 0.14f), base, lerp(base, Color.Black, 0.55f), t.panelBorder, t.text, null)
    }
    HuntStyle.GOLD -> HuntPalette(Color(0xFFFFE08A), Color(0xFFE0A92E), Color(0xFF7A5410), Color(0xFFFFF0C0), Color(0xFF2A1A00), Color(0xFFFFD66B))
    HuntStyle.SUCCESS -> HuntPalette(Color(0xFF4ADE80), Color(0xFF22A35A), Color(0xFF0D5A2E), Color(0xFFB8F5CC), Color(0xFF04140A), Color(0xFF4ADE80))
    HuntStyle.DANGER -> HuntPalette(Color(0xFFFF7A90), Color(0xFFD93A5A), Color(0xFF6E1426), Color(0xFFFFC2CC), Color.White, null)
}

private val LIP = 5.dp

/**
 * The game's button: a lit face on a darker lip that sinks [LIP] when pressed and springs back,
 * with a tap sound and haptic. [text] is shown as given (callers decide on upper case).
 */
@Composable
fun HuntButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: HuntStyle = HuntStyle.PRIMARY,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    sfx: Sfx = Sfx.TAP,
    minHeight: Dp = 52.dp,
    fontSize: Int = 15,
) {
    val t = LocalGameTokens.current
    val p = huntPalette(style, t)
    val shape = t.shape(12.dp)
    val feedback = LocalGameFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth by animateDpAsState(if (pressed && enabled) LIP - 1.dp else 0.dp, tween(GameMotion.TAP), label = "press")
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.42f)
            .padding(bottom = LIP),
        // A caller's fillMaxWidth must reach the face, not only the lip.
        propagateMinConstraints = true,
    ) {
        Box(Modifier.matchParentSize().offset(y = LIP).background(p.lip, shape))
        Box(
            Modifier
                .offset(y = depth)
                .then(if (enabled && p.glow != null && t.id == GameThemeId.SYSTEM) Modifier.shapeGlow(p.glow.copy(alpha = 0.4f), shape, 14.dp) else Modifier)
                .background(Brush.verticalGradient(listOf(lerp(p.top, Color.White, 0.12f), p.top, p.bottom)), shape)
                .border(1.dp, p.rim.copy(alpha = if (enabled) 0.9f else 0.4f), shape)
                .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button) {
                    feedback.play(sfx)
                    onClick()
                }
                .heightIn(min = minHeight)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (icon != null) Icon(icon, contentDescription = null, tint = p.text, modifier = Modifier.size(20.dp))
                Text(
                    text,
                    color = p.text,
                    fontFamily = t.display,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize.sp,
                    letterSpacing = if (t.bracketTitles) 2.sp else 0.5.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
