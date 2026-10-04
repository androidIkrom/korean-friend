package uz.hangulfriend.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import uz.hangulfriend.data.HeroGender
import uz.hangulfriend.data.LessonStatus
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.data.StoryRepository
import uz.hangulfriend.data.StudyRepository
import uz.hangulfriend.share.ProgressStats
import uz.hangulfriend.study.GameRules
import uz.hangulfriend.study.Rank
import uz.hangulfriend.study.RankRules
import uz.hangulfriend.ui.avatar.titleRes
import uz.hangulfriend.ui.kit.GlowBar
import uz.hangulfriend.ui.kit.HudChip
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntPanel
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.LevelBadge
import uz.hangulfriend.ui.kit.RailButton
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.kit.pulseRing
import uz.hangulfriend.ui.kit.shape
import uz.hangulfriend.ui.theme.BadgeState
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.theme.GameBackground
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.theme.RankBadge
import uz.hangulfriend.ui.LocalDockInset

data class HomeStats(
    val level: GameRules.LevelInfo = GameRules.level(0),
    val todayXp: Int = 0,
    val goal: Int = GameRules.DEFAULT_GOAL,
    val streak: Int = 0,
    val rank: Rank = Rank.E,
    val hero: HeroGender = HeroGender.BOY,
    val learnedWords: Int = 0,
    val completedLessons: Int = 0,
    val storiesDone: Int = 0,
    val achievements: Int = 0,
    /** Next stage of the current lesson; null without a current lesson or when it is finished. */
    val stageName: Int? = null,
)

class HomeViewModel(
    content: ContentRepository,
    private val study: StudyRepository,
    private val settings: SettingsRepository,
    private val game: GameRepository,
    private val progress: ProgressRepository,
    private val story: StoryRepository,
    private val snapshot: ProgressStats,
    private val clock: Clock,
) : ViewModel() {
    private val catalog = content.catalog()

    private val _dueCount = MutableStateFlow(0)
    val dueCount: StateFlow<Int> = _dueCount

    private val _stats = MutableStateFlow(HomeStats())
    val stats: StateFlow<HomeStats> = _stats

    private val _rankUp = MutableStateFlow<RankUp?>(null)
    val rankUp: StateFlow<RankUp?> = _rankUp

    val currentLesson: StateFlow<CatalogEntry?> = settings.settings
        .map { s -> catalog.find { it.id == s.currentLessonId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Due-ness and "today" change with time, not only with the database, so the screen refreshes on every resume. */
    fun refresh() {
        viewModelScope.launch {
            val s = settings.settings.first()
            _dueCount.value = study.dueQueue(s.dailyNewLimit).size
            val level = GameRules.level(game.observeTotalXp().first())
            val rank = RankRules.rankFor(level.level)
            val snap = snapshot.snapshot()
            val current = s.currentLessonId?.let { progress.observeAll().first()[it] }
            _stats.value = HomeStats(
                level = level,
                todayXp = game.todayXp(),
                goal = s.dailyGoalXp,
                streak = GameRules.streak(game.dailyXp(), LocalDate.now(clock), s.dailyGoalXp),
                rank = rank,
                hero = s.hero,
                learnedWords = snap.learnedWords,
                completedLessons = snap.completedLessons,
                storiesDone = story.observeDone().first().size,
                achievements = game.observeAchievements().first().size,
                stageName = s.currentLessonId?.let { stageNameFor(current?.status ?: LessonStatus.NOT_STARTED, current?.stage ?: 0) },
            )
            val lastSeen = settings.lastSeenRank.first()
            if (lastSeen == null) {
                settings.setLastSeenRank(rank)
            } else {
                _rankUp.value = RankRules.rankUpToShow(rank, lastSeen)?.let { RankUp(lastSeen, it, level.level, s.hero) }
            }
        }
    }

    fun acceptRankUp() {
        val shown = _rankUp.value ?: return
        _rankUp.value = null
        viewModelScope.launch { settings.setLastSeenRank(shown.to) }
    }
}

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onStartReview: () -> Unit,
    onContinueLesson: (String) -> Unit,
    onOpenMap: () -> Unit,
    onGames: () -> Unit,
    onMistakes: () -> Unit,
    onAchievements: () -> Unit,
    onVocab: () -> Unit,
    modifier: Modifier = Modifier,
    shareRail: @Composable () -> Unit = {},
) {
    val due by vm.dueCount.collectAsStateWithLifecycle()
    val current by vm.currentLesson.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val rankUp by vm.rankUp.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    val t = LocalGameTokens.current
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var questY by remember { mutableIntStateOf(0) }
    GameBackground(modifier) {
        Column(
            Modifier.verticalScroll(scroll).padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp + LocalDockInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Hud(stats, due, onStartReview, onAchievements)
            Box(Modifier.fillMaxWidth().height(350.dp)) {
                HomeScene(stats.rank, stats.hero, Modifier.fillMaxWidth().height(318.dp).align(Alignment.TopCenter))
                Column(Modifier.align(Alignment.TopStart).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RailButton(
                        Icons.Outlined.Flag, stringResource(R.string.rail_quest),
                        onClick = { scope.launch { scroll.animateScrollTo(questY) } },
                        dot = stats.todayXp < stats.goal,
                    )
                    RailButton(Icons.Outlined.Translate, stringResource(R.string.rail_words), onVocab)
                    RailButton(Icons.Outlined.ReportProblem, stringResource(R.string.rail_errors), onMistakes)
                }
                Column(Modifier.align(Alignment.TopEnd).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RailButton(Icons.Outlined.SportsEsports, stringResource(R.string.rail_arena), onGames, accent = t.accent2)
                    RailButton(Icons.Outlined.EmojiEvents, stringResource(R.string.rail_badges), onAchievements, accent = t.accent2)
                    shareRail()
                }
                NamePlate(stats.rank, Modifier.align(Alignment.BottomCenter))
            }
            StatsStrip(stats)
            Box(Modifier.onGloballyPositioned { questY = it.positionInParent().y.toInt() }) {
                QuestPanel(questLines(stats.todayXp, stats.goal, due, stats.stageName))
            }
            val lesson = current
            if (lesson != null) {
                GateButton(
                    label = stringResource(R.string.lobby_gate_label, lesson.unit, lesson.lesson, lesson.titleUz),
                    onClick = { onContinueLesson(lesson.id) },
                )
            } else {
                HuntButton(stringResource(R.string.home_open_map).uppercase(), onOpenMap, Modifier.fillMaxWidth(), sfx = Sfx.OPEN)
            }
            if (due > 0) {
                HuntButton(
                    stringResource(R.string.home_action_review, due).uppercase(),
                    onStartReview,
                    Modifier.fillMaxWidth(),
                    style = HuntStyle.SECONDARY,
                    icon = Icons.Outlined.Replay,
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
    rankUp?.let { RankUpDialog(it, vm::acceptRankUp) }
}

/** Level, rank line and XP bar, then the streak / due / badges chips. */
@Composable
private fun Hud(stats: HomeStats, due: Int, onReview: () -> Unit, onBadges: () -> Unit) {
    val t = LocalGameTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LevelBadge(stats.level.level)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        stringResource(R.string.lobby_rank_line, stats.rank.name, stringResource(stats.rank.titleRes())).uppercase(),
                        color = t.text,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.home_level_progress, stats.level.xpIntoLevel, stats.level.xpForNext),
                        color = t.muted,
                        fontSize = 11.sp,
                    )
                }
                GlowBar(stats.level.xpIntoLevel.toFloat() / stats.level.xpForNext.coerceAtLeast(1))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudChip(Icons.Filled.LocalFireDepartment, pluralStringResource(R.plurals.home_streak_days, stats.streak, stats.streak), Color(0xFFFFAA50))
            HudChip(Icons.Outlined.Style, stringResource(R.string.hud_due, due), t.accent2, onClick = onReview.takeIf { due > 0 })
            HudChip(Icons.Outlined.EmojiEvents, stats.achievements.toString(), Color(0xFFFFD66B), onClick = onBadges)
        }
    }
}

