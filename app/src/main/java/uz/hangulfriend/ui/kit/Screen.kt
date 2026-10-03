package uz.hangulfriend.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import uz.hangulfriend.R
import uz.hangulfriend.ui.theme.LocalGameTokens

/** A screen's title bar: back arrow, the upper-case display title with a short glowing underline, then [trailing]. */
@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    val t = LocalGameTokens.current
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.session_close), tint = t.muted)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title.uppercase(),
                color = t.text,
                fontFamily = t.display,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = 2.sp,
            )
            Box(
                Modifier
                    .width(56.dp)
                    .height(3.dp)
                    .shapeGlow(t.accent.copy(alpha = 0.7f), RectangleShape, 6.dp)
                    .background(Brush.horizontalGradient(listOf(t.accent, t.accent2))),
            )
        }
        trailing()
    }
}

/** A filter chip in the theme shape: accent fill when [selected], tap sound. */
@Composable
fun HuntChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val shape = t.shape(6.dp)
    Box(
        Modifier
            .heightIn(min = 36.dp)
            .clip(shape)
            .background(if (selected) t.accent.copy(alpha = 0.22f) else t.panel, shape)
            .border(1.dp, if (selected) t.accent else t.panelBorder.copy(alpha = 0.5f), shape)
            .selectable(selected = selected, role = Role.Tab) {
                feedback.play(Sfx.TAP)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) t.accent else t.text,
            fontFamily = t.display,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
        )
    }
}

/** A `[ SYSTEM ]` dialog: a scanning panel with [title], [content] and a row of [actions]. */
@Composable
fun SystemDialog(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = LocalGameTokens.current
    Dialog(onDismissRequest = onDismiss) {
        // The glass panel is translucent; an opaque backing keeps the screen behind from showing through.
        HuntPanel(
            Modifier.background(t.background, t.shape(12.dp)).verticalScroll(rememberScrollState()),
            title = title,
            scan = true,
            padding = 18.dp,
        ) {
            content()
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), content = actions)
        }
    }
}

/** A diamond that lights up when [selected]: the game's radio mark. */
@Composable
fun DiamondMark(selected: Boolean, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    Box(modifier.size(22.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(14.dp)
                .rotate(45f)
                .then(if (selected) Modifier.shapeGlow(t.accent.copy(alpha = 0.7f), RectangleShape, 6.dp) else Modifier)
                .background(if (selected) t.accent else t.background)
                .border(1.5.dp, if (selected) t.accent else t.muted),
        )
    }
}

/** One choice in a list (a lesson to start from): diamond mark, title and subtitle in a theme-shaped row. */
@Composable
fun SelectRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val shape = t.shape(8.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(shape)
            .background(if (selected) t.accent.copy(alpha = 0.16f) else t.panel.copy(alpha = 0.6f), shape)
            .border(1.dp, if (selected) t.accent else t.panelBorder.copy(alpha = 0.3f), shape)
            .selectable(selected = selected, role = Role.RadioButton) {
                feedback.play(Sfx.TAP)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DiamondMark(selected)
        Column(Modifier.weight(1f)) {
            Text(title, color = t.text, fontSize = 16.sp)
            Text(subtitle, color = t.muted, fontSize = 13.sp)
        }
    }
}

/** A square accent action button with an icon (vocabulary add, tutor send). */
@Composable
fun IconAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, sfx: Sfx = Sfx.TAP) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val shape = t.shape(10.dp)
    Box(
        modifier
            .size(52.dp)
            .then(if (enabled) Modifier.shapeGlow(t.accent.copy(alpha = 0.5f), shape, 10.dp) else Modifier)
            .clip(shape)
            .background(if (enabled) t.accent else t.muted.copy(alpha = 0.3f), shape)
            .clickable(enabled = enabled, role = Role.Button) {
                feedback.play(sfx)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = t.background)
    }
}
