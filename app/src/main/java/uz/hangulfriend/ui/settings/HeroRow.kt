package uz.hangulfriend.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.hangulfriend.R
import uz.hangulfriend.data.HeroGender
import uz.hangulfriend.study.Rank
import uz.hangulfriend.ui.avatar.Avatar
import uz.hangulfriend.ui.kit.shape
import uz.hangulfriend.ui.theme.LocalGameTokens

/** Boy / girl hero picker, each card showing the hero at rank D (onboarding and settings). */
@Composable
fun HeroRow(current: HeroGender, onPick: (HeroGender) -> Unit) {
    val t = LocalGameTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.hero_title), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroGender.entries.forEach { hero ->
                val selected = hero == current
                val shape = t.shape(8.dp)
                Column(
                    Modifier
                        .weight(1f)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(hero) })
                        .background(t.panel, shape)
                        .border(if (selected) 2.dp else 1.dp, if (selected) t.accent else t.panelBorder, shape)
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Avatar(Rank.D, hero, Modifier.size(72.dp, 88.dp))
                    Text(
                        stringResource(if (hero == HeroGender.BOY) R.string.hero_boy else R.string.hero_girl),
                        color = if (selected) t.accent else t.text,
                        fontFamily = t.ui,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
