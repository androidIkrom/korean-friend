package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.ai.AiResult
import uz.hangulfriend.ui.tutor.aiStatusText

class TutorTextTest {
    @Test fun aiStatusText_coversNonSuccess() {
        assertNull(aiStatusText(AiResult.Success("x")))
        val ids = listOf(AiResult.QuotaExhausted, AiResult.Offline, AiResult.NotConfigured, AiResult.Failed("e")).map { aiStatusText(it) }
        assertTrue(ids.all { it != null && it != 0 })
        assertEquals(4, ids.toSet().size)
    }
}
