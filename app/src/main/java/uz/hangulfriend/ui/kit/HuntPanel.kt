package uz.hangulfriend.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.ui.theme.LocalGameTokens

/** Upper-case `[ TITLE ]` in System, plain bold title in Neon. */
@Composable
fun PanelTitle(title: String, color: Color? = null) {
    val t = LocalGameTokens.current
    Text(
        if (t.bracketTitles) "[ ${title.uppercase()} ]" else title,
        color = color ?: if (t.bracketTitles) t.accent else t.text,
        fontFamily = t.display,
        fontWeight = if (t.bracketTitles) FontWeight.SemiBold else FontWeight.Bold,
        fontSize = if (t.bracketTitles) 12.sp else 17.sp,
        letterSpacing = if (t.bracketTitles) 2.5.sp else 0.sp,
    )
}

/**
 * A game window: glass fill, glowing rim in the theme's shape, System corner brackets, and an
 * optional scan line sweeping through it.
 */
@Composable
fun HuntPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    accent: Color? = null,
    scan: Boolean = false,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = LocalGameTokens.current
    val shape = t.shape(12.dp)
    val rim = accent ?: t.panelBorder
    val system = t.id == GameThemeId.SYSTEM
    Column(
        modifier
            .fillMaxWidth()
            .then(if (system) Modifier.shapeGlow(rim.copy(alpha = 0.3f), shape, 12.dp) else Modifier)
            .clip(shape)
            .background(t.panel, shape)
            .then(if (scan) Modifier.scanLine((accent ?: t.accent).copy(alpha = 0.14f)) else Modifier)
            .border(1.dp, rim, shape)
            .then(if (system) Modifier.cornerTicks(accent ?: t.accent) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) PanelTitle(title, accent)
        content()
    }
}
