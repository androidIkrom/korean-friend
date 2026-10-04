package uz.hangulfriend.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hangulfriend.R
import uz.hangulfriend.content.Word
import uz.hangulfriend.hangul.AnswerChecker
import uz.hangulfriend.hangul.Feedback
import uz.hangulfriend.srs.Rating
import uz.hangulfriend.ui.AudioButton
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntPanel
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.OptionState
import uz.hangulfriend.ui.kit.OptionTile
import uz.hangulfriend.ui.session.ExerciseOutcome
import uz.hangulfriend.ui.session.feedbackText
import uz.hangulfriend.ui.theme.LocalGameTokens

/** One answer among lettered tiles; [questContent] (a sentence, the audio) goes inside the quest card. */
@Composable
fun ChoiceView(
    prompt: String,
    options: List<String>,
    answers: List<String>,
    why: String?,
    onResult: (ExerciseOutcome) -> Unit,
    onNext: () -> Unit,
    aiQuestion: ((String) -> String)? = null,
    questContent: @Composable ColumnScope.() -> Unit = {},
) {
    val start by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    val shuffled = rememberSaveable(options) { ArrayList(options.shuffled()) }
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    val messages = FeedbackMessages()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        QuestCard(prompt, questContent)
        shuffled.forEachIndexed { i, option ->
            val chosen = picked
            val state = when {
                chosen == null -> OptionState.IDLE
                option in answers -> OptionState.RIGHT
                option == chosen -> OptionState.WRONG
                else -> OptionState.DIM
            }
            OptionTile(
                text = option,
                onClick = {
                    if (picked == null) {
                        picked = option
                        onResult(ExerciseOutcome.Checked(option in answers, false, System.currentTimeMillis() - start, null))
                    }
                },
                state = state,
                letter = 'A' + i,
                enabled = chosen == null,
            )
        }
        picked?.let { chosen ->
            val correct = chosen in answers
            FeedbackPanel(
                FeedbackInfo(
                    correct = correct,
                    message = messages.of(feedbackText(if (correct) Feedback.CORRECT else Feedback.WRONG)),
                    correctAnswer = answers.first(),
                    why = why?.takeIf { !correct },
                    askAi = aiQuestion?.invoke(chosen),
                ),
                onNext,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuildSentenceView(
    prompt: String,
    tokens: List<String>,
    answers: List<String>,
    hint: String?,
    why: String?,
    onResult: (ExerciseOutcome) -> Unit,
    onNext: () -> Unit,
    aiQuestion: ((String) -> String)? = null,
) {
    val t = LocalGameTokens.current
    val start by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    val pool = rememberSaveable(tokens) { ArrayList(tokens.indices.shuffled()) }
    val chosen = rememberSaveable(saver = IntListSaver) { mutableStateListOf<Int>() }
    var usedHint by rememberSaveable { mutableStateOf(false) }
    var feedback by rememberSaveable(stateSaver = FeedbackInfoSaver) { mutableStateOf<FeedbackInfo?>(null) }
    val messages = FeedbackMessages()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        QuestCard(prompt) {
            Text(
                chosen.joinToString(" ") { tokens[it] }.ifEmpty { "…" },
                color = if (chosen.isEmpty()) t.muted else t.accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 30.sp,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            pool.filter { it !in chosen }.forEach { i ->
                HuntButton(
                    tokens[i],
                    onClick = { if (feedback == null) chosen += i },
                    style = HuntStyle.SECONDARY,
                    minHeight = 42.dp,
                    fontSize = 17,
                    horizontalPadding = 14.dp,
                )
            }
        }
        HintButton(hint, enabled = feedback == null) { usedHint = true }
        val shown = feedback
        if (shown == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                HuntButton(
                    stringResource(R.string.ex_clear),
                    onClick = { chosen.clear() },
                    style = HuntStyle.SECONDARY,
                    icon = Icons.AutoMirrored.Outlined.Backspace,
                    enabled = chosen.isNotEmpty(),
                    fontSize = 13,
                    horizontalPadding = 12.dp,
                )
                HuntButton(
                    stringResource(R.string.ex_check),
                    onClick = {
                        val built = chosen.joinToString(" ") { tokens[it] }
                        val result = AnswerChecker.check(built, answers)
                        feedback = FeedbackInfo(
                            result.correct,
                            messages.of(feedbackText(result.feedback)),
                            result.closest,
                            why?.takeIf { !result.correct },
                            askAi = aiQuestion?.invoke(built),
                        )
                        onResult(ExerciseOutcome.Checked(result.correct, usedHint, System.currentTimeMillis() - start, result))
                    },
                    enabled = chosen.size == tokens.size,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            FeedbackPanel(shown, onNext)
        }
    }
}

@Composable
fun FlashcardView(word: Word, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    val t = LocalGameTokens.current
    var revealed by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        QuestCard(stringResource(R.string.ex_flashcard_prompt)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(word.ko, color = t.text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                AudioButton(word.audio)
            }
        }
        if (!revealed) {
            HuntButton(stringResource(R.string.ex_show), onClick = { revealed = true }, modifier = Modifier.fillMaxWidth())
        } else {
            HuntPanel(accent = t.accent2) {
                Text(word.uz, color = t.text, style = MaterialTheme.typography.titleLarge)
                Text(word.exampleKo, color = t.text, style = MaterialTheme.typography.bodyLarge)
                Text(word.exampleUz, color = t.muted, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Triple(Rating.AGAIN, R.string.rate_again, HuntStyle.DANGER),
                    Triple(Rating.HARD, R.string.rate_hard, HuntStyle.SECONDARY),
                    Triple(Rating.GOOD, R.string.rate_good, HuntStyle.PRIMARY),
                    Triple(Rating.EASY, R.string.rate_easy, HuntStyle.SUCCESS),
                ).forEach { (rating, label, style) ->
                    HuntButton(
                        stringResource(label),
                        onClick = {
                            onResult(ExerciseOutcome.Rated(rating))
                            onNext()
                        },
                        modifier = Modifier.weight(1f),
                        style = style,
                        minHeight = 46.dp,
                        fontSize = 13,
                        horizontalPadding = 4.dp,
                    )
                }
            }
        }
    }
}

/** Read a Korean word, pick its meaning. */
@Composable
fun WordChooseView(word: Word, options: List<Word>, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    ChoiceView(
        prompt = stringResource(R.string.ex_word_choose_prompt),
        options = options.map { it.uz },
        answers = listOf(word.uz),
        why = null,
        onResult = onResult,
        onNext = onNext,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(word.ko, color = LocalGameTokens.current.text, style = MaterialTheme.typography.headlineSmall)
            if (word.audio != null) AudioButton(word.audio)
        }
    }
}
