package uz.hangulfriend.ui.kit

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.data.Settings

class FeedbackPolicyTest {
    @Test fun settingsGateEachChannelSeparately() {
        val soundOnly = Settings(soundOn = true, hapticsOn = false)
        assertTrue(FeedbackPolicy.sound(soundOnly))
        assertFalse(FeedbackPolicy.vibrate(soundOnly))
        val hapticsOnly = Settings(soundOn = false, hapticsOn = true)
        assertFalse(FeedbackPolicy.sound(hapticsOnly))
        assertTrue(FeedbackPolicy.vibrate(hapticsOnly))
    }

    @Test fun hapticsMatchTheMoment() {
        assertEquals(Haptic.TICK, FeedbackPolicy.haptic(Sfx.TAP))
        assertEquals(Haptic.DOUBLE, FeedbackPolicy.haptic(Sfx.WRONG))
        assertEquals(Haptic.HEAVY, FeedbackPolicy.haptic(Sfx.RANK_UP))
        assertTrue(FeedbackPolicy.volume(Sfx.TAP) < FeedbackPolicy.volume(Sfx.CLEAR))
    }

    @Test fun hapticFallback() {
        assertFalse(FeedbackPolicy.usesPredefined(28))
        assertTrue(FeedbackPolicy.usesPredefined(29))
        assertTrue(Haptic.entries.all { FeedbackPolicy.fallbackMs(it) in 10..80 })
    }

    @Test fun everySfxHasRawFile() {
        for (sfx in Sfx.entries) {
            val f = File("src/main/res/raw/sfx_${sfx.name.lowercase()}.wav")
            assertTrue("missing ${f.path}", f.isFile && f.length() > 1000)
        }
    }
}
