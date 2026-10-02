package uz.hangulfriend.ui.home

import androidx.annotation.StringRes
import uz.hangulfriend.R
import uz.hangulfriend.data.LessonStatus

/** One row of the daily quest panel: `label [done/target]`; [detail] names the lesson stage. */
data class QuestLine(@StringRes val label: Int, val done: Int, val target: Int, @StringRes val detail: Int? = null) {
    val complete: Boolean get() = done >= target
}

private val stageNames = listOf(
    R.string.stage_vocab, R.string.stage_grammar, R.string.stage_dialogue, R.string.stage_practice, R.string.stage_test,
)

/** The next stage of the current lesson, or null when the lesson is finished. */
@StringRes
fun stageNameFor(status: LessonStatus, stage: Int): Int? = when (status) {
    LessonStatus.COMPLETED, LessonStatus.VERIFIED -> null
    else -> stageNames[stage.coerceIn(0, stageNames.lastIndex)]
}

/** Daily XP, due reviews (nothing due counts as done) and, when there is one, the current lesson's next stage. */
fun questLines(todayXp: Int, goal: Int, due: Int, @StringRes stageName: Int?): List<QuestLine> = buildList {
    add(QuestLine(R.string.quest_daily_xp, todayXp.coerceAtMost(goal), goal))
    add(QuestLine(R.string.quest_review, 0, due))
    if (stageName != null) add(QuestLine(R.string.quest_lesson, 0, 1, stageName))
}
