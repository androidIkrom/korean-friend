package uz.hangulfriend.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uz.hangulfriend.R
import uz.hangulfriend.content.CatalogEntry
import uz.hangulfriend.content.ContentRepository
import uz.hangulfriend.data.HeroGender
import uz.hangulfriend.ui.settings.HeroRow
import uz.hangulfriend.ui.settings.LanguageRow
import uz.hangulfriend.study.OnboardingService

class OnboardingViewModel(content: ContentRepository, private val onboarding: OnboardingService) : ViewModel() {
    val catalog: List<CatalogEntry> = content.catalog()

    private val _selected = MutableStateFlow<String?>(null)
    val selected: StateFlow<String?> = _selected

    private val _hero = MutableStateFlow(HeroGender.BOY)
    val hero: StateFlow<HeroGender> = _hero

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    fun pickHero(hero: HeroGender) {
        _hero.value = hero
    }

    fun select(id: String) {
        _selected.value = id
    }

    fun complete(onDone: () -> Unit) {
        val id = _selected.value ?: return
        _busy.value = true
        viewModelScope.launch {
            onboarding.complete(id, _hero.value)
            _busy.value = false
            onDone()
        }
    }
}

/** Lists the book's lessons grouped by unit and lets the learner pick one. Also used by settings. */
@Composable
fun LessonPicker(
    catalog: List<CatalogEntry>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier) { lessonPickerItems(catalog, selected, onSelect) }
}

/** The picker's rows, for a screen that already scrolls in its own LazyColumn. */
fun LazyListScope.lessonPickerItems(catalog: List<CatalogEntry>, selected: String?, onSelect: (String) -> Unit) {
    catalog.groupBy { it.unit }.forEach { (unit, entries) ->
        item(key = "u$unit") {
            Text(
                stringResource(R.string.unit_title, unit, entries.first().topicUz),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        items(entries, key = { it.id }) { e ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(e.id) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = e.id == selected, onClick = { onSelect(e.id) })
                Column {
                    Text(e.titleKo, style = MaterialTheme.typography.bodyLarge)
                    Text(e.titleUz, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(vm: OnboardingViewModel, onDone: () -> Unit) {
    val selected by vm.selected.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val hero by vm.hero.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.onboarding_welcome), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onboarding_intro))
        LanguageRow()
        HeroRow(hero, vm::pickHero)
        Text(stringResource(R.string.onboarding_question), style = MaterialTheme.typography.titleMedium)
        LessonPicker(vm.catalog, selected, vm::select, Modifier.weight(1f))
        Button(
            onClick = { vm.complete(onDone) },
            enabled = selected != null && !busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.onboarding_continue)) }
    }
}
