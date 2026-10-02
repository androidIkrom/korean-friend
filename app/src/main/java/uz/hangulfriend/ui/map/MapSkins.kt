package uz.hangulfriend.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hangulfriend.R
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.data.LessonStatus
import uz.hangulfriend.ui.statusLabel
import uz.hangulfriend.ui.theme.BadgeState
import uz.hangulfriend.ui.theme.GameButton
import uz.hangulfriend.ui.theme.GamePanel
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.theme.ProgressBar
import uz.hangulfriend.ui.theme.RankBadge

class MapActions(
    val onOpenLesson: (String) -> Unit,
    val onQuickCheck: (String) -> Unit,
    val onBoss: (Int) -> Unit,
    val onToggle: (Int) -> Unit,
)

private fun UnitRow.enterTarget(): String? =
    (lessons.firstOrNull { it.available && it.status != LessonStatus.COMPLETED && it.status != LessonStatus.VERIFIED }
        ?: lessons.firstOrNull { it.available })?.entry?.id

/** One unit as a gate in the System tower. */
@Composable
fun GateItem(u: UnitRow, expanded: Boolean, actions: MapActions) {
    val rank = UNIT_RANKS.getOrElse(u.unit - 1) { "?" }
    if (expanded) {
        UnitPanel(u, actions) {
            RankBadge(rank, BadgeState.ACTIVE, size = 30.dp)
        }
    } else {
        CompactUnit(u, actions) {
            RankBadge(rank, if (u.state == UnitState.LOCKED) BadgeState.LOCKED else BadgeState.DONE, size = 24.dp)
        }
    }
}

/** One unit as a station on the Neon metro line; [passedTop]/[passedBottom] paint the line solid above/below it. */
@Composable
fun StationItem(u: UnitRow, expanded: Boolean, first: Boolean, last: Boolean, passedTop: Boolean, passedBottom: Boolean, actions: MapActions) {
    val t = LocalGameTokens.current
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(Modifier.width(36.dp).fillMaxHeight()) {
            val x = size.width / 2
            val cy = 26.dp.toPx()
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            if (!first) {
                drawLine(
                    if (passedTop) t.accent2 else t.muted, Offset(x, 0f), Offset(x, cy),
                    strokeWidth = if (passedTop) 6.dp.toPx() else 3.dp.toPx(), pathEffect = if (passedTop) null else dash,
                )
            }
            if (!last) {
                drawLine(
                    if (passedBottom) t.accent2 else t.muted, Offset(x, cy), Offset(x, size.height),
                    strokeWidth = if (passedBottom) 6.dp.toPx() else 3.dp.toPx(), pathEffect = if (passedBottom) null else dash,
                )
            }
            when (u.state) {
                UnitState.ACTIVE -> {
                    drawCircle(t.accent.copy(alpha = 0.2f), 22.dp.toPx(), Offset(x, cy))
                    drawCircle(t.background, 15.dp.toPx(), Offset(x, cy))
                    drawCircle(t.accent, 15.dp.toPx(), Offset(x, cy), style = Stroke(5.dp.toPx()))
                }
                UnitState.CLEARED, UnitState.OPEN -> {
                    drawCircle(Color.White, 10.dp.toPx(), Offset(x, cy))
                    drawCircle(t.accent2, 10.dp.toPx(), Offset(x, cy), style = Stroke(4.dp.toPx()))
                }
                UnitState.LOCKED -> {
                    drawCircle(t.background, 8.dp.toPx(), Offset(x, cy))
                    drawCircle(t.muted, 8.dp.toPx(), Offset(x, cy), style = Stroke(2.dp.toPx()))
                }
            }
        }
        Box(Modifier.weight(1f).padding(vertical = 4.dp)) {
            if (expanded) UnitPanel(u, actions, borderColor = t.accent) {} else CompactUnit(u, actions) {}
        }
    }
}

