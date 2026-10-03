package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.hangulfriend.ui.onboarding.ONBOARDING_STEPS
import uz.hangulfriend.ui.onboarding.canFinish
import uz.hangulfriend.ui.onboarding.nextStep

class OnboardingStepsTest {
    @Test fun stepsGoForwardThenFinish() {
        assertEquals(3, ONBOARDING_STEPS)
        assertEquals(1, nextStep(0))
        assertEquals(2, nextStep(1))
        assertNull(nextStep(2))
    }

    @Test fun finishNeedsALesson() {
        assertEquals(false, canFinish(2, null))
        assertEquals(true, canFinish(2, "u02_l1"))
        assertEquals(false, canFinish(1, "u02_l1"))
    }
}