@Composable
private fun NamePlate(rank: Rank, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RankBadge(rank.name, BadgeState.ACTIVE, size = 22.dp)
        Text(
            stringResource(rank.titleRes()).uppercase(),
            color = t.text,
            fontFamily = t.display,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = 4.sp,
        )
    }
}

@Composable
private fun StatsStrip(stats: HomeStats) {
    val t = LocalGameTokens.current
    HuntPanel(padding = 12.dp) {
        Row(Modifier.fillMaxWidth()) {
            listOf(
                R.string.home_stat_words to stats.learnedWords,
                R.string.home_stat_lessons to stats.completedLessons,
                R.string.home_stat_stories to stats.storiesDone,
                R.string.home_stat_achievements to stats.achievements,
            ).forEach { (label, value) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value.toString(), color = t.text, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(stringResource(label).uppercase(), color = t.muted, fontSize = 9.sp, letterSpacing = 1.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun QuestPanel(lines: List<QuestLine>) {
    val t = LocalGameTokens.current
    HuntPanel(title = stringResource(R.string.home_quest), scan = true) {
        lines.forEach { q ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (q.complete) "◆" else "◇", color = if (q.complete) CorrectGreen else t.accent, fontSize = 14.sp)
                    val label = q.detail?.let { stringResource(q.label, stringResource(it)) } ?: stringResource(q.label)
                    Text(label, color = if (q.complete) t.muted else t.text, modifier = Modifier.weight(1f))
                    Text(
                        "${q.done}/${q.target}",
                        color = if (q.complete) CorrectGreen else t.accent,
                        fontFamily = t.display,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (q.target > 1) {
                    GlowBar(q.done.toFloat() / q.target, height = 4, color = if (q.complete) CorrectGreen else t.accent)
                }
            }
        }
        Text(stringResource(R.string.home_quest_footer), color = t.muted, fontSize = 12.sp)
    }
}

/** The big call to action: which gate is next, and a pulse that says "go". */
@Composable
private fun GateButton(label: String, onClick: () -> Unit) {
    val t = LocalGameTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label.uppercase(), color = t.accent, fontFamily = t.display, fontSize = 11.sp, letterSpacing = 2.sp, maxLines = 1)
        HuntButton(
            stringResource(R.string.map_enter_gate),
            onClick,
            Modifier.fillMaxWidth().pulseRing(t.accent, t.shape(12.dp)),
            icon = Icons.Filled.Bolt,
            sfx = Sfx.OPEN,
            minHeight = 60.dp,
            fontSize = 18,
        )
    }
}
