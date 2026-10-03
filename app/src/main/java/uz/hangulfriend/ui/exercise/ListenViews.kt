package uz.hangulfriend.ui.exercise

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import uz.hangulfriend.R
import uz.hangulfriend.content.Exercise
import uz.hangulfriend.content.Word
import uz.hangulfriend.ui.AudioButton
import uz.hangulfriend.ui.LocalAudioPlayer
import uz.hangulfriend.ui.session.ExerciseOutcome

/** Plays [file] once when the exercise appears. */
@Composable
private fun AutoPlay(file: String?) {
    val player = LocalAudioPlayer.current
    LaunchedEffect(file) { if (file != null) player?.play(file) }
}

@Composable
fun ListenChooseView(word: Word, options: List<Word>, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    AutoPlay(word.audio)
    ChoiceView(
        prompt = stringResource(R.string.ex_listen_choose_prompt),
        options = options.map { it.uz },
        answers = listOf(word.uz),
        why = word.ko,
        onResult = onResult,
        onNext = onNext,
    ) { AudioButton(word.audio) }
}

@Composable
fun DictationView(word: Word, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    AutoPlay(word.audio)
    TypingView(
        prompt = stringResource(R.string.ex_dictation_prompt),
        body = { AudioButton(word.audio) },
        answers = listOf(word.ko),
        hint = null,
        why = word.uz,
        showSamplesOnWrong = false,
        onResult = onResult,
        onNext = onNext,
    )
}

@Composable
fun ListenQuestionView(e: Exercise, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    AutoPlay(e.audio)
    ChoiceView(e.promptUz, e.options.orEmpty(), e.answers, e.whyUz ?: e.audioText, onResult, onNext) { AudioButton(e.audio) }
}
