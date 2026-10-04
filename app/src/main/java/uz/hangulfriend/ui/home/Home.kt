package uz.hangulfriend.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import uz.hangulfriend.ui.kit.LocalHazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.LocalDate
import kotlin.math.sin
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
import uz.hangulfriend.ui.LocalDockInset
import uz.hangulfriend.ui.avatar.Avatar
import uz.hangulfriend.ui.avatar.titleRes
import uz.hangulfriend.ui.kit.GlowBar
import uz.hangulfriend.ui.kit.Gold
import uz.hangulfriend.ui.kit.HudChip
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntStyle
import uz.hangulfriend.ui.kit.LocalGameFeedback
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.kit.frameClock
import uz.hangulfriend.ui.kit.glass
import uz.hangulfriend.ui.kit.springPress
import uz.hangulfriend.ui.theme.CorrectGreen
import uz.hangulfriend.ui.theme.GameBackground
import uz.hangulfriend.ui.theme.LocalGameTokens

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
    GameBackground(modifier) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp + LocalDockInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TopPills(stats, due, onStartReview, onAchievements, shareRail)
            // The sheet overlaps the hero's feet, so the art never ends in a hard line and the glass blurs it.
            Box {
                HeroStage(stats)
                Sheet(
                    stats, due, current, onStartReview, onContinueLesson, onOpenMap, onGames, onMistakes, onAchievements, onVocab,
                    Modifier.padding(top = HERO_HEIGHT - SHEET_OVERLAP),
                )
            }
        }
    }
    rankUp?.let { RankUpDialog(it, vm::acceptRankUp) }
}

/** Streak, due cards and badges as glass pills, with the share button at the end. */
@Composable
private fun TopPills(stats: HomeStats, due: Int, onReview: () -> Unit, onBadges: () -> Unit, share: @Composable () -> Unit) {
    val t = LocalGameTokens.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        HudChip(Icons.Filled.LocalFireDepartment, pluralStringResource(R.plurals.home_streak_days, stats.streak, stats.streak), Color(0xFFFFAA50))
        HudChip(Icons.Outlined.Style, stringResource(R.string.hud_due, due), t.accent2, onClick = onReview.takeIf { due > 0 })
        HudChip(Icons.Outlined.EmojiEvents, stats.achievements.toString(), Gold, onClick = onBadges)
        share()
    }
}

