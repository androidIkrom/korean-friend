package uz.hangulfriend.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import uz.hangulfriend.R
import uz.hangulfriend.content.CatalogEntry
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.data.LessonProgressEntity
import uz.hangulfriend.data.LessonStatus
import uz.hangulfriend.data.ProgressRepository
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.ui.theme.GameBackground
import uz.hangulfriend.ui.theme.LocalGameTokens

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

class BookMapViewModel(content: ContentRepository, progress: ProgressRepository, settings: SettingsRepository) : ViewModel() {
    private val catalog = content.catalog()
    private val available = catalog.map { it.id }.filter(content::isAvailable).toSet()

    val units: StateFlow<List<UnitRow>> = combine(progress.observeAll(), settings.settings) { p, s ->
        buildUnits(buildRows(catalog, available, p), s.currentLessonId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildUnits(buildRows(catalog, available, emptyMap()), null))
}

/** Units as a gate tower (System, 9 on top) or a metro line (Neon, 1 on top); the active unit is expanded. */
@Composable
fun BookMapScreen(
    vm: BookMapViewModel,
    onOpenLesson: (String) -> Unit,
    onQuickCheck: (String) -> Unit,
    onBoss: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LocalGameTokens.current
    val units by vm.units.collectAsStateWithLifecycle()
    var opened by rememberSaveable { mutableStateOf(listOf<Int>()) }
    val actions = MapActions(onOpenLesson, onQuickCheck, onBoss) { unit ->
        opened = if (unit in opened) opened - unit else opened + unit
    }
    val tower = t.id == GameThemeId.SYSTEM
    val ordered = if (tower) units.reversed() else units
    val activeIndex = ordered.indexOfFirst { it.state == UnitState.ACTIVE }
    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex >= 0) {
        // Item 0 is the header, so this leaves one unit visible above the active one.
        if (activeIndex >= 0) listState.scrollToItem(activeIndex)
    }
    val cleared = units.count { it.state == UnitState.CLEARED }
    // The metro line is solid from the top down to the active station (or along cleared units when none is active).
    fun solidBelow(i: Int): Boolean = if (activeIndex >= 0) {
        i < activeIndex
    } else {
        ordered[i].state == UnitState.CLEARED && ordered.getOrNull(i + 1)?.state == UnitState.CLEARED
    }
    GameBackground(modifier) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(if (tower) 8.dp else 0.dp),
        ) {
            item(key = "header") {
                Column(Modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(if (tower) R.string.map_tower_title else R.string.map_metro_title),
                        color = t.text,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                    )
                    Text(stringResource(R.string.map_cleared_count, cleared, units.size), color = t.muted, fontSize = 13.sp)
                }
            }
            itemsIndexed(ordered, key = { _, u -> "unit${u.unit}" }) { i, u ->
                val expanded = u.state == UnitState.ACTIVE || u.unit in opened
                if (tower) {
                    GateItem(u, expanded, actions)
                } else {
                    StationItem(
                        u, expanded,
                        first = i == 0,
                        last = i == ordered.lastIndex,
                        passedTop = i > 0 && solidBelow(i - 1),
                        passedBottom = solidBelow(i),
                        actions = actions,
                    )
                }
            }
        }
    }
}
