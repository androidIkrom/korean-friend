package uz.hangulfriend.ui.kit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hangulfriend.ui.theme.LocalGameTokens

/** The level in a round glass badge: small "LV" over a big number. */
@Composable
fun LevelBadge(level: Int, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    Column(
        modifier.size(54.dp).glass(CircleShape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("LV", color = t.muted, fontFamily = t.ui, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, letterSpacing = 1.sp)
        Text(level.toString(), color = t.text, fontFamily = t.numbers, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
    }
}

/** A rounded progress track with a gradient fill (XP, quests); [color] tints the fill's end. */
@Composable
fun GlowBar(fraction: Float, modifier: Modifier = Modifier, color: Color? = null, height: Int = 8) {
    val t = LocalGameTokens.current
    val end = color ?: t.accent
    val start = if (color == null) t.accent2 else color.copy(alpha = 0.7f)
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "glowBar")
    val shape = RoundedCornerShape(height.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.1f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(f)
                .clip(shape)
                .background(Brush.horizontalGradient(listOf(start, end))),
        )
    }
}

/** A small glass pill in the HUD (streak, due cards, badges); tappable when [onClick] is given. */
@Composable
fun RowScope.HudChip(icon: ImageVector, text: String, color: Color, onClick: (() -> Unit)? = null) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .weight(1f)
            .springPress(interaction, onClick != null)
            .heightIn(min = 36.dp)
            .glass(CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                        feedback.play(Sfx.TAP)
                        onClick()
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(text, color = t.text, fontFamily = t.ui, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A square glass tile of the lobby: icon over a short label, with an optional "new" dot. */
@Composable
fun RailButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color? = null, dot: Boolean = false) {
    val t = LocalGameTokens.current
    val c = accent ?: t.accent
    val feedback = LocalGameFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Box(modifier.size(62.dp).springPress(interaction)) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .glass(RoundedCornerShape(16.dp))
                .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                    feedback.play(Sfx.TAP)
                    onClick()
                }
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Icon(icon, contentDescription = null, tint = c, modifier = Modifier.size(22.dp))
            Text(label, color = t.muted, fontFamily = t.ui, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (dot) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(8.dp)
                    .background(t.danger, CircleShape),
            )
        }
    }
}
