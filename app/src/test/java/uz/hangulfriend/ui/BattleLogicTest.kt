package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.Fixtures
import uz.hangulfriend.srs.Rating
import uz.hangulfriend.study.ExerciseItem
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.session.ExerciseOutcome
import uz.hangulfriend.ui.session.FinalScore
import uz.hangulfriend.ui.session.SessionMode
import uz.hangulfriend.ui.session.SessionState
import uz.hangulfriend.ui.session.cleared
import uz.hangulfriend.ui.session.verdictSfx

class BattleLogicTest {
    @Test fun verdictSound() {
        assertEquals(Sfx.CORRECT, ExerciseOutcome.Checked(true, false, 0, null).verdictSfx())
        assertEquals(Sfx.WRONG, ExerciseOutcome.Checked(false, false, 0, null).verdictSfx())
        assertEquals(Sfx.CORRECT, ExerciseOutcome.Matched(emptySet(), 4).verdictSfx())
        assertEquals(null, ExerciseOutcome.Rated(Rating.AGAIN).verdictSfx())
        assertEquals(null, ExerciseOutcome.Skipped.verdictSfx())
    }

    private val items = listOf(ExerciseItem.Flashcard(Fixtures.validLesson().words.first()))

    private fun done(correct: Int, scored: Int, failed: Boolean = false, final: FinalScore? = null) =
        SessionState(loading = false, items = items, index = 1, correctCount = correct, scored = scored, failed = failed, final = final)

    @Test fun clearedPerMode() {
        assertTrue(done(1, 1).cleared(SessionMode.PRACTICE))
        assertTrue(done(0, 1).cleared(SessionMode.REVIEW))
        assertTrue(done(4, 5).cleared(SessionMode.TEST))
        assertFalse(done(3, 5).cleared(SessionMode.TEST))
        assertFalse(done(1, 1, failed = true).cleared(SessionMode.BOSS))
        assertTrue(done(1, 1).cleared(SessionMode.BOSS))
        assertTrue(done(1, 1, final = FinalScore(1, 2, 1, 2)).cleared(SessionMode.FINAL))
        assertFalse(done(0, 1, final = FinalScore(0, 2, 1, 2)).cleared(SessionMode.FINAL))
        assertFalse(SessionState(loading = false).cleared(SessionMode.PRACTICE))
    }
}
