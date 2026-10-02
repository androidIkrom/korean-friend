package uz.hangulfriend.ui.story

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.StoryRepository
import uz.hangulfriend.story.EpisodeState
import uz.hangulfriend.story.StoryRules
import uz.hangulfriend.ui.theme.GameCard
import uz.hangulfriend.ui.theme.LocalGameTokens

data class EpisodeRow(val entry: CatalogEntry, val storyTitle: String?, val state: EpisodeState)

class StoryListViewModel(content: ContentRepository, progress: ProgressRepository, story: StoryRepository) : ViewModel() {
    /** Story titles by lesson id; parsing every lesson is asset IO, so it runs off the main thread once. */
    private val titles = flow {
        emit(content.catalog().associate { e -> e.id to content.lesson(e.id)?.story?.titleUz })
    }.flowOn(Dispatchers.IO)

    val rows: StateFlow<List<EpisodeRow>> = combine(titles, progress.observeAll(), story.observeDone()) { t, p, done ->
        content.catalog().map { e ->
            EpisodeRow(e, t[e.id], StoryRules.state(p[e.id]?.status, hasStory = t[e.id] != null, done = e.id in done))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun StoryListScreen(vm: StoryListViewModel, onOpen: (String) -> Unit) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.stories_title), style = MaterialTheme.typography.headlineMedium)
        rows.groupBy { it.entry.unit }.forEach { (unit, episodes) ->
            Text(stringResource(R.string.story_unit, unit), style = MaterialTheme.typography.titleMedium)
            episodes.forEach { row -> EpisodeCard(row, onOpen) }
        }
    }
}

@Composable
private fun EpisodeCard(row: EpisodeRow, onOpen: (String) -> Unit) {
    val open = row.state == EpisodeState.NEW || row.state == EpisodeState.DONE
    GameCard(
        Modifier.fillMaxWidth().alpha(if (open) 1f else 0.5f).clickable(enabled = open) { onOpen(row.entry.id) },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val t = LocalGameTokens.current
            Icon(
                when (row.state) {
                    EpisodeState.LOCKED -> Icons.Filled.Lock
                    EpisodeState.COMING_SOON -> Icons.Filled.HourglassEmpty
                    EpisodeState.NEW -> Icons.AutoMirrored.Filled.MenuBook
                    EpisodeState.DONE -> Icons.Filled.CheckCircle
                },
                contentDescription = null,
                tint = when (row.state) {
                    EpisodeState.NEW -> t.accent
                    EpisodeState.DONE -> t.accent2
                    else -> t.muted
                },
                modifier = Modifier.size(28.dp),
            )
            Column(Modifier.weight(1f)) {
                Text("${row.entry.unit}-${row.entry.lesson}. ${row.storyTitle ?: row.entry.titleUz}", style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        when (row.state) {
                            EpisodeState.LOCKED -> R.string.story_locked
                            EpisodeState.COMING_SOON -> R.string.story_soon
                            EpisodeState.NEW -> R.string.story_new
                            EpisodeState.DONE -> R.string.story_done
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
