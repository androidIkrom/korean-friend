package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.hangulfriend.content.CatalogEntry
import uz.hangulfriend.data.LessonProgressEntity
import uz.hangulfriend.data.LessonStatus
import uz.hangulfriend.ui.map.buildRows

class BookMapViewModelTest {
    private fun entry(id: String) = CatalogEntry(id, 1, 1, "가", "a", "Oila")

    private val catalog = listOf(entry("u01_l1"), entry("u02_l1"), entry("u02_l2"), entry("u03_l1"))

    @Test fun rows_mergeCatalogAvailabilityAndProgress() {
        val rows = buildRows(
            catalog = catalog,
            available = setOf("u02_l1", "u02_l2"),
            progress = mapOf(
                "u01_l1" to LessonProgressEntity("u01_l1", LessonStatus.PASSED, 0, null),
                "u02_l1" to LessonProgressEntity("u02_l1", LessonStatus.IN_PROGRESS, 2, null),
                "u02_l2" to LessonProgressEntity("u02_l2", LessonStatus.COMPLETED, 4, 90),
            ),
        ).associateBy { it.entry.id }

        assertEquals(false, rows.getValue("u01_l1").available)
        assertEquals(LessonStatus.PASSED, rows.getValue("u01_l1").status)
        assertEquals(40, rows.getValue("u02_l1").percent)
        assertEquals(100, rows.getValue("u02_l2").percent)
        assertEquals(LessonStatus.NOT_STARTED, rows.getValue("u03_l1").status)
        assertEquals(0, rows.getValue("u03_l1").percent)
    }

    @Test fun rows_keepCatalogOrder() =
        assertEquals(catalog.map { it.id }, buildRows(catalog, emptySet(), emptyMap()).map { it.entry.id })
}
