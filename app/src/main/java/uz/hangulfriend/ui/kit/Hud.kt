package uz.hangulfriend.ui.kit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.theme.LocalGameTokens

/** The level in a cut frame: small "LV" over a big number. */
@Composable
fun LevelBadge(level: Int, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    val shape = t.shape(10.dp)
    Column(
        modifier
            .size(54.dp)
            .shapeGlow(t.accent.copy(alpha = 0.35f), shape, 10.dp)
            .background(Brush.verticalGradient(listOf(lerpPanel(t.accent, 0.28f), t.panel.copy(alpha = 1f))), shape)
            .border(1.dp, t.accent, shape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("LV", color = t.muted, fontFamily = t.display, fontSize = 10.sp, letterSpacing = 1.sp)
        Text(level.toString(), color = Color.White, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 22.sp)
    }
}

@Composable
private fun lerpPanel(c: Color, f: Float): Color = androidx.compose.ui.graphics.lerp(LocalGameTokens.current.panel.copy(alpha = 1f), c, f)

/** A thick, glowing progress bar with a gradient fill (XP, quests). */
@Composable
fun GlowBar(fraction: Float, modifier: Modifier = Modifier, color: Color? = null, height: Int = 8) {
    val t = LocalGameTokens.current
    val c = color ?: t.accent
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "glowBar")
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(c.copy(alpha = 0.12f))
            .border(1.dp, c.copy(alpha = 0.35f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(f)
                .then(if (f > 0f) Modifier.shapeGlow(c.copy(alpha = 0.7f), RectangleShape, 8.dp) else Modifier)
                .background(Brush.horizontalGradient(listOf(lerp2(c, 0.35f), c))),
        )
    }
}

private fun lerp2(c: Color, f: Float) = androidx.compose.ui.graphics.lerp(c, Color(0xFF1A3AA0), f)

/** A small framed counter in the HUD (streak, due cards, badges); tappable when [onClick] is given. */
@Composable
fun RowScope.HudChip(icon: ImageVector, text: String, color: Color, onClick: (() -> Unit)? = null) {
    val t = LocalGameTokens.current
    val shape = t.shape(8.dp)
    val feedback = LocalGameFeedback.current
    Row(
        Modifier
            .weight(1f)
            .heightIn(min = 34.dp)
            .clip(shape)
            .background(t.panel, shape)
            .border(1.dp, color.copy(alpha = 0.65f), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button) {
                        feedback.play(Sfx.TAP)
                        onClick()
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, color = t.text, fontFamily = t.display, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A square side button of the lobby: icon over a short label, with an optional red "new" dot. */
@Composable
fun RailButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color? = null, dot: Boolean = false) {
    val t = LocalGameTokens.current
    val c = accent ?: t.accent
    val shape = t.shape(10.dp)
    val feedback = LocalGameFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(GameMotion.TAP), label = "rail")
    Box(modifier.size(62.dp).scale(scale)) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .then(if (t.id == GameThemeId.SYSTEM) Modifier.shapeGlow(c.copy(alpha = 0.25f), shape, 8.dp) else Modifier)
                .clip(shape)
                .background(t.panel, shape)
                .border(1.dp, c.copy(alpha = 0.7f), shape)
                .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                    feedback.play(Sfx.TAP)
                    onClick()
                }
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Icon(icon, contentDescription = null, tint = c, modifier = Modifier.size(22.dp))
            Text(label.uppercase(), color = t.muted, fontFamily = t.display, fontSize = 8.5.sp, letterSpacing = 0.3.sp, maxLines = 1, softWrap = false)
        }
        if (dot) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(8.dp)
                    .shapeGlow(t.danger, RectangleShape, 4.dp)
                    .background(t.danger),
            )
        }
    }
}
