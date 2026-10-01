package uz.hangulfriend.ui.story

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.hangulfriend.R
import uz.hangulfriend.content.ChooseReply
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.content.StoryLine
import uz.hangulfriend.content.StoryQuiz
import uz.hangulfriend.content.StoryStep
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.data.StoryRepository
import uz.hangulfriend.story.StoryPlayer
import uz.hangulfriend.ui.AudioButton
import uz.hangulfriend.ui.LocalAudioPlayer

class EpisodeViewModel(
    private val lessonId: String,
    private val content: ContentRepository,
    private val story: StoryRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _player = MutableStateFlow<StoryPlayer?>(null)
    val player: StateFlow<StoryPlayer?> = _player

    /** XP from finishing; null until the episode is finished and recorded. */
    private val _xp = MutableStateFlow<Int?>(null)
    val xp: StateFlow<Int?> = _xp

    /** The clip to play now; a solved choice is spoken in Aziz's voice. Lines speak when they appear. */
    private val _speak = MutableStateFlow<String?>(null)
    val speak: StateFlow<String?> = _speak

    val names: Map<String, String> = content.characters().associate { it.id to it.nameUz }

    init {
        viewModelScope.launch {
            val s = withContext(Dispatchers.IO) { content.lesson(lessonId)?.story } ?: return@launch
            update(StoryPlayer(s))
        }
    }

    fun next() = _player.value?.let { update(it.next()) }

    fun choose(option: String) {
        val before = _player.value ?: return
        val after = before.choose(option)
        val solved = before.current as? ChooseReply
        if (solved != null && option == solved.answer) _speak.value = solved.audio
        update(after, speakLine = solved == null)
    }

    private fun update(p: StoryPlayer, speakLine: Boolean = true) {
        _player.value = p
        val line = p.current as? StoryLine
        if (speakLine && line != null) _speak.value = line.audio
        if (p.finished && _xp.value == null) {
            viewModelScope.launch { _xp.value = story.complete(lessonId, settings.settings.first().dailyGoalXp) }
        }
    }
}

@Composable
fun EpisodeScreen(vm: EpisodeViewModel, onClose: () -> Unit) {
    val player by vm.player.collectAsStateWithLifecycle()
    val xp by vm.xp.collectAsStateWithLifecycle()
    val speak by vm.speak.collectAsStateWithLifecycle()
    val audio = LocalAudioPlayer.current
    LaunchedEffect(speak) { speak?.let { audio?.play(it) } }
    val p = player ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val list = rememberLazyListState()
    LaunchedEffect(p.shown.size, p.finished) { list.animateScrollToItem(maxOf(0, p.shown.size - 1)) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LinearProgressIndicator(progress = { p.progress }, modifier = Modifier.fillMaxWidth())
        LazyColumn(Modifier.weight(1f), state = list, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(p.shown) { i, step ->
                val solved = i < p.shown.size - 1 || p.finished
                StepBubble(step, solved, vm.names)
            }
        }
        when (val step = p.current) {
            is StoryLine -> Button(onClick = vm::next, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.story_next)) }
            is ChooseReply -> Choices(step.promptUz, step.options, p.eliminated, p.lastWrongWhy, vm::choose)
            is StoryQuiz -> Choices(step.promptUz, step.options, p.eliminated, p.lastWrongWhy, vm::choose)
            null -> Finish(xp, onClose)
        }
    }
}

@Composable
private fun StepBubble(step: StoryStep, solved: Boolean, names: Map<String, String>) {
    when (step) {
        is StoryLine -> Bubble(names[step.speaker] ?: step.speaker, step.ko, step.uz, step.audio, mine = step.speaker == "aziz")
        is ChooseReply -> if (solved) Bubble(names[step.speaker] ?: step.speaker, step.answer, step.uz, step.audio, mine = true)
        is StoryQuiz -> if (solved) Text("✓ ${step.promptUz} — ${step.answer}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Bubble(name: String, ko: String, uz: String, audio: String?, mine: Boolean) {
    var showUz by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Card(
            Modifier.widthIn(max = 300.dp).clickable { showUz = !showUz },
            colors = if (mine) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors(),
        ) {
            Row(Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f, fill = false)) {
                    Text(name, style = MaterialTheme.typography.labelMedium)
                    Text(ko, style = MaterialTheme.typography.titleMedium)
                    if (showUz) Text(uz, style = MaterialTheme.typography.bodySmall)
                }
                AudioButton(audio)
            }
        }
    }
}

@Composable
private fun Choices(prompt: String, options: List<String>, eliminated: Set<String>, why: String?, onChoose: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(prompt, style = MaterialTheme.typography.titleMedium)
        options.forEach { option ->
            OutlinedButton(onClick = { onChoose(option) }, enabled = option !in eliminated, modifier = Modifier.fillMaxWidth()) {
                Text(option)
            }
        }
        why?.let { Text("❌ $it", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun Finish(xp: Int?, onClose: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.story_finish_title), style = MaterialTheme.typography.titleLarge)
            when {
                xp == null -> CircularProgressIndicator()
                xp > 0 -> Text(stringResource(R.string.story_xp, xp), style = MaterialTheme.typography.headlineSmall)
                else -> Text(stringResource(R.string.story_replayed))
            }
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.story_close)) }
        }
    }
}
