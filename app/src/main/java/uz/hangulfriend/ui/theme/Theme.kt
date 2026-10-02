package uz.hangulfriend.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import uz.hangulfriend.data.GameThemeId

// Bright verdict colours: both game themes are dark, so the old Material green/red were hard to read.
val CorrectGreen = Color(0xFF4ADE80)
val WrongRed = Color(0xFFFF6B85)

/** Both game themes are dark; the system light/dark setting is not consulted. */
@Composable
fun HangulFriendTheme(themeId: GameThemeId = GameThemeId.SYSTEM, content: @Composable () -> Unit) {
    val tokens = tokensFor(themeId)
    CompositionLocalProvider(LocalGameTokens provides tokens) {
        MaterialTheme(
            colorScheme = gameColorScheme(tokens),
            typography = gameTypography(tokens.display),
            shapes = gameShapes(tokens),
            content = content,
        )
    }
}
