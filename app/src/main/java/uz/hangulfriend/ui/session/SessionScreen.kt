package uz.hangulfriend.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.hangulfriend.R
import uz.hangulfriend.study.GameRules
import uz.hangulfriend.ui.exercise.ExerciseView
import uz.hangulfriend.ui.kit.ComboChip
import uz.hangulfriend.ui.kit.EdgeFlash
import uz.hangulfriend.ui.kit.FloatingText
import uz.hangulfriend.ui.kit.FloorBar
import uz.hangulfriend.ui.kit.Gold
import uz.hangulfriend.ui.kit.HeartRow
import uz.hangulfriend.ui.kit.LocalGameFeedback
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.theme.WrongRed

/** Length of the final test (stage 7c spec §3). */
const val FINAL_SECONDS = 25 * 60

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

    fun timeUp() {
        viewModelScope.launch { lock.withLock { controller.timeUp() } }
    }
}

/** The value [value] had before its latest change (itself until it first changes). */
@Composable
private fun rememberPrevious(value: Int): Int {
    val ref = remember { intArrayOf(value, value) }
    if (ref[1] != value) {
        ref[0] = ref[1]
        ref[1] = value
    }
    return ref[0]
}

@Composable
fun SessionScreen(vm: SessionViewModel, mode: SessionMode, onClose: () -> Unit) {
    val s by vm.state.collectAsStateWithLifecycle()
    val feedback = LocalGameFeedback.current
    // Final test: a 25-minute countdown that survives rotation; at zero the test ends.
    val startMs = rememberSaveable { System.currentTimeMillis() }
    var leftSec by remember { mutableIntStateOf(FINAL_SECONDS) }
    if (mode == SessionMode.FINAL) {
        LaunchedEffect(s.finished, s.loading) {
            while (!s.finished && !s.loading) {
                leftSec = (FINAL_SECONDS - (System.currentTimeMillis() - startMs) / 1000).toInt().coerceAtLeast(0)
                if (leftSec == 0) {
                    vm.timeUp()
                    break
                }
                delay(1000)
            }
        }
    }
    // A combo hit layers its whoosh just after the answer's own sound.
    val firstCombo = remember { s.combo }
    LaunchedEffect(s.combo) {
        if (s.combo != firstCombo && s.combo >= GameRules.COMBO_FROM) {
            delay(140)
            feedback.play(Sfx.COMBO)
        }
    }
    val xpBefore = rememberPrevious(s.xpEarned)
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (!s.finished) BattleHud(s, mode, leftSec, onClose)
            when {
                s.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                s.finished -> GateClearedView(s, mode, onClose)
                else -> Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 16.dp),
                ) {
                    val item = s.current!!
                    key(s.index) { ExerciseView(item, onResult = vm::submit, onNext = vm::next) }
                }
            }
        }
        if (!s.finished) {
            val verdict = s.lastCorrect
            EdgeFlash(if (verdict == true) CorrectGreen else WrongRed, key = if (s.answered && verdict != null) s.index + 1 else 0)
            FloatingText(
                "+${s.xpEarned - xpBefore} XP",
                key = s.xpEarned,
                color = Gold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 96.dp),
            )
        }
    }
}

/** Close, the floor bar, then hearts, combo, XP and the timer. */
@Composable
private fun BattleHud(s: SessionState, mode: SessionMode, leftSec: Int, onClose: () -> Unit) {
    val t = LocalGameTokens.current
    val total = s.items.size
    val done = (s.index + if (s.answered) 1 else 0).coerceAtMost(total)
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.session_close), tint = t.muted)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.battle_floor, (s.index + 1).coerceAtMost(total.coerceAtLeast(1)), total).uppercase(),
                color = t.muted,
                fontFamily = t.display,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
            )
            FloorBar(done, total)
        }
    }
    val showCombo = s.combo >= GameRules.COMBO_FROM
    if (s.hearts != null || showCombo || s.xpEarned > 0 || mode == SessionMode.FINAL) {
        Row(
            Modifier.fillMaxWidth().padding(start = 48.dp, top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            s.hearts?.let { h ->
                val desc = stringResource(R.string.battle_hearts, h)
                HeartRow(h, SessionController.BOSS_HEARTS, Modifier.semantics { contentDescription = desc })
            }
            if (showCombo) ComboChip(s.combo)
            Spacer(Modifier.weight(1f))
            if (s.xpEarned > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp))
                    Text("${s.xpEarned} XP", color = Gold, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            if (mode == SessionMode.FINAL && !s.finished) {
                val urgent = leftSec < 60
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = if (urgent) t.danger else t.muted, modifier = Modifier.size(16.dp))
                    Text(
                        "%02d:%02d".format(leftSec / 60, leftSec % 60),
                        color = if (urgent) t.danger else t.text,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(2.dp))
}
