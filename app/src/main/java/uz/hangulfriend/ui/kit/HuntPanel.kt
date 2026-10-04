package uz.hangulfriend.ui.kit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hangulfriend.ui.theme.LocalGameTokens

/** A panel title as written (sentence case), in the theme's UI face. */
@Composable
fun PanelTitle(title: String, color: Color? = null) {
    val t = LocalGameTokens.current
    Text(title, color = color ?: t.text, fontFamily = t.ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
}

/**
 * A glass card: the backdrop blurred behind a 24dp rounded panel, no border. [accent] colours the title;
 * [scan] is kept for older callers and draws nothing.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun HuntPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    accent: Color? = null,
    scan: Boolean = false,
    padding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(24.dp))
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) PanelTitle(title, accent)
        content()
    }
}
