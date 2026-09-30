package uz.hangulfriend.ui.exercise

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import uz.hangulfriend.R
import uz.hangulfriend.ai.TutorPrompts
import uz.hangulfriend.content.ExerciseType
import uz.hangulfriend.ui.tutor.LocalTutor
import uz.hangulfriend.study.ExerciseItem
import uz.hangulfriend.ui.session.ExerciseOutcome

/** Renders any [ExerciseItem]. Callers should wrap it in `key(...)` so each item starts with fresh state. */
@Composable
fun ExerciseView(item: ExerciseItem, onResult: (ExerciseOutcome) -> Unit, onNext: () -> Unit) {
    when (item) {
        is ExerciseItem.Flashcard -> FlashcardView(item.word, onResult, onNext)
        is ExerciseItem.WordTyping -> TypingView(
            prompt = stringResource(R.string.ex_typing_prompt),
            body = { Text(item.word.uz, style = MaterialTheme.typography.headlineSmall) },
            answers = listOf(item.word.ko),
            hint = null,
            why = null,
            showSamplesOnWrong = false,
            onResult = onResult,
            onNext = onNext,
        )
        is ExerciseItem.Match -> MatchView(item.words, onResult, onNext)
        is ExerciseItem.ListenChoose -> ListenChooseView(item.word, item.options, onResult, onNext)
        is ExerciseItem.Dictation -> DictationView(item.word, onResult, onNext)
        is ExerciseItem.Speak -> SpeakView(item.ko, item.uz, item.audio, onResult, onNext)
        is ExerciseItem.Authored -> {
            val e = item.exercise
            val ask: (String) -> String = { answer -> TutorPrompts.mistakeQuestion(e, answer) }
            val tutor = LocalTutor.current
            when (e.type) {
                ExerciseType.SITUATION_CHOICE ->
                    ChoiceView(e.promptUz, e.options.orEmpty(), e.answers, e.whyUz, onResult, onNext, aiQuestion = ask)
                ExerciseType.LISTEN_QUESTION -> ListenQuestionView(e, onResult, onNext)
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
                    body = { Text(e.sourceUz.orEmpty(), style = MaterialTheme.typography.headlineSmall) },
                    answers = e.answers, hint = e.hintUz, why = e.whyUz, showSamplesOnWrong = true,
                    onResult = onResult, onNext = onNext, aiQuestion = ask,
                    aiCheck = tutor?.let { t -> { answer -> t.checkTranslation(e, answer) } },
                )
            }
        }
    }
}
