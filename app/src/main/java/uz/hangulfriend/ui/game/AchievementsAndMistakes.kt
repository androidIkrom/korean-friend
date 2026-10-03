package uz.hangulfriend.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.hangulfriend.R
import uz.hangulfriend.data.CardKind
import uz.hangulfriend.data.GameRepository
import uz.hangulfriend.data.LessonLookup
import uz.hangulfriend.study.GameRules
import uz.hangulfriend.ui.kit.Gold
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntPanel
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.ScreenHeader
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.kit.breathing
import uz.hangulfriend.ui.kit.shapeGlow
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.theme.LocalGameTokens

class AchievementsViewModel(game: GameRepository) : ViewModel() {
    val unlocked: StateFlow<Set<String>> = game.observeAchievements()
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
}

/** Badges as diamond medallions: unlocked ones glow gold and breathe, locked ones wait under a lock. */
@Composable
fun AchievementsScreen(vm: AchievementsViewModel, onBack: () -> Unit) {
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val t = LocalGameTokens.current
    Column(Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(stringResource(R.string.achievements_title), onBack) {
            Text("${unlocked.size}/${GameRules.ACHIEVEMENTS.size}", color = Gold, fontFamily = t.display, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
        }
        GameRules.ACHIEVEMENTS.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { id -> Medal(id, id in unlocked, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Medal(id: String, has: Boolean, modifier: Modifier) {
    val t = LocalGameTokens.current
    val color = if (has) Gold else t.muted.copy(alpha = 0.6f)
    HuntPanel(modifier, accent = if (has) Gold else null) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(76.dp).then(if (has) Modifier.breathing(0.05f) else Modifier), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(52.dp)
                        .rotate(45f)
                        .then(if (has) Modifier.shapeGlow(Gold.copy(alpha = 0.55f), RectangleShape, 14.dp) else Modifier)
                        .background(if (has) Gold.copy(alpha = 0.18f) else t.background)
                        .border(2.dp, color),
                )
                Icon(
                    if (has) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                stringResource(achievementTitle(id)),
                color = if (has) t.text else t.muted,
                fontFamily = t.display,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Text(stringResource(achievementDesc(id)), color = t.muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

data class MistakeRow(val ko: String, val uz: String)

class MistakesViewModel(private val lessons: LessonLookup, private val game: GameRepository) : ViewModel() {
    private val _rows = MutableStateFlow<List<MistakeRow>>(emptyList())
    val rows: StateFlow<List<MistakeRow>> = _rows

    fun refresh() {
        viewModelScope.launch {
            _rows.value = game.mistakes().mapNotNull { card ->
                val lesson = lessons.lesson(card.lessonId) ?: return@mapNotNull null
                if (card.kind == CardKind.GRAMMAR) {
                    lesson.grammar.find { it.id == card.itemId }?.let { MistakeRow(it.pattern, it.meaningUz) }
                } else {
                    lesson.words.find { it.id == card.itemId }?.let { MistakeRow(it.ko, it.uz) }
                }
            }.distinct()
        }
    }
}

@Composable
fun MistakesScreen(vm: MistakesViewModel, onPractice: () -> Unit, onBack: () -> Unit) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    val t = LocalGameTokens.current
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Column(Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScreenHeader(stringResource(R.string.mistakes_title), onBack)
        if (rows.isEmpty()) {
            HuntPanel(accent = CorrectGreen) {
                Text(stringResource(R.string.mistakes_empty), color = t.text, fontSize = 16.sp)
            }
        } else {
            HuntButton(
                stringResource(R.string.mistakes_practice).uppercase(),
                onClick = onPractice,
                modifier = Modifier.fillMaxWidth(),
                style = HuntStyle.DANGER,
                icon = Icons.Filled.Bolt,
                sfx = Sfx.OPEN,
            )
            HuntPanel(accent = t.danger) {
                rows.forEachIndexed { i, r ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${i + 1}", color = t.danger, fontFamily = t.display, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.ko, color = t.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(r.uz, color = t.muted, fontSize = 13.sp)
                        }
                    }
                    if (i < rows.lastIndex) HorizontalDivider(color = t.danger.copy(alpha = 0.25f))
                }
            }
        }
    }
}
