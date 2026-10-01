package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.hangulfriend.R
import uz.hangulfriend.data.LessonStatus
import uz.hangulfriend.ui.home.questLines
import uz.hangulfriend.ui.home.stageNameFor

class HomeQuestTest {
    @Test fun threeLinesWithLesson() {
        val q = questLines(todayXp = 40, goal = 80, due = 12, stageName = R.string.stage_practice)
        assertEquals(listOf(40 to 80, 0 to 12, 0 to 1), q.map { it.done to it.target })
        assertEquals(R.string.stage_practice, q[2].detail)
    }

    @Test fun noLessonLineWithoutCurrent() = assertEquals(2, questLines(0, 80, 0, null).size)

    @Test fun reviewDoneWhenNothingDue() = assertEquals(0 to 0, questLines(0, 80, 0, null)[1].let { it.done to it.target })

    @Test fun xpCappedAtGoal() = assertEquals(80, questLines(130, 80, 0, null)[0].done)

    @Test fun stageNames() {
        assertEquals(R.string.stage_vocab, stageNameFor(LessonStatus.NOT_STARTED, 0))
        assertEquals(R.string.stage_test, stageNameFor(LessonStatus.IN_PROGRESS, 4))
        assertEquals(R.string.stage_test, stageNameFor(LessonStatus.IN_PROGRESS, 9))
        assertEquals(null, stageNameFor(LessonStatus.COMPLETED, 4))
        assertEquals(null, stageNameFor(LessonStatus.VERIFIED, 4))
    }
}
