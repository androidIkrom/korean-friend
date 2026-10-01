package uz.hangulfriend.ui.map

import uz.hangulfriend.data.LessonStatus

enum class UnitState { CLEARED, ACTIVE, OPEN, LOCKED }

data class UnitRow(val unit: Int, val topicUz: String, val lessons: List<LessonRow>, val state: UnitState)

/** Gate ranks of units 1–9 on the System map (spec §5.3). */
val UNIT_RANKS = listOf("E", "E", "D", "D", "C", "C", "B", "A", "S")

private fun LessonRow.done() = status == LessonStatus.COMPLETED || status == LessonStatus.VERIFIED

/**
 * Groups lesson rows into units. A unit is CLEARED when every lesson is done, LOCKED when none has content;
 * the ACTIVE one holds the current lesson, or else is the first unit still in play.
 */
fun buildUnits(rows: List<LessonRow>, currentLessonId: String?): List<UnitRow> {
    val grouped = rows.groupBy { it.entry.unit }.toSortedMap()
    val base = grouped.map { (unit, lessons) ->
        val state = when {
            lessons.all { it.done() } -> UnitState.CLEARED
            lessons.none { it.available } -> UnitState.LOCKED
            else -> UnitState.OPEN
        }
        UnitRow(unit, lessons.first().entry.topicUz, lessons, state)
    }
    val inPlay = base.filter { it.state == UnitState.OPEN }
    val active = inPlay.firstOrNull { u -> u.lessons.any { it.entry.id == currentLessonId } } ?: inPlay.firstOrNull()
    return base.map { if (it === active) it.copy(state = UnitState.ACTIVE) else it }
}