@Composable
private fun CompactUnit(u: UnitRow, actions: MapActions, badge: @Composable () -> Unit) {
    val t = LocalGameTokens.current
    val shape = RoundedCornerShape(t.panelCorner)
    val locked = u.state == UnitState.LOCKED
    val ko = stringArrayResource(R.array.unit_topics_ko).getOrNull(u.unit - 1)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .alpha(if (locked) 0.6f else 1f)
            .background(t.panel.copy(alpha = 0.6f), shape)
            .border(1.dp, t.panelBorder.copy(alpha = 0.35f), shape)
            .clickable(enabled = !locked, role = Role.Button) { actions.onToggle(u.unit) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        badge()
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.unit_title, u.unit, u.topicUz), color = if (locked) t.muted else t.text, fontSize = 14.sp)
            if (ko != null && t.id == GameThemeId.NEON) Text(ko, color = t.muted, fontSize = 12.sp)
        }
        when (u.state) {
            UnitState.CLEARED -> Text(stringResource(R.string.map_cleared), color = t.accent, fontSize = 11.sp, letterSpacing = 1.sp)
            UnitState.LOCKED -> Icon(Icons.Filled.Lock, contentDescription = null, tint = t.muted)
            else -> Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.muted)
        }
    }
}

@Composable
private fun UnitPanel(u: UnitRow, actions: MapActions, borderColor: Color? = null, badge: @Composable () -> Unit) {
    val t = LocalGameTokens.current
    val ko = stringArrayResource(R.array.unit_topics_ko).getOrNull(u.unit - 1)
    GamePanel(null, borderColor = borderColor) {
        Row(
            Modifier.clickable(role = Role.Button) { actions.onToggle(u.unit) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            badge()
            Column {
                if (u.state == UnitState.ACTIVE) {
                    Text(
                        stringResource(if (t.id == GameThemeId.SYSTEM) R.string.map_active_gate else R.string.map_active_station),
                        color = t.accent,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                    )
                }
                Text(
                    stringResource(R.string.unit_title, u.unit, u.topicUz),
                    color = t.text,
                    fontFamily = t.display,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
                if (ko != null && t.id == GameThemeId.NEON) Text(ko, color = t.muted, fontSize = 12.sp)
            }
        }
        u.lessons.forEach { row -> LessonLine(row, actions) }
        if (u.lessons.size == 2 && u.lessons.all { it.available }) {
            TextButton(onClick = { actions.onBoss(u.unit) }) { Text(stringResource(R.string.map_boss), color = t.accent2) }
        }
        u.enterTarget()?.let { id ->
            GameButton(
                stringResource(if (t.id == GameThemeId.SYSTEM) R.string.map_enter_gate else R.string.map_enter_station),
                onClick = { actions.onOpenLesson(id) },
            )
        }
    }
}

@Composable
private fun LessonLine(row: LessonRow, actions: MapActions) {
    val t = LocalGameTokens.current
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = row.available, role = Role.Button) { actions.onOpenLesson(row.entry.id) }
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.map_lesson_line, row.entry.lesson, row.entry.titleUz), color = t.text, fontSize = 13.sp)
                Text(row.entry.titleKo, color = t.muted, fontSize = 12.sp)
            }
            Text(
                if (row.available) statusLabel(row.status) else stringResource(R.string.coming_soon),
                color = t.accent,
                fontSize = 12.sp,
            )
        }
        ProgressBar(row.percent / 100f)
        if (row.available && row.status == LessonStatus.PASSED) {
            TextButton(onClick = { actions.onQuickCheck(row.entry.id) }) { Text(stringResource(R.string.map_quick_check)) }
        }
    }
}

/** The closing test of the book: always open, recommended once every lesson is done. */
@Composable
fun FinalCard(best: Int, lessonsDone: Int, lessonsTotal: Int, onStart: () -> Unit) {
    val t = LocalGameTokens.current
    GamePanel(null, borderColor = t.top) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RankBadge("S+", BadgeState.ACTIVE, size = 30.dp)
            Column {
                Text(stringResource(R.string.final_title), color = t.text, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(stringResource(R.string.final_info), color = t.muted, fontSize = 12.sp)
            }
        }
        if (best > 0) Text(stringResource(R.string.final_best, best), color = t.accent, fontSize = 13.sp)
        if (lessonsDone < lessonsTotal) {
            Text(stringResource(R.string.final_recommend, lessonsDone, lessonsTotal), color = t.muted, fontSize = 12.sp)
        }
        GameButton(stringResource(R.string.final_start), onClick = onStart)
    }
}
