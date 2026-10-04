package uz.hangulfriend.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import uz.hangulfriend.ai.TranslationVerdict
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import uz.hangulfriend.R
import uz.hangulfriend.hangul.AnswerChecker
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.session.ExerciseOutcome
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.session.feedbackText

/**
 * Free-text answer checked with [AnswerChecker]. [showSamplesOnWrong] lists every accepted answer
 * (translations have several valid forms). [aiQuestion] builds the "AI'dan so'rash" question from
 * the learner's answer. [aiCheck] gets a second opinion from the AI when the answer matches none of
 * [answers]; a null verdict (AI unavailable) keeps the plain check.
 */
@Composable
fun TypingView(
    prompt: String,
    body: (@Composable () -> Unit)?,
    answers: List<String>,
    hint: String?,
    why: String?,
    showSamplesOnWrong: Boolean,
    onResult: (ExerciseOutcome) -> Unit,
    onNext: () -> Unit,
    aiQuestion: ((String) -> String)? = null,
    aiCheck: (suspend (String) -> TranslationVerdict?)? = null,
) {
    val start by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    var usedHint by rememberSaveable { mutableStateOf(false) }
    var feedback by rememberSaveable(stateSaver = FeedbackInfoSaver) { mutableStateOf<FeedbackInfo?>(null) }
    val messages = FeedbackMessages()
    val keyboard = LocalSoftwareKeyboardController.current

    fun check() {
        if (feedback != null || checking || input.isBlank()) return
        keyboard?.hide()
        val answer = input
        val result = AnswerChecker.check(answer, answers)
        val elapsed = System.currentTimeMillis() - start
        val plain = FeedbackInfo(
            correct = result.correct,
            message = messages.of(feedbackText(result.feedback)),
            correctAnswer = result.closest,
            why = why?.takeIf { !result.correct },
            samples = if (!result.correct && showSamplesOnWrong) answers else emptyList(),
            askAi = aiQuestion?.invoke(answer),
        )
        if (result.correct || aiCheck == null) {
            feedback = plain
            onResult(ExerciseOutcome.Checked(result.correct, usedHint, elapsed, result))
            return
        }
        checking = true
        scope.launch {
            val verdict = aiCheck(answer)
            checking = false
            feedback = when {
                verdict == null -> plain
                verdict.correct -> FeedbackInfo(true, messages.of(R.string.ai_translation_ok), null, verdict.explanationUz)
                else -> plain.copy(correctAnswer = verdict.correctedKo, why = verdict.explanationUz)
            }
            onResult(ExerciseOutcome.Checked(verdict?.correct ?: false, usedHint, elapsed, result))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        QuestCard(prompt) { body?.invoke() }
        OutlinedTextField(
            value = input,
            onValueChange = { if (feedback == null) input = it },
            label = { Text(stringResource(R.string.ex_answer_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { check() }),
            modifier = Modifier.fillMaxWidth(),
        )
        HintButton(hint, enabled = feedback == null) { usedHint = true }
        val shown = feedback
        if (checking) {
            Text(stringResource(R.string.ai_checking), color = LocalGameTokens.current.muted)
        } else if (shown == null) {
            HuntButton(
                stringResource(R.string.ex_check),
                onClick = ::check,
                enabled = input.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            FeedbackPanel(shown, onNext)
        }
    }
}

/** Resolves string resources outside composition (inside click handlers). */
class FeedbackMessages internal constructor(private val resolve: (Int) -> String) {
    fun of(id: Int): String = resolve(id)
}

@Composable
fun FeedbackMessages(): FeedbackMessages {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return remember(ctx) { FeedbackMessages { ctx.getString(it) } }
}
