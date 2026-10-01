package uz.hangulfriend.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.hangulfriend.R
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.study.GameRules
import uz.hangulfriend.ui.game.achievementTitle
import uz.hangulfriend.ui.exercise.ExerciseView

class SessionViewModel(private val controller: SessionController) : ViewModel() {
    val state = controller.state

    /** Serializes submit/next so a quick "Keyingi" never overtakes grading. */
    private val lock = Mutex()

    init {
        viewModelScope.launch { controller.load() }
    }

    fun submit(outcome: ExerciseOutcome) {
        viewModelScope.launch { lock.withLock { controller.submit(outcome) } }
    }

    fun next() {
        viewModelScope.launch { lock.withLock { controller.next() } }
    }
}

@Composable
fun SessionScreen(vm: SessionViewModel, mode: SessionMode, onClose: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.session_close)) }
            LinearProgressIndicator(
                progress = { if (s.items.isEmpty()) 0f else s.index.toFloat() / s.items.size },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
            s.hearts?.let { h -> Text("❤️".repeat(h) + "🤍".repeat((SessionController.BOSS_HEARTS - h).coerceAtLeast(0))) }
            if (s.combo >= GameRules.COMBO_FROM) Text(stringResource(R.string.session_combo, s.combo), style = MaterialTheme.typography.labelLarge)
            if (s.xpEarned > 0) Text(stringResource(R.string.session_xp, s.xpEarned), style = MaterialTheme.typography.labelLarge)
        }
        when {
            s.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            s.finished -> ResultView(s, mode, onClose)
            else -> Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 16.dp),
            ) {
                val item = s.current!!
                key(s.index) { ExerciseView(item, onResult = vm::submit, onNext = vm::next) }
            }
        }
    }
}

@Composable
private fun ResultView(s: SessionState, mode: SessionMode, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (s.items.isEmpty()) {
            Text(stringResource(R.string.session_empty), style = MaterialTheme.typography.titleLarge)
        } else {
            Text(
                stringResource(R.string.session_result, s.correctCount, s.items.size, s.scorePercent),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(stringResource(R.string.session_xp, s.xpEarned), style = MaterialTheme.typography.titleLarge)
            if (mode == SessionMode.BOSS) {
                Text(
                    stringResource(if (s.failed) R.string.session_boss_lost else R.string.session_boss_won),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }
            s.newAchievements.forEach { id ->
                Text(
                    stringResource(R.string.session_new_achievement, stringResource(achievementTitle(id))),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
            if (mode == SessionMode.TEST) {
                val passed = s.scorePercent >= ProgressRepository.PASS_PERCENT
                Text(
                    stringResource(if (passed) R.string.session_test_passed else R.string.session_test_failed),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Button(onClick = onClose) { Text(stringResource(R.string.session_finish)) }
    }
}
