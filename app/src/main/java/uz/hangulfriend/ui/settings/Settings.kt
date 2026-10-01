package uz.hangulfriend.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val _pending = MutableStateFlow<String?>(null)

    /** The lesson picked in the list but not yet applied; applying marks earlier lessons as passed. */
    val pending: StateFlow<String?> = _pending

    fun pick(id: String) {
        _pending.value = id
    }

    fun applyPending() {
        val id = _pending.value ?: return
        viewModelScope.launch {
            onboarding.complete(id)
            _pending.value = null
        }
    }

    fun setDailyGoalXp(n: Int) {
        viewModelScope.launch { settingsRepo.setDailyGoalXp(n) }
    }

    fun setReminder(enabled: Boolean, minutes: Int) {
        viewModelScope.launch { settingsRepo.setReminder(enabled, minutes) }
    }

    fun setDailyNewLimit(n: Int) {
        viewModelScope.launch { settingsRepo.setDailyNewLimit(n) }
    }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
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
        var goal by remember(settings.dailyGoalXp) { mutableFloatStateOf(settings.dailyGoalXp.toFloat()) }
        Text(stringResource(R.string.settings_daily_goal, goal.roundToInt()), style = MaterialTheme.typography.titleMedium)
        Slider(
            value = goal,
            onValueChange = { goal = it },
            onValueChangeFinished = { vm.setDailyGoalXp(goal.roundToInt()) },
            valueRange = 20f..200f,
            steps = 17,
        )
        ReminderRow(settings, vm::setReminder)
        Text(stringResource(R.string.settings_current_lesson), style = MaterialTheme.typography.titleMedium)
        val picked = pending
        if (picked != null && picked != settings.currentLessonId) {
            Text(stringResource(R.string.settings_apply_note), style = MaterialTheme.typography.bodySmall)
            Button(onClick = vm::applyPending, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_apply))
            }
        }
        LessonPicker(vm.catalog, pending ?: settings.currentLessonId, vm::pick, Modifier.weight(1f))
    }
}
