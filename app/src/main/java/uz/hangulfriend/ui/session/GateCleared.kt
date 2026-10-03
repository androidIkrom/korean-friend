package uz.hangulfriend.ui.session

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import uz.hangulfriend.R
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.study.GameRules
import uz.hangulfriend.ui.game.achievementTitle
import uz.hangulfriend.ui.kit.GateRays
import uz.hangulfriend.ui.kit.GlowBar
import uz.hangulfriend.ui.kit.Gold
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntPanel
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.LocalGameFeedback
import uz.hangulfriend.ui.kit.LocalReducedMotion
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.kit.cutShape
import uz.hangulfriend.ui.kit.shapeGlow
import uz.hangulfriend.ui.theme.LocalGameTokens

/** Progress of a sub-step running from [from] to [to] on the reveal timeline [p] (0..1). */
private fun seg(p: Float, from: Float, to: Float): Float = ((p - from) / (to - from)).coerceIn(0f, 1f)

private const val REVEAL_MS = 2400
private const val MAX_LOOT = 8

/**
 * The end of a run: rays and the grade letter dropping in, stars landing one by one, XP counting up
 * into the level bar, then the stats, new badges and the words won as loot. The reveal plays once;
 * after rotation it shows its end state.
 */
@Composable
fun GateClearedView(s: SessionState, mode: SessionMode, onClose: () -> Unit) {
    if (s.items.isEmpty()) {
        EmptyRun(onClose)
        return
    }
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val reduced = LocalReducedMotion.current
    val cleared = s.cleared(mode)
    val percent = s.final?.percent ?: s.scorePercent
    val grade = GameRules.clearGrade(percent, s.failed)
    val stars = GameRules.clearStars(percent, s.failed)
    val color = if (cleared) t.accent else t.danger
    var revealed by rememberSaveable { mutableStateOf(false) }
    val p = remember { Animatable(if (revealed || reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!revealed) {
            revealed = true
            feedback.play(if (cleared) Sfx.CLEAR else Sfx.WRONG)
            p.animateTo(1f, tween(REVEAL_MS, easing = LinearEasing))
        }
    }
    val v = p.value
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
            GateRays(color.copy(alpha = 0.32f), Modifier.fillMaxSize().alpha(seg(v, 0f, 0.15f)))
            val drop = FastOutSlowInEasing.transform(seg(v, 0.05f, 0.3f))
            val gradeDesc = stringResource(R.string.clear_grade, grade)
            Text(
                grade,
                color = if (cleared) gradeColor(grade) else t.danger,
                fontFamily = t.display,
                fontWeight = FontWeight.Bold,
                fontSize = 104.sp,
                modifier = Modifier
                    .semantics { contentDescription = gradeDesc }
                    .scale(2.4f - 1.4f * drop)
                    .alpha(drop),
            )
        }
        Text(
            stringResource(if (cleared) R.string.clear_title else R.string.clear_failed).uppercase(),
            color = color,
            fontFamily = t.display,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            letterSpacing = 3.sp,
            textAlign = TextAlign.Center,
        )
        val starsDesc = stringResource(R.string.clear_stars, stars)
        Row(Modifier.clearAndSetSemantics { contentDescription = starsDesc }, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) { i ->
                val land = seg(v, 0.3f + 0.1f * i, 0.42f + 0.1f * i)
                val on = i < stars
                Icon(
                    if (on) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = if (on) Gold else t.muted.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(if (i == 1) 52.dp else 42.dp)
                        .scale(1.8f - 0.8f * FastOutSlowInEasing.transform(land))
                        .alpha(land)
                        .then(if (on && land >= 1f) Modifier.shapeGlow(Gold.copy(alpha = 0.35f), RoundedCornerShape(50), 10.dp) else Modifier),
                )
            }
        }
        RewardPanel(s, mode, seg(v, 0.55f, 0.9f))
        if (s.loot.isNotEmpty()) LootPanel(s, seg(v, 0.85f, 1f))
        HuntButton(
            stringResource(if (cleared) R.string.clear_claim else R.string.session_finish).uppercase(),
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().alpha(0.3f + 0.7f * seg(v, 0.6f, 1f)),
            style = if (cleared) HuntStyle.GOLD else HuntStyle.PRIMARY,
            sfx = if (cleared) Sfx.XP else Sfx.TAP,
        )
    }
}

