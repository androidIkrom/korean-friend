package uz.hangulfriend.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import uz.hangulfriend.data.GameRepository
import uz.hangulfriend.data.HeroGender
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.data.StoryRepository
import uz.hangulfriend.story.StoryPlayer
import uz.hangulfriend.study.GameRules
import uz.hangulfriend.study.Rank
import uz.hangulfriend.study.RankRules
import uz.hangulfriend.ui.AudioButton
import uz.hangulfriend.ui.LocalAudioPlayer
import uz.hangulfriend.ui.avatar.Avatar
import uz.hangulfriend.ui.avatar.SpeakerArt
import uz.hangulfriend.ui.theme.GameBackground
import uz.hangulfriend.ui.theme.GameButton
import uz.hangulfriend.ui.theme.GamePanel
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.kit.FloorBar
import uz.hangulfriend.ui.kit.GateRays
import uz.hangulfriend.ui.kit.GlowBar
import uz.hangulfriend.ui.kit.Gold
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntPanel
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.LocalGameFeedback
import uz.hangulfriend.ui.kit.OptionState
import uz.hangulfriend.ui.kit.OptionTile
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.settings.LocalPlayerName
import uz.hangulfriend.data.personalize
import uz.hangulfriend.study.bookOf

/** One clip to play; [id] grows with every new clip so a rotation does not replay the last one. */
data class SpeakEvent(val id: Int, val file: String)

/** Remembers which clips already played (lives in the view model, so it survives rotation). */
class PlayOnce {
    private var last = -1

    fun shouldPlay(id: Int): Boolean = (id > last).also { if (it) last = id }
}

/**
 * Header facts of the episode: its number in the book, its title, its step count, and the place name over
 * its gate (null draws no gate). [seed] keeps each lesson's gate scene the same every time.
 */
data class EpisodeMeta(val number: Int, val title: String, val total: Int, val gate: String? = null, val seed: Int = 0)

class EpisodeViewModel(
    private val lessonId: String,
    private val content: ContentRepository,
    private val story: StoryRepository,
    private val settings: SettingsRepository,
    private val game: GameRepository,
) : ViewModel() {
    private val _player = MutableStateFlow<StoryPlayer?>(null)
    val player: StateFlow<StoryPlayer?> = _player

    private val _meta = MutableStateFlow<EpisodeMeta?>(null)
    val meta: StateFlow<EpisodeMeta?> = _meta

    /** Aziz's current rank, so his art matches the avatar on Home. */
    private val _rank = MutableStateFlow(Rank.E)
    val rank: StateFlow<Rank> = _rank

    /** XP from finishing; null until the episode is finished and recorded. */
    private val _xp = MutableStateFlow<Int?>(null)
    val xp: StateFlow<Int?> = _xp

    /** The clip to play now; a solved choice is spoken in Aziz's voice. Lines speak when they appear. */
    private val _speak = MutableStateFlow<SpeakEvent?>(null)
    val speak: StateFlow<SpeakEvent?> = _speak
    val playOnce = PlayOnce()
    private var clips = 0

    private fun say(file: String?) {
        if (file != null) _speak.value = SpeakEvent(++clips, file)
    }

    private val characters = content.characters()
    val names: Map<String, String> = characters.associate { it.id to it.nameUz }
    val voices: Map<String, String> = characters.associate { it.id to it.voice }

    init {
        viewModelScope.launch {
            _rank.value = RankRules.rankFor(GameRules.level(game.observeTotalXp().first()).level)
            val s = withContext(Dispatchers.IO) { content.lesson(lessonId)?.story } ?: return@launch
            val book = bookOf(lessonId)
            val number = content.catalog().filter { it.book == book }.indexOfFirst { it.id == lessonId } + 1
            _meta.value = EpisodeMeta(number, s.titleUz, s.steps.size, s.gateUz, lessonId.hashCode())
            update(StoryPlayer(s))
        }
    }

    fun next() = _player.value?.let { update(it.next()) }

    fun choose(option: String) {
        val before = _player.value ?: return
        val after = before.choose(option)
        val solved = before.current as? ChooseReply
        if (solved != null && option == solved.answer) say(solved.audio)
        update(after, speakLine = solved == null)
    }

    private fun update(p: StoryPlayer, speakLine: Boolean = true) {
        _player.value = p
        val line = p.current as? StoryLine
        if (speakLine && line != null) say(line.audio)
        if (p.finished && _xp.value == null) {
            viewModelScope.launch { _xp.value = story.complete(lessonId, settings.settings.first().dailyGoalXp) }
        }
    }
}

/** Who is on stage: the speaker of the latest line or reply (a quiz keeps the previous speaker). */
private fun speakerOf(shown: List<StoryStep>): String? = shown.asReversed().firstNotNullOfOrNull {
    when (it) {
        is StoryLine -> it.speaker
        is ChooseReply -> it.speaker
        is StoryQuiz -> null
    }
}

