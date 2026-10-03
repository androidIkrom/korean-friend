package uz.hangulfriend.ui.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.random.Random
import kotlinx.coroutines.delay
import uz.hangulfriend.R
import uz.hangulfriend.hangul.AnswerChecker
import uz.hangulfriend.study.games.FlipResult
import uz.hangulfriend.study.games.GrammarChain
import uz.hangulfriend.study.games.MemoryCard
import uz.hangulfriend.study.games.MemoryGame
import uz.hangulfriend.study.games.SpeedRound
import uz.hangulfriend.ui.exercise.QuestCard
import uz.hangulfriend.ui.kit.EdgeFlash
import uz.hangulfriend.ui.kit.GlowBar
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.LocalGameFeedback
import uz.hangulfriend.ui.kit.LocalReducedMotion
import uz.hangulfriend.ui.kit.OptionState
import uz.hangulfriend.ui.kit.OptionTile
import uz.hangulfriend.ui.kit.ScreenHeader
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.kit.shake
import uz.hangulfriend.ui.kit.shape
import uz.hangulfriend.ui.kit.shapeGlow
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.theme.WrongRed

/** Hit or miss of the latest answer, keyed so each new answer flashes once. */
private data class Hit(val n: Int = 0, val correct: Boolean = true)

/** Picks the screen for [id]; a finished game shows [GameOver] and can be restarted. */
@Composable
fun GameScreen(vm: GamesViewModel, id: GameId, onBack: () -> Unit) {
    val result by vm.result.collectAsStateWithLifecycle()
    var round by remember { mutableIntStateOf(0) }
    var hit by remember { mutableStateOf(Hit()) }
    val feedback = LocalGameFeedback.current
    val onHit: (Boolean) -> Unit = { ok ->
        feedback.play(if (ok) Sfx.CORRECT else Sfx.WRONG)
        hit = Hit(hit.n + 1, ok)
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeader(stringResource(id.title), onBack)
            val done = result
            if (done != null) {
                GameOver(done) {
                    vm.clearResult()
                    round++
                }
            } else {
                key(round) {
                    when (id) {
                        GameId.MEMORY -> MemoryScreen(vm, onHit)
                        GameId.SPEED -> SpeedScreen(vm, typing = false, onHit)
                        GameId.RACE -> SpeedScreen(vm, typing = true, onHit)
                        GameId.CHAIN -> ChainScreen(vm, onHit)
                    }
                }
            }
        }
        if (result == null) EdgeFlash(if (hit.correct) CorrectGreen else WrongRed, key = hit.n)
    }
}

@Composable
private fun MemoryScreen(vm: GamesViewModel, onHit: (Boolean) -> Unit) {
    val t = LocalGameTokens.current
    val game = remember { MemoryGame(vm.words, Random.Default) }
    var cards by remember { mutableStateOf(game.cards) }
    var mismatch by remember { mutableStateOf(false) }
    LaunchedEffect(mismatch) {
        if (mismatch) {
            delay(800)
            game.hideMismatch()
            cards = game.cards
            mismatch = false
        }
    }
    LaunchedEffect(cards) {
        if (game.done) vm.finish(GameId.MEMORY, MemoryGame.score(game.moves), MemoryGame.PAIRS)
    }
    Text(stringResource(R.string.game_moves, game.moves), color = t.muted, fontFamily = t.display, letterSpacing = 1.sp)
    LazyVerticalGrid(GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(cards) { i, card ->
            MemoryTile(card, wrong = mismatch && card.faceUp && !card.matched) {
                when (game.flip(i)) {
                    FlipResult.MATCH -> onHit(true)
                    FlipResult.MISMATCH -> {
                        onHit(false)
                        mismatch = true
                    }
                    else -> Unit
                }
                cards = game.cards
            }
        }
    }
}

/** A card that flips on its vertical axis: "?" on the back, the word on the face; matched cards glow green. */
@Composable
private fun MemoryTile(card: MemoryCard, wrong: Boolean, onClick: () -> Unit) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val reduced = LocalReducedMotion.current
    val shown = card.faceUp || card.matched
    val turn by animateFloatAsState(if (shown) 180f else 0f, if (reduced) snap() else tween(320), label = "flip")
    val face = turn > 90f
    val shape = t.shape(10.dp)
    val rim = when {
        card.matched -> CorrectGreen
        wrong -> WrongRed
        face -> t.accent
        else -> t.panelBorder
    }
    Box(
        Modifier
            .aspectRatio(1f)
            .shake(if (wrong) 1 else 0)
            .graphicsLayer {
                rotationY = turn
                cameraDistance = 12 * density
            }
            .then(if (card.matched) Modifier.shapeGlow(CorrectGreen.copy(alpha = 0.5f), shape, 10.dp) else Modifier)
            .clip(shape)
            .background(if (face) rim.copy(alpha = 0.18f).compositeOver(t.panel.copy(alpha = 1f)) else t.panel, shape)
            .border(if (face) 1.5.dp else 1.dp, rim, shape)
            .clickable(enabled = !shown, role = Role.Button) {
                feedback.play(Sfx.TAP)
                onClick()
            }
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (face) {
            Text(
                card.text,
                color = t.text,
                textAlign = TextAlign.Center,
                style = if (card.korean) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodySmall,
                // The face is drawn on the back of the turned card, so it is mirrored back.
                modifier = Modifier.graphicsLayer { rotationY = 180f },
            )
        } else {
            Text("?", color = t.accent, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        }
    }
}

