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
import androidx.compose.runtime.saveable.rememberSaveable
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

/**
 * Tap a Korean word, then its meaning. A word counts as correct only if its first pairing was right.
 * [showNext] is false where the host screen already has its own "Keyingi" button.
 */
@Composable
fun MatchView(words: List<Word>, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit, showNext: Boolean = true) {
    // Column orders are kept as word ids so they survive rotation.
    val leftIds = rememberSaveable(words) { ArrayList(words.shuffled().map { it.id }) }
    val rightIds = rememberSaveable(words) { ArrayList(words.shuffled().map { it.id }) }
    val left = remember(leftIds) { leftIds.mapNotNull { id -> words.find { it.id == id } } }
    val right = remember(rightIds) { rightIds.mapNotNull { id -> words.find { it.id == id } } }
    val matched = rememberSaveable(saver = StringListSaver) { mutableStateListOf<String>() }
    val missed = rememberSaveable(saver = StringListSaver) { mutableStateListOf<String>() }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
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

    val matchedColors = ButtonDefaults.outlinedButtonColors(
        containerColor = CorrectGreen.copy(alpha = 0.15f),
        contentColor = CorrectGreen,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PromptText(stringResource(R.string.ex_match_prompt))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                left.forEach { w ->
                    val colors = when {
                        w.id in matched -> matchedColors
                        w.id == selected -> ButtonDefaults.outlinedButtonColors(containerColor = ButtonDefaults.buttonColors().containerColor.copy(alpha = 0.2f))
                        else -> ButtonDefaults.outlinedButtonColors()
                    }
                    OutlinedButton(
                        onClick = { if (w.id !in matched) selected = w.id },
                        colors = colors,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(w.ko) }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                right.forEach { w ->
                    val colors = when {
                        w.id in matched -> matchedColors
                        w.id == wrongFlash -> ButtonDefaults.outlinedButtonColors(contentColor = WrongRed)
                        else -> ButtonDefaults.outlinedButtonColors()
                    }
                    OutlinedButton(
                        onClick = {
                            if (w.id in matched) return@OutlinedButton
                            val pick = selected ?: return@OutlinedButton
                            if (pick == w.id) {
                                matched += w.id
                            } else {
                                if (pick !in missed) missed += pick
                                wrongFlash = w.id
                            }
                            selected = null
                        },
                        colors = colors,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(w.uz, maxLines = 2) }
                }
            }
        }
        if (done && showNext) Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ex_next)) }
    }
}
