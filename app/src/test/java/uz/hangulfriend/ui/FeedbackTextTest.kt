package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.hangul.Feedback
import uz.hangulfriend.ui.session.feedbackText

class FeedbackTextTest {
    @Test fun feedbackText_coversAllValues() {
        val ids = Feedback.entries.map { feedbackText(it) }
        assertTrue(ids.all { it != 0 })
        assertEquals(Feedback.entries.size, ids.toSet().size)
    }
}