/** The hero, large and floating over a soft aura, with the rank letter beside it. */
@Composable
private fun HeroStage(stats: HomeStats) {
    val t = LocalGameTokens.current
    val clock = frameClock()
    val haze = LocalHazeState.current
    Box(Modifier.fillMaxWidth().height(HERO_HEIGHT)) {
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(x = 34.dp)
                .size(330.dp)
                .drawBehind {
                    drawCircle(Brush.radialGradient(listOf(t.accent.copy(alpha = 0.32f), Color.Transparent)), radius = size.minDimension / 2)
                },
        )
        Avatar(
            stats.rank,
            stats.hero,
            Modifier
                .align(Alignment.BottomEnd)
                .size(290.dp, 350.dp)
                .then(if (haze != null) Modifier.hazeSource(haze, zIndex = 1f) else Modifier)
                .graphicsLayer {
                    translationY = sin(clock.floatValue * 1.05f) * 6.dp.toPx()
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    // Soft edges: the art fades out on the left and at the bottom instead of being cut.
                    drawRect(Brush.horizontalGradient(0f to Color.Transparent, 0.18f to Color.Black), blendMode = BlendMode.DstIn)
                    drawRect(Brush.verticalGradient(0.8f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
                },
            animated = true,
        )
        Column(Modifier.align(Alignment.CenterStart).padding(start = 4.dp, bottom = 40.dp)) {
            Text(
                stringResource(R.string.home_rank_label),
                color = t.muted,
                fontFamily = t.ui,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 1.3.sp,
            )
            Text(
                stats.rank.name,
                style = TextStyle(
                    fontFamily = t.numbers,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 76.sp,
                    lineHeight = 76.sp,
                    brush = Brush.verticalGradient(listOf(Color.White, t.accent)),
                    shadow = Shadow(Color.Black.copy(alpha = 0.45f), blurRadius = 18f),
                ),
            )
            Text(
                stringResource(R.string.home_rank_caption, stringResource(stats.rank.titleRes()), stats.level.level),
                color = t.text,
                fontFamily = t.ui,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
    }
}

/** The glass sheet under the hero: XP, the daily quest, shortcuts, the next gate and the totals. */
@Composable
private fun Sheet(
    stats: HomeStats,
    due: Int,
    current: CatalogEntry?,
    onReview: () -> Unit,
    onContinueLesson: (String) -> Unit,
    onOpenMap: () -> Unit,
    onGames: () -> Unit,
    onMistakes: () -> Unit,
    onAchievements: () -> Unit,
    onVocab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LocalGameTokens.current
    Column(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(28.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        XpTrack(stats)
        QuestCard(questLines(stats.todayXp, stats.goal, due, stats.stageName))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Shortcut(Icons.Outlined.Translate, stringResource(R.string.rail_words), onVocab)
            Shortcut(Icons.Outlined.ReportProblem, stringResource(R.string.rail_errors), onMistakes)
            Shortcut(Icons.Outlined.SportsEsports, stringResource(R.string.rail_arena), onGames)
            Shortcut(Icons.Outlined.EmojiEvents, stringResource(R.string.rail_badges), onAchievements)
        }
        val lesson = current
        if (lesson != null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.home_gate_caption, lesson.unit, lesson.lesson, lesson.titleUz),
                    color = t.muted,
                    fontFamily = t.ui,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                HuntButton(stringResource(R.string.home_enter_gate), { onContinueLesson(lesson.id) }, Modifier.fillMaxWidth(), icon = Icons.Filled.Bolt, sfx = Sfx.OPEN, minHeight = 56.dp, fontSize = 16)
            }
        } else {
            HuntButton(stringResource(R.string.home_open_map), onOpenMap, Modifier.fillMaxWidth(), sfx = Sfx.OPEN, minHeight = 56.dp, fontSize = 16)
        }
        if (due > 0) {
            HuntButton(stringResource(R.string.home_action_review, due), onReview, Modifier.fillMaxWidth(), style = HuntStyle.SECONDARY, icon = Icons.Outlined.Replay)
        }
        Totals(stats)
    }
}

@Composable
private fun XpTrack(stats: HomeStats) {
    val t = LocalGameTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlowBar(stats.level.xpIntoLevel.toFloat() / stats.level.xpForNext.coerceAtLeast(1), height = 6)
        Row {
            Text(
                stringResource(R.string.home_level_progress, stats.level.xpIntoLevel, stats.level.xpForNext),
                color = t.muted,
                fontFamily = t.ui,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            val next = RankRules.nextRank(stats.level.level)
            Text(
                if (next == null) stringResource(R.string.home_top_rank) else pluralStringResource(R.plurals.home_next_rank, next.second, next.first.name, next.second),
                color = t.muted,
                fontFamily = t.ui,
                fontSize = 12.sp,
            )
        }
    }
}

/** The daily quest: a ring of finished lines, then each line with its count. */
@Composable
private fun QuestCard(lines: List<QuestLine>) {
    val t = LocalGameTokens.current
    val finished = lines.count { it.complete }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuestRing(finished, lines.size)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_quest), color = t.text, fontFamily = t.ui, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(stringResource(R.string.home_quest_footer), color = t.muted, fontFamily = t.ui, fontSize = 11.sp)
            }
        }
        lines.forEach { q ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .size(18.dp)
                        .background(if (q.complete) CorrectGreen else Color.White.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (q.complete) Icon(Icons.Filled.Check, contentDescription = null, tint = t.background, modifier = Modifier.size(12.dp))
                }
                val label = q.detail?.let { stringResource(q.label, stringResource(it)) } ?: stringResource(q.label)
                Text(label, color = if (q.complete) t.muted else t.text, fontFamily = t.ui, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${q.done}/${q.target}", color = if (q.complete) CorrectGreen else t.accent, fontFamily = t.numbers, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun QuestRing(done: Int, total: Int) {
    val t = LocalGameTokens.current
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 4.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(Color.White.copy(alpha = 0.12f), 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            val sweep = if (total == 0) 0f else 360f * done / total
            drawArc(Brush.sweepGradient(listOf(t.accent2, t.accent, t.accent2)), -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("$done/$total", color = t.text, fontFamily = t.numbers, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

/** A shortcut tile in the sheet: icon over a short label. */
@Composable
private fun RowScope.Shortcut(icon: ImageVector, label: String, onClick: () -> Unit) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .weight(1f)
            .springPress(interaction)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button) {
                feedback.play(Sfx.TAP)
                onClick()
            }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = t.text, modifier = Modifier.size(20.dp))
        Text(label, color = t.muted, fontFamily = t.ui, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Learned words, finished lessons, stories and badges in one quiet row. */
@Composable
private fun Totals(stats: HomeStats) {
    val t = LocalGameTokens.current
    Row(Modifier.fillMaxWidth()) {
        listOf(
            R.string.home_stat_words to stats.learnedWords,
            R.string.home_stat_lessons to stats.completedLessons,
            R.string.home_stat_stories to stats.storiesDone,
            R.string.home_stat_achievements to stats.achievements,
        ).forEach { (label, value) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value.toString(), color = t.text, fontFamily = t.numbers, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text(stringResource(label), color = t.muted, fontFamily = t.ui, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private val HERO_HEIGHT = 400.dp

/** How far the glass sheet reaches up over the hero. */
private val SHEET_OVERLAP = 56.dp
