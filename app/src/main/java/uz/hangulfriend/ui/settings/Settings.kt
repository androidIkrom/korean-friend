package uz.hangulfriend.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import uz.hangulfriend.ui.kit.HuntButton
import uz.hangulfriend.ui.kit.HuntPanel
import uz.hangulfriend.ui.kit.HuntToggle
import uz.hangulfriend.ui.kit.SegmentSlider
import uz.hangulfriend.ui.onboarding.lessonPickerItems
import uz.hangulfriend.ui.theme.LocalGameTokens
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
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
import uz.hangulfriend.data.BackupService
import uz.hangulfriend.data.FlagRepository
import uz.hangulfriend.data.GameThemeId
import uz.hangulfriend.data.HeroGender
import uz.hangulfriend.data.Settings
import uz.hangulfriend.data.SettingsRepository
import uz.hangulfriend.study.OnboardingService
import uz.hangulfriend.ui.LocalDockInset

class SettingsViewModel(
    content: ContentRepository,
    private val settingsRepo: SettingsRepository,
    private val onboarding: OnboardingService,
    val backup: BackupService,
    val flags: FlagRepository,
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

    fun setHero(hero: HeroGender) {
        viewModelScope.launch { settingsRepo.setHero(hero) }
    }

    fun setTheme(id: GameThemeId) {
        viewModelScope.launch { settingsRepo.setTheme(id) }
    }

    fun setReminder(enabled: Boolean, minutes: Int) {
        viewModelScope.launch { settingsRepo.setReminder(enabled, minutes) }
    }

    fun setSound(on: Boolean) {
        viewModelScope.launch { settingsRepo.setSound(on) }
    }

    fun setHaptics(on: Boolean) {
        viewModelScope.launch { settingsRepo.setHaptics(on) }
    }

    fun setDailyNewLimit(n: Int) {
        viewModelScope.launch { settingsRepo.setDailyNewLimit(n) }
    }
}

@Composable
fun SettingsScreen(vm: SettingsViewModel, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val pending by vm.pending.collectAsStateWithLifecycle()
    val t = LocalGameTokens.current
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp + LocalDockInset.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "[ ${stringResource(R.string.dock_system).uppercase()} ]",
                color = t.text,
                fontFamily = t.display,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 4.sp,
            )
        }
        item {
            HuntPanel(title = stringResource(R.string.settings_section_hunter)) {
                LanguageRow()
                ThemeRow(settings.theme, vm::setTheme)
                HeroRow(settings.hero, vm::setHero)
            }
        }
        item {
            HuntPanel(title = stringResource(R.string.settings_section_targets)) {
                SegmentSlider(
                    title = { stringResource(R.string.settings_daily_new, it) },
                    value = settings.dailyNewLimit, min = 5, max = 50, step = 5, onChange = vm::setDailyNewLimit,
                )
                SegmentSlider(
                    title = { stringResource(R.string.settings_daily_goal, it) },
                    value = settings.dailyGoalXp, min = 20, max = 200, step = 10, onChange = vm::setDailyGoalXp,
                )
            }
        }
        item {
            HuntPanel(title = stringResource(R.string.settings_section_feedback)) {
                HuntToggle(settings.soundOn, vm::setSound, stringResource(R.string.settings_sound))
                HuntToggle(settings.hapticsOn, vm::setHaptics, stringResource(R.string.settings_haptics))
                ReminderRow(settings, vm::setReminder)
            }
        }
        item {
            HuntPanel(title = stringResource(R.string.settings_section_data)) {
                BackupRow(vm.backup)
                FlagsRow(vm.flags)
            }
        }
        item {
            HuntPanel(title = stringResource(R.string.settings_current_lesson)) {
                val picked = pending
                if (picked != null && picked != settings.currentLessonId) {
                    Text(stringResource(R.string.settings_apply_note), color = t.muted, fontSize = 13.sp)
                    HuntButton(stringResource(R.string.settings_apply).uppercase(), vm::applyPending, Modifier.fillMaxWidth())
                }
            }
        }
        lessonPickerItems(vm.catalog, pending ?: settings.currentLessonId, vm::pick)
    }
}
