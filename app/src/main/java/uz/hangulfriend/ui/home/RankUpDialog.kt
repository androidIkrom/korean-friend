package uz.hangulfriend.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import uz.hangulfriend.data.HeroGender
import uz.hangulfriend.study.Rank
import uz.hangulfriend.ui.avatar.Avatar
import uz.hangulfriend.ui.avatar.newLayersAt
import uz.hangulfriend.ui.avatar.titleRes
import uz.hangulfriend.ui.theme.GameBackground
import uz.hangulfriend.ui.kit.GateRays
import uz.hangulfriend.ui.kit.Gold
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.LocalGameFeedback
import uz.hangulfriend.ui.kit.LocalReducedMotion
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.theme.GamePanel
import uz.hangulfriend.ui.theme.LocalGameTokens

data class RankUp(val from: Rank, val to: Rank, val level: Int, val hero: HeroGender)

/** Full-screen "SYSTEM · rank up" announcement (mockup "Rank oshdi · Tizim"). */
@Composable
fun RankUpDialog(rankUp: RankUp, onAccept: () -> Unit) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val reduced = LocalReducedMotion.current
    var announced by rememberSaveable { mutableStateOf(false) }
    // One timeline: the flare bursts, then the new letter drops in.
    val p = remember { Animatable(if (announced || reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!announced) {
            announced = true
            feedback.play(Sfx.RANK_UP)
            p.animateTo(1f, tween(1400, easing = LinearEasing))
        }
    }
    val flare = (p.value / 0.6f).coerceIn(0f, 1f)
    val drop = FastOutSlowInEasing.transform(((p.value - 0.35f) / 0.4f).coerceIn(0f, 1f))
    Dialog(onDismissRequest = onAccept, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GameBackground {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    GateRays(Gold.copy(alpha = 0.28f), Modifier.size(320.dp))
                    if (flare < 1f) {
                        Box(
                            Modifier
                                .size(300.dp)
                                .scale(0.3f + 1.2f * flare)
                                .alpha(1f - flare)
                                .background(Brush.radialGradient(listOf(Gold.copy(alpha = 0.85f), Color.Transparent)), CircleShape),
                        )
                    }
                    Avatar(rankUp.to, rankUp.hero, Modifier.size(220.dp, 266.dp), animated = true)
                }
                GamePanel(null) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("SYSTEM", color = t.accent, fontFamily = t.ui, fontWeight = FontWeight.SemiBold, letterSpacing = 1.3.sp, fontSize = 11.sp)
                        Text(stringResource(R.string.rankup_message, rankUp.level), color = t.muted, textAlign = TextAlign.Center)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            Text(rankUp.from.name, color = t.muted, fontFamily = t.ui, fontWeight = FontWeight.Bold, fontSize = 40.sp)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = t.accent)
                            Text(
                                rankUp.to.name,
                                color = t.accent,
                                fontFamily = t.ui,
                                fontWeight = FontWeight.Bold,
                                fontSize = 60.sp,
                                modifier = Modifier.scale(2.4f - 1.4f * drop).alpha(drop),
                            )
                        }
                        Text(
                            stringResource(R.string.home_title, stringResource(rankUp.to.titleRes())),
                            color = t.text,
                            fontFamily = t.ui,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        )
                    }
                    HorizontalDivider(color = t.panelBorder)
                    newLayersAt(rankUp.to, rankUp.hero).forEach { res ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = t.accent, modifier = Modifier.size(16.dp))
                            Text(stringResource(res), color = t.text)
                        }
                    }
                }
                HuntButton(
                    stringResource(R.string.rankup_accept),
                    onClick = onAccept,
                    modifier = Modifier.fillMaxWidth(),
                    style = HuntStyle.GOLD,
                )
            }
        }
    }
}
