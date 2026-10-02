package uz.hangulfriend.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.Fixtures
import uz.hangulfriend.study.ExerciseItem
import uz.hangulfriend.ui.lesson.GradeOnce

class GradeOnceTest {
    private val a = ExerciseItem.Flashcard(Fixtures.word("u02_l1_w001", "치마", "yubka"))
    private val b = ExerciseItem.WordTyping(Fixtures.word("u02_l1_w001", "치마", "yubka"))

    @Test fun sameItemGradedOnce() {
        val once = GradeOnce()
        assertTrue(once.shouldGrade(a))
        assertFalse(once.shouldGrade(a))
        assertFalse(once.shouldGrade(ExerciseItem.Flashcard(Fixtures.word("u02_l1_w001", "치마", "yubka"))))
    }

    @Test fun differentItemGraded() {
        val once = GradeOnce()
        assertTrue(once.shouldGrade(a))
        assertTrue(once.shouldGrade(b))
    }
}
