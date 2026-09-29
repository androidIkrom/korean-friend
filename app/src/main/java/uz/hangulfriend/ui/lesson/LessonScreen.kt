package uz.hangulfriend.ui.lesson

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.hangulfriend.R
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.study.ExerciseItem
import uz.hangulfriend.study.Grader
import uz.hangulfriend.study.SessionBuilder
import uz.hangulfriend.ui.session.ExerciseOutcome

class LessonViewModel(
    private val controller: LessonController,
    private val builder: SessionBuilder,
    private val grader: Grader,
) : ViewModel() {
    val state = controller.state

    init {
        viewModelScope.launch { controller.load() }
    }

    fun goToStage(stage: Int) {
        viewModelScope.launch { controller.goToStage(stage) }
    }

    fun vocabChunks(lesson: Lesson) = builder.vocabChunks(lesson)

    fun grammarCheck(lesson: Lesson, grammarId: String): ExerciseItem.Authored? = builder.grammarExercise(lesson, grammarId)

    fun grade(item: ExerciseItem, outcome: ExerciseOutcome) {
        viewModelScope.launch {
            when {
                item is ExerciseItem.Match && outcome is ExerciseOutcome.Matched -> grader.gradeMatch(item, outcome.firstTryCorrect)
                outcome is ExerciseOutcome.Checked -> grader.gradeAuto(item, outcome.correct, outcome.usedHint, outcome.elapsedMs)
                else -> Unit
            }
        }
    }
}

private val stageLabels = listOf(
    R.string.stage_vocab,
    R.string.stage_grammar,
    R.string.stage_dialogue,
    R.string.stage_practice,
    R.string.stage_test,
)

@Composable
fun LessonScreen(
    vm: LessonViewModel,
    onBack: () -> Unit,
    onStartPractice: (String) -> Unit,
    onStartTest: (String) -> Unit,
    onStartLessonReview: (String) -> Unit,
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val lesson = s.lesson
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
            Column(Modifier.weight(1f)) {
                Text(lesson?.titleKo.orEmpty(), style = MaterialTheme.typography.titleLarge)
                Text(lesson?.titleUz.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            }
        }
        when {
            s.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            lesson == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.coming_soon))
            }
            else -> {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    stageLabels.forEachIndexed { i, label ->
                        FilterChip(
                            selected = s.stage == i,
                            onClick = { vm.goToStage(i) },
                            label = { Text("${i + 1}. ${stringResource(label)}") },
                        )
                    }
                }
                Box(Modifier.weight(1f)) {
                    when (s.stage) {
                        0 -> VocabStage(lesson, vm) { vm.goToStage(1) }
                        1 -> GrammarStage(lesson, vm) { vm.goToStage(2) }
                        2 -> DialogueStage(lesson, s.characters) { vm.goToStage(3) }
                        3 -> StartStage(stringResource(R.string.practice_intro), stringResource(R.string.practice_start)) {
                            onStartPractice(lesson.id)
                        }
                        else -> StartStage(stringResource(R.string.test_intro), stringResource(R.string.test_start)) {
                            onStartTest(lesson.id)
                        }
                    }
                }
                OutlinedButton(
                    onClick = { onStartLessonReview(lesson.id) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) { Text(stringResource(R.string.lesson_review)) }
            }
        }
    }
}

@Composable
private fun StartStage(intro: String, button: String, onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(intro, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text(button) }
    }
}
