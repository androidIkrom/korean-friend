package uz.hangulfriend.ui.exercise

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hangulfriend.R
import uz.hangulfriend.ai.TutorPrompts
import uz.hangulfriend.ui.currentLanguage
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntPanel
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.LocalReducedMotion
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.theme.WrongRed
import uz.hangulfriend.ui.tutor.LocalTutor
import uz.hangulfriend.ui.tutor.TutorSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.lerp
import uz.hangulfriend.ui.kit.glass

/** What the learner sees after checking an answer. */
data class FeedbackInfo(
    val correct: Boolean,
    val message: String,
    val correctAnswer: String?,
    val why: String?,
    val samples: List<String> = emptyList(),
    /** Question for the AI tutor about this mistake; the "AI'dan so'rash" button shows when set and wrong. */
    val askAi: String? = null,
)

/** Slides up and fades in on first appearance. */
@Composable
private fun Modifier.riseIn(): Modifier {
    val reduced = LocalReducedMotion.current
    val a = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) }
    val dy = with(LocalDensity.current) { 24.dp.toPx() }
    return graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * dy
    }
}

@Composable
fun FeedbackPanel(info: FeedbackInfo, onNext: () -> Unit) {
    val t = LocalGameTokens.current
    val color: Color = if (info.correct) CorrectGreen else WrongRed
    Column(
        Modifier
            .fillMaxWidth()
            .riseIn()
            .glass(RoundedCornerShape(24.dp), tint = lerp(t.surface, color, 0.28f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (info.correct) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
            Text(
                info.message,
                color = color,
                fontFamily = t.ui,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        if (!info.correct && info.correctAnswer != null) {
            Text(stringResource(R.string.ex_correct_answer, info.correctAnswer), color = t.text, style = MaterialTheme.typography.bodyLarge)
        }
        if (info.samples.isNotEmpty()) {
            Text(stringResource(R.string.ex_sample_answers), color = t.muted, style = MaterialTheme.typography.labelLarge)
            info.samples.forEach { Text("• $it", color = t.text) }
        }
        info.why?.let { Text(it, color = t.text, style = MaterialTheme.typography.bodyMedium) }
        val question = info.askAi
        if (!info.correct && question != null && LocalTutor.current != null) {
            var open by remember { mutableStateOf(false) }
            HuntButton(
                stringResource(R.string.ai_ask_mistake),
                onClick = { open = true },
                modifier = Modifier.fillMaxWidth(),
                style = HuntStyle.SECONDARY,
                icon = Icons.Outlined.AutoAwesome,
                minHeight = 44.dp,
                fontSize = 14,
            )
            if (open) TutorSheet(TutorPrompts.role(currentLanguage()), question) { open = false }
        }
        HuntButton(
            stringResource(R.string.ex_next),
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            style = if (info.correct) HuntStyle.SUCCESS else HuntStyle.PRIMARY,
        )
    }
}

/** A "Yordam" button that reveals [hint] and reports its first use. */
@Composable
fun HintButton(hint: String?, enabled: Boolean, onUsed: () -> Unit) {
    if (hint == null) return
    var shown by rememberSaveable { mutableStateOf(false) }
    if (shown) {
        Text("💡 $hint", color = LocalGameTokens.current.text, style = MaterialTheme.typography.bodyMedium)
    } else {
        HuntButton(
            stringResource(R.string.ex_hint),
            onClick = { shown = true; onUsed() },
            style = HuntStyle.SECONDARY,
            enabled = enabled,
            icon = Icons.Outlined.Lightbulb,
            minHeight = 40.dp,
            fontSize = 13,
        )
    }
}

/** The question on a glass card under a small "Quest" label; [content] (the Korean sentence, audio…) sits inside it. */
@Composable
fun QuestCard(prompt: String, content: @Composable ColumnScope.() -> Unit = {}) {
    val t = LocalGameTokens.current
    HuntPanel {
        Text(
            stringResource(R.string.battle_quest),
            color = t.accent,
            fontFamily = t.ui,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 1.3.sp,
        )
        Text(prompt, color = t.text, fontFamily = t.ui, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp)
        content()
    }
}

@Composable
fun KoreanText(text: String) {
    Text(text, color = LocalGameTokens.current.text, style = MaterialTheme.typography.headlineMedium)
}
