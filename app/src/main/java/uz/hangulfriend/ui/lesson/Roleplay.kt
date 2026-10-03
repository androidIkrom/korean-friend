package uz.hangulfriend.ui.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.hangulfriend.R
import uz.hangulfriend.content.Character
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.content.Line
import uz.hangulfriend.speech.SpeechError
import uz.hangulfriend.ui.AudioButton
import uz.hangulfriend.ui.LocalAudioPlayer
import uz.hangulfriend.ui.exercise.LocalSpeechInput
import uz.hangulfriend.ui.exercise.SpeakPanel
import uz.hangulfriend.ui.exercise.speakErrorText
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.theme.GameCard

data class RoleplayStep(val index: Int, val line: Line, val mine: Boolean)

fun roleplaySteps(lines: List<Line>, myRole: String): List<RoleplayStep> =
    lines.mapIndexed { i, line -> RoleplayStep(i, line, line.speaker == myRole) }

fun speakers(lines: List<Line>): List<String> = lines.map { it.speaker }.distinct()

/**
 * The learner picks a character and speaks that character's lines; the app plays the others.
 * Pronunciation practice only: nothing is graded.
 */
@Composable
fun RoleplayView(lesson: Lesson, characters: Map<String, Character>) {
    val speech = LocalSpeechInput.current
    if (speech == null || !speech.isAvailable()) {
        Text(stringResource(speakErrorText(SpeechError.LANGUAGE_UNAVAILABLE)))
        return
    }
    var role by rememberSaveable { mutableStateOf<String?>(null) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val name = { id: String -> characters[id]?.nameUz ?: id }
    val chosen = role

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (chosen == null) {
            Text(stringResource(R.string.roleplay_choose), style = MaterialTheme.typography.titleMedium)
            speakers(lesson.dialogue.lines).forEach { id ->
                HuntButton(name(id), onClick = { role = id; index = 0 }, modifier = Modifier.fillMaxWidth(), style = HuntStyle.SECONDARY)
            }
            return@Column
        }
        val steps = roleplaySteps(lesson.dialogue.lines, chosen)
        steps.take(index).forEach { s -> Text("${name(s.line.speaker)}: ${s.line.ko}", style = MaterialTheme.typography.bodyMedium) }
        val step = steps.getOrNull(index)
        if (step == null) {
            Text(stringResource(R.string.roleplay_done), style = MaterialTheme.typography.titleLarge)
            HuntButton(stringResource(R.string.roleplay_again), onClick = { index = 0 }, modifier = Modifier.fillMaxWidth())
            HuntButton(
                stringResource(R.string.roleplay_other),
                onClick = { role = null },
                modifier = Modifier.fillMaxWidth(),
                style = HuntStyle.SECONDARY,
            )
            return@Column
        }
        key(index) { RoleplayTurn(step, name(step.line.speaker)) { index++ } }
    }
}

@Composable
private fun RoleplayTurn(step: RoleplayStep, speaker: String, onNext: () -> Unit) {
    val player = LocalAudioPlayer.current
    if (!step.mine) LaunchedEffect(Unit) { step.line.audio?.let { player?.play(it) } }
    GameCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (step.mine) stringResource(R.string.roleplay_your_turn) else speaker, style = MaterialTheme.typography.labelLarge)
            Text(step.line.uz, style = MaterialTheme.typography.bodyMedium)
            if (step.mine) {
                SpeakPanel(expected = step.line.ko, onScored = {})
            } else {
                Text(step.line.ko, style = MaterialTheme.typography.headlineSmall)
                AudioButton(step.line.audio)
            }
            HuntButton(stringResource(R.string.ex_next).uppercase(), onClick = onNext, modifier = Modifier.fillMaxWidth())
        }
    }
}
