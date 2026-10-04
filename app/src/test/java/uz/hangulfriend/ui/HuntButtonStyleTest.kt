package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.huntPalette
import uz.hangulfriend.ui.kit.pressScale
import uz.hangulfriend.ui.theme.contrastRatio
import uz.hangulfriend.ui.theme.tokensFor

class HuntButtonStyleTest {
    @Test fun pressSpringsDownUnlessReduced() {
        assertEquals(0.95f, pressScale(pressed = true, reduced = false), 0.001f)
        assertEquals(1f, pressScale(pressed = true, reduced = true), 0.001f)
        assertEquals(1f, pressScale(pressed = false, reduced = false), 0.001f)
    }

    @Test fun primaryIsTheLightFill() = GameThemeId.entries.forEach { id ->
        val t = tokensFor(id)
        assertEquals(t.text, huntPalette(HuntStyle.PRIMARY, t).fill)
        assertNull(huntPalette(HuntStyle.SECONDARY, t).fill)
    }

    @Test fun labelsReadOnTheirFill() = GameThemeId.entries.forEach { id ->
        val t = tokensFor(id)
        HuntStyle.entries.forEach { style ->
            val p = huntPalette(style, t)
            val behind = p.fill ?: t.surface
            assertTrue("$id $style", contrastRatio(p.text, behind) >= 4.5f)
        }
    }
}
