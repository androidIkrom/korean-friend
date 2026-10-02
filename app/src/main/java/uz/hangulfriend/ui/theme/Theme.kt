package uz.hangulfriend.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import uz.hangulfriend.data.GameThemeId

val CorrectGreen = Color(0xFF2E7D32)
val WrongRed = Color(0xFFC62828)

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