/** A draining timer bar that turns red in the last 10 seconds. */
@Composable
private fun TimerBar(leftMs: Long) {
    val t = LocalGameTokens.current
    val urgent = leftMs < 10_000
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.game_time_left, (leftMs / 1000).toInt()),
            color = if (urgent) t.danger else t.text,
            fontFamily = t.display,
            fontWeight = FontWeight.Bold,
        )
        GlowBar(SpeedRound.timeFraction(leftMs), Modifier.weight(1f), color = if (urgent) t.danger else null, height = 8)
    }
}

/** Speed round (pick the meaning) or, with [typing], Hangul race (type the Korean word). */
@Composable
private fun SpeedScreen(vm: GamesViewModel, typing: Boolean, onHit: (Boolean) -> Unit) {
    val t = LocalGameTokens.current
    val round = remember { SpeedRound(vm.words, Random.Default) }
    var question by remember { mutableStateOf(round.next()) }
    var typed by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var left by remember { mutableLongStateOf(SpeedRound.DURATION_MS) }
    val id = if (typing) GameId.RACE else GameId.SPEED
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(100)
            left -= 100
        }
        val score = if (typing) typed else round.score
        vm.finish(id, score, score)
    }
    TimerBar(left)
    Text(stringResource(R.string.game_score, if (typing) typed else round.score), color = t.muted, fontFamily = t.display)
    if (typing) {
        fun submit() {
            val ok = AnswerChecker.check(input, listOf(question.word.ko)).correct
            if (ok) typed++
            onHit(ok)
            input = ""
            question = round.next()
        }
        QuestCard(stringResource(R.string.ex_typing_prompt)) {
            Text(question.word.uz, color = t.text, style = MaterialTheme.typography.headlineSmall)
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (input.isNotBlank()) submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        HuntButton(stringResource(R.string.ex_check).uppercase(), onClick = ::submit, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth())
    } else {
        QuestCard(stringResource(R.string.game_speed_prompt)) {
            Text(question.word.ko, color = t.text, style = MaterialTheme.typography.displaySmall)
        }
        question.options.forEachIndexed { i, option ->
            OptionTile(
                text = option,
                onClick = {
                    onHit(round.answer(option))
                    question = round.next()
                },
                state = OptionState.IDLE,
                letter = 'A' + i,
            )
        }
    }
}

@Composable
private fun ChainScreen(vm: GamesViewModel, onHit: (Boolean) -> Unit) {
    val t = LocalGameTokens.current
    val chain = remember { GrammarChain(vm.exercises, Random.Default) }
    var current by remember { mutableStateOf(chain.current()) }
    var input by remember { mutableStateOf("") }
    var missed by remember { mutableStateOf<String?>(null) }
    if (chain.total == 0) {
        Text(stringResource(R.string.game_no_content), color = t.muted)
        return
    }
    Text(stringResource(R.string.game_chain_length, chain.length, chain.total), color = t.accent, fontFamily = t.display, fontWeight = FontWeight.Bold)
    GlowBar(if (chain.total == 0) 0f else chain.length.toFloat() / chain.total, height = 6)
    val e = current
    if (e == null) {
        missed?.let { Text(stringResource(R.string.ex_correct_answer, it), color = WrongRed) }
        LaunchedEffect(Unit) { vm.finish(GameId.CHAIN, chain.length, chain.length) }
        return
    }
    fun submit() {
        val ok = chain.submit(input)
        if (!ok) missed = e.answers.first()
        onHit(ok)
        input = ""
        current = chain.current()
    }
    QuestCard(stringResource(R.string.game_chain_desc)) {
        Text(stringResource(R.string.ex_conjugate_prompt, e.base.orEmpty(), e.form.orEmpty()), color = t.text, style = MaterialTheme.typography.headlineSmall)
    }
    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (input.isNotBlank()) submit() }),
        modifier = Modifier.fillMaxWidth(),
    )
    HuntButton(stringResource(R.string.ex_check).uppercase(), onClick = ::submit, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth())
}
