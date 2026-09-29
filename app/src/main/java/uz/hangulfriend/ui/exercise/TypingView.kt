package uz.hangulfriend.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import uz.hangulfriend.R
import uz.hangulfriend.hangul.AnswerChecker
import uz.hangulfriend.ui.session.ExerciseOutcome
import uz.hangulfriend.ui.session.feedbackText

/**
 * Free-text answer checked with [AnswerChecker]. [showSamplesOnWrong] lists every accepted answer
 * (translations have several valid forms).
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
) {
    val start by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var input by remember { mutableStateOf("") }
    var usedHint by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<FeedbackInfo?>(null) }
    val messages = FeedbackMessages()
    val keyboard = LocalSoftwareKeyboardController.current

    fun check() {
        if (feedback != null || input.isBlank()) return
        keyboard?.hide()
        val result = AnswerChecker.check(input, answers)
        feedback = FeedbackInfo(
            correct = result.correct,
            message = messages.of(feedbackText(result.feedback)),
            correctAnswer = result.closest,
            why = why?.takeIf { !result.correct },
            samples = if (!result.correct && showSamplesOnWrong) answers else emptyList(),
        )
        onResult(ExerciseOutcome.Checked(result.correct, usedHint, System.currentTimeMillis() - start, result))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PromptText(prompt)
        body?.invoke()
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
        if (shown == null) {
            Button(onClick = ::check, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ex_check))
            }
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
