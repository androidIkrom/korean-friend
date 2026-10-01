package uz.hangulfriend.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uz.hangulfriend.R
import uz.hangulfriend.content.CatalogEntry
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.data.LessonProgressEntity
import uz.hangulfriend.data.LessonStatus
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.ui.statusLabel

data class LessonRow(val entry: CatalogEntry, val available: Boolean, val status: LessonStatus, val percent: Int)

/** Lesson stages are 0..4, each worth 20%; a completed lesson is 100%. */
fun buildRows(
    catalog: List<CatalogEntry>,
    available: Set<String>,
    progress: Map<String, LessonProgressEntity>,
): List<LessonRow> = catalog.map { entry ->
    val p = progress[entry.id]
    val status = p?.status ?: LessonStatus.NOT_STARTED
    val percent = when (status) {
        LessonStatus.COMPLETED, LessonStatus.VERIFIED -> 100
        else -> (p?.stage ?: 0) * 20
    }
    LessonRow(entry, entry.id in available, status, percent)
}

class BookMapViewModel(content: ContentRepository, progress: ProgressRepository) : ViewModel() {
    private val catalog = content.catalog()
    private val available = catalog.map { it.id }.filter(content::isAvailable).toSet()

    val rows: StateFlow<List<LessonRow>> = progress.observeAll()
        .map { buildRows(catalog, available, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildRows(catalog, available, emptyMap()))
}

@Composable
fun BookMapScreen(
    vm: BookMapViewModel,
    onOpenLesson: (String) -> Unit,
    onQuickCheck: (String) -> Unit,
    onBoss: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.groupBy { it.entry.unit }.forEach { (unit, unitRows) ->
            item(key = "unit$unit") {
                Row(Modifier.padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.unit_title, unit, unitRows.first().entry.topicUz),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    // A boss needs both lessons of the unit to have content.
                    if (unitRows.size == 2 && unitRows.all { it.available }) {
                        TextButton(onClick = { onBoss(unit) }) { Text(stringResource(R.string.map_boss)) }
                    }
                }
            }
            items(unitRows, key = { it.entry.id }) { row -> LessonRowCard(row, onOpenLesson, onQuickCheck) }
        }
    }
}

@Composable
private fun LessonRowCard(row: LessonRow, onOpenLesson: (String) -> Unit, onQuickCheck: (String) -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .alpha(if (row.available) 1f else 0.5f)
            .clickable(enabled = row.available) { onOpenLesson(row.entry.id) },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(row.entry.titleKo, style = MaterialTheme.typography.titleMedium)
            Text(row.entry.titleUz, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(progress = { row.percent / 100f }, modifier = Modifier.weight(1f))
                Text(
                    if (row.available) statusLabel(row.status) else stringResource(R.string.coming_soon),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (row.available && row.status == LessonStatus.PASSED) {
                TextButton(onClick = { onQuickCheck(row.entry.id) }) { Text(stringResource(R.string.map_quick_check)) }
            }
        }
    }
}
