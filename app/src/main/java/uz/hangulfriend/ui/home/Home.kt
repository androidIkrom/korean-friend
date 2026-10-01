package uz.hangulfriend.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.hangulfriend.R
import uz.hangulfriend.content.CatalogEntry
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.data.GameRepository
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.data.StudyRepository
import uz.hangulfriend.study.GameRules

data class HomeStats(
    val level: GameRules.LevelInfo = GameRules.level(0),
    val todayXp: Int = 0,
    val goal: Int = GameRules.DEFAULT_GOAL,
    val streak: Int = 0,
)

class HomeViewModel(
    content: ContentRepository,
    private val study: StudyRepository,
    private val settings: SettingsRepository,
    private val game: GameRepository,
    private val clock: Clock,
) : ViewModel() {
    private val catalog = content.catalog()

    private val _dueCount = MutableStateFlow(0)
    val dueCount: StateFlow<Int> = _dueCount

    private val _stats = MutableStateFlow(HomeStats())
    val stats: StateFlow<HomeStats> = _stats

    val currentLesson: StateFlow<CatalogEntry?> = settings.settings
        .map { s -> catalog.find { it.id == s.currentLessonId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Due-ness and "today" change with time, not only with the database, so the screen refreshes on every resume. */
    fun refresh() {
        viewModelScope.launch {
            val s = settings.settings.first()
            _dueCount.value = study.dueQueue(s.dailyNewLimit).size
            _stats.value = HomeStats(
                level = GameRules.level(game.observeTotalXp().first()),
                todayXp = game.todayXp(),
                goal = s.dailyGoalXp,
                streak = GameRules.streak(game.dailyXp(), LocalDate.now(clock), s.dailyGoalXp),
            )
        }
    }
}

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onStartReview: () -> Unit,
    onContinueLesson: (String) -> Unit,
    onGames: () -> Unit,
    onMistakes: () -> Unit,
    onAchievements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val due by vm.dueCount.collectAsStateWithLifecycle()
    val current by vm.currentLesson.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Column(
        modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        StatsCard(stats)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.home_review_count, due), style = MaterialTheme.typography.titleLarge)
                if (due == 0) Text(stringResource(R.string.home_review_done))
                Button(onClick = onStartReview, enabled = due > 0, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_review_start))
                }
            }
        }
        current?.let { lesson ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_continue), style = MaterialTheme.typography.labelLarge)
                    Text(lesson.titleKo, style = MaterialTheme.typography.titleLarge)
                    Text(lesson.titleUz)
                    FilledTonalButton(onClick = { onContinueLesson(lesson.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_continue))
                    }
                }
            }
        }
        OutlinedButton(onClick = onGames, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.home_games)) }
        OutlinedButton(onClick = onMistakes, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.home_mistakes)) }
        OutlinedButton(onClick = onAchievements, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_achievements))
        }
    }
}

@Composable
private fun StatsCard(stats: HomeStats) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (stats.todayXp.toFloat() / stats.goal).coerceIn(0f, 1f) },
                    modifier = Modifier.size(72.dp),
                    strokeWidth = 8.dp,
                )
                Text(stringResource(R.string.home_streak, stats.streak), style = MaterialTheme.typography.labelMedium)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.home_level, stats.level.level), style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(
                    progress = { stats.level.xpIntoLevel.toFloat() / stats.level.xpForNext },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.home_level_progress, stats.level.xpIntoLevel, stats.level.xpForNext),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(stringResource(R.string.home_goal, stats.todayXp, stats.goal), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
