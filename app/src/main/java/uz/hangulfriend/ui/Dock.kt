package uz.hangulfriend.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hangulfriend.R
import uz.hangulfriend.ui.kit.GameMotion
import uz.hangulfriend.ui.kit.LocalGameFeedback
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.kit.breathing
import uz.hangulfriend.ui.kit.shapeGlow
import uz.hangulfriend.ui.theme.LocalGameTokens

/** The four dock tabs; the raised ◆ HUNT button sits between GATES and STORY. */
enum class DockTab(val route: String, val label: Int, val icon: ImageVector) {
    LOBBY(Routes.HOME, R.string.dock_lobby, Icons.Outlined.Home),
    GATES(Routes.MAP, R.string.dock_gates, Icons.Outlined.Map),
    STORY(Routes.STORIES, R.string.dock_story, Icons.AutoMirrored.Outlined.MenuBook),
    SYSTEM(Routes.SETTINGS, R.string.dock_system, Icons.Outlined.Tune),
}

fun dockTabFor(route: String?): DockTab? = DockTab.entries.firstOrNull { it.route == route }

/** ◆ HUNT goes straight into the current lesson's gate, or to the map when no lesson is chosen yet. */
fun huntTarget(currentLessonId: String?): String = currentLessonId?.let { Routes.lesson(it) } ?: Routes.MAP

@Composable
fun Dock(current: DockTab?, onTab: (DockTab) -> Unit, onHunt: () -> Unit, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(t.background.copy(alpha = 0f), t.background.copy(alpha = 0.97f), t.background), endY = 120f))
            .navigationBarsPadding(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, t.accent.copy(alpha = 0.55f), Color.Transparent))),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom,
        ) {
            DockItem(DockTab.LOBBY, current == DockTab.LOBBY, onTab)
            DockItem(DockTab.GATES, current == DockTab.GATES, onTab)
            HuntDiamond(onHunt)
            DockItem(DockTab.STORY, current == DockTab.STORY, onTab)
            DockItem(DockTab.SYSTEM, current == DockTab.SYSTEM, onTab)
        }
    }
}

@Composable
private fun DockItem(tab: DockTab, selected: Boolean, onTab: (DockTab) -> Unit) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val color = if (selected) t.accent else t.muted
    val bar by animateDpAsState(if (selected) 20.dp else 0.dp, tween(GameMotion.FAST), label = "dockBar")
    val lift by animateFloatAsState(if (selected) 1.12f else 1f, tween(GameMotion.FAST), label = "dockLift")
    Column(
        Modifier
            .width(64.dp)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!selected) feedback.play(Sfx.TAP)
                onTab(tab)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp).scale(lift))
        Text(
            stringResource(tab.label).uppercase(),
            color = color,
            fontFamily = t.display,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            maxLines = 1,
        )
        Box(
            Modifier
                .width(bar)
                .height(3.dp)
                .then(if (selected) Modifier.shapeGlow(t.accent.copy(alpha = 0.8f), RectangleShape, 6.dp) else Modifier)
                .background(t.accent),
        )
    }
}

/** The raised diamond in the middle of the dock: one tap to the current hunt. */
@Composable
private fun HuntDiamond(onHunt: () -> Unit) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.9f else 1f, tween(GameMotion.TAP), label = "huntPress")
    val label = stringResource(R.string.dock_hunt)
    Box(Modifier.offset(y = (-10).dp).size(76.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(70.dp)
                .breathing(0.06f)
                .drawBehind {
                    drawCircle(Brush.radialGradient(listOf(t.accent2.copy(alpha = 0.45f), Color.Transparent)), radius = size.minDimension * 0.7f)
                },
        )
        Box(
            Modifier
                .size(54.dp)
                .scale(press)
                .rotate(45f)
                .shapeGlow(t.accent2.copy(alpha = 0.7f), RectangleShape, 14.dp)
                .background(Brush.linearGradient(listOf(t.accent2, t.accent), start = Offset.Zero, end = Offset(160f, 160f)))
                .border(2.dp, Color.White.copy(alpha = 0.85f))
                .semantics { contentDescription = label }
                .clickable(role = Role.Button, interactionSource = interaction, indication = null) {
                    feedback.play(Sfx.OPEN)
                    onHunt()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = t.background, modifier = Modifier.size(28.dp).rotate(-45f))
        }
    }
}
