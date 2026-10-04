package uz.hangulfriend.study

import uz.hangulfriend.content.CatalogEntry

/**
 * A lesson's place in the series. Book 2 keeps its original ids (`u02_l1`) so saved progress stays valid;
 * other books carry their number (`b1_u03_l2`).
 */
data class LessonId(val book: Int, val unit: Int, val lesson: Int) {
    /** The stored id. */
    val id: String get() = of(book, unit, lesson)

    /** The label people see: "2-1" in book 2, "1·3-2" in the others. */
    val code: String get() = if (book == 2) "$unit-$lesson" else "$book·$unit-$lesson"

    companion object {
        private val PATTERN = Regex("""^(?:b(\d)_)?u(\d{2})_l(\d)$""")

        fun parse(id: String): LessonId? {
            val m = PATTERN.matchEntire(id) ?: return null
            val book = m.groupValues[1].ifEmpty { "2" }.toInt()
            return LessonId(book, m.groupValues[2].toInt(), m.groupValues[3].toInt())
        }

        fun of(book: Int, unit: Int, lesson: Int): String =
            (if (book == 2) "" else "b${book}_") + "u%02d_l%d".format(unit, lesson)
    }
}

/** Best-score id of a book's final test; book 2 keeps the id it has always used. */
fun finalTestId(book: Int): String = if (book == 2) FINAL_TEST_ID else "${FINAL_TEST_ID}_b$book"

/** The lesson's label for people: "2-1" in book 2, "1·3-2" in the others. */
val CatalogEntry.code: String get() = LessonId(book, unit, lesson).code

/** The book a lesson id belongs to; book 2 for anything that is not a lesson id. */
fun bookOf(lessonId: String?): Int = lessonId?.let { LessonId.parse(it)?.book } ?: 2
