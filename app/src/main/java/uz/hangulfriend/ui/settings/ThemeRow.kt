package uz.hangulfriend.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.hangulfriend.R
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.kit.shape
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.theme.tokensFor

/** Interface style picker: two cards, each previewing its theme's colours. */
@Composable
fun ThemeRow(current: GameThemeId, onPick: (GameThemeId) -> Unit) {
    val active = LocalGameTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GameThemeId.entries.forEach { id ->
                val t = tokensFor(id)
                val selected = id == current
                val shape = t.shape(8.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(id) })
                        .background(t.background, shape)
                        .border(if (selected) 2.dp else 1.dp, if (selected) active.accent else t.panelBorder, shape)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(t.panel, t.accent, t.accent2).forEach { c ->
                            Box(Modifier.size(18.dp).background(c, RoundedCornerShape(t.panelCorner / 3)).border(1.dp, t.panelBorder))
                        }
                    }
                    Text(
                        stringResource(if (id == GameThemeId.SYSTEM) R.string.theme_system else R.string.theme_neon),
                        color = t.text,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
