package uz.hangulfriend.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.theme.gameColorScheme
import uz.hangulfriend.ui.theme.tokensFor

class GameThemeTest {
    @Test fun systemTokens() {
        val t = tokensFor(GameThemeId.SYSTEM)
        assertEquals(Color(0xFF5CC8FF), t.accent)
        assertTrue(t.bracketTitles)
    }

    @Test fun neonTokens() {
        val t = tokensFor(GameThemeId.NEON)
        assertEquals(Color(0xFFFF3D9A), t.accent)
        assertEquals(14.dp, t.panelCorner)
    }

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
}
