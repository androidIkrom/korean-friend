package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.hangulfriend.content.CatalogEntry
import uz.hangulfriend.data.LessonStatus
import uz.hangulfriend.ui.map.LessonRow
import uz.hangulfriend.ui.map.UnitState
import uz.hangulfriend.ui.map.UnitState.ACTIVE
import uz.hangulfriend.ui.map.UnitState.CLEARED
import uz.hangulfriend.ui.map.UnitState.LOCKED
import uz.hangulfriend.ui.map.UnitState.OPEN
import uz.hangulfriend.ui.map.buildUnits

class UnitStateTest {
    private fun row(unit: Int, lesson: Int, status: LessonStatus = LessonStatus.NOT_STARTED, available: Boolean = true) =
        LessonRow(CatalogEntry("u0${unit}_l$lesson", unit, lesson, "가", "a", "Mavzu $unit"), available, status, 0)

    private fun unit(n: Int, s1: LessonStatus = LessonStatus.NOT_STARTED, s2: LessonStatus = s1, available: Boolean = true) =
        listOf(row(n, 1, s1, available), row(n, 2, s2, available))

    private fun states(rows: List<LessonRow>, current: String?): List<UnitState> = buildUnits(rows, current).map { it.state }

    private val done = LessonStatus.COMPLETED

    @Test fun clearedWhenBothDone() =
        assertEquals(listOf(CLEARED, ACTIVE), states(unit(1, done, LessonStatus.VERIFIED) + unit(2), null))

    @Test fun currentLessonUnitIsActive() = assertEquals(
        listOf(CLEARED, OPEN, ACTIVE),
        states(unit(1, done) + unit(2) + unit(3, LessonStatus.IN_PROGRESS, LessonStatus.NOT_STARTED), "u03_l1"),
    )

    @Test fun firstUnclearedOpenUnitIsActiveWithoutCurrent() =
        assertEquals(listOf(CLEARED, ACTIVE, OPEN), states(unit(1, done) + unit(2) + unit(3), null))

    @Test fun clearedCurrentUnitMovesActiveOn() =
        assertEquals(listOf(CLEARED, CLEARED, ACTIVE), states(unit(1, done) + unit(2, done) + unit(3), "u02_l2"))

    @Test fun lockedWhenNoLessonAvailable() =
        assertEquals(listOf(ACTIVE, LOCKED), states(unit(1) + unit(2, available = false), null))

    @Test fun noActiveWhenEverythingLocked() =
        assertEquals(listOf(LOCKED, LOCKED), states(unit(1, available = false) + unit(2, available = false), null))

    @Test fun keepsTopicAndOrder() {
        val units = buildUnits(unit(2) + unit(1), null)
        assertEquals(listOf(1, 2), units.map { it.unit })
        assertEquals("Mavzu 1", units[0].topicUz)
    }
}