/** S is gold, A violet, B the accent, lower grades muted. */
@Composable
private fun gradeColor(grade: String): Color {
    val t = LocalGameTokens.current
    return when (grade) {
        "S" -> Gold
        "A" -> t.accent2
        "B" -> t.accent
        else -> t.text
    }
}

/** XP counting up into the level bar, then the run's numbers. [xpP] runs the count (0..1). */
@Composable
private fun RewardPanel(s: SessionState, mode: SessionMode, xpP: Float) {
    val t = LocalGameTokens.current
    HuntPanel {
        if (s.xpEarned > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "+${(s.xpEarned * xpP).roundToInt()} XP",
                    color = Gold,
                    fontFamily = t.display,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    modifier = Modifier.weight(1f),
                )
                s.totalXpAfter?.let { total ->
                    Text("LV ${GameRules.level(total).level}", color = t.text, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            s.totalXpAfter?.let { total ->
                val after = GameRules.level(total)
                val before = GameRules.level((total - s.xpEarned).coerceAtLeast(0))
                val from = if (after.level > before.level) 0f else before.xpIntoLevel.toFloat() / before.xpForNext
                val to = after.xpIntoLevel.toFloat() / after.xpForNext
                GlowBar(from + (to - from) * xpP, height = 10)
            }
        }
        Text(stringResource(R.string.session_result, s.correctCount, s.items.size, s.scorePercent), color = t.text, fontSize = 16.sp)
        s.final?.let { f ->
            Text(stringResource(R.string.final_listening, f.listening, f.listeningTotal), color = t.text)
            Text(stringResource(R.string.final_reading, f.reading, f.readingTotal), color = t.text)
            Text(
                stringResource(
                    when (f.level) {
                        2 -> R.string.final_level2
                        1 -> R.string.final_level1
                        else -> R.string.final_level0
                    },
                ),
                color = t.text,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (mode == SessionMode.BOSS) {
            Text(stringResource(if (s.failed) R.string.session_boss_lost else R.string.session_boss_won), color = t.text, fontWeight = FontWeight.SemiBold)
        }
        if (mode == SessionMode.TEST) {
            val passed = s.scorePercent >= ProgressRepository.PASS_PERCENT
            Text(stringResource(if (passed) R.string.session_test_passed else R.string.session_test_failed), color = t.text, fontWeight = FontWeight.SemiBold)
        }
        s.newAchievements.forEach { id ->
            Text(stringResource(R.string.session_new_achievement, stringResource(achievementTitle(id))), color = Gold, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** The words won, as gold-framed loot chips (Korean over the meaning). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LootPanel(s: SessionState, appear: Float) {
    val t = LocalGameTokens.current
    val chip = if (t.id == GameThemeId.SYSTEM) cutShape(6.dp) else RoundedCornerShape(10.dp)
    HuntPanel(Modifier.alpha(appear), title = stringResource(R.string.clear_loot), accent = Gold) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            s.loot.take(MAX_LOOT).forEach { w ->
                Column(
                    Modifier
                        .background(Gold.copy(alpha = 0.1f), chip)
                        .border(1.dp, Gold.copy(alpha = 0.7f), chip)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(w.ko, color = t.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(w.uz, color = t.muted, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
        if (s.loot.size > MAX_LOOT) Text("+${s.loot.size - MAX_LOOT}", color = Gold, fontFamily = t.display)
    }
}

@Composable
private fun EmptyRun(onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HuntPanel {
            Text(stringResource(R.string.session_empty), color = LocalGameTokens.current.text, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        HuntButton(stringResource(R.string.session_finish).uppercase(), onClick = onClose, modifier = Modifier.fillMaxWidth())
    }
}
