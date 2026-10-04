package uz.hangulfriend.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import uz.hangulfriend.ui.kit.GateRays
import uz.hangulfriend.ui.kit.Gold
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntPanel
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.LocalGameFeedback
import uz.hangulfriend.ui.kit.LocalReducedMotion
import uz.hangulfriend.ui.kit.ScreenHeader
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.kit.shape
import uz.hangulfriend.ui.theme.LocalGameTokens

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

private fun GameId.icon(): ImageVector = when (this) {
    GameId.MEMORY -> Icons.Outlined.GridView
    GameId.SPEED -> Icons.Filled.Bolt
    GameId.RACE -> Icons.Outlined.Keyboard
    GameId.CHAIN -> Icons.Outlined.Link
}

/** The Arena: one card per game with its record and a PLAY button. */
@Composable
fun GamesScreen(vm: GamesViewModel, onOpen: (GameId) -> Unit, onBack: () -> Unit) {
    val t = LocalGameTokens.current
    val best by vm.best.collectAsStateWithLifecycle()
    val playable = vm.words.isNotEmpty()
    Column(Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(stringResource(R.string.arena_title), onBack) {
            Icon(Icons.Outlined.SportsEsports, contentDescription = null, tint = t.accent2, modifier = Modifier.padding(end = 8.dp))
        }
        if (!playable) Text(stringResource(R.string.game_no_content), color = t.muted)
        GameId.entries.forEach { id ->
            HuntPanel(accent = t.accent2) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val shape = t.shape(8.dp)
                    Box(
                        Modifier.size(48.dp).background(t.accent2.copy(alpha = 0.18f), shape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(id.icon(), contentDescription = null, tint = t.accent2) }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(id.title), color = t.text, fontFamily = t.ui, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(stringResource(id.desc), color = t.muted, fontSize = 13.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.game_best, best[id] ?: 0), color = Gold, fontFamily = t.ui, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    HuntButton(
                        stringResource(R.string.game_play),
                        onClick = { onOpen(id) },
                        enabled = playable,
                        icon = Icons.Filled.Bolt,
                        sfx = Sfx.OPEN,
                        minHeight = 42.dp,
                        fontSize = 13,
                    )
                }
            }
        }
    }
}

/** Shown when a game ends: rays behind the score, the gold XP, a popping record badge, the CLEAR sound. */
@Composable
fun GameOver(result: GameResult, onAgain: () -> Unit) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val reduced = LocalReducedMotion.current
    val pop = remember { Animatable(if (reduced) 1f else 0.3f) }
    LaunchedEffect(result) {
        feedback.play(Sfx.CLEAR)
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f))
    }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            GateRays(Gold.copy(alpha = 0.3f), Modifier.fillMaxSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(result.score.toString(), color = t.text, fontFamily = t.ui, fontWeight = FontWeight.Bold, fontSize = 64.sp, modifier = Modifier.scale(pop.value))
                Text(stringResource(R.string.game_score_label), color = t.muted, fontFamily = t.ui)
            }
        }
        Text(stringResource(R.string.session_xp, result.xp), color = Gold, fontFamily = t.ui, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        if (result.record) {
            Text(
                stringResource(R.string.game_new_record),
                color = Gold,
                fontFamily = t.ui,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.scale(pop.value),
            )
        }
        HuntButton(stringResource(R.string.game_again), onClick = onAgain, modifier = Modifier.fillMaxWidth(), style = HuntStyle.GOLD, sfx = Sfx.OPEN)
    }
}
