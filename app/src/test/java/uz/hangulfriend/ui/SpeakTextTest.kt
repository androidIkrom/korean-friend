package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.speech.SpeechError
import uz.hangulfriend.ui.exercise.speakErrorText

class SpeakTextTest {
    @Test fun speakErrorText_coversAllErrors() {
        val ids = SpeechError.entries.map { speakErrorText(it) }
        assertTrue(ids.all { it != 0 })
        assertEquals(SpeechError.entries.size, ids.toSet().size)
    }
}
