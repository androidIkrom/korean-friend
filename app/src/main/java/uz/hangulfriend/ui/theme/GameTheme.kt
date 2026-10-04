package uz.hangulfriend.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hangulfriend.R
import uz.hangulfriend.data.GameThemeId

/** Everything a game-styled screen needs from the active theme (stage 10 spec: "Glass hero"). */
data class GameTokens(
    val id: GameThemeId,
    val background: Color,
    /** Tonal card colour; [raised] sits one step above it. */
    val surface: Color,
    val raised: Color,
    /** Tint laid over the blurred backdrop on glass panels. */
    val glassTint: Color,
    /** Translucent fill for screens that still paint their own panels. */
    val panel: Color,
    /** A faint hairline, for the rare places that still need an edge. */
    val panelBorder: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val accent2: Color,
    val top: Color,
    val danger: Color,
    val panelCorner: Dp,
    /** UI text (Sora). [display] is the same family, kept for older call sites. */
    val ui: FontFamily,
    val display: FontFamily,
    /** Big numbers and the rank letter (Unbounded). */
    val numbers: FontFamily,
    /** Stage 9 `[ TITLE ]` titles; off in the glass look. */
    val bracketTitles: Boolean,
)

private val Sora = FontFamily(
    Font(R.font.sora_regular, FontWeight.Normal),
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_extrabold, FontWeight.ExtraBold),
)

private val Unbounded = FontFamily(
    Font(R.font.unbounded_semibold, FontWeight.SemiBold),
    Font(R.font.unbounded_extrabold, FontWeight.ExtraBold),
)

private fun tokens(
    id: GameThemeId,
    background: Color,
    surface: Color,
    text: Color,
    muted: Color,
    accent: Color,
    accent2: Color,
    top: Color,
) = GameTokens(
    id = id,
    background = background,
    surface = surface,
    raised = lerp(surface, text, 0.06f),
    glassTint = surface.copy(alpha = 0.55f),
    panel = surface.copy(alpha = 0.72f),
    panelBorder = Color.White.copy(alpha = 0.1f),
    text = text,
    muted = muted,
    accent = accent,
    accent2 = accent2,
    top = top,
    danger = Color(0xFFFF6B85),
    panelCorner = 24.dp,
    ui = Sora,
    display = Sora,
    numbers = Unbounded,
    bracketTitles = false,
)

private val System = tokens(
    GameThemeId.SYSTEM,
    background = Color(0xFF07070F),
    surface = Color(0xFF15103A),
    text = Color(0xFFF2F0FF),
    muted = Color(0xFFB9B2E6),
    accent = Color(0xFFA98BFF),
    accent2 = Color(0xFF5CC8FF),
    top = Color(0xFFA98BFF),
)

private val Neon = tokens(
    GameThemeId.NEON,
    background = Color(0xFF0A0613),
    surface = Color(0xFF1A0B2A),
    text = Color(0xFFFFF0F8),
    muted = Color(0xFFC9B3D9),
    accent = Color(0xFFFF3D9A),
    accent2 = Color(0xFF2EE6FF),
    top = Color(0xFFFFE45C),
)

fun tokensFor(id: GameThemeId): GameTokens = when (id) {
    GameThemeId.SYSTEM -> System
    GameThemeId.NEON -> Neon
}

/** WCAG contrast ratio of two opaque colours: 1 (same) to 21 (black on white). */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/** Dark Material colours derived from the tokens, so Material components follow the theme. */
fun gameColorScheme(t: GameTokens): ColorScheme = darkColorScheme(
    primary = t.accent,
    onPrimary = t.background,
    primaryContainer = lerp(t.surface, t.accent, 0.28f),
    onPrimaryContainer = t.text,
    secondary = t.accent2,
    onSecondary = t.background,
    secondaryContainer = lerp(t.surface, t.accent2, 0.22f),
    onSecondaryContainer = t.text,
    tertiary = t.top,
    background = t.background,
    onBackground = t.text,
    surface = t.surface,
    onSurface = t.text,
    surfaceVariant = t.raised,
    onSurfaceVariant = t.muted,
    surfaceContainer = t.surface,
    surfaceContainerLow = t.surface,
    surfaceContainerHigh = t.raised,
    surfaceContainerHighest = t.raised,
    outline = t.panelBorder,
    error = t.danger,
)

/** Soft corners for every Material component, the same in both themes. */
@Suppress("UNUSED_PARAMETER")
fun gameShapes(t: GameTokens): Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** The glass type scale (34 / 24 / 18 / 15 / 13 / 11 sp) in [ui]; Korean falls back to the system font. */
fun gameTypography(ui: FontFamily): Typography {
    val base = Typography()
    fun style(size: Int, weight: FontWeight, line: Int) = TextStyle(fontFamily = ui, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)
    return base.copy(
        displayLarge = style(34, FontWeight.ExtraBold, 40),
        displayMedium = style(34, FontWeight.ExtraBold, 40),
        displaySmall = style(24, FontWeight.ExtraBold, 30),
        headlineLarge = style(24, FontWeight.ExtraBold, 30),
        headlineMedium = style(24, FontWeight.SemiBold, 30),
        headlineSmall = style(18, FontWeight.ExtraBold, 24),
        titleLarge = style(18, FontWeight.SemiBold, 24),
        titleMedium = style(15, FontWeight.SemiBold, 21),
        titleSmall = style(13, FontWeight.SemiBold, 18),
        bodyLarge = style(15, FontWeight.Normal, 22),
        bodyMedium = style(13, FontWeight.Normal, 19),
        bodySmall = style(11, FontWeight.Normal, 16),
        labelLarge = style(15, FontWeight.SemiBold, 20),
        labelMedium = style(13, FontWeight.SemiBold, 18),
        labelSmall = style(11, FontWeight.SemiBold, 16),
    )
}

val LocalGameTokens = staticCompositionLocalOf { tokensFor(GameThemeId.SYSTEM) }
