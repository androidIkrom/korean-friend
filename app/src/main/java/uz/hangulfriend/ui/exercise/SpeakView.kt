package uz.hangulfriend.ui.exercise

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
import uz.hangulfriend.R
import uz.hangulfriend.hangul.Hangul
import uz.hangulfriend.hangul.SpeechScore
import uz.hangulfriend.hangul.SpeechScorer
import uz.hangulfriend.speech.SpeechError
import uz.hangulfriend.speech.SpeechEvent
import uz.hangulfriend.speech.SpeechInput
import uz.hangulfriend.ui.AudioButton
import uz.hangulfriend.ui.session.ExerciseOutcome
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.theme.WrongRed

val LocalSpeechInput = staticCompositionLocalOf<SpeechInput?> { null }

fun speakErrorText(e: SpeechError): Int = when (e) {
    SpeechError.NO_MATCH -> R.string.speak_err_no_match
    SpeechError.LANGUAGE_UNAVAILABLE -> R.string.speak_err_language
    SpeechError.NO_PERMISSION -> R.string.speak_err_permission
    SpeechError.NETWORK -> R.string.speak_err_network
    SpeechError.OTHER -> R.string.speak_err_other
}

/** [expected] with every Hangul syllable green, or red when [SpeechScore.mismatched] has its index. */
fun scoredText(expected: String, score: SpeechScore): AnnotatedString = buildAnnotatedString {
    expected.forEachIndexed { i, c ->
        if (!Hangul.isSyllable(c)) {
            append(c)
        } else {
            withStyle(SpanStyle(color = if (i in score.mismatched) WrongRed else CorrectGreen)) { append(c) }
        }
    }
}

private sealed interface SpeakPhase {
    data object Idle : SpeakPhase
    data object Listening : SpeakPhase
    data class Scored(val score: SpeechScore) : SpeakPhase
    data class Failed(val error: SpeechError) : SpeakPhase
}

/** Mic, recognizer and scoring for one sentence. [onScored] fires after each attempt. */
@Composable
fun SpeakPanel(expected: String, onScored: (SpeechScore) -> Unit, onFailed: (SpeechError) -> Unit = {}) {
    val speech = LocalSpeechInput.current
    val context = LocalContext.current
    var phase by remember { mutableStateOf<SpeakPhase>(SpeakPhase.Idle) }

    fun start() {
        val s = speech ?: run {
            phase = SpeakPhase.Failed(SpeechError.LANGUAGE_UNAVAILABLE).also { onFailed(it.error) }
            return
        }
        phase = SpeakPhase.Listening
        s.listen { event ->
            phase = when (event) {
                is SpeechEvent.Result -> SpeakPhase.Scored(SpeechScorer.score(expected, event.candidates)).also { onScored(it.score) }
                is SpeechEvent.Error -> SpeakPhase.Failed(event.reason).also { onFailed(it.error) }
            }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else phase = SpeakPhase.Failed(SpeechError.NO_PERMISSION).also { onFailed(it.error) }
    }

    fun onMic() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) start() else permission.launch(Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(speech) { onDispose { speech?.cancel() } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (val p = phase) {
            SpeakPhase.Idle -> Text(expected, style = MaterialTheme.typography.headlineSmall)
            SpeakPhase.Listening -> {
                Text(expected, style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.speak_listening), style = MaterialTheme.typography.titleMedium)
            }
            is SpeakPhase.Scored -> {
                Text(scoredText(expected, p.score), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.speak_result, (p.score.similarity * 100).roundToInt()))
                Text(stringResource(if (p.score.passed) R.string.speak_passed else R.string.speak_failed))
            }
            is SpeakPhase.Failed -> {
                Text(expected, style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(speakErrorText(p.error)), color = WrongRed)
            }
        }
        Button(onClick = ::onMic, enabled = phase != SpeakPhase.Listening, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Mic, contentDescription = null)
            Text(
                stringResource(
                    when (phase) {
                        is SpeakPhase.Scored -> R.string.speak_again
                        is SpeakPhase.Failed -> R.string.speak_retry
                        else -> R.string.speak_start
                    },
                ),
            )
        }
    }
}

/** Speaking exercise: only the first attempt counts; "O'tkazib yuborish" is always possible. */
@Composable
fun SpeakView(ko: String, uz: String, audio: String?, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    val start by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var submitted by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PromptText(stringResource(R.string.speak_prompt))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(uz, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            AudioButton(audio)
        }
        SpeakPanel(
            expected = ko,
            onScored = { score ->
                if (!submitted) {
                    submitted = true
                    onResult(ExerciseOutcome.Checked(score.passed, false, System.currentTimeMillis() - start, null))
                }
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!submitted) {
                OutlinedButton(
                    onClick = {
                        submitted = true
                        onResult(ExerciseOutcome.Skipped)
                        onNext()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.speak_skip)) }
            } else {
                Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.ex_next)) }
            }
        }
    }
}
