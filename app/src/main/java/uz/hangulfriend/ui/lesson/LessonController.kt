package uz.hangulfriend.ui.lesson

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import uz.hangulfriend.content.Character
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.content.Lesson
import uz.hangulfriend.data.CardOrigin
import uz.hangulfriend.data.LessonStatus
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.StudyRepository

/** Stages 0 Lug'at, 1 Grammatika, 2 Dialog, 3 Mashqlar, 4 Test. */
const val STAGE_COUNT = 5

data class LessonState(
    val loading: Boolean = true,
    val lesson: Lesson? = null,
    val stage: Int = 0,
    val characters: Map<String, Character> = emptyMap(),
    val status: LessonStatus = LessonStatus.NOT_STARTED,
)

/** How a floor of the lesson gate looks. Floors are never locked. */
enum class FloorState { CLEARED, CURRENT, OPEN }

/** Floors before the current stage are cleared; a finished lesson shows every floor cleared. */
fun floorStates(stage: Int, status: LessonStatus): List<FloorState> {
    val done = status == LessonStatus.COMPLETED || status == LessonStatus.VERIFIED
    return List(STAGE_COUNT) { i ->
        when {
            i == stage -> FloorState.CURRENT
            done || i < stage -> FloorState.CLEARED
            else -> FloorState.OPEN
        }
    }
}

class LessonController(
    private val lessonId: String,
    private val content: ContentRepository,
    private val study: StudyRepository,
    private val progress: ProgressRepository,
) {
    private val _state = MutableStateFlow(LessonState())
    val state: StateFlow<LessonState> = _state

    /** Opening a lesson makes its cards active and resumes the stage the learner left off at. */
    suspend fun load() {
        val lesson = content.lesson(lessonId)
        if (lesson == null) {
            _state.value = LessonState(loading = false)
            return
        }
        val order = content.catalog().indexOfFirst { it.id == lessonId }.coerceAtLeast(0)
        study.ensureCards(lesson, order, CardOrigin.LESSON)
        val stage = progress.observeAll().first()[lessonId]?.stage ?: 0
        progress.setStage(lessonId, stage)
        val status = progress.observeAll().first().getValue(lessonId).status
        _state.value = LessonState(
            loading = false,
            lesson = lesson,
            stage = stage,
            characters = content.characters().associateBy { it.id },
            status = status,
        )
    }

    /** Re-reads the lesson status, e.g. after the test session passed it. */
    suspend fun refreshStatus() {
        val status = progress.observeAll().first()[lessonId]?.status ?: return
        _state.update { it.copy(status = status) }
    }

    suspend fun goToStage(stage: Int) {
        val clamped = stage.coerceIn(0, STAGE_COUNT - 1)
        progress.setStage(lessonId, clamped)
        _state.update { it.copy(stage = clamped) }
    }
}
