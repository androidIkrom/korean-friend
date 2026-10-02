package uz.hangulfriend.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uz.hangulfriend.R
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.content.Exercise
import uz.hangulfriend.content.Word
import uz.hangulfriend.data.GameRepository
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.study.GameRules
import uz.hangulfriend.ui.theme.GameCard

enum class GameId(val route: String, val title: Int, val desc: Int) {
    MEMORY("memory", R.string.game_memory, R.string.game_memory_desc),
    SPEED("speed", R.string.game_speed, R.string.game_speed_desc),
    RACE("race", R.string.game_race, R.string.game_race_desc),
    CHAIN("chain", R.string.game_chain, R.string.game_chain_desc),
    ;

    companion object {
        fun fromRoute(route: String) = entries.first { it.route == route }
    }
}

data class GameResult(val score: Int, val xp: Int, val record: Boolean)

/** Shared by the games list and every game screen: content pool, records and rewards. */
class GamesViewModel(
    content: ContentRepository,
    private val game: GameRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val lessons = content.catalog().filter { content.isAvailable(it.id) }.mapNotNull { content.lesson(it.id) }
    val words: List<Word> = lessons.flatMap { it.words }
    val exercises: List<Exercise> = lessons.flatMap { it.exercises }

    private val _best = MutableStateFlow<Map<GameId, Int>>(emptyMap())
    val best: StateFlow<Map<GameId, Int>> = _best

    private val _result = MutableStateFlow<GameResult?>(null)
    val result: StateFlow<GameResult?> = _result

    init {
        reloadBest()
    }

    private fun reloadBest() {
        viewModelScope.launch { _best.value = GameId.entries.associateWith { game.best(it.route) } }
    }

    /** [correct] answers earn [GameRules.XP_GAME] each; [score] is what the record board keeps. */
    fun finish(id: GameId, score: Int, correct: Int) {
        viewModelScope.launch {
            val xp = game.award(correct * GameRules.XP_GAME, GameRepository.REASON_GAME, settings.settings.first().dailyGoalXp)
            val record = game.submitScore(id.route, score)
            _result.value = GameResult(score, xp, record)
            reloadBest()
        }
    }

    fun clearResult() {
        _result.value = null
    }
}

@Composable
fun GamesScreen(vm: GamesViewModel, onOpen: (GameId) -> Unit) {
    val best by vm.best.collectAsStateWithLifecycle()
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.games_title), style = MaterialTheme.typography.headlineMedium)
        if (vm.words.isEmpty()) Text(stringResource(R.string.game_no_content))
        GameId.entries.forEach { id ->
            GameCard(Modifier.fillMaxWidth().clickable(enabled = vm.words.isNotEmpty()) { onOpen(id) }) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(id.title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(id.desc), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.game_best, best[id] ?: 0), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** Shown when a game ends: score, XP and record. */
@Composable
fun GameOver(result: GameResult, onAgain: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.game_score, result.score), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.session_xp, result.xp), style = MaterialTheme.typography.titleLarge)
        if (result.record) Text(stringResource(R.string.game_new_record), style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAgain) { Text(stringResource(R.string.game_again)) }
    }
}