@Composable
fun EpisodeScreen(vm: EpisodeViewModel, onClose: () -> Unit) {
    val player by vm.player.collectAsStateWithLifecycle()
    val meta by vm.meta.collectAsStateWithLifecycle()
    val rank by vm.rank.collectAsStateWithLifecycle()
    val xp by vm.xp.collectAsStateWithLifecycle()
    val speak by vm.speak.collectAsStateWithLifecycle()
    val audio = LocalAudioPlayer.current
    LaunchedEffect(speak?.id) { speak?.let { if (vm.playOnce.shouldPlay(it.id)) audio?.play(it.file) } }
    val t = LocalGameTokens.current
    GameBackground {
        val p = player ?: run {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@GameBackground
        }
        val list = rememberLazyListState()
        // The learner plays Aziz: his label shows the player's own name when one is set.
        val playerName = LocalPlayerName.current
        val names = remember(vm.names, playerName) { if (playerName.isBlank()) vm.names else vm.names + ("aziz" to playerName) }
        LaunchedEffect(p.shown.size, p.finished) { list.animateScrollToItem(maxOf(0, p.shown.size - 1)) }
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.story_close), tint = t.text)
                }
                val total = meta?.total
                if (total != null) FloorBar(p.shown.size, total, Modifier.weight(1f)) else GlowBar(p.progress, Modifier.weight(1f))
                meta?.let { Text("${p.shown.size}/${it.total}", color = t.muted, fontFamily = t.ui, fontSize = 12.sp) }
            }
            meta?.let {
                Text(
                    stringResource(R.string.episode_chip, it.number, it.title),
                    color = t.text,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .background(t.panel, RoundedCornerShape(t.panelCorner))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            val speaker = speakerOf(p.shown)
            val gate = meta?.gate
            Box(Modifier.fillMaxWidth().height(if (gate != null) GATE_STAGE else 180.dp)) {
                if (gate != null) GateScene(meta?.seed ?: 0, Modifier.matchParentSize())
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (gate != null) GateTitle(gate)
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                        when {
                            speaker == null -> Unit
                            speaker == "aziz" -> Avatar(rank, HeroGender.BOY, Modifier.size(150.dp, 180.dp))
                            else -> SpeakerArt(vm.voices[speaker].orEmpty(), Modifier.size(150.dp, 180.dp))
                        }
                    }
                }
            }
            LazyColumn(Modifier.weight(1f), state = list, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(p.shown) { i, step ->
                    val solved = i < p.shown.size - 1 || p.finished
                    StepPanel(step, solved, names)
                }
            }
            when (val step = p.current) {
                is StoryLine -> GameButton(stringResource(R.string.story_next), onClick = vm::next)
                is ChooseReply -> Choices(personalize(step.promptUz, playerName), step.options, step.answer, p.eliminated, p.lastWrongWhy?.let { personalize(it, playerName) }, vm::choose)
                is StoryQuiz -> Choices(personalize(step.promptUz, playerName), step.options, step.answer, p.eliminated, p.lastWrongWhy?.let { personalize(it, playerName) }, vm::choose)
                null -> Finish(xp, onClose)
            }
        }
    }
}

@Composable
private fun StepPanel(step: StoryStep, solved: Boolean, names: Map<String, String>) {
    val t = LocalGameTokens.current
    when (step) {
        is StoryLine -> Line(names[step.speaker] ?: step.speaker, step.ko, step.uz, step.audio, mine = step.speaker == "aziz")
        is ChooseReply -> if (solved) Line(names[step.speaker] ?: step.speaker, step.answer, step.uz, step.audio, mine = true)
        is StoryQuiz -> if (solved) Text("✓ ${step.promptUz} — ${step.answer}", color = t.muted, fontSize = 13.sp)
    }
}

/** One spoken line; tapping it shows the Uzbek translation. */
@Composable
private fun Line(name: String, ko: String, uz: String, audio: String?, mine: Boolean) {
    val t = LocalGameTokens.current
    var showUz by remember { mutableStateOf(false) }
    GamePanel(null, Modifier.clickable { showUz = !showUz }, borderColor = if (mine) t.accent else t.accent2) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                color = if (mine) t.accent else t.accent2,
                fontFamily = t.ui,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            AudioButton(audio)
        }
        Text(ko, color = t.text, fontSize = 18.sp)
        if (showUz) Text(uz, color = t.muted, fontSize = 13.sp)
    }
}

/** Lettered answer tiles; a wrong pick turns red and stays out of play. Each pick plays its hit sound. */
@Composable
private fun Choices(
    prompt: String,
    options: List<String>,
    answer: String,
    eliminated: Set<String>,
    why: String?,
    onChoose: (String) -> Unit,
) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    HuntPanel(title = stringResource(R.string.episode_choice), scan = true) {
        Text(prompt, color = t.text, fontWeight = FontWeight.SemiBold)
        options.forEachIndexed { i, option ->
            val out = option in eliminated
            OptionTile(
                text = option,
                onClick = {
                    feedback.play(if (option == answer) Sfx.CORRECT else Sfx.WRONG)
                    onChoose(option)
                },
                state = if (out) OptionState.WRONG else OptionState.IDLE,
                letter = 'A' + i,
                enabled = !out,
            )
        }
        why?.let { Text("❌ $it", color = t.danger) }
    }
}

/** The episode's reward: rays behind the gold XP, the CLEAR sound once, and a gold close button. */
@Composable
private fun Finish(xp: Int?, onClose: () -> Unit) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    var cheered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(xp) {
        if (!cheered && xp != null) {
            cheered = true
            feedback.play(if (xp > 0) Sfx.CLEAR else Sfx.XP)
        }
    }
    HuntPanel(accent = Gold) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.story_finish_title),
                color = t.text,
                fontFamily = t.ui,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
            Box(Modifier.size(150.dp, 90.dp), contentAlignment = Alignment.Center) {
                GateRays(Gold.copy(alpha = 0.3f), Modifier.size(150.dp))
                when {
                    xp == null -> CircularProgressIndicator()
                    xp > 0 -> Text(stringResource(R.string.story_xp, xp), color = Gold, fontFamily = t.ui, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    else -> Text(stringResource(R.string.story_replayed), color = t.muted)
                }
            }
        }
        HuntButton(
            stringResource(R.string.story_close),
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            style = HuntStyle.GOLD,
            sfx = Sfx.XP,
        )
    }
}

/** Height of the gate stage: the place name, the speaker and the steps of the gate under them. */
private val GATE_STAGE = 290.dp
