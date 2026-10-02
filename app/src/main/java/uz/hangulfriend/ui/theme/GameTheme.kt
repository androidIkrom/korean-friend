package uz.hangulfriend.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.hangulfriend.R
import uz.hangulfriend.data.GameThemeId

/** Everything a game-styled screen needs from the active theme (stage 6 spec §2.1). */
data class GameTokens(
    val id: GameThemeId,
    val background: Color,
    val panel: Color,
    val panelBorder: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accent2: Color,
    val top: Color,
    val danger: Color,
    val panelCorner: Dp,
    val display: FontFamily,
    /** System titles read `[ TITLE ]`; Neon titles are plain. */
    val bracketTitles: Boolean,
)

private val ChakraPetch = FontFamily(
    Font(R.font.chakra_petch_regular, FontWeight.Normal),
    Font(R.font.chakra_petch_semibold, FontWeight.SemiBold),
    Font(R.font.chakra_petch_bold, FontWeight.Bold),
)

private fun oxanium(weight: Int) = Font(
    R.font.oxanium_variable,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Oxanium = FontFamily(oxanium(400), oxanium(600), oxanium(800))

private val System = GameTokens(
    id = GameThemeId.SYSTEM,
    background = Color(0xFF04060C),
    panel = Color(0xE6060E20),
    panelBorder = Color(0xBF5CC8FF),
    text = Color(0xFFDCEBFF),
    muted = Color(0xFF8FA8C8),
    accent = Color(0xFF5CC8FF),
    accent2 = Color(0xFFA98BFF),
    top = Color(0xFFA98BFF),
    danger = Color(0xFFFF6B85),
    panelCorner = 0.dp,
    display = ChakraPetch,
    bracketTitles = true,
)

private val Neon = GameTokens(
    id = GameThemeId.NEON,
    background = Color(0xFF0A0613),
    panel = Color(0xC7180C2E),
    panelBorder = Color(0x592EE6FF),
    text = Color(0xFFEDE7FF),
    muted = Color(0xFFA79CC8),
    accent = Color(0xFFFF3D9A),
    accent2 = Color(0xFF2EE6FF),
    top = Color(0xFFFFE45C),
    danger = Color(0xFFFF6B85),
    panelCorner = 14.dp,
    display = Oxanium,
    bracketTitles = false,
)

fun tokensFor(id: GameThemeId): GameTokens = when (id) {
    GameThemeId.SYSTEM -> System
    GameThemeId.NEON -> Neon
}

/** Dark Material colours derived from the tokens, so screens not yet redesigned follow the theme. */
fun gameColorScheme(t: GameTokens): ColorScheme {
    val surface = t.panel.copy(alpha = 1f)
    return darkColorScheme(
        primary = t.accent,
        onPrimary = t.background,
        primaryContainer = surface,
        onPrimaryContainer = t.text,
        secondary = t.accent2,
        onSecondary = t.background,
        secondaryContainer = surface,
        onSecondaryContainer = t.text,
        tertiary = t.top,
        background = t.background,
        onBackground = t.text,
        surface = surface,
        onSurface = t.text,
        surfaceVariant = surface,
        onSurfaceVariant = t.muted,
        surfaceContainer = surface,
        surfaceContainerLow = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surface,
        outline = t.panelBorder,
        error = t.danger,
    )
}

/** Corner radii for every Material component: near-square in System, soft in Neon. */
fun gameShapes(t: GameTokens): Shapes = if (t.id == GameThemeId.SYSTEM) {
    val r = RoundedCornerShape(2.dp)
    Shapes(extraSmall = r, small = r, medium = r, large = r, extraLarge = r)
} else {
    Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(18.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )
}

/** Display, headline and title styles use the theme's display face; body text keeps the system font (Korean). */
fun gameTypography(display: FontFamily): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = display),
        displayMedium = base.displayMedium.copy(fontFamily = display),
        displaySmall = base.displaySmall.copy(fontFamily = display),
        headlineLarge = base.headlineLarge.copy(fontFamily = display),
        headlineMedium = base.headlineMedium.copy(fontFamily = display),
        headlineSmall = base.headlineSmall.copy(fontFamily = display),
        titleLarge = base.titleLarge.copy(fontFamily = display),
        titleMedium = base.titleMedium.copy(fontFamily = display),
        titleSmall = base.titleSmall.copy(fontFamily = display),
        labelLarge = base.labelLarge.copy(fontFamily = display),
    )
}

val LocalGameTokens = staticCompositionLocalOf { tokensFor(GameThemeId.SYSTEM) }
