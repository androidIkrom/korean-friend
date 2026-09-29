package uz.hangulfriend.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
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
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.data.StudyRepository

class HomeViewModel(
    content: ContentRepository,
    private val study: StudyRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val catalog = content.catalog()

    private val _dueCount = MutableStateFlow(0)
    val dueCount: StateFlow<Int> = _dueCount

    val currentLesson: StateFlow<CatalogEntry?> = settings.settings
        .map { s -> catalog.find { it.id == s.currentLessonId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Due-ness changes with time, not only with the database, so the screen refreshes on every resume. */
    fun refresh() {
        viewModelScope.launch {
            _dueCount.value = study.dueQueue(settings.settings.first().dailyNewLimit).size
        }
    }
}

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onStartReview: () -> Unit,
    onContinueLesson: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val due by vm.dueCount.collectAsStateWithLifecycle()
    val current by vm.currentLesson.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.home_review_count, due), style = MaterialTheme.typography.titleLarge)
                if (due == 0) Text(stringResource(R.string.home_review_done))
                Button(onClick = onStartReview, enabled = due > 0, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_review_start))
                }
            }
        }
        current?.let { lesson ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.home_continue), style = MaterialTheme.typography.labelLarge)
                    Text(lesson.titleKo, style = MaterialTheme.typography.titleLarge)
                    Text(lesson.titleUz)
                    FilledTonalButton(onClick = { onContinueLesson(lesson.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_continue))
                    }
                }
            }
        }
    }
}
