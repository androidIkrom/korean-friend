package uz.hangulfriend.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import uz.hangulfriend.ui.theme.GameCard

class AchievementsViewModel(game: GameRepository) : ViewModel() {
    val unlocked: StateFlow<Set<String>> = game.observeAchievements()
        .map { list -> list.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
}

@Composable
fun AchievementsScreen(vm: AchievementsViewModel) {
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.achievements_title), style = MaterialTheme.typography.headlineMedium)
        GameRules.ACHIEVEMENTS.forEach { id ->
            val has = id in unlocked
            GameCard(Modifier.fillMaxWidth().alpha(if (has) 1f else 0.45f)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (has) "🏆" else "🔒", style = MaterialTheme.typography.headlineSmall)
                    Column {
                        Text(stringResource(achievementTitle(id)), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(achievementDesc(id)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
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
fun MistakesScreen(vm: MistakesViewModel, onPractice: () -> Unit) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.mistakes_title), style = MaterialTheme.typography.headlineMedium)
        if (rows.isEmpty()) {
            Text(stringResource(R.string.mistakes_empty))
        } else {
            Button(onClick = onPractice, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.mistakes_practice)) }
            rows.forEach { r ->
                GameCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(r.ko, style = MaterialTheme.typography.titleMedium)
                        Text(r.uz, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
