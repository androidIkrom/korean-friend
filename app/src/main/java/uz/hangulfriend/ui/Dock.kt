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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import uz.hangulfriend.ui.kit.glass
import uz.hangulfriend.ui.kit.springPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
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

/** Space the floating dock covers at the bottom of a tab screen; scroll content ends this far from the edge. */
val LocalDockInset = staticCompositionLocalOf { 0.dp }

@Composable
fun Dock(current: DockTab?, onTab: (DockTab) -> Unit, onHunt: () -> Unit, modifier: Modifier = Modifier) {
    // A floating glass pill; the Hunt button sits half above it.
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .glass(RoundedCornerShape(32.dp)),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockItem(DockTab.LOBBY, current == DockTab.LOBBY, onTab)
            DockItem(DockTab.GATES, current == DockTab.GATES, onTab)
            Box(Modifier.width(64.dp))
            DockItem(DockTab.STORY, current == DockTab.STORY, onTab)
            DockItem(DockTab.SYSTEM, current == DockTab.SYSTEM, onTab)
        }
        HuntButtonRound(onHunt, Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp))
    }
}

@Composable
private fun DockItem(tab: DockTab, selected: Boolean, onTab: (DockTab) -> Unit) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val color = if (selected) t.accent else t.muted
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .width(60.dp)
            .springPress(interaction)
            .semantics { this.selected = selected }
            .clickable(role = Role.Tab, interactionSource = interaction, indication = null) {
                if (!selected) feedback.play(Sfx.TAP)
                onTab(tab)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(
            stringResource(tab.label),
            color = color,
            fontFamily = t.ui,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

/** The raised accent circle in the middle of the dock: one tap to the current hunt. */
@Composable
private fun HuntButtonRound(onHunt: () -> Unit, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val label = stringResource(R.string.dock_hunt)
    Box(
        modifier
            .size(56.dp)
            .springPress(interaction)
            .breathing(0.04f)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(t.accent2, t.accent)))
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, interactionSource = interaction, indication = null) {
                feedback.play(Sfx.OPEN)
                onHunt()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Bolt, contentDescription = null, tint = t.background, modifier = Modifier.size(28.dp))
    }
}
