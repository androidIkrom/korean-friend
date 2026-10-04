package uz.hangulfriend.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.theme.contrastRatio
import uz.hangulfriend.ui.theme.gameColorScheme
import uz.hangulfriend.ui.theme.gameShapes
import uz.hangulfriend.ui.theme.tokensFor

class GameThemeTest {
    @Test fun systemPalette() = with(tokensFor(GameThemeId.SYSTEM)) {
        assertEquals(Color(0xFF07070F), background)
        assertEquals(Color(0xFF15103A), surface)
        assertEquals(Color(0xFFA98BFF), accent)
        assertEquals(Color(0xFF5CC8FF), accent2)
    }

    @Test fun neonPalette() = with(tokensFor(GameThemeId.NEON)) {
        assertEquals(Color(0xFF0A0613), background)
        assertEquals(Color(0xFF1A0B2A), surface)
        assertEquals(Color(0xFFFF3D9A), accent)
        assertEquals(Color(0xFF2EE6FF), accent2)
    }

    @Test fun glassLookInBothThemes() = GameThemeId.entries.forEach { id ->
        val t = tokensFor(id)
        assertFalse("$id", t.bracketTitles)
        assertEquals("$id", 24.dp, t.panelCorner)
        assertEquals("$id", 0.55f, t.glassTint.alpha, 0.001f)
    }

    @Test fun textReadsOnSurface() = GameThemeId.entries.forEach { id ->
        val t = tokensFor(id)
        assertTrue("$id text", contrastRatio(t.text, t.surface) >= 4.5f)
        assertTrue("$id muted", contrastRatio(t.muted, t.surface) >= 3.0f)
        assertTrue("$id text on raised", contrastRatio(t.text, t.raised) >= 4.5f)
    }

    @Test fun contrastOfBlackOnWhiteIs21() = assertEquals(21f, contrastRatio(Color.Black, Color.White), 0.01f)

    @Test fun colorSchemeFollowsTokens() {
        val t = tokensFor(GameThemeId.NEON)
        val c = gameColorScheme(t)
        assertEquals(t.accent, c.primary)
        assertEquals(t.accent2, c.secondary)
        assertEquals(t.background, c.background)
        assertEquals(t.danger, c.error)
        assertEquals(t.text, c.onBackground)
        assertEquals(1f, c.surface.alpha)
    }

    @Test fun shapesAreSoftInBothThemes() = GameThemeId.entries.forEach { id ->
        val s = gameShapes(tokensFor(id))
        assertEquals(RoundedCornerShape(16.dp), s.medium)
        assertEquals(RoundedCornerShape(24.dp), s.large)
        assertEquals(RoundedCornerShape(28.dp), s.extraLarge)
    }

    /** Face-up memory cards, the learner's chat bubbles and dialogue lines must stand out from plain surfaces. */
    @Test fun containersAreDistinct() = GameThemeId.entries.forEach { id ->
        val c = gameColorScheme(tokensFor(id))
        val colors = listOf(c.primaryContainer, c.secondaryContainer, c.surfaceVariant)
        assertEquals("$id", 3, colors.distinct().size)
        colors.forEach { assertEquals("$id opaque", 1f, it.alpha) }
    }
}
