package uz.hangulfriend.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.graphics.compositeOver
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
        assertEquals(0.12f, huntPalette(HuntStyle.SECONDARY, t).fill.alpha, 0.01f)
    }

    @Test fun labelsReadOnTheirFill() = GameThemeId.entries.forEach { id ->
        val t = tokensFor(id)
        HuntStyle.entries.forEach { style ->
            val p = huntPalette(style, t)
            // A translucent fill is read over the theme surface.
            val behind = if (p.fill.alpha < 1f) p.fill.compositeOver(t.surface) else p.fill
            assertTrue("$id $style", contrastRatio(p.text, behind) >= 4.5f)
        }
    }
}
