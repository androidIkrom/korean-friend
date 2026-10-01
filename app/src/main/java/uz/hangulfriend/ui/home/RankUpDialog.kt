package uz.hangulfriend.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import uz.hangulfriend.R
import uz.hangulfriend.study.Rank
import uz.hangulfriend.ui.avatar.Avatar
import uz.hangulfriend.ui.avatar.newLayersAt
import uz.hangulfriend.ui.avatar.titleRes
import uz.hangulfriend.ui.theme.GameBackground
import uz.hangulfriend.ui.theme.GameButton
import uz.hangulfriend.ui.theme.GamePanel
import uz.hangulfriend.ui.theme.LocalGameTokens

data class RankUp(val from: Rank, val to: Rank, val level: Int)

/** Full-screen "[ SYSTEM ] rank up" announcement (mockup "Rank oshdi · Tizim"). */
@Composable
fun RankUpDialog(rankUp: RankUp, onAccept: () -> Unit) {
    val t = LocalGameTokens.current
    Dialog(onDismissRequest = onAccept, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GameBackground {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Avatar(rankUp.to, Modifier.size(220.dp, 266.dp))
                GamePanel(null) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("[ SYSTEM ]", color = t.accent, fontFamily = t.display, letterSpacing = 3.sp, fontSize = 12.sp)
                        Text(stringResource(R.string.rankup_message, rankUp.level), color = t.muted, textAlign = TextAlign.Center)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text(rankUp.from.name, color = t.muted, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 40.sp)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = t.accent)
                            Text(rankUp.to.name, color = t.accent, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 60.sp)
                        }
                        Text(
                            stringResource(R.string.home_title, stringResource(rankUp.to.titleRes())).uppercase(),
                            color = t.text,
                            fontFamily = t.display,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 20.sp,
                        )
                    }
                    HorizontalDivider(color = t.panelBorder)
                    newLayersAt(rankUp.to).forEach { res ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = t.accent, modifier = Modifier.size(16.dp))
                            Text(stringResource(res), color = t.text)
                        }
                    }
                }
                GameButton(stringResource(R.string.rankup_accept), onClick = onAccept)
            }
        }
    }
}
