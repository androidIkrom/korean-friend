package uz.hangulfriend.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import uz.hangulfriend.ui.theme.BadgeState
import uz.hangulfriend.ui.theme.GameBackground
import uz.hangulfriend.ui.theme.GameButton
import uz.hangulfriend.ui.theme.GamePanel
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.theme.ProgressBar
import uz.hangulfriend.ui.theme.RankBadge

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
    modifier: Modifier = Modifier,
    headerAction: @Composable () -> Unit = {},
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
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TopRow(stats, headerAction)
            HomeScene(stats.rank, stats.hero, Modifier.fillMaxWidth().height(300.dp))
            NamePlate(stats.rank)
            StatusPanel(stats)
            QuestPanel(questLines(stats.todayXp, stats.goal, due, stats.stageName))
            val lesson = current
            if (lesson != null) {
                GameButton(
                    stringResource(R.string.home_continue_lesson, lesson.unit, lesson.lesson).uppercase(),
                    onClick = { onContinueLesson(lesson.id) },
                )
            } else {
                GameButton(stringResource(R.string.home_open_map).uppercase(), onClick = onOpenMap)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SideAction(Icons.Filled.Replay, stringResource(R.string.home_action_review, due), due > 0, onStartReview, Modifier.weight(1f))
                SideAction(Icons.Filled.SportsEsports, stringResource(R.string.home_action_games), true, onGames, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SideAction(Icons.Filled.ErrorOutline, stringResource(R.string.home_action_mistakes), true, onMistakes, Modifier.weight(1f))
                SideAction(
                    Icons.Filled.EmojiEvents, stringResource(R.string.home_action_achievements), true, onAchievements, Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(4.dp))
        }
    }
    rankUp?.let { RankUpDialog(it, vm::acceptRankUp) }
}

@Composable
private fun TopRow(stats: HomeStats, headerAction: @Composable () -> Unit) {
    val t = LocalGameTokens.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("LV.", color = t.muted, fontFamily = t.display, letterSpacing = 2.sp, fontSize = 14.sp)
        Text(
            stats.level.level.toString(),
            color = t.accent,
            fontFamily = t.display,
            fontWeight = FontWeight.Bold,
            fontSize = 44.sp,
            modifier = Modifier.padding(start = 6.dp).weight(1f),
        )
        Row(
            Modifier
                .border(1.dp, t.panelBorder, RoundedCornerShape(t.panelCorner))
                .background(t.panel, RoundedCornerShape(t.panelCorner))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFFB36B), modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.home_streak_days, stats.streak), color = t.text, fontWeight = FontWeight.SemiBold)
        }
        headerAction()
    }
}

@Composable
private fun NamePlate(rank: Rank) {
    val t = LocalGameTokens.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AZIZ", color = t.text, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 6.sp)
            RankBadge(rank.name, BadgeState.ACTIVE, size = 22.dp)
        }
        Text(stringResource(R.string.home_title, stringResource(rank.titleRes())), color = t.muted, fontSize = 13.sp)
    }
}

@Composable
private fun StatusPanel(stats: HomeStats) {
    val t = LocalGameTokens.current
    GamePanel(stringResource(R.string.home_status)) {
        Text(
            stringResource(R.string.home_level_progress, stats.level.xpIntoLevel, stats.level.xpForNext),
            color = t.muted,
            fontSize = 12.sp,
        )
        ProgressBar(stats.level.xpIntoLevel.toFloat() / stats.level.xpForNext.coerceAtLeast(1))
        Row(Modifier.fillMaxWidth()) {
            listOf(
                R.string.home_stat_words to stats.learnedWords,
                R.string.home_stat_lessons to stats.completedLessons,
                R.string.home_stat_stories to stats.storiesDone,
                R.string.home_stat_achievements to stats.achievements,
            ).forEach { (label, value) ->
                Column(Modifier.weight(1f)) {
                    Text(stringResource(label).uppercase(), color = t.muted, fontSize = 10.sp, letterSpacing = 1.sp, maxLines = 1)
                    Text(value.toString(), color = t.text, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            }
        }
    }
}

@Composable
private fun QuestPanel(lines: List<QuestLine>) {
    val t = LocalGameTokens.current
    GamePanel(stringResource(R.string.home_quest)) {
        lines.forEach { q ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val box = RoundedCornerShape(t.panelCorner / 3)
                Column(
                    Modifier
                        .size(18.dp)
                        .border(1.dp, t.accent, box)
                        .background(if (q.complete) t.accent.copy(alpha = 0.25f) else Color.Transparent, box),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (q.complete) Icon(Icons.Filled.Check, contentDescription = null, tint = t.accent, modifier = Modifier.size(14.dp))
                }
                val label = q.detail?.let { stringResource(q.label, stringResource(it)) } ?: stringResource(q.label)
                Text(label, color = if (q.complete) t.muted else t.text, modifier = Modifier.weight(1f))
                Text("[${q.done}/${q.target}]", color = t.accent, fontWeight = FontWeight.SemiBold)
            }
        }
        HorizontalDivider(color = t.panelBorder.copy(alpha = 0.4f))
        Text(stringResource(R.string.home_quest_footer), color = t.accent, fontSize = 12.sp)
    }
}

@Composable
private fun SideAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = LocalGameTokens.current
    val shape = RoundedCornerShape(t.panelCorner)
    Row(
        modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .background(t.panel, shape)
            .border(1.dp, t.panelBorder, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = t.accent, modifier = Modifier.size(20.dp))
        Text(label, color = t.text, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
