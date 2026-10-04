package uz.hangulfriend.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import uz.hangulfriend.R
import uz.hangulfriend.content.CatalogEntry
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.content.isHangulLesson
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.StoryRepository
import uz.hangulfriend.story.EpisodeState
import uz.hangulfriend.story.StoryRules
import uz.hangulfriend.ui.kit.LocalGameFeedback
import uz.hangulfriend.ui.kit.PanelTitle
import uz.hangulfriend.ui.kit.ScreenHeader
import uz.hangulfriend.ui.kit.Sfx
import uz.hangulfriend.ui.kit.pulseRing
import uz.hangulfriend.ui.kit.shape
import uz.hangulfriend.ui.theme.LocalGameTokens
import uz.hangulfriend.ui.LocalDockInset

data class EpisodeRow(val entry: CatalogEntry, val storyTitle: String?, val state: EpisodeState)

class StoryListViewModel(content: ContentRepository, progress: ProgressRepository, story: StoryRepository) : ViewModel() {
    /** Story titles by lesson id; parsing every lesson is asset IO, so it runs off the main thread once. */
    private val titles = flow {
        emit(content.catalog().associate { e -> e.id to content.lesson(e.id)?.story?.titleUz })
    }.flowOn(Dispatchers.IO)

    val rows: StateFlow<List<EpisodeRow>> = combine(titles, progress.observeAll(), story.observeDone()) { t, p, done ->
        // The Hangul lessons teach letters and have no episodes.
        content.catalog().filterNot { isHangulLesson(it.id) }.map { e ->
            EpisodeRow(e, t[e.id], StoryRules.state(p[e.id]?.status, hasStory = t[e.id] != null, done = e.id in done))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun StoryListScreen(vm: StoryListViewModel, onOpen: (String) -> Unit) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    Column(Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState()).padding(bottom = LocalDockInset.current), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScreenHeader(stringResource(R.string.stories_title))
        val books = rows.map { it.entry.book }.distinct()
        rows.groupBy { it.entry.book }.forEach { (book, bookRows) ->
            if (books.size > 1) {
                Text(
                    stringResource(R.string.book_label, book),
                    color = LocalGameTokens.current.accent,
                    fontFamily = LocalGameTokens.current.ui,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            bookRows.groupBy { it.entry.unit }.forEach { (unit, episodes) ->
                Box(Modifier.padding(top = 6.dp)) { PanelTitle(stringResource(R.string.story_unit, unit)) }
                episodes.forEach { row -> EpisodeCard(row, onOpen) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** One episode: a NEW one glows and pulses, a DONE one shows a check, the rest are faded. */
@Composable
private fun EpisodeCard(row: EpisodeRow, onOpen: (String) -> Unit) {
    val t = LocalGameTokens.current
    val feedback = LocalGameFeedback.current
    val open = row.state == EpisodeState.NEW || row.state == EpisodeState.DONE
    val isNew = row.state == EpisodeState.NEW
    val color = when (row.state) {
        EpisodeState.NEW -> t.accent
        EpisodeState.DONE -> t.accent2
        else -> t.muted
    }
    val shape = t.shape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .alpha(if (open) 1f else 0.5f)
            .then(if (isNew) Modifier.pulseRing(t.accent, shape) else Modifier)
            .clip(shape)
            .background(t.panel, shape)
            .border(1.dp, color.copy(alpha = if (open) 0.9f else 0.4f), shape)
            .clickable(enabled = open, role = Role.Button) {
                feedback.play(Sfx.OPEN)
                onOpen(row.entry.id)
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            when (row.state) {
                EpisodeState.LOCKED -> Icons.Filled.Lock
                EpisodeState.COMING_SOON -> Icons.Filled.HourglassEmpty
                EpisodeState.NEW -> Icons.AutoMirrored.Filled.MenuBook
                EpisodeState.DONE -> Icons.Filled.CheckCircle
            },
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                "${row.entry.unit}-${row.entry.lesson} · ${row.storyTitle ?: row.entry.titleUz}",
                color = t.text,
                fontFamily = t.ui,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Text(
                stringResource(
                    when (row.state) {
                        EpisodeState.LOCKED -> R.string.story_locked
                        EpisodeState.COMING_SOON -> R.string.story_soon
                        EpisodeState.NEW -> R.string.story_new
                        EpisodeState.DONE -> R.string.story_done
                    },
                ),
                color = if (isNew) t.accent else t.muted,
                fontSize = 12.sp,
            )
        }
    }
}
