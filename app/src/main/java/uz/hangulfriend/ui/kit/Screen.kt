package uz.hangulfriend.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import uz.hangulfriend.R
import uz.hangulfriend.ui.theme.LocalGameTokens

/** A screen's title bar: a round glass back button, the title as written, then [trailing]. */
@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, trailing: @Composable RowScope.() -> Unit = {}) {
    val t = LocalGameTokens.current
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.session_close), onBack)
        Text(
            title,
            color = t.text,
            fontFamily = t.ui,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/** A 44dp round glass button with an icon (back, close). */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(44.dp)
            .springPress(interaction)
            .glass(CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                feedback.play(Sfx.TAP)
                onClick()
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = t.text, modifier = Modifier.size(20.dp))
    }
}

/** A filter chip: a glass pill, filled with the accent when [selected]; tap sound. */
@Composable
fun HuntChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .heightIn(min = 36.dp)
            .springPress(interaction)
            .then(if (selected) Modifier.clip(CircleShape).background(t.accent) else Modifier.glass(CircleShape))
            .selectable(selected = selected, interactionSource = interaction, indication = null, role = Role.Tab) {
                feedback.play(Sfx.TAP)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) t.background else t.text,
            fontFamily = t.ui,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}

/**
 * The app's dialog: a 28dp sheet in the theme's raised surface (a dialog window has no backdrop to blur),
 * with [title], [content] and a row of [actions].
 */
@Composable
fun SystemDialog(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = LocalGameTokens.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(t.raised)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, color = t.text, fontFamily = t.ui, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            content()
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), content = actions)
        }
    }
}

/** The radio mark: a round dot, filled with the accent and a check when [selected]. */
@Composable
fun DiamondMark(selected: Boolean, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    Box(
        modifier
            .size(22.dp)
            .background(if (selected) t.accent else Color.White.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = t.background, modifier = Modifier.size(14.dp))
    }
}

/** One choice in a list (a lesson to start from): mark, title and subtitle on a raised 16dp row. */
@Composable
fun SelectRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .springPress(interaction)
            .clip(shape)
            .background(if (selected) t.accent.copy(alpha = 0.2f) else t.raised.copy(alpha = 0.7f), shape)
            .selectable(selected = selected, interactionSource = interaction, indication = null, role = Role.RadioButton) {
                feedback.play(Sfx.TAP)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DiamondMark(selected)
        Column(Modifier.weight(1f)) {
            Text(title, color = t.text, fontFamily = t.ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = t.muted, fontFamily = t.ui, fontSize = 13.sp)
        }
    }
}

/** A round accent action button with an icon (vocabulary add, tutor send). */
@Composable
fun IconAction(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, sfx: Sfx = Sfx.TAP) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(56.dp)
            .springPress(interaction, enabled)
            .clip(CircleShape)
            .background(if (enabled) t.accent else t.muted.copy(alpha = 0.3f))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button) {
                feedback.play(sfx)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = t.background)
    }
}
