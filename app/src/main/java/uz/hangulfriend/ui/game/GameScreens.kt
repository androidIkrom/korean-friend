package uz.hangulfriend.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.random.Random
import kotlinx.coroutines.delay
import uz.hangulfriend.R
import uz.hangulfriend.hangul.AnswerChecker
import uz.hangulfriend.study.games.FlipResult
import uz.hangulfriend.study.games.GrammarChain
import uz.hangulfriend.study.games.MemoryGame
import uz.hangulfriend.study.games.SpeedRound
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.theme.WrongRed

/** Picks the screen for [id]; a finished game shows [GameOver] and can be restarted. */
@Composable
fun GameScreen(vm: GamesViewModel, id: GameId) {
    val result by vm.result.collectAsStateWithLifecycle()
    var round by remember { mutableIntStateOf(0) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(id.title), style = MaterialTheme.typography.headlineSmall)
        val done = result
        if (done != null) {
            GameOver(done) {
                vm.clearResult()
                round++
            }
        } else {
            androidx.compose.runtime.key(round) {
                when (id) {
                    GameId.MEMORY -> MemoryScreen(vm)
                    GameId.SPEED -> SpeedScreen(vm, typing = false)
                    GameId.RACE -> SpeedScreen(vm, typing = true)
                    GameId.CHAIN -> ChainScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun MemoryScreen(vm: GamesViewModel) {
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
        // Score: 100 minus 5 per move beyond the perfect 6, never below 10.
        if (game.done) vm.finish(GameId.MEMORY, (100 - (game.moves - MemoryGame.PAIRS) * 5).coerceAtLeast(10), MemoryGame.PAIRS)
    }
    Text(stringResource(R.string.game_moves, game.moves))
    LazyVerticalGrid(GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(cards) { i, card ->
            val color = when {
                card.matched -> CorrectGreen.copy(alpha = 0.25f)
                card.faceUp -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            Card(
                onClick = {
                    if (game.flip(i) == FlipResult.MISMATCH) mismatch = true
                    cards = game.cards
                },
                modifier = Modifier.aspectRatio(1f),
                colors = CardDefaults.cardColors(containerColor = color),
            ) {
                Column(Modifier.fillMaxWidth().padding(4.dp).weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (card.faceUp || card.matched) {
                        Text(card.text, textAlign = TextAlign.Center, style = if (card.korean) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodySmall)
                    } else {
                        Text("?", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

/** Speed round (pick the meaning) or, with [typing], Hangul race (type the Korean word). */
@Composable
private fun SpeedScreen(vm: GamesViewModel, typing: Boolean) {
    val round = remember { SpeedRound(vm.words, Random.Default) }
    var question by remember { mutableStateOf(round.next()) }
    var typed by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var flash by remember { mutableStateOf<Boolean?>(null) }
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
    LaunchedEffect(flash) {
        if (flash != null) {
            delay(300)
            flash = null
        }
    }
    Text(stringResource(R.string.game_time_left, (left / 1000).toInt()), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.game_score, if (typing) typed else round.score))
    val tint = when (flash) {
        true -> CorrectGreen
        false -> WrongRed
        null -> MaterialTheme.colorScheme.onSurface
    }
    if (typing) {
        Text(question.word.uz, style = MaterialTheme.typography.headlineSmall, color = tint)
        fun submit() {
            val ok = AnswerChecker.check(input, listOf(question.word.ko)).correct
            if (ok) typed++
            flash = ok
            input = ""
            question = round.next()
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = ::submit, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ex_check))
        }
    } else {
        Text(question.word.ko, style = MaterialTheme.typography.displaySmall, color = tint)
        question.options.forEach { option ->
            OutlinedButton(
                onClick = {
                    flash = round.answer(option)
                    question = round.next()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(option) }
        }
    }
}

@Composable
private fun ChainScreen(vm: GamesViewModel) {
    val chain = remember { GrammarChain(vm.exercises, Random.Default) }
    var current by remember { mutableStateOf(chain.current()) }
    var input by remember { mutableStateOf("") }
    var missed by remember { mutableStateOf<String?>(null) }
    if (chain.total == 0) {
        Text(stringResource(R.string.game_no_content))
        return
    }
    Text(stringResource(R.string.game_chain_length, chain.length, chain.total), style = MaterialTheme.typography.titleMedium)
    val e = current
    if (e == null) {
        missed?.let { Text(stringResource(R.string.ex_correct_answer, it), color = WrongRed) }
        LaunchedEffect(Unit) { vm.finish(GameId.CHAIN, chain.length, chain.length) }
        return
    }
    Text(stringResource(R.string.ex_conjugate_prompt, e.base.orEmpty(), e.form.orEmpty()), style = MaterialTheme.typography.headlineSmall)
    fun submit() {
        if (!chain.submit(input)) missed = e.answers.first()
        input = ""
        current = chain.current()
    }
    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = ::submit, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.ex_check))
    }
}
