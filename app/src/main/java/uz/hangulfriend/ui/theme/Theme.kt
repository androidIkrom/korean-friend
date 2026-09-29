package uz.hangulfriend.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CorrectGreen = Color(0xFF2E7D32)
val WrongRed = Color(0xFFC62828)

private val Light = lightColorScheme(
    primary = Color(0xFF3949AB),
    secondary = Color(0xFFD81B60),
    tertiary = Color(0xFF00897B),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFFF48FB1),
    tertiary = Color(0xFF80CBC4),
)

@Composable
fun HangulFriendTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
