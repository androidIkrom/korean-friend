package uz.hangulfriend.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.ui.story.PlayOnce

class PlayOnceTest {
    @Test fun sameIdPlaysOnce() {
        val once = PlayOnce()
        assertTrue(once.shouldPlay(1))
        assertFalse(once.shouldPlay(1))
    }

    @Test fun newIdPlays() {
        val once = PlayOnce()
        assertTrue(once.shouldPlay(1))
        assertTrue(once.shouldPlay(2))
        assertFalse(once.shouldPlay(2))
    }
}
