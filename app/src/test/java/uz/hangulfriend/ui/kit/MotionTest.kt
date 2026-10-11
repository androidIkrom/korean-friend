package uz.hangulfriend.ui.kit

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test fun everyFrameByDefault() {
        assertTrue(frameDue(0L, 1L, 0))
        assertTrue(frameDue(5L, 5L, 0))
    }

    @Test fun throttledClockWaitsForTheInterval() {
        assertFalse(frameDue(0L, 50_000_000L, 66))
        assertFalse(frameDue(0L, 65_999_999L, 66))
        assertTrue(frameDue(0L, 66_000_000L, 66))
        assertTrue(frameDue(100_000_000L, 200_000_000L, 66))
    }
}
