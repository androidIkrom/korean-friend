package uz.hangulfriend.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.theme.GameTokens
import uz.hangulfriend.ui.theme.LocalGameTokens

enum class HuntStyle { PRIMARY, SECONDARY, GOLD, SUCCESS, DANGER }

/** One button style: its [fill] (null = glass) and its label colour. */
data class HuntPalette(val fill: Color?, val text: Color)

fun huntPalette(style: HuntStyle, t: GameTokens): HuntPalette = when (style) {
    HuntStyle.PRIMARY -> HuntPalette(t.text, t.background)
    HuntStyle.SECONDARY -> HuntPalette(null, t.text)
    HuntStyle.GOLD -> HuntPalette(Color(0xFFF5C451), Color(0xFF2A1A00))
    HuntStyle.SUCCESS -> HuntPalette(CorrectGreen, Color(0xFF04140A))
    HuntStyle.DANGER -> HuntPalette(t.danger, Color(0xFF2A0610))
}

private val ButtonShape = RoundedCornerShape(16.dp)

/**
 * The game's button: a flat 16dp fill (or glass for SECONDARY) that springs down when pressed, with a tap
 * sound and haptic. [text] is shown as given.
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
    horizontalPadding: Dp = 18.dp,
) {
    val t = LocalGameTokens.current
    val p = huntPalette(style, t)
    val feedback = LocalGameFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .springPress(interaction, enabled)
            .alpha(if (enabled) 1f else 0.4f)
            .then(if (p.fill == null) Modifier.glass(ButtonShape) else Modifier.background(p.fill, ButtonShape))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button) {
                feedback.play(sfx)
                onClick()
            }
            .heightIn(min = minHeight)
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        contentAlignment = Alignment.Center,
        // A caller's fillMaxWidth must reach the fill.
        propagateMinConstraints = true,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = p.text, modifier = Modifier.size(20.dp))
            Text(
                text,
                color = p.text,
                fontFamily = t.ui,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
