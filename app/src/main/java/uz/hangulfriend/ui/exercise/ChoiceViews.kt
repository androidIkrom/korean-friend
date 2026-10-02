package uz.hangulfriend.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.hangulfriend.R
import uz.hangulfriend.content.Word
import uz.hangulfriend.hangul.AnswerChecker
import uz.hangulfriend.hangul.Feedback
import uz.hangulfriend.srs.Rating
import uz.hangulfriend.ui.AudioButton
import uz.hangulfriend.ui.session.ExerciseOutcome
import uz.hangulfriend.ui.session.feedbackText
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.theme.WrongRed

@Composable
fun ChoiceView(
    prompt: String,
    options: List<String>,
    answers: List<String>,
    why: String?,
    onResult: (ExerciseOutcome) -> Unit,
    onNext: () -> Unit,
    aiQuestion: ((String) -> String)? = null,
) {
    val start by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    val shuffled = rememberSaveable(options) { ArrayList(options.shuffled()) }
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    val messages = FeedbackMessages()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PromptText(prompt)
        shuffled.forEach { option ->
            val chosen = picked
            val colors = when {
                chosen == null -> ButtonDefaults.outlinedButtonColors()
                option in answers -> ButtonDefaults.outlinedButtonColors(contentColor = CorrectGreen)
                option == chosen -> ButtonDefaults.outlinedButtonColors(contentColor = WrongRed)
                else -> ButtonDefaults.outlinedButtonColors()
            }
            OutlinedButton(
                onClick = {
                    if (picked == null) {
                        picked = option
                        onResult(ExerciseOutcome.Checked(option in answers, false, System.currentTimeMillis() - start, null))
                    }
                },
                colors = colors,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(option, style = MaterialTheme.typography.titleMedium) }
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
    val start by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    val pool = rememberSaveable(tokens) { ArrayList(tokens.indices.shuffled()) }
    val chosen = rememberSaveable(saver = IntListSaver) { mutableStateListOf<Int>() }
    var usedHint by rememberSaveable { mutableStateOf(false) }
    var feedback by rememberSaveable(stateSaver = FeedbackInfoSaver) { mutableStateOf<FeedbackInfo?>(null) }
    val messages = FeedbackMessages()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PromptText(prompt)
        Text(
            chosen.joinToString(" ") { tokens[it] }.ifEmpty { "…" },
            style = MaterialTheme.typography.headlineSmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            pool.filter { it !in chosen }.forEach { i ->
                AssistChip(onClick = { if (feedback == null) chosen += i }, label = { Text(tokens[i]) })
            }
        }
        HintButton(hint, enabled = feedback == null) { usedHint = true }
        val shown = feedback
        if (shown == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { chosen.clear() }) { Text(stringResource(R.string.ex_clear)) }
                Button(
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
                ) { Text(stringResource(R.string.ex_check)) }
            }
        } else {
            FeedbackPanel(shown, onNext)
        }
    }
}

@Composable
fun FlashcardView(word: Word, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    var revealed by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PromptText(stringResource(R.string.ex_flashcard_prompt))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            KoreanText(word.ko)
            AudioButton(word.audio)
        }
        if (!revealed) {
            Button(onClick = { revealed = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ex_show)) }
        } else {
            Text(word.uz, style = MaterialTheme.typography.titleLarge)
            Text(word.exampleKo, style = MaterialTheme.typography.bodyLarge)
            Text(word.exampleUz, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    Rating.AGAIN to R.string.rate_again,
                    Rating.HARD to R.string.rate_hard,
                    Rating.GOOD to R.string.rate_good,
                    Rating.EASY to R.string.rate_easy,
                ).forEach { (rating, label) ->
                    OutlinedButton(
                        onClick = {
                            onResult(ExerciseOutcome.Rated(rating))
                            onNext()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(label), maxLines = 1) }
                }
            }
        }
    }
}
