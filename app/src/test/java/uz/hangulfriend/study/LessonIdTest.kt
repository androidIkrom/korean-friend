package uz.hangulfriend.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.hangulfriend.data.lessonOfRef

class LessonIdTest {
    @Test fun bookTwoKeepsItsIds() {
        assertEquals(LessonId(2, 2, 1), LessonId.parse("u02_l1"))
        assertEquals("u02_l1", LessonId(2, 2, 1).id)
    }

    @Test fun otherBooksCarryTheirNumber() {
        assertEquals(LessonId(1, 3, 2), LessonId.parse("b1_u03_l2"))
        assertEquals("b3_u09_l1", LessonId.of(3, 9, 1))
    }

    @Test fun rejectsAnythingElse() {
        listOf("", "user", "final_test", "u2_l1", "b1_u03", "x_u02_l1", "u02_l1_w001").forEach { assertNull(it, LessonId.parse(it)) }
    }

    @Test fun codes() {
        assertEquals("2-1", LessonId(2, 2, 1).code)
        assertEquals("1·3-2", LessonId(1, 3, 2).code)
    }

    @Test fun finalIdsPerBook() {
        assertEquals(FINAL_TEST_ID, finalTestId(2))
        assertEquals("${FINAL_TEST_ID}_b1", finalTestId(1))
    }

    @Test fun flagRefsOfEveryBook() {
        assertEquals("u02_l1", lessonOfRef("u02_l1_e07"))
        assertEquals("b1_u03_l2", lessonOfRef("b1_u03_l2_w004"))
    }
}
