package uz.hangulfriend.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import uz.hangulfriend.R
import uz.hangulfriend.content.Word
import uz.hangulfriend.ui.session.ExerciseOutcome
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.theme.WrongRed

/** Tap a Korean word, then its meaning. A word counts as correct only if its first pairing was right. */
@Composable
fun MatchView(words: List<Word>, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    val left = remember(words) { words.shuffled() }
    val right = remember(words) { words.shuffled() }
    val matched = remember { mutableStateListOf<String>() }
    val missed = remember { mutableStateListOf<String>() }
    var selected by remember { mutableStateOf<String?>(null) }
    var wrongFlash by remember { mutableStateOf<String?>(null) }
    val done = matched.size == words.size

    LaunchedEffect(wrongFlash) {
        if (wrongFlash != null) {
            delay(600)
            wrongFlash = null
        }
    }
    LaunchedEffect(done) {
        if (done) onResult(ExerciseOutcome.Matched(words.map { it.id }.toSet() - missed.toSet(), words.size))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PromptText(stringResource(R.string.ex_match_prompt))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                left.forEach { w ->
                    val colors = when {
                        w.id in matched -> ButtonDefaults.outlinedButtonColors(contentColor = CorrectGreen)
                        w.id == selected -> ButtonDefaults.outlinedButtonColors(containerColor = ButtonDefaults.buttonColors().containerColor.copy(alpha = 0.2f))
                        else -> ButtonDefaults.outlinedButtonColors()
                    }
                    OutlinedButton(
                        onClick = { if (w.id !in matched) selected = w.id },
                        enabled = w.id !in matched,
                        colors = colors,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(w.ko) }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                right.forEach { w ->
                    val colors = when {
                        w.id in matched -> ButtonDefaults.outlinedButtonColors(contentColor = CorrectGreen)
                        w.id == wrongFlash -> ButtonDefaults.outlinedButtonColors(contentColor = WrongRed)
                        else -> ButtonDefaults.outlinedButtonColors()
                    }
                    OutlinedButton(
                        onClick = {
                            val pick = selected ?: return@OutlinedButton
                            if (pick == w.id) {
                                matched += w.id
                            } else {
                                if (pick !in missed) missed += pick
                                wrongFlash = w.id
                            }
                            selected = null
                        },
                        enabled = w.id !in matched,
                        colors = colors,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(w.uz, maxLines = 2) }
                }
            }
        }
        if (done) Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ex_next)) }
    }
}
