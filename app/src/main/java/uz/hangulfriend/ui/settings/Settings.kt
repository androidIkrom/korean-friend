package uz.hangulfriend.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.hangulfriend.R
import uz.hangulfriend.content.CatalogEntry
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.data.Settings
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.study.OnboardingService
import uz.hangulfriend.ui.onboarding.LessonPicker

class SettingsViewModel(
    content: ContentRepository,
    private val settingsRepo: SettingsRepository,
    private val onboarding: OnboardingService,
) : ViewModel() {
    val catalog: List<CatalogEntry> = content.catalog()

    val settings: StateFlow<Settings> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun setCurrentLesson(id: String) {
        viewModelScope.launch { onboarding.complete(id) }
    }

    fun setDailyNewLimit(n: Int) {
        viewModelScope.launch { settingsRepo.setDailyNewLimit(n) }
    }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var limit by remember(settings.dailyNewLimit) { mutableFloatStateOf(settings.dailyNewLimit.toFloat()) }
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.settings_daily_new, limit.toInt()), style = MaterialTheme.typography.titleMedium)
        Slider(
            value = limit,
            onValueChange = { limit = it },
            onValueChangeFinished = { vm.setDailyNewLimit(limit.toInt()) },
            valueRange = 5f..50f,
            steps = 8,
        )
        Text(stringResource(R.string.settings_current_lesson), style = MaterialTheme.typography.titleMedium)
        LessonPicker(vm.catalog, settings.currentLessonId, vm::setCurrentLesson, Modifier.weight(1f))
    }
}
