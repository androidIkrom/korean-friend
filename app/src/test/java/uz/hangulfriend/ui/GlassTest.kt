package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.kit.glassTintAlpha
import uz.hangulfriend.ui.kit.meshBlobs
import uz.hangulfriend.ui.theme.tokensFor

class GlassTest {
    @Test fun heavierTintWithoutBlur() {
        assertEquals(0.55f, glassTintAlpha(31), 0.001f)
        assertEquals(0.55f, glassTintAlpha(35), 0.001f)
        assertTrue(glassTintAlpha(30) >= 0.7f)
        assertTrue(glassTintAlpha(26) >= 0.7f)
    }

    @Test fun meshUsesTheThemeAccents() = GameThemeId.entries.forEach { id ->
        val t = tokensFor(id)
        val blobs = meshBlobs(id)
        assertTrue("$id count", blobs.size in 2..3)
        blobs.forEach { b ->
            assertTrue("$id colour", b.color.copy(alpha = 1f) in listOf(t.accent, t.accent2))
            assertTrue("$id radius", b.radius in 0.3f..0.8f)
        }
    }
}
