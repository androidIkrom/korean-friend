package uz.hangulfriend.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hangulfriend.R
import uz.hangulfriend.content.LetterKind
import uz.hangulfriend.hangul.Feedback
import uz.hangulfriend.hangul.Hangul
import uz.hangulfriend.study.ExerciseItem
import uz.hangulfriend.ui.AudioButton
import uz.hangulfriend.ui.LocalAudioPlayer
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.session.ExerciseOutcome
import uz.hangulfriend.ui.session.feedbackText
import uz.hangulfriend.ui.theme.LocalGameTokens

/** A jamo or syllable shown large; a final consonant carries its 받침 label. */
@Composable
fun BigLetter(text: String, final: Boolean = false) {
    val t = LocalGameTokens.current
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text, color = t.text, fontSize = 56.sp, lineHeight = 64.sp, fontWeight = FontWeight.Medium)
        if (final) Text(stringResource(R.string.hangul_final_label), color = t.muted, fontFamily = t.ui, fontSize = 13.sp)
    }
}

@Composable
private fun PlayOnce(file: String?) {
    val player = LocalAudioPlayer.current
    LaunchedEffect(file) { if (file != null) player?.play(file) }
}

@Composable
fun LetterSoundView(item: ExerciseItem.LetterSound, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    val l = item.letter
    ChoiceView(
        prompt = stringResource(R.string.ex_letter_sound_prompt),
        options = item.options.map { it.roman },
        answers = listOf(l.roman),
        why = l.tipUz,
        onResult = onResult,
        onNext = onNext,
    ) { BigLetter(l.jamo, final = l.kind == LetterKind.FINAL) }
}

@Composable
fun LetterListenView(item: ExerciseItem.LetterListen, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    val l = item.letter
    PlayOnce(l.audio)
    ChoiceView(
        prompt = stringResource(if (l.kind == LetterKind.FINAL) R.string.ex_letter_listen_final_prompt else R.string.ex_letter_listen_prompt),
        options = item.options.map { it.jamo },
        answers = listOf(l.jamo),
        why = "${l.say} · ${l.roman}",
        onResult = onResult,
        onNext = onNext,
    ) { AudioButton(l.audio) }
}

@Composable
fun ReadWordView(item: ExerciseItem.ReadWord, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    val ex = item.letter.example
    ChoiceView(
        prompt = stringResource(R.string.ex_read_word_prompt),
        options = item.options.map { it.example.roman },
        answers = listOf(ex.roman),
        why = "${ex.ko} — ${ex.uz}",
        onResult = onResult,
        onNext = onNext,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BigLetter(ex.ko)
            AudioButton(ex.audio)
        }
    }
}

/** Tap one part per row; the syllable they compose shows as it grows. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuildSyllableView(item: ExerciseItem.BuildSyllable, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    val t = LocalGameTokens.current
    val start by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var initial by rememberSaveable { mutableStateOf<Char?>(null) }
    var medial by rememberSaveable { mutableStateOf<Char?>(null) }
    var final by rememberSaveable { mutableStateOf<Char?>(null) }
    var feedback by rememberSaveable(stateSaver = FeedbackInfoSaver) { mutableStateOf<FeedbackInfo?>(null) }
    val messages = FeedbackMessages()
    val parts = Hangul.parts(item.target)!!
    val needsFinal = item.finals.isNotEmpty()
    val built = initial?.let { i -> medial?.let { m -> Hangul.compose(i, m, final) } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        QuestCard(stringResource(R.string.ex_build_syllable_prompt)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BigLetter(item.target.toString())
                if (item.audio != null) AudioButton(item.audio)
            }
            Text(
                listOfNotNull(initial, medial, final).joinToString(" + ").ifEmpty { "…" } + (built?.let { " = $it" } ?: ""),
                color = if (built == null) t.muted else t.accent,
                fontSize = 22.sp,
            )
        }
        val rows = buildList {
            add(Triple(R.string.ex_build_initial, item.initials) { c: Char -> initial = c })
            add(Triple(R.string.ex_build_vowel, item.medials) { c: Char -> medial = c })
            if (needsFinal) add(Triple(R.string.ex_build_final, item.finals) { c: Char -> final = c })
        }
        val picked = listOf(initial, medial, final)
        rows.forEachIndexed { row, (label, choices, pick) ->
            Text(stringResource(label), color = t.muted, fontFamily = t.ui, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { c ->
                    HuntButton(
                        c.toString(),
                        onClick = { if (feedback == null) pick(c) },
                        style = if (picked[row] == c) HuntStyle.PRIMARY else HuntStyle.SECONDARY,
                        minHeight = 56.dp,
                        fontSize = 28,
                        horizontalPadding = 18.dp,
                    )
                }
            }
        }
        val shown = feedback
        if (shown == null) {
            HuntButton(
                stringResource(R.string.ex_check),
                onClick = {
                    val correct = built == item.target
                    feedback = FeedbackInfo(
                        correct = correct,
                        message = messages.of(feedbackText(if (correct) Feedback.CORRECT else Feedback.WRONG)),
                        correctAnswer = listOfNotNull(parts.initial, parts.medial, parts.final).joinToString(" + ") + " = ${item.target}",
                        why = null,
                    )
                    onResult(ExerciseOutcome.Checked(correct, false, System.currentTimeMillis() - start, null))
                },
                enabled = initial != null && medial != null && (!needsFinal || final != null),
            )
        } else {
            FeedbackPanel(shown, onNext)
        }
    }
}
