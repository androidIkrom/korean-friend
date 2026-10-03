package uz.hangulfriend.ui.exercise

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import uz.hangulfriend.R
import uz.hangulfriend.ai.TutorPrompts
import uz.hangulfriend.content.ExerciseType
import uz.hangulfriend.study.ExerciseItem
import uz.hangulfriend.ui.currentLanguage
import uz.hangulfriend.ui.kit.LocalGameFeedback
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.kit.shake
import uz.hangulfriend.ui.session.ExerciseOutcome
import uz.hangulfriend.ui.session.verdictSfx
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.tutor.LocalTutor

/**
 * Renders any [ExerciseItem]. Callers should wrap it in `key(...)` so each item starts with fresh state.
 * Every verdict plays its hit sound; a miss also shakes the item.
 */
@Composable
fun ExerciseView(item: ExerciseItem, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    val feedback = LocalGameFeedback.current
    var misses by rememberSaveable { mutableIntStateOf(0) }
    val hit: (ExerciseOutcome) -> Unit = { outcome ->
        outcome.verdictSfx()?.let { sfx ->
            feedback.play(sfx)
            if (sfx == Sfx.WRONG) misses++
        }
        onResult(outcome)
    }
    Column(Modifier.shake(misses)) {
        FlagButton(item)
        ExerciseBody(item, hit, onNext)
    }
}

@Composable
private fun ExerciseBody(item: ExerciseItem, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    when (item) {
        is ExerciseItem.Flashcard -> FlashcardView(item.word, onResult, onNext)
        is ExerciseItem.WordTyping -> TypingView(
            prompt = stringResource(R.string.ex_typing_prompt),
            body = { Text(item.word.uz, color = LocalGameTokens.current.text, style = MaterialTheme.typography.headlineSmall) },
            answers = listOf(item.word.ko),
            hint = null,
            why = null,
            showSamplesOnWrong = false,
            onResult = onResult,
            onNext = onNext,
        )
        is ExerciseItem.Match -> MatchView(item.words, onResult, onNext)
        is ExerciseItem.WordChoose -> WordChooseView(item.word, item.options, onResult, onNext)
        is ExerciseItem.ListenChoose -> ListenChooseView(item.word, item.options, onResult, onNext)
        is ExerciseItem.Dictation -> DictationView(item.word, onResult, onNext)
        is ExerciseItem.Speak -> SpeakView(item.ko, item.uz, item.audio, onResult, onNext)
        is ExerciseItem.Authored -> {
            val e = item.exercise
            val lang = currentLanguage()
            val ask: (String) -> String = { answer -> TutorPrompts.mistakeQuestion(e, answer, lang) }
            val tutor = LocalTutor.current
            when (e.type) {
                ExerciseType.SITUATION_CHOICE ->
                    ChoiceView(e.promptUz, e.options.orEmpty(), e.answers, e.whyUz, onResult, onNext, aiQuestion = ask)
                ExerciseType.LISTEN_QUESTION -> ListenQuestionView(e, onResult, onNext)
                ExerciseType.READ_CHOICE ->
                    ChoiceView(e.promptUz, e.options.orEmpty(), e.answers, e.whyUz, onResult, onNext) {
                        Text(e.sentence.orEmpty(), color = LocalGameTokens.current.text, style = MaterialTheme.typography.titleMedium)
                    }
                ExerciseType.BUILD_SENTENCE ->
                    BuildSentenceView(e.promptUz, e.tokens.orEmpty(), e.answers, e.hintUz, e.whyUz, onResult, onNext, aiQuestion = ask)
                ExerciseType.CONJUGATE -> TypingView(
                    prompt = e.promptUz,
                    body = { KoreanText(stringResource(R.string.ex_conjugate_prompt, e.base.orEmpty(), e.form.orEmpty())) },
                    answers = e.answers, hint = e.hintUz, why = e.whyUz, showSamplesOnWrong = false,
                    onResult = onResult, onNext = onNext, aiQuestion = ask,
                )
                ExerciseType.FILL_BLANK, ExerciseType.FIND_ERROR -> TypingView(
                    prompt = e.promptUz,
                    body = { KoreanText(e.sentence.orEmpty()) },
                    answers = e.answers, hint = e.hintUz, why = e.whyUz, showSamplesOnWrong = false,
                    onResult = onResult, onNext = onNext, aiQuestion = ask,
                )
                ExerciseType.TRANSLATE -> TypingView(
                    prompt = e.promptUz,
                    body = { Text(e.sourceUz.orEmpty(), color = LocalGameTokens.current.text, style = MaterialTheme.typography.headlineSmall) },
                    answers = e.answers, hint = e.hintUz, why = e.whyUz, showSamplesOnWrong = true,
                    onResult = onResult, onNext = onNext, aiQuestion = ask,
                    aiCheck = tutor?.let { t -> { answer -> t.checkTranslation(e, answer, lang) } },
                )
            }
        }
    }
}
