package uz.hangulfriend.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uz.hangulfriend.R
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.study.ExerciseItem
import uz.hangulfriend.study.Grader
import uz.hangulfriend.study.SessionBuilder
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntPanel
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.LocalGameFeedback
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.kit.pulseRing
import uz.hangulfriend.ui.kit.shape
import uz.hangulfriend.ui.session.ExerciseOutcome
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.study.LessonId

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

    fun refreshStatus() {
        viewModelScope.launch { controller.refreshStatus() }
    }

    fun vocabChunks(lesson: Lesson) = builder.vocabChunks(lesson)

    fun grammarCheck(lesson: Lesson, grammarId: String): ExerciseItem.Authored? = builder.grammarExercise(lesson, grammarId)

    private val gradeOnce = GradeOnce()

    /** Grades a lesson check; going back to an answered check with "Orqaga" does not grade it again. */
    fun grade(item: ExerciseItem, outcome: ExerciseOutcome) {
        if (!gradeOnce.shouldGrade(item)) return
        viewModelScope.launch {
            when {
                item is ExerciseItem.Match && outcome is ExerciseOutcome.Matched -> grader.gradeMatch(item, outcome.firstTryCorrect)
                outcome is ExerciseOutcome.Checked -> grader.gradeAuto(item, outcome.correct, outcome.usedHint, outcome.elapsedMs)
                else -> Unit
            }
        }
    }
}

/** Remembers which exercises were already graded in this lesson visit. */
class GradeOnce {
    private val seen = mutableSetOf<ExerciseItem>()

    fun shouldGrade(item: ExerciseItem): Boolean = seen.add(item)
}

private val stageLabels = listOf(
    R.string.stage_vocab,
    R.string.stage_grammar,
    R.string.stage_dialogue,
    R.string.stage_practice,
    R.string.stage_test,
)

private const val BOSS_FLOOR = STAGE_COUNT - 1

@Composable
fun LessonScreen(
    vm: LessonViewModel,
    onBack: () -> Unit,
    onStartPractice: (String) -> Unit,
    onStartTest: (String) -> Unit,
    onStartLessonReview: (String) -> Unit,
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val t = LocalGameTokens.current
    val lesson = s.lesson
    LifecycleResumeEffect(Unit) {
        vm.refreshStatus()
        onPauseOrDispose { }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = t.muted) }
            Column(Modifier.weight(1f)) {
                lesson?.let {
                    Text(
                        stringResource(R.string.gate_code, LessonId.parse(it.id)?.code ?: "${it.unit}-${it.lesson}"),
                        color = t.accent,
                        fontFamily = t.ui,
                        fontSize = 11.sp,
                    )
                }
                Text(lesson?.titleKo.orEmpty(), color = t.text, fontFamily = t.ui, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(lesson?.titleUz.orEmpty(), color = t.muted, fontSize = 13.sp)
            }
        }
        when {
            s.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            lesson == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.coming_soon), color = t.text)
            }
            else -> {
                FloorRail(floorStates(s.stage, s.status), onFloor = vm::goToStage)
                Box(Modifier.weight(1f)) {
                    when (s.stage) {
                        0 -> VocabStage(lesson, vm) { vm.goToStage(1) }
                        1 -> GrammarStage(lesson, vm) { vm.goToStage(2) }
                        2 -> DialogueStage(lesson, s.characters) { vm.goToStage(3) }
                        3 -> StartFloor(
                            title = stringResource(R.string.stage_practice),
                            intro = stringResource(R.string.practice_intro),
                            button = stringResource(R.string.practice_start),
                            boss = false,
                        ) { onStartPractice(lesson.id) }
                        else -> StartFloor(
                            title = stringResource(R.string.floor_boss),
                            intro = stringResource(R.string.test_intro),
                            button = stringResource(R.string.test_start),
                            boss = true,
                        ) { onStartTest(lesson.id) }
                    }
                }
                HuntButton(
                    stringResource(R.string.lesson_review),
                    onClick = { onStartLessonReview(lesson.id) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    style = HuntStyle.SECONDARY,
                    icon = Icons.Outlined.Replay,
                    minHeight = 44.dp,
                    fontSize = 13,
                )
            }
        }
    }
}

/**
 * The gate's five floors in a row, joined by a line lit up to the last cleared floor. The current
 * floor glows; the last one is the BOSS. Every floor can be opened.
 */
@Composable
private fun FloorRail(states: List<FloorState>, onFloor: (Int) -> Unit) {
    val t = LocalGameTokens.current
    val lastCleared = states.indexOfLast { it == FloorState.CLEARED }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .drawBehind {
                // The corridor between floors, behind the tiles at their centre height.
                val y = 22.dp.toPx()
                val step = size.width / states.size
                drawLine(t.muted.copy(alpha = 0.35f), Offset(step / 2, y), Offset(size.width - step / 2, y), 2.dp.toPx())
                if (lastCleared > 0) {
                    drawLine(t.accent, Offset(step / 2, y), Offset(step / 2 + step * lastCleared, y), 3.dp.toPx())
                }
            },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        states.forEachIndexed { i, state ->
            FloorTile(
                index = i,
                state = state,
                label = stringResource(stageLabels[i]),
                modifier = Modifier.weight(1f),
                onClick = { onFloor(i) },
            )
        }
    }
}

@Composable
private fun FloorTile(index: Int, state: FloorState, label: String, modifier: Modifier, onClick: () -> Unit) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val boss = index == BOSS_FLOOR
    val color = when {
        boss -> t.danger
        state == FloorState.OPEN -> t.muted
        else -> t.accent
    }
    val shape = RoundedCornerShape(16.dp)
    val current = state == FloorState.CURRENT
    Column(
        modifier
            .semantics {
                contentDescription = label
                selected = current
            }
            .clickable(role = Role.Tab) {
                feedback.play(Sfx.TAP)
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(width = 52.dp, height = 44.dp)
                .then(if (current) Modifier.pulseRing(color, shape) else Modifier)
                .background(if (state == FloorState.OPEN) t.background else color.copy(alpha = if (current) 0.28f else 0.16f).compositeOver(t.background), shape)
                .border(if (current) 2.dp else 1.dp, color.copy(alpha = if (state == FloorState.OPEN) 0.5f else 1f), shape),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state == FloorState.CLEARED && !boss -> Icon(Icons.Filled.Check, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                boss -> Text("BOSS", color = color, fontFamily = t.ui, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                else -> Text("F${index + 1}", color = if (current) Color.White else color, fontFamily = t.ui, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Text(
            label,
            color = if (current) t.text else t.muted,
            fontSize = 10.sp,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
    }
}

/** The practice portal or the BOSS floor: a short brief and the button that opens the battle. */
@Composable
private fun StartFloor(title: String, intro: String, button: String, boss: Boolean, onStart: () -> Unit) {
    val t = LocalGameTokens.current
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HuntPanel(title = title, accent = if (boss) t.danger else null, scan = true) {
            Text(intro, color = t.text, fontSize = 16.sp, lineHeight = 22.sp)
            val shape = t.shape(12.dp)
            HuntButton(
                button,
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().pulseRing(if (boss) t.danger else t.accent, shape),
                style = if (boss) HuntStyle.DANGER else HuntStyle.PRIMARY,
                icon = Icons.Filled.Bolt,
                sfx = Sfx.OPEN,
            )
        }
        Box(Modifier.height(24.dp))
    }
}
